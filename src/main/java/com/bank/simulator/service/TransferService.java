package com.bank.simulator.service;

import com.bank.simulator.dto.TransactionResponse;
import com.bank.simulator.dto.TransferRequest;
import com.bank.simulator.entity.AccountStatus;
import com.bank.simulator.entity.AuditAction;
import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.IdempotencyKey;
import com.bank.simulator.entity.Role;
import com.bank.simulator.entity.Transaction;
import com.bank.simulator.entity.TransactionStatus;
import com.bank.simulator.entity.TransactionType;
import com.bank.simulator.entity.User;
import com.bank.simulator.entity.UserStatus;
import com.bank.simulator.exception.AccountNotFoundException;
import com.bank.simulator.exception.ClientNotFoundException;
import com.bank.simulator.exception.DuplicateIdempotencyKeyException;
import com.bank.simulator.exception.InsufficientBalanceException;
import com.bank.simulator.exception.InvalidAccountStatusException;
import com.bank.simulator.exception.SameAccountTransferException;
import com.bank.simulator.exception.TransientTransactionException;
import com.bank.simulator.exception.UnauthorizedOperationException;
import com.bank.simulator.repository.BankAccountRepository;
import com.bank.simulator.repository.TransactionRepository;
import com.bank.simulator.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final int MAX_RETRIES = 3;

    private final UserRepository userRepository;
    private final IdempotencyService idempotencyService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final TransferTransactionExecutor transferTransactionExecutor;

    public TransferService(
            UserRepository userRepository,
            IdempotencyService idempotencyService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper,
            TransferTransactionExecutor transferTransactionExecutor
    ) {
        this.userRepository = userRepository;
        this.idempotencyService = idempotencyService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.transferTransactionExecutor = transferTransactionExecutor;
    }

    public TransactionResponse processTransfer(String senderUsername, TransferRequest request, String idempotencyKeyHeader) {
        // 1. Check Idempotency Key
        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            Optional<IdempotencyKey> existing = idempotencyService.findByKey(idempotencyKeyHeader);
            if (existing.isPresent()) {
                IdempotencyKey ik = existing.get();
                if (ik.getResponseBody() != null) {
                    try {
                        return objectMapper.readValue(ik.getResponseBody(), TransactionResponse.class);
                    } catch (Exception ex) {
                        log.error("Failed to deserialize cached idempotency response", ex);
                    }
                } else {
                    throw new DuplicateIdempotencyKeyException("Transaction with Idempotency-Key '" + idempotencyKeyHeader + "' is currently being processed");
                }
            }
        }

        User senderUser = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new ClientNotFoundException("Sender client profile not found for username: " + senderUsername));

        // Reserve idempotency key if present
        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            idempotencyService.reserveKey(idempotencyKeyHeader, senderUser.getId());
        }

        // 2. Controlled Retry Loop for Transient Lock/DB Failures
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                TransactionResponse response = transferTransactionExecutor.executeTransfer(senderUser, request);

                // Save cached response in idempotency record
                if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
                    try {
                        String jsonResp = objectMapper.writeValueAsString(response);
                        idempotencyService.recordResponse(idempotencyKeyHeader, response.getTransactionReference(), jsonResp, 200);
                    } catch (Exception ex) {
                        log.error("Failed to serialize idempotency response", ex);
                    }
                }

                return response;

            } catch (TransientDataAccessException ex) {
                log.warn("Transient database/lock exception on transfer attempt {}/{}", attempt, MAX_RETRIES, ex);
                auditLogService.logEvent(
                        senderUsername,
                        AuditAction.TRANSFER_RETRY,
                        "TRANSFER",
                        request.getReceiverAccountNumber(),
                        "Transient failure on attempt " + attempt + ". Retrying transfer...",
                        "RETRY"
                );

                if (attempt >= MAX_RETRIES) {
                    if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
                        idempotencyService.removeKeyOnFailure(idempotencyKeyHeader);
                    }
                    throw new TransientTransactionException("Transfer failed after " + MAX_RETRIES + " transient retry attempts", ex);
                }

                try {
                    Thread.sleep(50L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (RuntimeException ex) {
                // Permanent business failure -> clean up key and throw immediately
                if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
                    idempotencyService.removeKeyOnFailure(idempotencyKeyHeader);
                }
                throw ex;
            }
        }

        throw new TransientTransactionException("Unexpected execution exit in transfer service", null);
    }

    @Transactional
    public TransactionResponse executeTransferInTransaction(User senderUser, TransferRequest request) {
        return transferTransactionExecutor.executeTransfer(senderUser, request);
    }
}
