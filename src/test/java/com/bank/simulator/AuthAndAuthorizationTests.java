package com.bank.simulator;

import com.bank.simulator.dto.LoginRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndAuthorizationTests {

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

    private String getAdminToken() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("admin")
                .password("Admin@123")
                .build();

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseStr).get("token").asText();
    }

    private String getClientToken() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("client")
                .password("Client@123")
                .build();

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseStr).get("token").asText();
    }

    @Test
    @DisplayName("1. Valid BANK_ADMIN login -> success + JWT")
    void testValidAdminLogin() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("admin")
                .password("Admin@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("BANK_ADMIN"));
    }

    @Test
    @DisplayName("2. Valid CLIENT login -> success + JWT")
    void testValidClientLogin() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("client")
                .password("Client@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("client"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    @DisplayName("3. Wrong password -> rejected 401")
    void testWrongPassword() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("admin")
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("4. Non-existing username -> rejected 401")
    void testNonExistingUsername() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("non_existing_user")
                .password("Admin@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("5. BLOCKED user -> rejected 401")
    void testBlockedUser() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("blocked_user")
                .password("Blocked@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("User account is BLOCKED. Access denied."));
    }

    @Test
    @DisplayName("6. INACTIVE user -> rejected 401")
    void testInactiveUser() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("inactive_user")
                .password("Inactive@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("User account is INACTIVE. Access denied."));
    }

    @Test
    @DisplayName("7. No JWT accessing protected endpoint -> 401")
    void testNoJwtAccessingProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("8. Invalid JWT -> 401")
    void testInvalidJwt() throws Exception {
        mockMvc.perform(get("/admin/test")
                        .header("Authorization", "Bearer invalid.jwt.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("10. BANK_ADMIN accessing /admin/test -> allowed 200 OK")
    void testAdminAccessAdminTest() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(get("/admin/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("BANK_ADMIN endpoint accessed successfully!"));
    }

    @Test
    @DisplayName("11. CLIENT accessing /admin/test -> forbidden 403")
    void testClientAccessAdminTest() throws Exception {
        String token = getClientToken();
        mockMvc.perform(get("/admin/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("12. CLIENT accessing /client/test -> allowed 200 OK")
    void testClientAccessClientTest() throws Exception {
        String token = getClientToken();
        mockMvc.perform(get("/client/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("CLIENT endpoint accessed successfully!"));
    }

    @Test
    @DisplayName("13. BANK_ADMIN accessing /user/test -> allowed 200 OK")
    void testAdminAccessUserTest() throws Exception {
        String token = getAdminToken();
        mockMvc.perform(get("/user/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authenticated user endpoint accessed successfully!"));
    }

    @Test
    @DisplayName("14. CLIENT accessing /user/test -> allowed 200 OK")
    void testClientAccessUserTest() throws Exception {
        String token = getClientToken();
        mockMvc.perform(get("/user/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authenticated user endpoint accessed successfully!"));
    }

    @Test
    @DisplayName("15. Blank username/password -> 400 Bad Request validation error")
    void testBlankCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("")
                .password("")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
