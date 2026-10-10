package com.turtledot.studio.qr.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class GuestPurchaseRequest {

    @NotNull(message = "Media ID is required")
    private Long mediaId;

    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private BigDecimal amount;
    private String paymentMethod = "UPI";
    private String paymentReference;

    public GuestPurchaseRequest() {}

    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
}
