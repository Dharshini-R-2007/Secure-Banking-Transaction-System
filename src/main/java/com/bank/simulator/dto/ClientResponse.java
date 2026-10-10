package com.bank.simulator.dto;

import com.bank.simulator.entity.AccountStatus;
import com.bank.simulator.entity.Role;
import com.bank.simulator.entity.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClientResponse {

    private Long id;
    private String username;
    private Role role;
    private UserStatus status;
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus accountStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public ClientResponse() {
    }

    public ClientResponse(Long id, String username, Role role, UserStatus status, String accountNumber, BigDecimal balance, AccountStatus accountStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = status;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
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

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static ClientResponseBuilder builder() {
        return new ClientResponseBuilder();
    }

    public static class ClientResponseBuilder {
        private Long id;
        private String username;
        private Role role;
        private UserStatus status;
        private String accountNumber;
        private BigDecimal balance;
        private AccountStatus accountStatus;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ClientResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ClientResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public ClientResponseBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public ClientResponseBuilder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public ClientResponseBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public ClientResponseBuilder balance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        public ClientResponseBuilder accountStatus(AccountStatus accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public ClientResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ClientResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ClientResponse build() {
            return new ClientResponse(id, username, role, status, accountNumber, balance, accountStatus, createdAt, updatedAt);
        }
    }
}
