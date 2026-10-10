package com.bank.simulator.service;

import com.bank.simulator.dto.AccountResponse;
import com.bank.simulator.entity.AccountStatus;
import com.bank.simulator.entity.AuditAction;
import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.exception.AccountNotFoundException;
import com.bank.simulator.exception.InvalidAccountStatusException;
import com.bank.simulator.repository.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final AuditLogService auditLogService;

    public AdminAccountService(BankAccountRepository bankAccountRepository, AuditLogService auditLogService) {
        this.bankAccountRepository = bankAccountRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return bankAccountRepository.findAll().stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found with id: " + id));
        return mapToAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found with account number: " + accountNumber));
        return mapToAccountResponse(account);
    }

    @Transactional
    public AccountResponse blockAccount(Long id) {
        BankAccount account = getAccountOrThrow(id);
        account.setStatus(AccountStatus.BLOCKED);
        BankAccount saved = bankAccountRepository.save(account);

        auditLogService.logEvent("ADMIN", AuditAction.ACCOUNT_BLOCKED, "BANK_ACCOUNT", account.getAccountNumber(), "Account status changed to BLOCKED", "SUCCESS");

        return mapToAccountResponse(saved);
    }

    @Transactional
    public AccountResponse activateAccount(Long id) {
        BankAccount account = getAccountOrThrow(id);
        if (account.getStatus() == AccountStatus.CLOSED) {
            auditLogService.logEvent("ADMIN", AuditAction.ACCOUNT_ACTIVATED, "BANK_ACCOUNT", account.getAccountNumber(), "Activation rejected: CLOSED account cannot be activated", "FAILED");
            throw new InvalidAccountStatusException("CLOSED account cannot be activated");
        }
        account.setStatus(AccountStatus.ACTIVE);
        BankAccount saved = bankAccountRepository.save(account);

        auditLogService.logEvent("ADMIN", AuditAction.ACCOUNT_ACTIVATED, "BANK_ACCOUNT", account.getAccountNumber(), "Account status changed to ACTIVE", "SUCCESS");

        return mapToAccountResponse(saved);
    }

    @Transactional
    public AccountResponse closeAccount(Long id) {
        BankAccount account = getAccountOrThrow(id);
        account.setStatus(AccountStatus.CLOSED);
        BankAccount saved = bankAccountRepository.save(account);

        auditLogService.logEvent("ADMIN", AuditAction.ACCOUNT_CLOSED, "BANK_ACCOUNT", account.getAccountNumber(), "Account status changed to CLOSED", "SUCCESS");

        return mapToAccountResponse(saved);
    }

    private BankAccount getAccountOrThrow(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found with id: " + id));
    }

    private AccountResponse mapToAccountResponse(BankAccount account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUser() != null ? account.getUser().getId() : null)
                .username(account.getUser() != null ? account.getUser().getUsername() : null)
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
