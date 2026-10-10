package com.turtledot.studio.album;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "albums")
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    private Long studioId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "folder_path", length = 500)
    private String folderPath;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AlbumStatus status = AlbumStatus.ACTIVE;

    @Column(name = "is_commercial")
    private Boolean isCommercial = false;

    @Column(name = "price_per_photo", precision = 10, scale = 2)
    private BigDecimal pricePerPhoto;

    @Column(name = "allow_guest_purchases")
    private Boolean allowGuestPurchases = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public Album() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public AlbumStatus getStatus() { return status; }
    public void setStatus(AlbumStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getIsCommercial() { return isCommercial; }
    public void setIsCommercial(Boolean isCommercial) { this.isCommercial = isCommercial; }

    public BigDecimal getPricePerPhoto() { return pricePerPhoto; }
    public void setPricePerPhoto(BigDecimal pricePerPhoto) { this.pricePerPhoto = pricePerPhoto; }

    public Boolean getAllowGuestPurchases() { return allowGuestPurchases; }
    public void setAllowGuestPurchases(Boolean allowGuestPurchases) { this.allowGuestPurchases = allowGuestPurchases; }
}
