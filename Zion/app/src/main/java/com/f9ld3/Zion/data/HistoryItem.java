package com.f9ld3.Zion.data;

public class HistoryItem {
    private String id;
    private String mediaId;
    private String mediaTitle;
    private String mediaType; // "video", "podcast", "blog"
    private String thumbnailUrl;
    private String uploaderName;
    private long viewedAt;
    private int watchProgress; // Percentage (0-100) or seconds watched

    // Constructors
    public HistoryItem() {}

    public HistoryItem(String mediaId, String mediaTitle, String mediaType) {
        this.mediaId = mediaId;
        this.mediaTitle = mediaTitle;
        this.mediaType = mediaType;
        this.viewedAt = System.currentTimeMillis();
        this.watchProgress = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }

    public String getMediaTitle() { return mediaTitle; }
    public void setMediaTitle(String mediaTitle) { this.mediaTitle = mediaTitle; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }

    public long getViewedAt() { return viewedAt; }
    public void setViewedAt(long viewedAt) { this.viewedAt = viewedAt; }

    public int getWatchProgress() { return watchProgress; }
    public void setWatchProgress(int watchProgress) { this.watchProgress = watchProgress; }
}