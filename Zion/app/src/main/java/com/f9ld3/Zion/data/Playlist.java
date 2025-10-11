package com.f9ld3.Zion.data;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private String description;
    private List<String> mediaIds;
    private long createdAt;
    private long updatedAt;

    // Constructors
    public Playlist() {
        this.mediaIds = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Playlist(String name, String description) {
        this.name = name;
        this.description = description;
        this.mediaIds = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }

    public List<String> getMediaIds() { return mediaIds; }
    public void setMediaIds(List<String> mediaIds) {
        this.mediaIds = mediaIds;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public void addMedia(String mediaId) {
        if (mediaIds == null) {
            mediaIds = new ArrayList<>();
        }
        if (!mediaIds.contains(mediaId)) {
            mediaIds.add(mediaId);
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public void removeMedia(String mediaId) {
        if (mediaIds != null) {
            mediaIds.remove(mediaId);
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public int getMediaCount() {
        return mediaIds != null ? mediaIds.size() : 0;
    }
}