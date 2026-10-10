package com.turtledot.studio.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turtledot.studio.auth.dto.LoginRequest;
import com.turtledot.studio.payment.dto.CreatePaymentRequest;
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
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class PaymentAndStorageSyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest adminLogin = new LoginRequest("admin@studio.local", "Admin@123456");
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = res.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(responseBody).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Admin can record payment and query dashboard stats and ledger")
    void testRecordPaymentAndDashboardStats() throws Exception {
        // 1. Record payment for Client 1
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setClientId(1L);
        req.setAmount(new BigDecimal("1250.00"));
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setPaymentReference("UPI-DEMO-TEST-992");
        req.setPaymentDate(LocalDate.now());
        req.setNotes("Automated test deposit");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(1250.00))
                .andExpect(jsonPath("$.data.paymentMethod").value("UPI"));

        // 2. Fetch studio payments list
        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        // 3. Query Dashboard Stats
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").isNumber())
                .andExpect(jsonPath("$.data.totalClients").isNumber());

        // 4. Query Monthly Revenue Trend
        mockMvc.perform(get("/api/dashboard/monthly-revenue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Global search finds matching clients and events")
    void testGlobalSearch() throws Exception {
        mockMvc.perform(get("/api/search?q=Rajesh")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clients[0].name").value(org.hamcrest.Matchers.containsString("Rajesh")));
    }

    @Test
    @DisplayName("Storage sync scan analyzes physical volume integrity")
    void testStorageSyncScan() throws Exception {
        mockMvc.perform(get("/api/storage/sync")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").isString())
                .andExpect(jsonPath("$.data.totalDbRecords").isNumber());
    }
}
