package com.turtledot.studio.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turtledot.studio.auth.dto.LoginRequest;
import com.turtledot.studio.client.dto.ClientRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ClientAndEmployeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setupTokens() throws Exception {
        // Login Admin
        LoginRequest adminLogin = new LoginRequest("admin@studio.local", "Admin@123456");
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Login Employee
        LoginRequest empLogin = new LoginRequest("employee@studio.local", "Employee@123456");
        MvcResult empResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empLogin)))
                .andExpect(status().isOk())
                .andReturn();
        employeeToken = objectMapper.readTree(empResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Admin can create new client and receive auto-generated client code")
    void testAdminCreateClient() throws Exception {
        ClientRequest req = new ClientRequest();
        req.setName("Test Wedding Couple");
        req.setEmail("couple@test.com");
        req.setPhone("+1 555-987-6543");
        req.setTotalAmount(new BigDecimal("3500.00"));

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientCode").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Test Wedding Couple"));
    }

    @Test
    @DisplayName("Employee cannot create client (requires ADMIN role)")
    void testEmployeeCannotCreateClient() throws Exception {
        ClientRequest req = new ClientRequest();
        req.setName("Unauthorized Attempt");

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employee can view assigned client, but cannot access unauthorized client")
    void testEmployeeAccessRestrictions() throws Exception {
        // Client 1 is assigned to employee in SeedDataInitializer
        mockMvc.perform(get("/api/clients/1")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Rajesh Kumar & Priya Sharma"));

        // Unassigned client ID 9999 should return 403 Forbidden or 404
        mockMvc.perform(get("/api/clients/9999")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }
}
