package com.f9ld3.Zion.data;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private String description;
    private String creatorUid;
    private String creatorName;
    private String thumbnailUrl; // First media item thumbnail or custom
    private boolean isPublic; // true = public, false = private
    private List<String> mediaIds; // List of media IDs in playlist
    private int itemCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Required empty constructor for Firestore
    public Playlist() {
        this.mediaIds = new ArrayList<>();
        this.isPublic = true; // Default to public
    }

    public Playlist(String id, String name, String description, String creatorUid,
                    String creatorName, boolean isPublic) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creatorUid = creatorUid;
        this.creatorName = creatorName;
        this.isPublic = isPublic;
        this.mediaIds = new ArrayList<>();
        this.itemCount = 0;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatorUid() { return creatorUid; }
    public void setCreatorUid(String creatorUid) { this.creatorUid = creatorUid; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public List<String> getMediaIds() { return mediaIds; }
    public void setMediaIds(List<String> mediaIds) {
        this.mediaIds = mediaIds;
        this.itemCount = mediaIds != null ? mediaIds.size() : 0;
    }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public void addMedia(String mediaId) {
        if (mediaIds == null) {
            mediaIds = new ArrayList<>();
        }
        if (!mediaIds.contains(mediaId)) {
            mediaIds.add(mediaId);
            itemCount = mediaIds.size();
            updatedAt = Timestamp.now();
        }
    }

    public void removeMedia(String mediaId) {
        if (mediaIds != null && mediaIds.contains(mediaId)) {
            mediaIds.remove(mediaId);
            itemCount = mediaIds.size();
            updatedAt = Timestamp.now();
        }
    }

    public boolean containsMedia(String mediaId) {
        return mediaIds != null && mediaIds.contains(mediaId);
    }
}