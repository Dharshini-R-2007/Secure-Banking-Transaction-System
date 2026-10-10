package com.bank.simulator.service;

import com.bank.simulator.dto.PageResponse;
import com.bank.simulator.dto.TransactionResponse;
import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.Transaction;
import com.bank.simulator.entity.TransactionStatus;
import com.bank.simulator.entity.TransactionType;
import com.bank.simulator.entity.User;
import com.bank.simulator.exception.AccountNotFoundException;
import com.bank.simulator.exception.ClientNotFoundException;
import com.bank.simulator.exception.TransactionNotFoundException;
import com.bank.simulator.repository.BankAccountRepository;
import com.bank.simulator.repository.TransactionRepository;
import com.bank.simulator.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    public TransactionQueryService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            BankAccountRepository bankAccountRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getClientTransactions(
            String username,
            int page,
            int size,
            String typeStr,
            String statusStr
    ) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ClientNotFoundException("User profile not found for username: " + username));

        BankAccount account = bankAccountRepository.findByUser(user)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found for user: " + username));

        TransactionType type = parseTransactionType(typeStr);
        TransactionStatus status = parseTransactionStatus(statusStr);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<TransactionResponse> pageResult = transactionRepository
                .findByAccountFiltered(account, type, status, pageable)
                .map(this::mapToTransactionResponse);

        return PageResponse.from(pageResult);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAdminTransactions(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionResponse> pageResult = transactionRepository.findAll(pageable)
                .map(this::mapToTransactionResponse);
        return PageResponse.from(pageResult);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getAdminTransactionByReference(String reference) {
        Transaction txn = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with reference: " + reference));
        return mapToTransactionResponse(txn);
    }

    private TransactionType parseTransactionType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) return null;
        try {
            return TransactionType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private TransactionStatus parseTransactionStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return null;
        try {
            return TransactionStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
