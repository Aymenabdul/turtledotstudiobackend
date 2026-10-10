package com.turtledot.studio.payment.dto;

import com.turtledot.studio.payment.Payment;
import com.turtledot.studio.payment.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class PaymentResponseDto {
    private Long id;
    private Long studioId;
    private Long clientId;
    private String clientName;
    private Long eventId;
    private String eventName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String paymentReference;
    private LocalDate paymentDate;
    private String notes;
    private String invoiceNumber;
    private Long createdBy;
    private Instant createdAt;

    public PaymentResponseDto() {}

    public static PaymentResponseDto fromEntity(Payment payment, String clientName, String eventName) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(payment.getId());
        dto.setStudioId(payment.getStudioId());
        dto.setClientId(payment.getClientId());
        dto.setClientName(clientName);
        dto.setEventId(payment.getEventId());
        dto.setEventName(eventName);
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentReference(payment.getPaymentReference());
        String inv = payment.getInvoiceNumber();
        if (inv == null || inv.isBlank()) {
            int year = payment.getPaymentDate() != null ? payment.getPaymentDate().getYear() : 2026;
            long pid = payment.getId() != null ? payment.getId() : 1L;
            inv = String.format("INV-%04d-%05d", year, pid);
        }
        dto.setInvoiceNumber(inv);
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setNotes(payment.getNotes());
        dto.setCreatedBy(payment.getCreatedBy());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
