package com.bank.simulator.service;

import com.bank.simulator.dto.TransactionResponse;
import com.bank.simulator.dto.TransferRequest;
import com.bank.simulator.entity.AccountStatus;
import com.bank.simulator.entity.AuditAction;
import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.Role;
import com.bank.simulator.entity.Transaction;
import com.bank.simulator.entity.TransactionStatus;
import com.bank.simulator.entity.TransactionType;
import com.bank.simulator.entity.User;
import com.bank.simulator.entity.UserStatus;
import com.bank.simulator.exception.AccountNotFoundException;
import com.bank.simulator.exception.InsufficientBalanceException;
import com.bank.simulator.exception.InvalidAccountStatusException;
import com.bank.simulator.exception.SameAccountTransferException;
import com.bank.simulator.exception.UnauthorizedOperationException;
import com.bank.simulator.repository.BankAccountRepository;
import com.bank.simulator.repository.TransactionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Component
public class TransferTransactionExecutor {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final AuditLogService auditLogService;
    private final jakarta.persistence.EntityManager entityManager;

    public TransferTransactionExecutor(
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            LedgerService ledgerService,
            AuditLogService auditLogService,
            jakarta.persistence.EntityManager entityManager
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
        this.auditLogService = auditLogService;
        this.entityManager = entityManager;
    }

    @Transactional
    public TransactionResponse executeTransfer(User senderUser, TransferRequest request) {
        // Validate sender user status
        if (senderUser.getStatus() != UserStatus.ACTIVE) {
            auditLogService.logEvent(senderUser.getUsername(), AuditAction.TRANSFER_FAILED, "TRANSFER", request.getReceiverAccountNumber(), "Sender user account is not ACTIVE", "FAILED");
            throw new UnauthorizedOperationException("Sender user account is " + senderUser.getStatus() + ". Operation denied.");
        }

        // Find initial sender account
        BankAccount initialSenderAcc = bankAccountRepository.findByUser(senderUser)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found for sender: " + senderUser.getUsername()));

        // Find initial receiver account
        BankAccount initialReceiverAcc = bankAccountRepository.findByAccountNumber(request.getReceiverAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Receiver bank account not found with account number: " + request.getReceiverAccountNumber()));

        // Validation: Cannot transfer to self
        if (Objects.equals(initialSenderAcc.getId(), initialReceiverAcc.getId())) {
            auditLogService.logEvent(senderUser.getUsername(), AuditAction.TRANSFER_FAILED, "TRANSFER", request.getReceiverAccountNumber(), "Sender and receiver accounts are identical", "FAILED");
            throw new SameAccountTransferException("Sender and receiver bank accounts cannot be the same account");
        }

        // Validation: Receiver must belong to a CLIENT
        if (initialReceiverAcc.getUser() == null || initialReceiverAcc.getUser().getRole() != Role.CLIENT) {
            auditLogService.logEvent(senderUser.getUsername(), AuditAction.TRANSFER_FAILED, "TRANSFER", request.getReceiverAccountNumber(), "Receiver must be a valid CLIENT account", "FAILED");
            throw new UnauthorizedOperationException("Transfers are allowed only to CLIENT bank accounts");
        }

        // Deterministic Locking Order by Account Primary Key (ID)
        Long firstLockId = Math.min(initialSenderAcc.getId(), initialReceiverAcc.getId());
        Long secondLockId = Math.max(initialSenderAcc.getId(), initialReceiverAcc.getId());

        BankAccount firstLocked = bankAccountRepository.findByIdWithLock(firstLockId)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found during lock acquisition: " + firstLockId));
        BankAccount secondLocked = bankAccountRepository.findByIdWithLock(secondLockId)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found during lock acquisition: " + secondLockId));

        // Refresh entities from DB to guarantee latest committed balance under pessimistic lock
        entityManager.refresh(firstLocked);
        entityManager.refresh(secondLocked);

        BankAccount senderAcc = (firstLocked.getId().equals(initialSenderAcc.getId())) ? firstLocked : secondLocked;
        BankAccount receiverAcc = (firstLocked.getId().equals(initialReceiverAcc.getId())) ? firstLocked : secondLocked;

        // Validation: Account status checks
        if (senderAcc.getStatus() != AccountStatus.ACTIVE) {
            auditLogService.logEvent(senderUser.getUsername(), AuditAction.TRANSFER_FAILED, "TRANSFER", senderAcc.getAccountNumber(), "Sender account is " + senderAcc.getStatus(), "FAILED");
            throw new InvalidAccountStatusException("Sender bank account is " + senderAcc.getStatus() + ". Transfer rejected.");
        }

        if (receiverAcc.getStatus() != AccountStatus.ACTIVE) {
            auditLogService.logEvent(senderUser.getUsername(), AuditAction.TRANSFER_FAILED, "TRANSFER", receiverAcc.getAccountNumber(), "Receiver account is " + receiverAcc.getStatus(), "FAILED");
            throw new InvalidAccountStatusException("Receiver bank account is " + receiverAcc.getStatus() + ". Transfer rejected.");
        }

        // Validation: Amount check
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            auditLogService.logEvent(senderUser.getUsername(), AuditAction.TRANSFER_FAILED, "TRANSFER", receiverAcc.getAccountNumber(), "Invalid amount: " + amount, "FAILED");
            throw new IllegalArgumentException("Transfer amount must be positive and greater than 0");
        }

        // Validation: Sufficient balance check
        if (senderAcc.getBalance().compareTo(amount) < 0) {
            auditLogService.logEvent(senderUser.getUsername(), AuditAction.TRANSFER_FAILED, "TRANSFER", senderAcc.getAccountNumber(), "Insufficient balance: available ₹" + senderAcc.getBalance() + ", requested ₹" + amount, "FAILED");
            throw new InsufficientBalanceException("Insufficient account balance. Available: " + senderAcc.getBalance() + ", Requested: " + amount);
        }

        // Execute Debit & Credit
        senderAcc.setBalance(senderAcc.getBalance().subtract(amount));
        receiverAcc.setBalance(receiverAcc.getBalance().add(amount));

        bankAccountRepository.save(senderAcc);
        bankAccountRepository.save(receiverAcc);

        // Record Transaction
        String txnRef = "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction transaction = Transaction.builder()
                .transactionReference(txnRef)
                .senderAccount(senderAcc)
                .receiverAccount(receiverAcc)
                .amount(amount)
                .transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription())
                .build();

        Transaction savedTxn = transactionRepository.save(transaction);

        // Record Ledger Entries
        ledgerService.recordTransferLedger(savedTxn, senderAcc, receiverAcc, amount);

        // Audit Log
        auditLogService.logEvent(
                senderUser.getUsername(),
                AuditAction.TRANSFER_SUCCESS,
                "TRANSACTION",
                txnRef,
                "Transferred ₹" + amount + " from account " + senderAcc.getAccountNumber() + " to account " + receiverAcc.getAccountNumber(),
                "SUCCESS"
        );

        return mapToTransactionResponse(savedTxn);
    }

    private TransactionResponse mapToTransactionResponse(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .transactionReference(txn.getTransactionReference())
                .senderAccountNumber(txn.getSenderAccount() != null ? txn.getSenderAccount().getAccountNumber() : null)
                .receiverAccountNumber(txn.getReceiverAccount() != null ? txn.getReceiverAccount().getAccountNumber() : null)
                .amount(txn.getAmount())
                .transactionType(txn.getTransactionType())
                .status(txn.getStatus())
                .description(txn.getDescription())
                .failureReason(txn.getFailureReason())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
