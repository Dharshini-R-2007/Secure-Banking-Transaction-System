package com.bank.simulator.dto;

import com.bank.simulator.entity.AccountStatus;

import java.math.BigDecimal;

public class AccountBalanceResponse {

    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;

    public AccountBalanceResponse() {
    }

    public AccountBalanceResponse(String accountNumber, BigDecimal balance, AccountStatus status) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.status = status;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public static AccountBalanceResponseBuilder builder() {
        return new AccountBalanceResponseBuilder();
    }

    public static class AccountBalanceResponseBuilder {
        private String accountNumber;
        private BigDecimal balance;
        private AccountStatus status;

        public AccountBalanceResponseBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public AccountBalanceResponseBuilder balance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        public AccountBalanceResponseBuilder status(AccountStatus status) {
            this.status = status;
            return this;
        }

        public AccountBalanceResponse build() {
            return new AccountBalanceResponse(accountNumber, balance, status);
        }
    }
}
