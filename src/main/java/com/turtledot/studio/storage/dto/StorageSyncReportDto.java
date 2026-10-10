package com.turtledot.studio.storage.dto;

import java.util.ArrayList;
import java.util.List;

public class StorageSyncReportDto {
    private long totalDbRecords;
    private long totalDiskFiles;
    private long totalDbBytes;
    private long totalDiskBytes;
    private String status; // HEALTHY, OUT_OF_SYNC
    private List<MissingFileDto> missingFiles = new ArrayList<>();
    private List<OrphanFileDto> orphanFiles = new ArrayList<>();

    public StorageSyncReportDto() {}

    public static class MissingFileDto {
        private Long mediaId;
        private String fileName;
        private String expectedPath;
        private Long clientId;
        private Long albumId;

        public MissingFileDto(Long mediaId, String fileName, String expectedPath, Long clientId, Long albumId) {
            this.mediaId = mediaId;
            this.fileName = fileName;
            this.expectedPath = expectedPath;
            this.clientId = clientId;
            this.albumId = albumId;
        }

        public Long getMediaId() { return mediaId; }
        public String getFileName() { return fileName; }
        public String getExpectedPath() { return expectedPath; }
        public Long getClientId() { return clientId; }
        public Long getAlbumId() { return albumId; }
    }

    public static class OrphanFileDto {
        private String fileName;
        private String relativePath;
        private long fileSize;
        private long lastModified;

        public OrphanFileDto(String fileName, String relativePath, long fileSize, long lastModified) {
            this.fileName = fileName;
            this.relativePath = relativePath;
            this.fileSize = fileSize;
            this.lastModified = lastModified;
        }

        public String getFileName() { return fileName; }
        public String getRelativePath() { return relativePath; }
        public long getFileSize() { return fileSize; }
        public long getLastModified() { return lastModified; }
    }

    // Getters and Setters
    public long getTotalDbRecords() { return totalDbRecords; }
    public void setTotalDbRecords(long totalDbRecords) { this.totalDbRecords = totalDbRecords; }

    public long getTotalDiskFiles() { return totalDiskFiles; }
    public void setTotalDiskFiles(long totalDiskFiles) { this.totalDiskFiles = totalDiskFiles; }

    public long getTotalDbBytes() { return totalDbBytes; }
    public void setTotalDbBytes(long totalDbBytes) { this.totalDbBytes = totalDbBytes; }

    public long getTotalDiskBytes() { return totalDiskBytes; }
    public void setTotalDiskBytes(long totalDiskBytes) { this.totalDiskBytes = totalDiskBytes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<MissingFileDto> getMissingFiles() { return missingFiles; }
    public void setMissingFiles(List<MissingFileDto> missingFiles) { this.missingFiles = missingFiles; }

    public List<OrphanFileDto> getOrphanFiles() { return orphanFiles; }
    public void setOrphanFiles(List<OrphanFileDto> orphanFiles) { this.orphanFiles = orphanFiles; }
}
