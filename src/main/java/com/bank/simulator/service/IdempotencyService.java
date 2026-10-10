package com.bank.simulator.service;

import com.bank.simulator.entity.IdempotencyKey;
import com.bank.simulator.entity.User;
import com.bank.simulator.exception.ClientNotFoundException;
import com.bank.simulator.exception.DuplicateIdempotencyKeyException;
import com.bank.simulator.repository.IdempotencyKeyRepository;
import com.bank.simulator.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserRepository userRepository;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository, UserRepository userRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyKey> findByKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return idempotencyKeyRepository.findByKey(key);
    }

    @Transactional
    public IdempotencyKey reserveKey(String key, Long userId) {
        if (key == null || key.isBlank()) {
            return null;
        }

        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByKey(key);
        if (existing.isPresent()) {
            throw new DuplicateIdempotencyKeyException("Duplicate Idempotency-Key: Request with key '" + key + "' has already been processed or is in progress");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ClientNotFoundException("User not found for idempotency reservation: " + userId));

        IdempotencyKey ik = IdempotencyKey.builder()
                .key(key)
                .user(user)
                .build();

        try {
            return idempotencyKeyRepository.saveAndFlush(ik);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateIdempotencyKeyException("Concurrent request with duplicate Idempotency-Key '" + key + "' detected");
        }
    }

    @Transactional
    public void recordResponse(String key, String transactionReference, String responseBody, int statusCode) {
        if (key == null || key.isBlank()) {
            return;
        }
        idempotencyKeyRepository.findByKey(key).ifPresent(ik -> {
            ik.setTransactionReference(transactionReference);
            ik.setResponseBody(responseBody);
            ik.setStatusCode(statusCode);
            idempotencyKeyRepository.save(ik);
        });
    }

    @Transactional
    public void removeKeyOnFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        idempotencyKeyRepository.findByKey(key).ifPresent(idempotencyKeyRepository::delete);
    }
}
