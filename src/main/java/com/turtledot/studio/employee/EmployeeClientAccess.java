package com.turtledot.studio.employee;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "employee_client_access")
public class EmployeeClientAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "assigned_at", updatable = false)
    private Instant assignedAt = Instant.now();

    @Column(name = "assigned_by")
    private Long assignedBy;

    public EmployeeClientAccess() {}

    public EmployeeClientAccess(Long studioId, Long employeeId, Long clientId, Long eventId, Long assignedBy) {
        this.studioId = studioId;
        this.employeeId = employeeId;
        this.clientId = clientId;
        this.eventId = eventId;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public Long getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Long assignedBy) { this.assignedBy = assignedBy; }
}
