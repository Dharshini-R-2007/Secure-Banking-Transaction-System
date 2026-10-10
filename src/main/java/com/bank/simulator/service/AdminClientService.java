package com.bank.simulator.service;

import com.bank.simulator.dto.ClientResponse;
import com.bank.simulator.dto.CreateClientRequest;
import com.bank.simulator.entity.AccountStatus;
import com.bank.simulator.entity.AuditAction;
import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.Role;
import com.bank.simulator.entity.User;
import com.bank.simulator.entity.UserStatus;
import com.bank.simulator.exception.ClientNotFoundException;
import com.bank.simulator.exception.UsernameAlreadyExistsException;
import com.bank.simulator.repository.BankAccountRepository;
import com.bank.simulator.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminClientService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountNumberGeneratorService accountNumberGeneratorService;
    private final AuditLogService auditLogService;

    public AdminClientService(
            UserRepository userRepository,
            BankAccountRepository bankAccountRepository,
            PasswordEncoder passwordEncoder,
            AccountNumberGeneratorService accountNumberGeneratorService,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountNumberGeneratorService = accountNumberGeneratorService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username '" + request.getUsername() + "' is already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CLIENT)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        String accountNumber = accountNumberGeneratorService.generateAccountNumber();

        BankAccount bankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .user(savedUser)
                .balance(BigDecimal.ZERO.setScale(2))
                .status(AccountStatus.ACTIVE)
                .build();

        BankAccount savedAccount = bankAccountRepository.save(bankAccount);

        auditLogService.logEvent("ADMIN", AuditAction.CLIENT_CREATED, "USER", savedUser.getUsername(), "Client created with account number " + accountNumber, "SUCCESS");
        auditLogService.logEvent("ADMIN", AuditAction.ACCOUNT_CREATED, "BANK_ACCOUNT", accountNumber, "Bank account created with initial balance 0.00", "SUCCESS");

        return mapToClientResponse(savedUser, savedAccount);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {
        List<User> clients = userRepository.findByRole(Role.CLIENT);
        return clients.stream()
                .map(user -> {
                    BankAccount account = bankAccountRepository.findByUser(user).orElse(null);
                    return mapToClientResponse(user, account);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.CLIENT)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));

        BankAccount account = bankAccountRepository.findByUser(user).orElse(null);
        return mapToClientResponse(user, account);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> searchClientsByUsername(String usernameKeyword) {
        List<User> clients = userRepository.findByRoleAndUsernameContainingIgnoreCase(Role.CLIENT, usernameKeyword);
        return clients.stream()
                .map(user -> {
                    BankAccount account = bankAccountRepository.findByUser(user).orElse(null);
                    return mapToClientResponse(user, account);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ClientResponse blockClient(Long id) {
        User user = getClientUserOrThrow(id);
        user.setStatus(UserStatus.BLOCKED);
        User savedUser = userRepository.save(user);
        BankAccount account = bankAccountRepository.findByUser(savedUser).orElse(null);

        auditLogService.logEvent("ADMIN", AuditAction.CLIENT_BLOCKED, "USER", user.getUsername(), "Client status changed to BLOCKED", "SUCCESS");

        return mapToClientResponse(savedUser, account);
    }

    @Transactional
    public ClientResponse activateClient(Long id) {
        User user = getClientUserOrThrow(id);
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);
        BankAccount account = bankAccountRepository.findByUser(savedUser).orElse(null);

        auditLogService.logEvent("ADMIN", AuditAction.CLIENT_ACTIVATED, "USER", user.getUsername(), "Client status changed to ACTIVE", "SUCCESS");

        return mapToClientResponse(savedUser, account);
    }

    @Transactional
    public ClientResponse deactivateClient(Long id) {
        User user = getClientUserOrThrow(id);
        user.setStatus(UserStatus.INACTIVE);
        User savedUser = userRepository.save(user);
        BankAccount account = bankAccountRepository.findByUser(savedUser).orElse(null);

        auditLogService.logEvent("ADMIN", AuditAction.CLIENT_DEACTIVATED, "USER", user.getUsername(), "Client status changed to INACTIVE", "SUCCESS");

        return mapToClientResponse(savedUser, account);
    }

    private User getClientUserOrThrow(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == Role.CLIENT)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));
    }

    private ClientResponse mapToClientResponse(User user, BankAccount account) {
        return ClientResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .accountNumber(account != null ? account.getAccountNumber() : null)
                .balance(account != null ? account.getBalance() : null)
                .accountStatus(account != null ? account.getStatus() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
