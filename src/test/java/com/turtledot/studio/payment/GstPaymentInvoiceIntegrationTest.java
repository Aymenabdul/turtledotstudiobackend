package com.turtledot.studio.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turtledot.studio.auth.dto.LoginRequest;
import com.turtledot.studio.client.Client;
import com.turtledot.studio.client.ClientRepository;
import com.turtledot.studio.event.Event;
import com.turtledot.studio.event.EventRepository;
import com.turtledot.studio.payment.dto.CreatePaymentRequest;
import com.turtledot.studio.qr.QrToken;
import com.turtledot.studio.qr.QrTokenRepository;
import com.turtledot.studio.qr.dto.GalleryPaymentRequest;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
public class GstPaymentInvoiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private QrTokenRepository qrTokenRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private com.turtledot.studio.studio.StudioRepository studioRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        com.turtledot.studio.studio.Studio studio = studioRepository.findById(1L).orElse(null);
        if (studio != null) {
            studio.setCurrency("INR");
            studioRepository.save(studio);
        }

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
    @DisplayName("Admin can record payment and download official GST tax invoice as PDF")
    void testAdminDownloadPaymentInvoicePdf() throws Exception {
        // 1. Record a payment
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setClientId(1L);
        req.setAmount(new BigDecimal("108258.16"));
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setPaymentReference("PAY-1790173835690");
        req.setPaymentDate(LocalDate.now());
        req.setNotes("Automated gallery payment (100%)");

        MvcResult createRes = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long paymentId = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Download the GST invoice as PDF
        MvcResult pdfRes = mockMvc.perform(get("/api/payments/" + paymentId + "/invoice")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("GST_Invoice_INV_" + paymentId + ".pdf")))
                .andReturn();

        byte[] pdfBytes = pdfRes.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(500);

        // Verify PDF Magic Bytes "%PDF-"
        String headerStr = new String(pdfBytes, 0, Math.min(10, pdfBytes.length));
        assertThat(headerStr).startsWith("%PDF-");
    }

    @Test
    @DisplayName("Client can pay through gallery and download official GST tax invoice as PDF")
    void testClientDownloadGalleryInvoicePdf() throws Exception {
        // 1. Setup Client, Event, and QR Token
        Client client = new Client();
        client.setStudioId(1L);
        client.setClientCode("CL-GST-" + System.currentTimeMillis());
        client.setName("Priya Sharma");
        client.setEmail("priya.sharma@example.com");
        client.setPhone("+91 98765 43210");
        client.setTotalAmount(new BigDecimal("25000.00"));
        client = clientRepository.save(client);

        Event event = new Event();
        event.setStudioId(1L);
        event.setClientId(client.getId());
        event.setEventCode("EV-GST-" + System.currentTimeMillis());
        event.setEventName("Priya & Rahul Wedding");
        event.setEventType("Wedding");
        event.setEventDate(LocalDate.now());
        event = eventRepository.save(event);

        String tokenStr = "GST_TOKEN_" + UUID.randomUUID().toString().substring(0, 8);
        QrToken qrToken = new QrToken(
                1L, client.getId(), event.getId(), tokenStr, tokenStr,
                Instant.now().plusSeconds(86400)
        );
        qrTokenRepository.save(qrToken);

        // 2. Client makes payment via gallery
        GalleryPaymentRequest payReq = new GalleryPaymentRequest(
                tokenStr, new BigDecimal("12500.00"), "UPI", "UPI-GALLERY-GST-88", "50%"
        );
        mockMvc.perform(post("/api/qr/gallery/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payments").isArray())
                .andExpect(jsonPath("$.data.payments[0].amount").value(12500.0));

        // 3. Client downloads latest GST invoice without login (via QR token)
        MvcResult latestPdfRes = mockMvc.perform(get("/api/qr/gallery/" + tokenStr + "/invoice"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("GST_Invoice_" + tokenStr + ".pdf")))
                .andReturn();

        byte[] latestPdfBytes = latestPdfRes.getResponse().getContentAsByteArray();
        assertThat(latestPdfBytes).isNotNull();
        assertThat(new String(latestPdfBytes, 0, Math.min(10, latestPdfBytes.length))).startsWith("%PDF-");
    }

    @Test
    @DisplayName("Number to words conversion correctly translates currency and values")
    void testNumberToWordsConverter() {
        String inrWords = NumberToWordsConverter.convert(new BigDecimal("25000.50"), "INR");
        assertThat(inrWords).isEqualTo("Twenty Five Thousand Rupees and Fifty Paise Only");

        String usdWords = NumberToWordsConverter.convert(new BigDecimal("1500.00"), "USD");
        assertThat(usdWords).isEqualTo("One Thousand Five Hundred US Dollars Only");
    }
}
