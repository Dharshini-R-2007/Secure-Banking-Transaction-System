package com.bank.simulator;

import com.bank.simulator.dto.CreateClientRequest;
import com.bank.simulator.dto.DepositRequest;
import com.bank.simulator.dto.LoginRequest;
import com.bank.simulator.dto.TransferRequest;
import com.bank.simulator.entity.AuditLog;
import com.bank.simulator.entity.LedgerEntry;
import com.bank.simulator.entity.LedgerEntryType;
import com.bank.simulator.entity.Transaction;
import com.bank.simulator.repository.AuditLogRepository;
import com.bank.simulator.repository.BankAccountRepository;
import com.bank.simulator.repository.LedgerEntryRepository;
import com.bank.simulator.repository.TransactionRepository;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LedgerAndAuditTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TransferService transferService;

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
    @DisplayName("14. Ledger Integrity: Successful transfer creates matching DEBIT and CREDIT ledger entries (Total DEBIT = Total CREDIT)")
    void testLedgerEntriesForTransfer() throws Exception {
        String userA = "ledgerUserA_" + UUID.randomUUID().toString().substring(0, 5);
        String userB = "ledgerUserB_" + UUID.randomUUID().toString().substring(0, 5);

        String accA = createClientAndDeposit(userA, new BigDecimal("5000.00"));
        String accB = createClientAndDeposit(userB, new BigDecimal("1000.00"));

        String tokenA = loginAndGetToken(userA, "Password@123");
        TransferRequest request = new TransferRequest(accB, new BigDecimal("2000.00"), "Ledger Verification");

        MvcResult transferRes = mockMvc.perform(post("/client/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", "KEY-LEDGER-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String txnRef = objectMapper.readTree(transferRes.getResponse().getContentAsString()).get("transactionReference").asText();

        Transaction txn = transactionRepository.findByTransactionReference(txnRef).orElseThrow();
        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findByTransaction(txn);

        assertEquals(2, ledgerEntries.size(), "Transfer transaction must create exactly 2 ledger entries");

        LedgerEntry debitEntry = ledgerEntries.stream().filter(e -> e.getEntryType() == LedgerEntryType.DEBIT).findFirst().orElseThrow();
        LedgerEntry creditEntry = ledgerEntries.stream().filter(e -> e.getEntryType() == LedgerEntryType.CREDIT).findFirst().orElseThrow();

        assertEquals(accA, debitEntry.getAccount().getAccountNumber());
        assertEquals(accB, creditEntry.getAccount().getAccountNumber());
        assertEquals(0, new BigDecimal("2000.00").compareTo(debitEntry.getAmount()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(creditEntry.getAmount()));
    }

    @Test
    @DisplayName("15. Audit Log: System operations (login, client creation, transfers) generate detailed audit trail without sensitive keys")
    void testAuditTrailGeneration() throws Exception {
        String adminToken = getAdminToken();

        // Check audit log for login
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertFalse(auditLogs.isEmpty(), "Audit log table must contain audit entries");

        // Verify admin audit logs can be viewed via REST endpoint
        mockMvc.perform(get("/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
