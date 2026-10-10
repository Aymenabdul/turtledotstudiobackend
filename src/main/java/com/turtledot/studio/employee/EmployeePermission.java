package com.turtledot.studio.employee;

import jakarta.persistence.*;

@Entity
@Table(name = "employee_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "permission"})
})
public class EmployeePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 100)
    private String permission;

    public EmployeePermission() {}

    public EmployeePermission(Long studioId, Long employeeId, String permission) {
        this.studioId = studioId;
        this.employeeId = employeeId;
        this.permission = permission;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
}
