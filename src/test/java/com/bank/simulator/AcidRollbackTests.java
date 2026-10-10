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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AcidRollbackTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransferService transferService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

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
    @Transactional
    @DisplayName("9. ACID Test: Exception during transfer rolls back sender debit & receiver credit completely")
    void testTransactionRollbackOnFailure() throws Exception {
        TestUserAccount userA = createTestUser("acidUserA", new BigDecimal("10000.00"));
        TestUserAccount userB = createTestUser("acidUserB", new BigDecimal("5000.00"));

        User userObjA = userRepository.findByUsername(userA.username).orElseThrow();

        // Attempting an invalid transfer (amount greater than available balance)
        TransferRequest overdrawReq = new TransferRequest(userB.accountNumber, new BigDecimal("20000.00"), "Overdraw Attempt");

        assertThrows(RuntimeException.class, () -> {
            transferService.executeTransferInTransaction(userObjA, overdrawReq);
        });

        // Verify balances remain completely unchanged (A = 10000.00, B = 5000.00)
        BankAccount finalSenderAcc = bankAccountRepository.findByUserId(userObjA.getId()).orElseThrow();
        BankAccount finalReceiverAcc = bankAccountRepository.findByAccountNumber(userB.accountNumber).orElseThrow();

        assertEquals(0, new BigDecimal("10000.00").compareTo(finalSenderAcc.getBalance()), "Sender balance must remain 10000.00");
        assertEquals(0, new BigDecimal("5000.00").compareTo(finalReceiverAcc.getBalance()), "Receiver balance must remain 5000.00");
    }
}
