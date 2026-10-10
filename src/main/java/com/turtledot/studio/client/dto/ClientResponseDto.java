package com.turtledot.studio.client.dto;

import com.turtledot.studio.client.Client;
import com.turtledot.studio.client.ClientStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class ClientResponseDto {
    private Long id;
    private Long studioId;
    private String clientCode;
    private String name;
    private String email;
    private String phone;
    private String alternatePhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String profileImageUrl;
    private ClientStatus status;
    private String notes;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal balanceAmount;
    private int eventCount;
    private Instant createdAt;
    private Instant updatedAt;

    public ClientResponseDto() {}

    public static ClientResponseDto fromEntity(Client client, BigDecimal amountPaid, int eventCount) {
        ClientResponseDto dto = new ClientResponseDto();
        dto.setId(client.getId());
        dto.setStudioId(client.getStudioId());
        dto.setClientCode(client.getClientCode());
        dto.setName(client.getName());
        dto.setEmail(client.getEmail());
        dto.setPhone(client.getPhone());
        dto.setAlternatePhone(client.getAlternatePhone());
        dto.setAddress(client.getAddress());
        dto.setCity(client.getCity());
        dto.setState(client.getState());
        dto.setCountry(client.getCountry());
        dto.setProfileImageUrl(client.getProfileImageUrl());
        dto.setStatus(client.getStatus());
        dto.setNotes(client.getNotes());
        
        BigDecimal total = client.getTotalAmount() != null ? client.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        BigDecimal balance = total.subtract(paid);

        dto.setTotalAmount(total);
        dto.setAmountPaid(paid);
        dto.setBalanceAmount(balance);
        dto.setEventCount(eventCount);
        dto.setCreatedAt(client.getCreatedAt());
        dto.setUpdatedAt(client.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }

    public String getClientCode() { return clientCode; }
    public void setClientCode(String clientCode) { this.clientCode = clientCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAlternatePhone() { return alternatePhone; }
    public void setAlternatePhone(String alternatePhone) { this.alternatePhone = alternatePhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public ClientStatus getStatus() { return status; }
    public void setStatus(ClientStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(BigDecimal balanceAmount) { this.balanceAmount = balanceAmount; }

    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
