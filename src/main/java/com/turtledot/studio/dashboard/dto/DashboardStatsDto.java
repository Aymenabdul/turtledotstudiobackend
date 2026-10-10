package com.turtledot.studio.dashboard.dto;

import java.math.BigDecimal;

public class DashboardStatsDto {
    private BigDecimal totalRevenue;
    private BigDecimal totalPending;
    private long totalClients;
    private long totalEvents;
    private long totalPhotos;
    private long totalVideos;
    private long totalStorageBytes;
    private long storageQuotaBytes;

    public DashboardStatsDto() {}

    // Getters and Setters
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getTotalPending() { return totalPending; }
    public void setTotalPending(BigDecimal totalPending) { this.totalPending = totalPending; }

    public long getTotalClients() { return totalClients; }
    public void setTotalClients(long totalClients) { this.totalClients = totalClients; }

    public long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }

    public long getTotalPhotos() { return totalPhotos; }
    public void setTotalPhotos(long totalPhotos) { this.totalPhotos = totalPhotos; }

    public long getTotalVideos() { return totalVideos; }
    public void setTotalVideos(long totalVideos) { this.totalVideos = totalVideos; }

    public long getTotalStorageBytes() { return totalStorageBytes; }
    public void setTotalStorageBytes(long totalStorageBytes) { this.totalStorageBytes = totalStorageBytes; }

    public long getStorageQuotaBytes() { return storageQuotaBytes; }
    public void setStorageQuotaBytes(long storageQuotaBytes) { this.storageQuotaBytes = storageQuotaBytes; }
}
