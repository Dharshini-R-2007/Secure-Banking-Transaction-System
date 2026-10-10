package com.bank.simulator.service;

import com.bank.simulator.dto.AccountBalanceResponse;
import com.bank.simulator.dto.AccountResponse;
import com.bank.simulator.dto.ClientProfileResponse;
import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.User;
import com.bank.simulator.exception.AccountNotFoundException;
import com.bank.simulator.exception.ClientNotFoundException;
import com.bank.simulator.repository.BankAccountRepository;
import com.bank.simulator.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientSelfService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    public ClientSelfService(UserRepository userRepository, BankAccountRepository bankAccountRepository) {
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional(readOnly = true)
    public ClientProfileResponse getClientProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ClientNotFoundException("User profile not found for username: " + username));

        BankAccount account = bankAccountRepository.findByUser(user).orElse(null);
        AccountResponse accountResponse = account != null ? mapToAccountResponse(account) : null;

        return ClientProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .account(accountResponse)
                .build();
    }

    @Transactional(readOnly = true)
    public AccountResponse getClientAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ClientNotFoundException("User profile not found for username: " + username));

        BankAccount account = bankAccountRepository.findByUser(user)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found for username: " + username));

        return mapToAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse getClientBalance(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ClientNotFoundException("User profile not found for username: " + username));

        BankAccount account = bankAccountRepository.findByUser(user)
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found for username: " + username));

        return AccountBalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }

    private AccountResponse mapToAccountResponse(BankAccount account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUser() != null ? account.getUser().getId() : null)
                .username(account.getUser() != null ? account.getUser().getUsername() : null)
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
