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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdempotencyTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private String getAdminToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "Admin@123");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String createClientAndDeposit(String username, BigDecimal depositAmount) throws Exception {
        String adminToken = getAdminToken();

        CreateClientRequest createReq = new CreateClientRequest(username, "Password@123");
        MvcResult createRes = mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String accNum = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("accountNumber").asText();

        if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            MvcResult accRes = mockMvc.perform(get("/admin/accounts/number/" + accNum)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andReturn();
            Long accId = objectMapper.readTree(accRes.getResponse().getContentAsString()).get("id").asLong();

            DepositRequest depReq = new DepositRequest(depositAmount, "Initial Test Deposit");
            mockMvc.perform(post("/admin/accounts/" + accId + "/deposit")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(depReq)))
                    .andExpect(status().isCreated());
        }

        return accNum;
    }

    @Test
    @DisplayName("12. Idempotency: Retrying exact same transfer with same Idempotency-Key returns cached response without double debit")
    void testIdempotentTransferRetry() throws Exception {
        String userA = "idemUserA_" + UUID.randomUUID().toString().substring(0, 5);
        String userB = "idemUserB_" + UUID.randomUUID().toString().substring(0, 5);

        String accA = createClientAndDeposit(userA, new BigDecimal("5000.00"));
        String accB = createClientAndDeposit(userB, new BigDecimal("1000.00"));

        String tokenA = loginAndGetToken(userA, "Password@123");
        TransferRequest request = new TransferRequest(accB, new BigDecimal("1500.00"), "Idempotency Test");

        String idempotencyKey = "UNIQUE-IDEM-KEY-" + UUID.randomUUID();

        // First Request
        MvcResult firstRes = mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(1500))
                .andReturn();

        String firstTxnRef = objectMapper.readTree(firstRes.getResponse().getContentAsString()).get("transactionReference").asText();

        // Second Request with SAME Idempotency-Key
        MvcResult secondRes = mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // returns cached response
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn();

        String secondTxnRef = objectMapper.readTree(secondRes.getResponse().getContentAsString()).get("transactionReference").asText();

        // Must return the exact same transaction reference!
        assertEquals(firstTxnRef, secondTxnRef, "Transaction reference must be identical for retried request");

        // Verify balance was debited ONLY ONCE (5000 - 1500 = 3500)
        User userObjA = userRepository.findByUsername(userA).orElseThrow();
        BankAccount bankAccountA = bankAccountRepository.findByUser(userObjA).orElseThrow();
        assertEquals(0, new BigDecimal("3500.00").compareTo(bankAccountA.getBalance()), "Balance must be debited only once");
    }

    @Test
    @DisplayName("13. Idempotency: Different keys create separate transactions")
    void testDifferentIdempotencyKeys() throws Exception {
        String userA = "idemUserA2_" + UUID.randomUUID().toString().substring(0, 5);
        String userB = "idemUserB2_" + UUID.randomUUID().toString().substring(0, 5);

        String accB = createClientAndDeposit(userB, new BigDecimal("1000.00"));
        createClientAndDeposit(userA, new BigDecimal("5000.00"));

        String tokenA = loginAndGetToken(userA, "Password@123");
        TransferRequest request = new TransferRequest(accB, new BigDecimal("500.00"), "Transfer 1");

        // Key 1
        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", "KEY-DIFF-1-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Key 2
        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", "KEY-DIFF-2-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Balance debited twice (5000 - 500 - 500 = 4000)
        mockMvc.perform(get("/client/account/balance")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(4000.00));
    }
}
