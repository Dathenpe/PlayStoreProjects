package com.f9ld3.Zion.ui.feed;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Post implements Serializable {
    public static final int MEDIA_TYPE_TEXT = 1;
    public static final int MEDIA_TYPE_IMAGE = 2;
    public static final int MEDIA_TYPE_VIDEO = 3;

    public String id;
    public String authorUid;
    public String authorName;
    public String authorAvatarUrl;
    public long timestamp;
    public String textContent;
    public List<MediaItem> mediaItems = new ArrayList<>();
    public int likeCount = 0; // NEW
    public int commentCount = 0; // NEW

    public Post() {}

    public Post(String id, String authorUid, String authorName, String authorAvatarUrl, String textContent, List<MediaItem> mediaItems) {
        this.id = id;
        this.authorUid = authorUid;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.timestamp = Timestamp.now().getSeconds();
        this.textContent = textContent;
        this.mediaItems = mediaItems;
    }

    // --- GETTERS ---
    public String getId() { return id; }
    public String getAuthorUid() { return authorUid; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public long getTimestamp() { return timestamp; }
    public String getTextContent() { return textContent; }
    public List<MediaItem> getMediaItems() { return mediaItems; }
    public int getLikeCount() { return likeCount; } // NEW
    public int getCommentCount() { return commentCount; } // NEW


    public void setId(String id) { this.id = id; }

    public int getMediaType() {
        if (mediaItems == null || mediaItems.isEmpty()) {
            return MEDIA_TYPE_TEXT;
        }
        MediaItem firstItem = mediaItems.get(0);
        if ("video".equals(firstItem.getMediaType())) {
            return MEDIA_TYPE_VIDEO;
        }
        if ("image".equals(firstItem.getMediaType())) {
            return MEDIA_TYPE_IMAGE;
        }
        return MEDIA_TYPE_TEXT;
    }

    public String getMediaUrl() {
        if (mediaItems != null && !mediaItems.isEmpty()) {
            return mediaItems.get(0).getUrl();
        }
        return null;
    }

    public String getThumbnailUrl() {
        if (mediaItems != null && !mediaItems.isEmpty()) {
            MediaItem firstItem = mediaItems.get(0);
            if ("video".equals(firstItem.getMediaType()) && firstItem.getThumbnailUrl() != null) {
                return firstItem.getThumbnailUrl();
            }
            return firstItem.getUrl();
        }
        return null;
    }
}