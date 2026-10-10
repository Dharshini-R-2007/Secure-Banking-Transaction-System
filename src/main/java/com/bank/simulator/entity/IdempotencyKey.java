package com.bank.simulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String key;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "transaction_reference", length = 64)
    private String transactionReference;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IdempotencyKey() {
    }

    public IdempotencyKey(Long id, String key, User user, String transactionReference, String responseBody, Integer statusCode, LocalDateTime createdAt) {
        this.id = id;
        this.key = key;
        this.user = user;
        this.transactionReference = transactionReference;
        this.responseBody = responseBody;
        this.statusCode = statusCode;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static IdempotencyKeyBuilder builder() {
        return new IdempotencyKeyBuilder();
    }

    public static class IdempotencyKeyBuilder {
        private Long id;
        private String key;
        private User user;
        private String transactionReference;
        private String responseBody;
        private Integer statusCode;
        private LocalDateTime createdAt;

        public IdempotencyKeyBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public IdempotencyKeyBuilder key(String key) {
            this.key = key;
            return this;
        }

        public IdempotencyKeyBuilder user(User user) {
            this.user = user;
            return this;
        }

        public IdempotencyKeyBuilder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public IdempotencyKeyBuilder responseBody(String responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        public IdempotencyKeyBuilder statusCode(Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public IdempotencyKeyBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public IdempotencyKey build() {
            return new IdempotencyKey(id, key, user, transactionReference, responseBody, statusCode, createdAt);
        }
    }
}
