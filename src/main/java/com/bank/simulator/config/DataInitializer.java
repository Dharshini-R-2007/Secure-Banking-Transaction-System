package com.bank.simulator.config;

import com.bank.simulator.entity.AccountStatus;
import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.Role;
import com.bank.simulator.entity.User;
import com.bank.simulator.entity.UserStatus;
import com.bank.simulator.repository.BankAccountRepository;
import com.bank.simulator.repository.UserRepository;
import com.bank.simulator.service.AccountNumberGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountNumberGeneratorService accountNumberGeneratorService;

    @Value("${app.admin.initial.username:admin}")
    private String adminUsername;

    @Value("${app.admin.initial.password:Admin@123}")
    private String adminPassword;

    public DataInitializer(
            UserRepository userRepository,
            BankAccountRepository bankAccountRepository,
            PasswordEncoder passwordEncoder,
            AccountNumberGeneratorService accountNumberGeneratorService
    ) {
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountNumberGeneratorService = accountNumberGeneratorService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[DEMO/DEV INIT] Checking for initial system users...");

        // 1. Initial BANK_ADMIN user
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.BANK_ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
            log.info("[DEMO/DEV INIT] Initial BANK_ADMIN created with username: '{}'", adminUsername);
        } else {
            log.info("[DEMO/DEV INIT] BANK_ADMIN '{}' already exists.", adminUsername);
        }

        // 2. Demo ACTIVE CLIENT user
        createSeedClientIfAbsent("client", "Client@123", UserStatus.ACTIVE);

        // 3. Demo BLOCKED user
        createSeedClientIfAbsent("blocked_user", "Blocked@123", UserStatus.BLOCKED);

        // 4. Demo INACTIVE user
        createSeedClientIfAbsent("inactive_user", "Inactive@123", UserStatus.INACTIVE);
    }

    private void createSeedClientIfAbsent(String username, String rawPassword, UserStatus status) {
        if (!userRepository.existsByUsername(username)) {
            User clientUser = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(Role.CLIENT)
                    .status(status)
                    .build();
            User savedUser = userRepository.save(clientUser);

            String accountNumber = accountNumberGeneratorService.generateAccountNumber();
            BankAccount account = BankAccount.builder()
                    .accountNumber(accountNumber)
                    .user(savedUser)
                    .balance(BigDecimal.ZERO.setScale(2))
                    .status(AccountStatus.ACTIVE)
                    .build();
            bankAccountRepository.save(account);

            log.info("[DEMO/DEV INIT] Demo CLIENT created: username='{}', status={}, accountNumber='{}'", username, status, accountNumber);
        }
    }
}
