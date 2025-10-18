package com.f9ld3.Zion.ui.feed;

import java.io.Serializable;

public class MediaItem implements Serializable {
    public String mediaType; // "image" or "video"
    public String url;
    public String thumbnailUrl; // Only for videos

    // Required for Firestore
    public MediaItem() {}

    public MediaItem(String mediaType, String url, String thumbnailUrl) {
        this.mediaType = mediaType;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
    }

    // Getters
    public String getMediaType() { return mediaType; }
    public String getUrl() { return url; }
    public String getThumbnailUrl() { return thumbnailUrl; }
}