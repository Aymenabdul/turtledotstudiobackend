package com.turtledot.studio.qr.dto;

import com.turtledot.studio.media.dto.MediaResponseDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ClientGalleryDto {
    private String studioName;
    private String studioLogoUrl;
    private String studioPhone;
    private String studioEmail;
    private Long clientId;
    private String clientName;
    private Long eventId;
    private String eventName;
    private String eventType;
    private LocalDate eventDate;
    private String location;
    private String description;
    private List<GalleryAlbumDto> albums;
    private int totalPhotos;
    private int totalVideos;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal balanceAmount;
    private double paidPercentage;
    private boolean videoLocked;
    private boolean photoLocked;
    private boolean downloadLocked;
    private BigDecimal amountNeededForVideoAccess;
    private BigDecimal amountNeededForDownload;
    private BigDecimal amountNeededForFullAccess;
    private String currency;

    public ClientGalleryDto() {}

    public static class GalleryAlbumDto {
        private Long id;
        private String name;
        private String description;
        private String coverImageUrl;
        private List<MediaResponseDto> media;
        private Boolean isCommercial = false;
        private BigDecimal pricePerPhoto;
        private Boolean allowGuestPurchases = true;

        public GalleryAlbumDto() {}

        public GalleryAlbumDto(Long id, String name, String description, String coverImageUrl, List<MediaResponseDto> media) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.coverImageUrl = coverImageUrl;
            this.media = media;
        }

        public GalleryAlbumDto(Long id, String name, String description, String coverImageUrl, List<MediaResponseDto> media,
                               Boolean isCommercial, BigDecimal pricePerPhoto, Boolean allowGuestPurchases) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.coverImageUrl = coverImageUrl;
            this.media = media;
            this.isCommercial = isCommercial != null ? isCommercial : false;
            this.pricePerPhoto = pricePerPhoto;
            this.allowGuestPurchases = allowGuestPurchases != null ? allowGuestPurchases : true;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCoverImageUrl() { return coverImageUrl; }
        public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

        public List<MediaResponseDto> getMedia() { return media; }
        public void setMedia(List<MediaResponseDto> media) { this.media = media; }

        public Boolean getIsCommercial() { return isCommercial; }
        public void setIsCommercial(Boolean isCommercial) { this.isCommercial = isCommercial; }

        public BigDecimal getPricePerPhoto() { return pricePerPhoto; }
        public void setPricePerPhoto(BigDecimal pricePerPhoto) { this.pricePerPhoto = pricePerPhoto; }

        public Boolean getAllowGuestPurchases() { return allowGuestPurchases; }
        public void setAllowGuestPurchases(Boolean allowGuestPurchases) { this.allowGuestPurchases = allowGuestPurchases; }
    }

    public static class GalleryPaymentDto {
        private Long id;
        private String invoiceNumber;
        private BigDecimal amount;
        private String paymentMethod;
        private String paymentReference;
        private LocalDate paymentDate;
        private String notes;

        public GalleryPaymentDto() {}

        public GalleryPaymentDto(Long id, String invoiceNumber, BigDecimal amount, String paymentMethod, String paymentReference, LocalDate paymentDate, String notes) {
            this.id = id;
            this.invoiceNumber = invoiceNumber;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.paymentReference = paymentReference;
            this.paymentDate = paymentDate;
            this.notes = notes;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getInvoiceNumber() { return invoiceNumber; }
        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public String getPaymentReference() { return paymentReference; }
        public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

        public LocalDate getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    private List<GalleryPaymentDto> payments = new java.util.ArrayList<>();

    // Getters and Setters
    public String getStudioName() { return studioName; }
    public void setStudioName(String studioName) { this.studioName = studioName; }

    public String getStudioLogoUrl() { return studioLogoUrl; }
    public void setStudioLogoUrl(String studioLogoUrl) { this.studioLogoUrl = studioLogoUrl; }

    public String getStudioPhone() { return studioPhone; }
    public void setStudioPhone(String studioPhone) { this.studioPhone = studioPhone; }

    public String getStudioEmail() { return studioEmail; }
    public void setStudioEmail(String studioEmail) { this.studioEmail = studioEmail; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

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

    public List<GalleryAlbumDto> getAlbums() { return albums; }
    public void setAlbums(List<GalleryAlbumDto> albums) { this.albums = albums; }

    public int getTotalPhotos() { return totalPhotos; }
    public void setTotalPhotos(int totalPhotos) { this.totalPhotos = totalPhotos; }

    public int getTotalVideos() { return totalVideos; }
    public void setTotalVideos(int totalVideos) { this.totalVideos = totalVideos; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(BigDecimal balanceAmount) { this.balanceAmount = balanceAmount; }

    public double getPaidPercentage() { return paidPercentage; }
    public void setPaidPercentage(double paidPercentage) { this.paidPercentage = paidPercentage; }

    public boolean isVideoLocked() { return videoLocked; }
    public void setVideoLocked(boolean videoLocked) { this.videoLocked = videoLocked; }

    public boolean isPhotoLocked() { return photoLocked; }
    public void setPhotoLocked(boolean photoLocked) { this.photoLocked = photoLocked; }

    public boolean isDownloadLocked() { return downloadLocked; }
    public void setDownloadLocked(boolean downloadLocked) { this.downloadLocked = downloadLocked; }

    public BigDecimal getAmountNeededForVideoAccess() { return amountNeededForVideoAccess; }
    public void setAmountNeededForVideoAccess(BigDecimal amountNeededForVideoAccess) { this.amountNeededForVideoAccess = amountNeededForVideoAccess; }

    public BigDecimal getAmountNeededForDownload() { return amountNeededForDownload; }
    public void setAmountNeededForDownload(BigDecimal amountNeededForDownload) { this.amountNeededForDownload = amountNeededForDownload; }

    public BigDecimal getAmountNeededForFullAccess() { return amountNeededForFullAccess; }
    public void setAmountNeededForFullAccess(BigDecimal amountNeededForFullAccess) { this.amountNeededForFullAccess = amountNeededForFullAccess; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public List<GalleryPaymentDto> getPayments() { return payments; }
    public void setPayments(List<GalleryPaymentDto> payments) { this.payments = payments; }
}
