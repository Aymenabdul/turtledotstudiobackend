package com.turtledot.studio.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turtledot.studio.client.Client;
import com.turtledot.studio.client.ClientRepository;
import com.turtledot.studio.event.Event;
import com.turtledot.studio.event.EventRepository;
import com.turtledot.studio.qr.QrToken;
import com.turtledot.studio.qr.QrTokenRepository;
import com.turtledot.studio.qr.dto.GalleryPaymentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TieredPaymentGatingIntegrationTest {

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

    @Test
    @DisplayName("Verify 50% allows photos only and 80% unlocks full access including videos")
    void testTieredPaymentAccessFlow() throws Exception {
        // 1. Create client with package price $1000
        Client client = new Client();
        client.setStudioId(1L);
        client.setClientCode("CL-TIER-" + System.currentTimeMillis());
        client.setName("Tier Test Client");
        client.setEmail("tiertest@example.com");
        client.setTotalAmount(new BigDecimal("1000.00"));
        client = clientRepository.save(client);

        // 2. Create Event
        Event event = new Event();
        event.setStudioId(1L);
        event.setClientId(client.getId());
        event.setEventCode("EV-TIER-" + System.currentTimeMillis());
        event.setEventName("Tier Test Wedding");
        event.setEventType("Wedding");
        event.setEventDate(LocalDate.now());
        event = eventRepository.save(event);

        // 3. Create QR token for gallery access
        String tokenStr = "TIER_TOKEN_" + UUID.randomUUID().toString().substring(0, 8);
        QrToken qrToken = new QrToken(
                1L, client.getId(), event.getId(), tokenStr, tokenStr,
                Instant.now().plusSeconds(86400)
        );
        qrTokenRepository.save(qrToken);

        // Step A: 0% Paid -> Photos, Videos, and Downloads locked
        mockMvc.perform(get("/api/qr/gallery/" + tokenStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidPercentage", is(0.0)))
                .andExpect(jsonPath("$.data.photoLocked", is(true)))
                .andExpect(jsonPath("$.data.videoLocked", is(true)))
                .andExpect(jsonPath("$.data.downloadLocked", is(true)))
                .andExpect(jsonPath("$.data.amountNeededForVideoAccess", is(800.0)))
                .andExpect(jsonPath("$.data.amountNeededForDownload", is(1000.0)));

        // Step B: Pay $500 (50%) -> Photos unlocked, Videos STILL locked, Downloads STILL locked
        GalleryPaymentRequest pay50 = new GalleryPaymentRequest(
                tokenStr, new BigDecimal("500.00"), "CARD", "TXN-50PERCENT", "50%"
        );
        mockMvc.perform(post("/api/qr/gallery/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pay50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidPercentage", is(50.0)))
                .andExpect(jsonPath("$.data.photoLocked", is(false)))
                .andExpect(jsonPath("$.data.videoLocked", is(true)))
                .andExpect(jsonPath("$.data.downloadLocked", is(true)))
                .andExpect(jsonPath("$.data.amountNeededForVideoAccess", is(300.0)))
                .andExpect(jsonPath("$.data.amountNeededForDownload", is(500.0)));

        // Step C: Pay $300 more -> Total $800 (80%) -> Videos unlocked, Downloads STILL LOCKED!
        GalleryPaymentRequest pay80 = new GalleryPaymentRequest(
                tokenStr, new BigDecimal("300.00"), "UPI", "TXN-80PERCENT", "80%"
        );
        mockMvc.perform(post("/api/qr/gallery/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pay80)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidPercentage", is(80.0)))
                .andExpect(jsonPath("$.data.photoLocked", is(false)))
                .andExpect(jsonPath("$.data.videoLocked", is(false)))
                .andExpect(jsonPath("$.data.downloadLocked", is(true)))
                .andExpect(jsonPath("$.data.amountNeededForVideoAccess", org.hamcrest.Matchers.closeTo(0.0, 0.01)))
                .andExpect(jsonPath("$.data.amountNeededForDownload", is(200.0)));

        // Step D: Pay remaining $200 -> Total $1000 (100%) -> DOWNLOADS FULLY UNLOCKED!
        GalleryPaymentRequest pay100 = new GalleryPaymentRequest(
                tokenStr, new BigDecimal("200.00"), "BANK_TRANSFER", "TXN-100PERCENT", "100%"
        );
        mockMvc.perform(post("/api/qr/gallery/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pay100)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidPercentage", is(100.0)))
                .andExpect(jsonPath("$.data.photoLocked", is(false)))
                .andExpect(jsonPath("$.data.videoLocked", is(false)))
                .andExpect(jsonPath("$.data.downloadLocked", is(false)))
                .andExpect(jsonPath("$.data.amountNeededForDownload", is(0.0)));
    }
}
