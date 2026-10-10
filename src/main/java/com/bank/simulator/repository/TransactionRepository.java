package com.bank.simulator.repository;

import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.Transaction;
import com.bank.simulator.entity.TransactionStatus;
import com.bank.simulator.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    @Query("SELECT t FROM Transaction t WHERE t.senderAccount = :account OR t.receiverAccount = :account")
    Page<Transaction> findByAccount(@Param("account") BankAccount account, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.senderAccount = :account OR t.receiverAccount = :account) " +
           "AND (:type IS NULL OR t.transactionType = :type) " +
           "AND (:status IS NULL OR t.status = :status)")
    Page<Transaction> findByAccountFiltered(
            @Param("account") BankAccount account,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            Pageable pageable
    );
}
