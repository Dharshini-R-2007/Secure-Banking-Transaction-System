package com.bank.simulator;

import com.bank.simulator.dto.CreateClientRequest;
import com.bank.simulator.dto.DepositRequest;
import com.bank.simulator.dto.LoginRequest;
import com.bank.simulator.dto.TransferRequest;
import com.bank.simulator.entity.BankAccount;
import com.bank.simulator.entity.User;
import com.bank.simulator.repository.BankAccountRepository;
import com.bank.simulator.repository.UserRepository;
import com.bank.simulator.service.RateLimiterService;
import com.bank.simulator.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConcurrencyAndLockingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransferService transferService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService.reset();
    }

    private static class TestUserAccount {
        String username;
        String accountNumber;
        Long accountId;
    }

    private String getAdminToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "Admin@123");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private TestUserAccount createTestUser(String basePrefix, BigDecimal depositAmount) throws Exception {
        String adminToken = getAdminToken();

        TestUserAccount tua = new TestUserAccount();
        tua.username = basePrefix + "_" + UUID.randomUUID().toString().substring(0, 8);

        CreateClientRequest createReq = new CreateClientRequest(tua.username, "Password@123");
        MvcResult createRes = mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        tua.accountNumber = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("accountNumber").asText();

        MvcResult accRes = mockMvc.perform(get("/admin/accounts/number/" + tua.accountNumber)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        tua.accountId = objectMapper.readTree(accRes.getResponse().getContentAsString()).get("id").asLong();

        if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            DepositRequest depReq = new DepositRequest(depositAmount, "Initial Test Deposit");
            mockMvc.perform(post("/admin/accounts/" + tua.accountId + "/deposit")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(depReq)))
                    .andExpect(status().isCreated());
        }

        return tua;
    }

    @Test
    @DisplayName("10. Concurrency & Pessimistic Locking: Two simultaneous ₹800 transfers from ₹1000 balance -> exactly 1 succeeds")
    void testConcurrentTransfersWithPessimisticLocking() throws Exception {
        TestUserAccount userA = createTestUser("concUserA", new BigDecimal("1000.00"));
        TestUserAccount userB = createTestUser("concUserB", new BigDecimal("0.00"));
        TestUserAccount userC = createTestUser("concUserC", new BigDecimal("0.00"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        java.util.List<Exception> exceptions = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        // Task 1: A -> B ₹800
        executor.submit(() -> {
            try {
                startLatch.await();
                TransferRequest req1 = new TransferRequest(userB.accountNumber, new BigDecimal("800.00"), "Conc Transfer 1");
                transferService.processTransfer(userA.username, req1, "KEY-CONC-1-" + UUID.randomUUID());
                successCount.incrementAndGet();
            } catch (Exception ex) {
                exceptions.add(ex);
                failureCount.incrementAndGet();
            } finally {
                finishLatch.countDown();
            }
        });

        // Task 2: A -> C ₹800
        executor.submit(() -> {
            try {
                startLatch.await();
                TransferRequest req2 = new TransferRequest(userC.accountNumber, new BigDecimal("800.00"), "Conc Transfer 2");
                transferService.processTransfer(userA.username, req2, "KEY-CONC-2-" + UUID.randomUUID());
                successCount.incrementAndGet();
            } catch (Exception ex) {
                exceptions.add(ex);
                failureCount.incrementAndGet();
            } finally {
                finishLatch.countDown();
            }
        });

        startLatch.countDown(); // Release both threads simultaneously
        finishLatch.await();
        executor.shutdown();

        String exDetails = exceptions.stream().map(e -> e.getClass().getSimpleName() + ": " + e.getMessage()).collect(java.util.stream.Collectors.joining("; "));
        assertEquals(1, successCount.get(), "Exactly one transfer should succeed. Thread exceptions: " + exDetails);
        assertEquals(1, failureCount.get(), "Exactly one transfer should fail due to insufficient balance");

        User userObjA = userRepository.findByUsername(userA.username).orElseThrow();
        BankAccount finalA = bankAccountRepository.findByUser(userObjA).orElseThrow();
        assertEquals(0, new BigDecimal("200.00").compareTo(finalA.getBalance()), "Final balance of A must be 200.00");
        assertTrue(finalA.getBalance().compareTo(BigDecimal.ZERO) >= 0, "Balance must never be negative");
    }

    @Test
    @DisplayName("11. Concurrency: 10 simultaneous transfers from ₹5000 balance -> exactly 5 succeed, balance reaches ₹0")
    void testMultipleConcurrentTransfers() throws Exception {
        TestUserAccount userA = createTestUser("multiConcA", new BigDecimal("5000.00"));
        TestUserAccount userB = createTestUser("multiConcB", new BigDecimal("0.00"));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        java.util.List<Exception> exceptions = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransferRequest req = new TransferRequest(userB.accountNumber, new BigDecimal("1000.00"), "Batch Transfer " + index);
                    transferService.processTransfer(userA.username, req, "KEY-MULTI-" + index + "-" + UUID.randomUUID());
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    exceptions.add(ex);
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        String exDetails = exceptions.stream().map(e -> e.getClass().getSimpleName() + ": " + e.getMessage()).collect(java.util.stream.Collectors.joining("; "));
        assertEquals(5, successCount.get(), "Exactly 5 transfers of 1000 should succeed out of 5000 balance. Thread exceptions: " + exDetails);
        assertEquals(5, failureCount.get(), "Exactly 5 transfers should fail");

        User userObjA = userRepository.findByUsername(userA.username).orElseThrow();
        BankAccount finalA = bankAccountRepository.findByUser(userObjA).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(finalA.getBalance()), "Final balance of A must be 0.00");
    }
}
