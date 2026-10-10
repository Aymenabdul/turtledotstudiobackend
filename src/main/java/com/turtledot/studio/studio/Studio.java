package com.turtledot.studio.studio;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "studios")
public class Studio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 50)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 255)
    private String website;

    @Column(length = 10)
    private String currency = "USD";

    @Column(length = 50)
    private String timezone = "UTC";

    @Column(name = "max_upload_size_mb")
    private Integer maxUploadSizeMb = 10240;

    @Column(length = 20)
    private String gstin;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "state_name", length = 100)
    private String stateName;

    @Column(name = "state_code", length = 10)
    private String stateCode;

    @Column(name = "gst_rate_percent", precision = 5, scale = 2)
    private java.math.BigDecimal gstRatePercent = new java.math.BigDecimal("18.00");

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private StudioStatus status = StudioStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public Studio() {}

    public Studio(String name, String slug, String email, String phone) {
        this.name = name;
        this.slug = slug;
        this.email = email;
        this.phone = phone;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Integer getMaxUploadSizeMb() { return maxUploadSizeMb; }
    public void setMaxUploadSizeMb(Integer maxUploadSizeMb) { this.maxUploadSizeMb = maxUploadSizeMb; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public java.math.BigDecimal getGstRatePercent() { return gstRatePercent; }
    public void setGstRatePercent(java.math.BigDecimal gstRatePercent) { this.gstRatePercent = gstRatePercent; }

    public StudioStatus getStatus() { return status; }
    public void setStatus(StudioStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
