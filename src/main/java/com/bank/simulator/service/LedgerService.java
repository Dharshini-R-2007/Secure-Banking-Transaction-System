package com.bank.simulator.service;

import com.bank.simulator.dto.LedgerEntryResponse;
import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.LedgerEntry;
import com.bank.simulator.entity.LedgerEntryType;
import com.bank.simulator.entity.Transaction;
import com.bank.simulator.repository.LedgerEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public void recordTransferLedger(Transaction transaction, BankAccount sender, BankAccount receiver, BigDecimal amount) {
        LedgerEntry debitEntry = LedgerEntry.builder()
                .transaction(transaction)
                .account(sender)
                .entryType(LedgerEntryType.DEBIT)
                .amount(amount)
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transaction(transaction)
                .account(receiver)
                .entryType(LedgerEntryType.CREDIT)
                .amount(amount)
                .build();

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);
    }

    @Transactional
    public void recordDepositLedger(Transaction transaction, BankAccount account, BigDecimal amount) {
        LedgerEntry creditEntry = LedgerEntry.builder()
                .transaction(transaction)
                .account(account)
                .entryType(LedgerEntryType.CREDIT)
                .amount(amount)
                .build();

        ledgerEntryRepository.save(creditEntry);
    }

    @Transactional
    public void recordWithdrawalLedger(Transaction transaction, BankAccount account, BigDecimal amount) {
        LedgerEntry debitEntry = LedgerEntry.builder()
                .transaction(transaction)
                .account(account)
                .entryType(LedgerEntryType.DEBIT)
                .amount(amount)
                .build();

        ledgerEntryRepository.save(debitEntry);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getLedgerEntriesByTransaction(Transaction transaction) {
        return ledgerEntryRepository.findByTransaction(transaction).stream()
                .map(this::mapToLedgerEntryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getLedgerEntriesByAccount(BankAccount account, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ledgerEntryRepository.findByAccount(account, pageable).map(this::mapToLedgerEntryResponse);
    }

    private LedgerEntryResponse mapToLedgerEntryResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .transactionReference(entry.getTransaction() != null ? entry.getTransaction().getTransactionReference() : null)
                .accountNumber(entry.getAccount() != null ? entry.getAccount().getAccountNumber() : null)
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
