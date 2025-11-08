package com.f9ld3.Zion.ui.notifications;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import java.util.Map;

/**
 * Represents a single notification item from the Firestore 'notifications' collection.
 */
public class Notification {
    @DocumentId
    private String id;
    private String targetUserId;
    private String type; // "follow", "post_like", "comment_like", "comment_reply", "post_comment"
    private String title;
    private String message;
    private Timestamp timestamp;
    private boolean read;
    private Map<String, Object> data; // Contains IDs like postId, commentId, followerId, etc.

    // Required for Firestore
    public Notification() {}

    // Getters
    public String getId() { return id; }
    public String getTargetUserId() { return targetUserId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
    public Map<String, Object> getData() { return data; }

    // Setters (needed for Firestore mapping)
    public void setId(String id) { this.id = id; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { this.read = read; }
    public void setData(Map<String, Object> data) { this.data = data; }
}