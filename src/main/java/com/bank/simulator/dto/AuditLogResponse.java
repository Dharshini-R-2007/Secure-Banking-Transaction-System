package com.bank.simulator.dto;

import com.bank.simulator.entity.AuditAction;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private Long id;
    private String username;
    private AuditAction action;
    private String entityType;
    private String entityId;
    private String description;
    private String result;
    private LocalDateTime timestamp;

    public AuditLogResponse() {
    }

    public AuditLogResponse(Long id, String username, AuditAction action, String entityType, String entityId, String description, String result, LocalDateTime timestamp) {
        this.id = id;
        this.username = username;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.result = result;
        this.timestamp = timestamp;
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

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static AuditLogResponseBuilder builder() {
        return new AuditLogResponseBuilder();
    }

    public static class AuditLogResponseBuilder {
        private Long id;
        private String username;
        private AuditAction action;
        private String entityType;
        private String entityId;
        private String description;
        private String result;
        private LocalDateTime timestamp;

        public AuditLogResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuditLogResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public AuditLogResponseBuilder action(AuditAction action) {
            this.action = action;
            return this;
        }

        public AuditLogResponseBuilder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public AuditLogResponseBuilder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public AuditLogResponseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public AuditLogResponseBuilder result(String result) {
            this.result = result;
            return this;
        }

        public AuditLogResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AuditLogResponse build() {
            return new AuditLogResponse(id, username, action, entityType, entityId, description, result, timestamp);
        }
    }
}
