package com.bank.simulator.service;

import com.bank.simulator.dto.LoginRequest;
import com.bank.simulator.dto.LoginResponse;
import com.bank.simulator.entity.AuditAction;
import com.bank.simulator.entity.User;
import com.bank.simulator.entity.UserStatus;
import com.bank.simulator.exception.InvalidCredentialsException;
import com.bank.simulator.exception.UserStatusException;
import com.bank.simulator.repository.UserRepository;
import com.bank.simulator.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final AuditLogService auditLogService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            BruteForceProtectionService bruteForceProtectionService,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.bruteForceProtectionService = bruteForceProtectionService;
        this.auditLogService = auditLogService;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();

        // 1. Check Brute-Force lockout
        if (bruteForceProtectionService.isLocked(username)) {
            auditLogService.logEvent(username, AuditAction.LOGIN_FAILURE, "USER", username, "Login attempt rejected due to brute force lockout", "REJECTED");
            throw new InvalidCredentialsException("Account locked due to multiple failed login attempts. Please try again after 15 minutes.");
        }

        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            bruteForceProtectionService.registerFailedAttempt(username);
            auditLogService.logEvent(username, AuditAction.LOGIN_FAILURE, "USER", username, "Failed login: user not found", "FAILED");
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            auditLogService.logEvent(username, AuditAction.LOGIN_FAILURE, "USER", username, "Login attempt rejected: User is BLOCKED", "FAILED");
            throw new UserStatusException("User account is BLOCKED. Access denied.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            auditLogService.logEvent(username, AuditAction.LOGIN_FAILURE, "USER", username, "Login attempt rejected: User is INACTIVE", "FAILED");
            throw new UserStatusException("User account is INACTIVE. Access denied.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            auditLogService.logEvent(username, AuditAction.LOGIN_FAILURE, "USER", username, "Login attempt rejected: User is not active", "FAILED");
            throw new UserStatusException("User account is not active.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            bruteForceProtectionService.registerFailedAttempt(username);
            auditLogService.logEvent(username, AuditAction.LOGIN_FAILURE, "USER", username, "Failed login: invalid password", "FAILED");
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Login success -> reset brute-force attempt counter
        bruteForceProtectionService.resetFailedAttempts(username);

        String token = jwtService.generateToken(user);
        auditLogService.logEvent(username, AuditAction.LOGIN_SUCCESS, "USER", username, "Login successful", "SUCCESS");

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}
