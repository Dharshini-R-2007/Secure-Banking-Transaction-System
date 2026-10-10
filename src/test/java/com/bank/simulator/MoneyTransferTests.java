package com.bank.simulator;

import com.bank.simulator.dto.CreateClientRequest;
import com.bank.simulator.dto.DepositRequest;
import com.bank.simulator.dto.LoginRequest;
import com.bank.simulator.dto.TransferRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MoneyTransferTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        String token;
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

        tua.token = loginAndGetToken(tua.username, "Password@123");
        return tua;
    }

    @Test
    @DisplayName("1. Successful money transfer from Client A to Client B")
    void testSuccessfulMoneyTransfer() throws Exception {
        TestUserAccount userA = createTestUser("txUserA1", new BigDecimal("10000.00"));
        TestUserAccount userB = createTestUser("txUserB1", new BigDecimal("5000.00"));

        TransferRequest request = new TransferRequest(userB.accountNumber, new BigDecimal("2000.00"), "Rent Payment");

        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + userA.token)
                        .header("Idempotency-Key", "KEY-TX-1-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(2000))
                .andExpect(jsonPath("$.senderAccountNumber").value(userA.accountNumber))
                .andExpect(jsonPath("$.receiverAccountNumber").value(userB.accountNumber));

        // Check A balance
        mockMvc.perform(get("/client/account/balance")
                        .header("Authorization", "Bearer " + userA.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(8000.00));

        // Check B balance
        mockMvc.perform(get("/client/account/balance")
                        .header("Authorization", "Bearer " + userB.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(7000.00));
    }

    @Test
    @DisplayName("2. Insufficient balance -> rejected 400 Bad Request")
    void testInsufficientBalanceTransfer() throws Exception {
        TestUserAccount userA = createTestUser("txUserA2", new BigDecimal("500.00"));
        TestUserAccount userB = createTestUser("txUserB2", new BigDecimal("1000.00"));

        TransferRequest request = new TransferRequest(userB.accountNumber, new BigDecimal("2000.00"), "Overdraft Attempt");

        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + userA.token)
                        .header("Idempotency-Key", "KEY-TX-2-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient account balance. Available: 500.00, Requested: 2000.00"));
    }

    @Test
    @DisplayName("3. Invalid receiver account -> rejected 404 Not Found")
    void testInvalidReceiverAccount() throws Exception {
        TestUserAccount userA = createTestUser("txUserA3", new BigDecimal("1000.00"));

        TransferRequest request = new TransferRequest("999999999999", new BigDecimal("100.00"), "Fake Receiver");

        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + userA.token)
                        .header("Idempotency-Key", "KEY-TX-3-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. Blocked receiver account -> rejected 400 Bad Request")
    void testBlockedReceiverAccount() throws Exception {
        String adminToken = getAdminToken();

        TestUserAccount userA = createTestUser("txUserA4", new BigDecimal("1000.00"));
        TestUserAccount userB = createTestUser("txUserB4", new BigDecimal("1000.00"));

        // Admin blocks B's account
        mockMvc.perform(patch("/admin/accounts/" + userB.accountId + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        TransferRequest request = new TransferRequest(userB.accountNumber, new BigDecimal("100.00"), "To Blocked");

        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + userA.token)
                        .header("Idempotency-Key", "KEY-TX-4-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Receiver bank account is BLOCKED. Transfer rejected."));
    }

    @Test
    @DisplayName("5. Blocked sender account -> rejected 400 Bad Request")
    void testBlockedSenderAccount() throws Exception {
        String adminToken = getAdminToken();

        TestUserAccount userA = createTestUser("txUserA5", new BigDecimal("1000.00"));
        TestUserAccount userB = createTestUser("txUserB5", new BigDecimal("1000.00"));

        // Admin blocks A's account
        mockMvc.perform(patch("/admin/accounts/" + userA.accountId + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        TransferRequest request = new TransferRequest(userB.accountNumber, new BigDecimal("100.00"), "From Blocked");

        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + userA.token)
                        .header("Idempotency-Key", "KEY-TX-5-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sender bank account is BLOCKED. Transfer rejected."));
    }

    @Test
    @DisplayName("6. Closed account transfer -> rejected 400 Bad Request")
    void testClosedAccountTransfer() throws Exception {
        String adminToken = getAdminToken();

        TestUserAccount userA = createTestUser("txUserA6", new BigDecimal("1000.00"));
        TestUserAccount userB = createTestUser("txUserB6", new BigDecimal("1000.00"));

        // Close B's account
        mockMvc.perform(patch("/admin/accounts/" + userB.accountId + "/close")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        TransferRequest request = new TransferRequest(userB.accountNumber, new BigDecimal("100.00"), "To Closed");

        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + userA.token)
                        .header("Idempotency-Key", "KEY-TX-6-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Receiver bank account is CLOSED. Transfer rejected."));
    }

    @Test
    @DisplayName("7. Zero or negative amount -> rejected 400 Bad Request")
    void testZeroOrNegativeAmountTransfer() throws Exception {
        TestUserAccount userA = createTestUser("txUserA7", new BigDecimal("1000.00"));
        TestUserAccount userB = createTestUser("txUserB7", new BigDecimal("1000.00"));

        TransferRequest zeroReq = new TransferRequest(userB.accountNumber, new BigDecimal("0.00"), "Zero Transfer");
        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + userA.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroReq)))
                .andExpect(status().isBadRequest());

        TransferRequest negReq = new TransferRequest(userB.accountNumber, new BigDecimal("-50.00"), "Negative Transfer");
        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + userA.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8. Sender = Receiver -> rejected 400 Bad Request")
    void testSameAccountTransfer() throws Exception {
        TestUserAccount userA = createTestUser("txUserA8", new BigDecimal("1000.00"));

        TransferRequest request = new TransferRequest(userA.accountNumber, new BigDecimal("100.00"), "Self Transfer");

        mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + userA.token)
                        .header("Idempotency-Key", "KEY-TX-8-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sender and receiver bank accounts cannot be the same account"));
    }
}
