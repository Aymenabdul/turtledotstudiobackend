package com.turtledot.studio.employee.dto;

import jakarta.validation.constraints.NotBlank;

public class EmployeeUpdateRequest {

    @NotBlank(message = "Employee name is required")
    private String fullName;

    private String phone;
    private String designation;
    private String department;
    private String notes;

    public EmployeeUpdateRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
