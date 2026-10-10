package com.bank.simulator;

import com.bank.simulator.dto.CreateClientRequest;
import com.bank.simulator.dto.LoginRequest;
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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClientAndAccountManagementTests {

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

    @Test
    @DisplayName("Admin creates Client A & B with auto-generated accounts")
    void testAdminCreatesClientWithAutoAccount() throws Exception {
        String adminToken = getAdminToken();

        // 1. Create Client A
        CreateClientRequest createA = new CreateClientRequest("clientA", "Password@123");
        MvcResult resultA = mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("clientA"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accountNumber").exists())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andReturn();

        String accNumA = objectMapper.readTree(resultA.getResponse().getContentAsString()).get("accountNumber").asText();
        assertNotNull(accNumA);

        // 2. Create Client B
        CreateClientRequest createB = new CreateClientRequest("clientB", "Password@123");
        MvcResult resultB = mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("clientB"))
                .andReturn();

        String accNumB = objectMapper.readTree(resultB.getResponse().getContentAsString()).get("accountNumber").asText();
        assertNotEquals(accNumA, accNumB, "Client A and Client B must have distinct account numbers");
    }

    @Test
    @DisplayName("Admin client management: view all, view by ID, search, block, activate, deactivate")
    void testAdminClientManagementLifecycle() throws Exception {
        String adminToken = getAdminToken();

        // Create a client
        CreateClientRequest createReq = new CreateClientRequest("testClientMgmt", "Password@123");
        MvcResult createRes = mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long clientId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // View all clients
        mockMvc.perform(get("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // View by ID
        mockMvc.perform(get("/admin/clients/" + clientId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testClientMgmt"));

        // Search by username
        mockMvc.perform(get("/admin/clients/search?username=testClientMgmt")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testClientMgmt"));

        // Block client
        mockMvc.perform(patch("/admin/clients/" + clientId + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        // Activate client
        mockMvc.perform(patch("/admin/clients/" + clientId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Deactivate client
        mockMvc.perform(patch("/admin/clients/" + clientId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("Admin account management: view all, view by ID, search number, block, activate, close")
    void testAdminAccountManagementLifecycle() throws Exception {
        String adminToken = getAdminToken();

        CreateClientRequest createReq = new CreateClientRequest("accountMgmtClient", "Password@123");
        MvcResult createRes = mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String accNum = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("accountNumber").asText();

        // View all accounts
        mockMvc.perform(get("/admin/accounts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // View by number
        MvcResult getByNumRes = mockMvc.perform(get("/admin/accounts/number/" + accNum)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(accNum))
                .andReturn();

        Long accountId = objectMapper.readTree(getByNumRes.getResponse().getContentAsString()).get("id").asLong();

        // View by ID
        mockMvc.perform(get("/admin/accounts/" + accountId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId));

        // Block account
        mockMvc.perform(patch("/admin/accounts/" + accountId + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        // Activate account
        mockMvc.perform(patch("/admin/accounts/" + accountId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Close account
        mockMvc.perform(patch("/admin/accounts/" + accountId + "/close")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // Try activating closed account -> Should be rejected with 400
        mockMvc.perform(patch("/admin/accounts/" + accountId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CLOSED account cannot be activated"));
    }

    @Test
    @DisplayName("Client self-service APIs: profile, account, balance")
    void testClientSelfServiceApis() throws Exception {
        String adminToken = getAdminToken();

        CreateClientRequest createReq = new CreateClientRequest("selfServiceClient", "Password@123");
        mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        String clientToken = loginAndGetToken("selfServiceClient", "Password@123");

        // Profile
        mockMvc.perform(get("/client/profile")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("selfServiceClient"))
                .andExpect(jsonPath("$.account.accountNumber").exists());

        // Account
        mockMvc.perform(get("/client/account")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("selfServiceClient"))
                .andExpect(jsonPath("$.balance").value(0));

        // Balance
        mockMvc.perform(get("/client/account/balance")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").exists())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Security rules: CLIENT cannot access admin endpoints")
    void testClientForbiddenFromAdminEndpoints() throws Exception {
        String adminToken = getAdminToken();

        CreateClientRequest createReq = new CreateClientRequest("ordinaryClient", "Password@123");
        mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        String clientToken = loginAndGetToken("ordinaryClient", "Password@123");

        // CLIENT trying to create another client -> 403
        CreateClientRequest rogueReq = new CreateClientRequest("rogueClient", "Password@123");
        mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rogueReq)))
                .andExpect(status().isForbidden());

        // CLIENT trying to view all clients -> 403
        mockMvc.perform(get("/admin/clients")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());

        // CLIENT trying to view all accounts -> 403
        mockMvc.perform(get("/admin/accounts")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Data integrity: Duplicate username rejected with 409 Conflict")
    void testDuplicateUsernameRejected() throws Exception {
        String adminToken = getAdminToken();

        CreateClientRequest createReq = new CreateClientRequest("duplicateUser", "Password@123");
        mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        // Duplicate create attempt
        mockMvc.perform(post("/admin/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username 'duplicateUser' is already taken"));
    }
}
