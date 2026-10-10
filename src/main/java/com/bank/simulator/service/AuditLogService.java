package com.bank.simulator.service;

import com.bank.simulator.dto.AuditLogResponse;
import com.bank.simulator.entity.AuditAction;
import com.bank.simulator.entity.AuditLog;
import com.bank.simulator.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logEvent(String username, AuditAction action, String entityType, String entityId, String description, String result) {
        AuditLog log = AuditLog.builder()
                .username(username)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .result(result)
                .build();
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllAuditLogs(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.findAll(pageable).map(this::mapToAuditLogResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByUsername(String username, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.findByUsername(username, pageable).map(this::mapToAuditLogResponse);
    }

    private AuditLogResponse mapToAuditLogResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .username(log.getUsername())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .result(log.getResult())
                .timestamp(log.getTimestamp())
                .build();
    }
}
