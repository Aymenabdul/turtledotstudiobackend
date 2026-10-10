package com.turtledot.studio.media.dto;

import com.turtledot.studio.media.Media;
import com.turtledot.studio.media.MediaType;
import java.time.Instant;

public class MediaResponseDto {
    private Long id;
    private Long studioId;
    private Long clientId;
    private Long eventId;
    private Long albumId;
    private MediaType mediaType;
    private String fileName;
    private String fileUrl;
    private String thumbnailUrl;
    private String previewUrl;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Long duration;
    private Boolean isFavorite;
    private Boolean locked = false;
    private Boolean downloadLocked = false;
    private Instant createdAt;

    public MediaResponseDto() {}

    public static MediaResponseDto fromEntity(Media media) {
        MediaResponseDto dto = new MediaResponseDto();
        dto.setId(media.getId());
        dto.setStudioId(media.getStudioId());
        dto.setClientId(media.getClientId());
        dto.setEventId(media.getEventId());
        dto.setAlbumId(media.getAlbumId());
        dto.setMediaType(media.getMediaType());
        dto.setFileName(media.getFileName());
        dto.setFileUrl(media.getFileUrl());
        dto.setThumbnailUrl(media.getThumbnailUrl());
        dto.setPreviewUrl(media.getPreviewUrl());
        dto.setMimeType(media.getMimeType());
        dto.setFileSize(media.getFileSize());
        dto.setWidth(media.getWidth());
        dto.setHeight(media.getHeight());
        dto.setDuration(media.getDuration());
        dto.setIsFavorite(media.getIsFavorite());
        dto.setCreatedAt(media.getCreatedAt());
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

    public Long getAlbumId() { return albumId; }
    public void setAlbumId(Long albumId) { this.albumId = albumId; }

    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }

    public Boolean getLocked() { return locked; }
    public void setLocked(Boolean locked) { this.locked = locked; }

    public Boolean getDownloadLocked() { return downloadLocked; }
    public void setDownloadLocked(Boolean downloadLocked) { this.downloadLocked = downloadLocked; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
