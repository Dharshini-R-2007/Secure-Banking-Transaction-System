package com.bank.simulator.repository;

import com.bank.simulator.entity.IdempotencyKey;
import com.bank.simulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByKey(String key);

    Optional<IdempotencyKey> findByKeyAndUser(String key, User user);

    boolean existsByKey(String key);
}
