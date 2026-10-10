package com.bank.simulator.repository;

import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.LedgerEntry;
import com.bank.simulator.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransaction(Transaction transaction);

    Page<LedgerEntry> findByAccount(BankAccount account, Pageable pageable);
}
