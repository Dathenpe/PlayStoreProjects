// main/java/com/f9ld3/Zion/ui/feed/MediaItem.java
package com.f9ld3.Zion.ui.feed;

import java.io.Serializable;
import java.util.Objects; // Import Objects for equals/hashCode

public class MediaItem implements Serializable {
    public String mediaType; // "image" or "video"
    public String url;
    public String thumbnailUrl; // Optional: Specifically for video thumbnails

    // Required for Firestore
    public MediaItem() {}

    public MediaItem(String mediaType, String url, String thumbnailUrl) {
        this.mediaType = mediaType;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
    }

    // --- Getters ---
    public String getMediaType() { return mediaType; }
    public String getUrl() { return url; }
    public String getThumbnailUrl() { return thumbnailUrl; }

    // --- Setters (Optional but good practice) ---
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public void setUrl(String url) { this.url = url; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    // --- equals() and hashCode() for List comparison ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaItem mediaItem = (MediaItem) o;
        return Objects.equals(mediaType, mediaItem.mediaType) &&
                Objects.equals(url, mediaItem.url) &&
                Objects.equals(thumbnailUrl, mediaItem.thumbnailUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaType, url, thumbnailUrl);
    }
}