package com.turtledot.studio.album.dto;

import com.turtledot.studio.album.AlbumStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AlbumRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotBlank(message = "Album name is required")
    private String name;

    private String description;
    private String coverImageUrl;
    private AlbumStatus status = AlbumStatus.ACTIVE;
    private Boolean isCommercial = false;
    private BigDecimal pricePerPhoto;
    private Boolean allowGuestPurchases = true;

    public AlbumRequest() {}

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public AlbumStatus getStatus() { return status; }
    public void setStatus(AlbumStatus status) { this.status = status; }

    public Boolean getIsCommercial() { return isCommercial; }
    public void setIsCommercial(Boolean isCommercial) { this.isCommercial = isCommercial; }

    public BigDecimal getPricePerPhoto() { return pricePerPhoto; }
    public void setPricePerPhoto(BigDecimal pricePerPhoto) { this.pricePerPhoto = pricePerPhoto; }

    public Boolean getAllowGuestPurchases() { return allowGuestPurchases; }
    public void setAllowGuestPurchases(Boolean allowGuestPurchases) { this.allowGuestPurchases = allowGuestPurchases; }
}
