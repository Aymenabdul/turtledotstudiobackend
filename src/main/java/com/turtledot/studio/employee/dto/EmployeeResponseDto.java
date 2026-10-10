package com.turtledot.studio.employee.dto;

import com.turtledot.studio.user.UserStatus;
import java.time.Instant;
import java.util.List;

public class EmployeeResponseDto {
    private Long id;
    private Long studioId;
    private Long userId;
    private String employeeCode;
    private String fullName;
    private String email;
    private String phone;
    private String designation;
    private String department;
    private String notes;
    private UserStatus status;
    private List<String> permissions;
    private List<Long> assignedClientIds;
    private int assignedClientsCount;
    private Instant createdAt;

    public EmployeeResponseDto() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    public List<Long> getAssignedClientIds() { return assignedClientIds; }
    public void setAssignedClientIds(List<Long> assignedClientIds) { this.assignedClientIds = assignedClientIds; }

    public int getAssignedClientsCount() { return assignedClientsCount; }
    public void setAssignedClientsCount(int assignedClientsCount) { this.assignedClientsCount = assignedClientsCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
