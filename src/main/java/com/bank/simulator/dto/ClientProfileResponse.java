package com.bank.simulator.dto;

import com.bank.simulator.entity.Role;
import com.bank.simulator.entity.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class ClientProfileResponse {

    private Long id;
    private String username;
    private Role role;
    private UserStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private AccountResponse account;

    public ClientProfileResponse() {
    }

    public ClientProfileResponse(Long id, String username, Role role, UserStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, AccountResponse account) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.account = account;
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

    public AccountResponse getAccount() {
        return account;
    }

    public void setAccount(AccountResponse account) {
        this.account = account;
    }

    public static ClientProfileResponseBuilder builder() {
        return new ClientProfileResponseBuilder();
    }

    public static class ClientProfileResponseBuilder {
        private Long id;
        private String username;
        private Role role;
        private UserStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private AccountResponse account;

        public ClientProfileResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ClientProfileResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public ClientProfileResponseBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public ClientProfileResponseBuilder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public ClientProfileResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ClientProfileResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ClientProfileResponseBuilder account(AccountResponse account) {
            this.account = account;
            return this;
        }

        public ClientProfileResponse build() {
            return new ClientProfileResponse(id, username, role, status, createdAt, updatedAt, account);
        }
    }
}
