package com.turtledot.studio.qr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class GalleryPaymentRequest {

    @NotBlank(message = "Gallery token is required")
    private String tokenValue;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    private String paymentMethod;
    private String paymentReference;
    private String tierTarget; // e.g. "80%" or "100%"

    public GalleryPaymentRequest() {}

    public GalleryPaymentRequest(String tokenValue, BigDecimal amount, String paymentMethod, String paymentReference, String tierTarget) {
        this.tokenValue = tokenValue;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.tierTarget = tierTarget;
    }

    public String getTokenValue() { return tokenValue; }
    public void setTokenValue(String tokenValue) { this.tokenValue = tokenValue; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getTierTarget() { return tierTarget; }
    public void setTierTarget(String tierTarget) { this.tierTarget = tierTarget; }
}
