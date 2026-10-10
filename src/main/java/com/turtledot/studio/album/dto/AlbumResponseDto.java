package com.turtledot.studio.album.dto;

import com.turtledot.studio.album.Album;
import com.turtledot.studio.album.AlbumStatus;
import java.time.Instant;

public class AlbumResponseDto {
    private Long id;
    private Long studioId;
    private Long clientId;
    private Long eventId;
    private String eventName;
    private String name;
    private String description;
    private String coverImageUrl;
    private AlbumStatus status;
    private int photoCount;
    private int videoCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isCommercial;
    private java.math.BigDecimal pricePerPhoto;
    private Boolean allowGuestPurchases;

    public AlbumResponseDto() {}

    public static AlbumResponseDto fromEntity(Album album, String eventName, int photoCount, int videoCount) {
        AlbumResponseDto dto = new AlbumResponseDto();
        dto.setId(album.getId());
        dto.setStudioId(album.getStudioId());
        dto.setClientId(album.getClientId());
        dto.setEventId(album.getEventId());
        dto.setEventName(eventName);
        dto.setName(album.getName());
        dto.setDescription(album.getDescription());
        dto.setCoverImageUrl(album.getCoverImageUrl());
        dto.setStatus(album.getStatus());
        dto.setPhotoCount(photoCount);
        dto.setVideoCount(videoCount);
        dto.setCreatedAt(album.getCreatedAt());
        dto.setUpdatedAt(album.getUpdatedAt());
        dto.setIsCommercial(album.getIsCommercial());
        dto.setPricePerPhoto(album.getPricePerPhoto());
        dto.setAllowGuestPurchases(album.getAllowGuestPurchases());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public AlbumStatus getStatus() { return status; }
    public void setStatus(AlbumStatus status) { this.status = status; }

    public int getPhotoCount() { return photoCount; }
    public void setPhotoCount(int photoCount) { this.photoCount = photoCount; }

    public int getVideoCount() { return videoCount; }
    public void setVideoCount(int videoCount) { this.videoCount = videoCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getIsCommercial() { return isCommercial; }
    public void setIsCommercial(Boolean isCommercial) { this.isCommercial = isCommercial; }

    public java.math.BigDecimal getPricePerPhoto() { return pricePerPhoto; }
    public void setPricePerPhoto(java.math.BigDecimal pricePerPhoto) { this.pricePerPhoto = pricePerPhoto; }

    public Boolean getAllowGuestPurchases() { return allowGuestPurchases; }
    public void setAllowGuestPurchases(Boolean allowGuestPurchases) { this.allowGuestPurchases = allowGuestPurchases; }
}
