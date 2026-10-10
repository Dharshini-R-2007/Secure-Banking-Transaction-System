package com.bank.simulator.service;

import com.bank.simulator.dto.DepositRequest;
import com.bank.simulator.dto.TransactionResponse;
import com.bank.simulator.dto.WithdrawalRequest;
import com.bank.simulator.entity.AccountStatus;
import com.bank.simulator.entity.AuditAction;
import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.Transaction;
import com.bank.simulator.entity.TransactionStatus;
import com.bank.simulator.entity.TransactionType;
import com.bank.simulator.exception.AccountNotFoundException;
import com.bank.simulator.exception.InsufficientBalanceException;
import com.bank.simulator.exception.InvalidAccountStatusException;
import com.bank.simulator.repository.BankAccountRepository;
import com.bank.simulator.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AdminTransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final AuditLogService auditLogService;

    public AdminTransactionService(
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            LedgerService ledgerService,
            AuditLogService auditLogService
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TransactionResponse processDeposit(Long accountId, DepositRequest request, String adminUsername) {
        BankAccount account = bankAccountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found with id: " + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            auditLogService.logEvent(adminUsername, AuditAction.DEPOSIT_FAILED, "ACCOUNT", account.getAccountNumber(), "Deposit rejected: Account is " + account.getStatus(), "FAILED");
            throw new InvalidAccountStatusException("Cannot deposit to account in status " + account.getStatus());
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            auditLogService.logEvent(adminUsername, AuditAction.DEPOSIT_FAILED, "ACCOUNT", account.getAccountNumber(), "Deposit rejected: Amount must be positive", "FAILED");
            throw new IllegalArgumentException("Deposit amount must be positive and greater than 0");
        }

        account.setBalance(account.getBalance().add(amount));
        BankAccount savedAccount = bankAccountRepository.save(account);

        String txnRef = "DEP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction transaction = Transaction.builder()
                .transactionReference(txnRef)
                .receiverAccount(savedAccount)
                .amount(amount)
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null ? request.getDescription() : "Admin Deposit")
                .build();

        Transaction savedTxn = transactionRepository.save(transaction);
        ledgerService.recordDepositLedger(savedTxn, savedAccount, amount);

        auditLogService.logEvent(adminUsername, AuditAction.DEPOSIT_SUCCESS, "TRANSACTION", txnRef, "Deposited ₹" + amount + " into account " + account.getAccountNumber(), "SUCCESS");

        return mapToTransactionResponse(savedTxn);
    }

    @Transactional
    public TransactionResponse processWithdrawal(Long accountId, WithdrawalRequest request, String adminUsername) {
        BankAccount account = bankAccountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found with id: " + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            auditLogService.logEvent(adminUsername, AuditAction.WITHDRAWAL_FAILED, "ACCOUNT", account.getAccountNumber(), "Withdrawal rejected: Account is " + account.getStatus(), "FAILED");
            throw new InvalidAccountStatusException("Cannot withdraw from account in status " + account.getStatus());
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            auditLogService.logEvent(adminUsername, AuditAction.WITHDRAWAL_FAILED, "ACCOUNT", account.getAccountNumber(), "Withdrawal rejected: Amount must be positive", "FAILED");
            throw new IllegalArgumentException("Withdrawal amount must be positive and greater than 0");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            auditLogService.logEvent(adminUsername, AuditAction.WITHDRAWAL_FAILED, "ACCOUNT", account.getAccountNumber(), "Withdrawal rejected: Insufficient balance", "FAILED");
            throw new InsufficientBalanceException("Insufficient account balance. Available: " + account.getBalance() + ", Requested: " + amount);
        }

        account.setBalance(account.getBalance().subtract(amount));
        BankAccount savedAccount = bankAccountRepository.save(account);

        String txnRef = "WTH" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction transaction = Transaction.builder()
                .transactionReference(txnRef)
                .senderAccount(savedAccount)
                .amount(amount)
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null ? request.getDescription() : "Admin Withdrawal")
                .build();

        Transaction savedTxn = transactionRepository.save(transaction);
        ledgerService.recordWithdrawalLedger(savedTxn, savedAccount, amount);

        auditLogService.logEvent(adminUsername, AuditAction.WITHDRAWAL_SUCCESS, "TRANSACTION", txnRef, "Withdrew ₹" + amount + " from account " + account.getAccountNumber(), "SUCCESS");

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
