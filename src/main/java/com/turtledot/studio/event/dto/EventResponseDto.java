package com.turtledot.studio.event.dto;

import com.turtledot.studio.event.Event;
import com.turtledot.studio.event.EventStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventResponseDto {
    private Long id;
    private Long studioId;
    private Long clientId;
    private String clientName;
    private String clientCode;
    private String eventCode;
    private String eventName;
    private String eventType;
    private LocalDate eventDate;
    private String location;
    private String description;
    private EventStatus status;
    private int albumCount;
    private int mediaCount;
    private String qrToken;
    private List<Long> assignedEmployeeIds = new ArrayList<>();
    private List<String> assignedEmployeeNames = new ArrayList<>();
    private Boolean crewConfigured;
    private Instant createdAt;
    private Instant updatedAt;

    public EventResponseDto() {}

    public static EventResponseDto fromEntity(Event event, String clientName) {
        return fromEntity(event, clientName, null, 0, 0, null);
    }

    public static EventResponseDto fromEntity(Event event, String clientName, String clientCode, int albumCount, int mediaCount, String qrToken) {
        EventResponseDto dto = new EventResponseDto();
        dto.setId(event.getId());
        dto.setStudioId(event.getStudioId());
        dto.setClientId(event.getClientId());
        dto.setClientName(clientName);
        dto.setClientCode(clientCode);
        dto.setEventCode(event.getEventCode());
        dto.setEventName(event.getEventName());
        dto.setEventType(event.getEventType());
        dto.setEventDate(event.getEventDate());
        dto.setLocation(event.getLocation());
        dto.setDescription(event.getDescription());
        dto.setStatus(event.getStatus());
        dto.setAlbumCount(albumCount);
        dto.setMediaCount(mediaCount);
        dto.setQrToken(qrToken);
        dto.setCrewConfigured(event.getCrewConfigured());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setUpdatedAt(event.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientCode() { return clientCode; }
    public void setClientCode(String clientCode) { this.clientCode = clientCode; }

    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public int getAlbumCount() { return albumCount; }
    public void setAlbumCount(int albumCount) { this.albumCount = albumCount; }

    public int getMediaCount() { return mediaCount; }
    public void setMediaCount(int mediaCount) { this.mediaCount = mediaCount; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }

    public List<Long> getAssignedEmployeeIds() { return assignedEmployeeIds; }
    public void setAssignedEmployeeIds(List<Long> assignedEmployeeIds) { this.assignedEmployeeIds = assignedEmployeeIds != null ? assignedEmployeeIds : new ArrayList<>(); }

    public List<String> getAssignedEmployeeNames() { return assignedEmployeeNames; }
    public void setAssignedEmployeeNames(List<String> assignedEmployeeNames) { this.assignedEmployeeNames = assignedEmployeeNames != null ? assignedEmployeeNames : new ArrayList<>(); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getCrewConfigured() { return crewConfigured; }
    public void setCrewConfigured(Boolean crewConfigured) { this.crewConfigured = crewConfigured; }
}
