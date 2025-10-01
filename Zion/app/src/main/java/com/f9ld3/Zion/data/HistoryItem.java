package com.f9ld3.Zion.data;

import com.google.firebase.Timestamp;

/**
 * Data model for an item in a user's viewing history.
 */
public class HistoryItem {
    public String mediaId;
    public String mediaTitle;
    public int mediaType;
    public String uploaderName;
    public String thumbnailUrl;
    public Timestamp viewedAt;

    // Required for Firestore automatic object mapping
    public HistoryItem() {}

    public HistoryItem(String mediaId, String mediaTitle, int mediaType, String uploaderName, String thumbnailUrl, Timestamp viewedAt) {
        this.mediaId = mediaId;
        this.mediaTitle = mediaTitle;
        this.mediaType = mediaType;
        this.uploaderName = uploaderName;
        this.thumbnailUrl = thumbnailUrl;
        this.viewedAt = viewedAt;
    }

    // Getters for Firestore and usage
    public String getMediaId() { return mediaId; }
    public String getMediaTitle() { return mediaTitle; }
    public int getMediaType() { return mediaType; }
    public String getUploaderName() { return uploaderName; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public Timestamp getViewedAt() { return viewedAt; }
}