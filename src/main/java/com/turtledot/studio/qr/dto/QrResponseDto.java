package com.turtledot.studio.qr.dto;

import com.turtledot.studio.qr.QrTokenStatus;
import java.time.Instant;

public class QrResponseDto {
    private Long id;
    private Long eventId;
    private String eventName;
    private Long clientId;
    private String clientName;
    private String tokenValue;
    private String galleryUrl;
    private String guestGalleryUrl;
    private Boolean hasCommercialAlbums = false;
    private QrTokenStatus status;
    private Instant createdAt;
    private Instant lastUsedAt;

    public QrResponseDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getTokenValue() { return tokenValue; }
    public void setTokenValue(String tokenValue) { this.tokenValue = tokenValue; }

    public String getGalleryUrl() { return galleryUrl; }
    public void setGalleryUrl(String galleryUrl) { this.galleryUrl = galleryUrl; }

    public String getGuestGalleryUrl() { return guestGalleryUrl; }
    public void setGuestGalleryUrl(String guestGalleryUrl) { this.guestGalleryUrl = guestGalleryUrl; }

    public Boolean getHasCommercialAlbums() { return hasCommercialAlbums; }
    public void setHasCommercialAlbums(Boolean hasCommercialAlbums) { this.hasCommercialAlbums = hasCommercialAlbums; }

    public QrTokenStatus getStatus() { return status; }
    public void setStatus(QrTokenStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
