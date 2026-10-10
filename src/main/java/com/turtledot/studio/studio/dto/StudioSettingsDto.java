package com.turtledot.studio.studio.dto;

import com.turtledot.studio.studio.Studio;

public class StudioSettingsDto {
    private Long id;
    private String name;
    private String slug;
    private String email;
    private String phone;
    private String address;
    private String website;
    private String currency;
    private String timezone;
    private String logoUrl;
    private Integer maxUploadSizeMb;
    private Long storageQuotaBytes;
    private Boolean convertAmounts;
    private java.math.BigDecimal exchangeRate;
    private String gstin;
    private String panNumber;
    private String stateName;
    private String stateCode;
    private java.math.BigDecimal gstRatePercent;

    public StudioSettingsDto() {}

    public static StudioSettingsDto fromEntity(Studio studio) {
        StudioSettingsDto dto = new StudioSettingsDto();
        dto.setId(studio.getId());
        dto.setName(studio.getName());
        dto.setSlug(studio.getSlug());
        dto.setEmail(studio.getEmail());
        dto.setPhone(studio.getPhone());
        dto.setAddress(studio.getAddress());
        dto.setWebsite(studio.getWebsite());
        dto.setCurrency(studio.getCurrency());
        dto.setTimezone(studio.getTimezone());
        dto.setLogoUrl(studio.getLogoUrl());
        dto.setMaxUploadSizeMb(studio.getMaxUploadSizeMb());
        dto.setStorageQuotaBytes(107374182400L); // 100 GB
        dto.setGstin(studio.getGstin());
        dto.setPanNumber(studio.getPanNumber());
        dto.setStateName(studio.getStateName());
        dto.setStateCode(studio.getStateCode());
        dto.setGstRatePercent(studio.getGstRatePercent() != null ? studio.getGstRatePercent() : new java.math.BigDecimal("18.00"));
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public Integer getMaxUploadSizeMb() { return maxUploadSizeMb; }
    public void setMaxUploadSizeMb(Integer maxUploadSizeMb) { this.maxUploadSizeMb = maxUploadSizeMb; }

    public Long getStorageQuotaBytes() { return storageQuotaBytes; }
    public void setStorageQuotaBytes(Long storageQuotaBytes) { this.storageQuotaBytes = storageQuotaBytes; }

    public Boolean getConvertAmounts() { return convertAmounts; }
    public void setConvertAmounts(Boolean convertAmounts) { this.convertAmounts = convertAmounts; }

    public java.math.BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(java.math.BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

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
}
