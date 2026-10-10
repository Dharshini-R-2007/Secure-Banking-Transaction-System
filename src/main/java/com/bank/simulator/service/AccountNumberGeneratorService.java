package com.bank.simulator.service;

import com.bank.simulator.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class AccountNumberGeneratorService {

    private static final long BASE_ACCOUNT_NUMBER = 100000000000L;
    private final BankAccountRepository bankAccountRepository;
    private final AtomicLong counter;

    public AccountNumberGeneratorService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
        Long maxId = bankAccountRepository.findMaxAccountId();
        long initialVal = (maxId != null) ? maxId : 0L;
        this.counter = new AtomicLong(initialVal);
    }

    public synchronized String generateAccountNumber() {
        String accountNumber;
        do {
            long nextVal = counter.incrementAndGet();
            accountNumber = String.valueOf(BASE_ACCOUNT_NUMBER + nextVal);
        } while (bankAccountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}
