package com.bank.simulator.service;

import com.bank.simulator.entity.AuditAction;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BruteForceProtectionService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCK_TIME_DURATION_MINUTES = 15;

    private static class FailedAttemptInfo {
        int attempts;
        LocalDateTime lockTime;

        FailedAttemptInfo(int attempts) {
            this.attempts = attempts;
            this.lockTime = null;
        }
    }

    private final Map<String, FailedAttemptInfo> attemptsMap = new ConcurrentHashMap<>();
    private final AuditLogService auditLogService;

    public BruteForceProtectionService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public boolean isLocked(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        FailedAttemptInfo info = attemptsMap.get(username.toLowerCase());
        if (info == null) {
            return false;
        }

        if (info.lockTime != null) {
            if (info.lockTime.plusMinutes(LOCK_TIME_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                return true;
            } else {
                // Lock expired
                attemptsMap.remove(username.toLowerCase());
                return false;
            }
        }

        return false;
    }

    public void registerFailedAttempt(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String key = username.toLowerCase();
        attemptsMap.compute(key, (k, info) -> {
            if (info == null) {
                info = new FailedAttemptInfo(1);
            } else {
                info.attempts++;
            }

            if (info.attempts >= MAX_FAILED_ATTEMPTS) {
                info.lockTime = LocalDateTime.now();
                auditLogService.logEvent(
                        username,
                        AuditAction.BRUTE_FORCE_LOCKED,
                        "USER",
                        username,
                        "User locked out due to " + info.attempts + " consecutive failed login attempts",
                        "BLOCKED"
                );
            }
            return info;
        });
    }

    public void resetFailedAttempts(String username) {
        if (username != null) {
            attemptsMap.remove(username.toLowerCase());
        }
    }

    public int getFailedAttempts(String username) {
        if (username == null) return 0;
        FailedAttemptInfo info = attemptsMap.get(username.toLowerCase());
        return info != null ? info.attempts : 0;
    }
}
