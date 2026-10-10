package com.turtledot.studio.auth.dto;

import com.turtledot.studio.user.Role;
import com.turtledot.studio.user.UserStatus;

public class UserSummaryDto {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private UserStatus status;
    private Long studioId;
    private String studioName;
    private String studioSlug;
    private String studioLogoUrl;
    private String studioCurrency;
    private java.util.List<String> permissions;

    public UserSummaryDto() {}

    public UserSummaryDto(Long id, String email, String fullName, String phone, Role role, UserStatus status,
                          Long studioId, String studioName, String studioSlug, String studioLogoUrl, String studioCurrency) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.studioId = studioId;
        this.studioName = studioName;
        this.studioSlug = studioSlug;
        this.studioLogoUrl = studioLogoUrl;
        this.studioCurrency = studioCurrency;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }

    public String getStudioName() { return studioName; }
    public void setStudioName(String studioName) { this.studioName = studioName; }

    public String getStudioSlug() { return studioSlug; }
    public void setStudioSlug(String studioSlug) { this.studioSlug = studioSlug; }

    public String getStudioLogoUrl() { return studioLogoUrl; }
    public void setStudioLogoUrl(String studioLogoUrl) { this.studioLogoUrl = studioLogoUrl; }

    public String getStudioCurrency() { return studioCurrency; }
    public void setStudioCurrency(String studioCurrency) { this.studioCurrency = studioCurrency; }

    public java.util.List<String> getPermissions() { return permissions; }
    public void setPermissions(java.util.List<String> permissions) { this.permissions = permissions; }
}
