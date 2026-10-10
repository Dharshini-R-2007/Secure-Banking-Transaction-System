package com.bank.simulator.dto;

import com.bank.simulator.entity.LedgerEntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LedgerEntryResponse {

    private Long id;
    private String transactionReference;
    private String accountNumber;
    private LedgerEntryType entryType;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public LedgerEntryResponse() {
    }

    public LedgerEntryResponse(Long id, String transactionReference, String accountNumber, LedgerEntryType entryType, BigDecimal amount, LocalDateTime createdAt) {
        this.id = id;
        this.transactionReference = transactionReference;
        this.accountNumber = accountNumber;
        this.entryType = entryType;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(LedgerEntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static LedgerEntryResponseBuilder builder() {
        return new LedgerEntryResponseBuilder();
    }

    public static class LedgerEntryResponseBuilder {
        private Long id;
        private String transactionReference;
        private String accountNumber;
        private LedgerEntryType entryType;
        private BigDecimal amount;
        private LocalDateTime createdAt;

        public LedgerEntryResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LedgerEntryResponseBuilder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public LedgerEntryResponseBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public LedgerEntryResponseBuilder entryType(LedgerEntryType entryType) {
            this.entryType = entryType;
            return this;
        }

        public LedgerEntryResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public LedgerEntryResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public LedgerEntryResponse build() {
            return new LedgerEntryResponse(id, transactionReference, accountNumber, entryType, amount, createdAt);
        }
    }
}
