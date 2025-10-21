// main/java/com/f9ld3/Zion/ui/feed/Comment.java
package com.f9ld3.Zion.ui.feed;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude; // <-- IMPORT THIS
import com.google.firebase.firestore.ServerTimestamp; // Import ServerTimestamp

import java.util.ArrayList; // Import ArrayList
import java.util.List; // Import List

public class Comment {
    // ... (all your existing fields) ...
    private String id;
    private String postId;
    private String authorUid;
    private String authorName;
    private String authorAvatarUrl;
    private String textContent;
    @ServerTimestamp // Use server timestamp for consistency
    private Timestamp timestamp;

    // --- New Fields ---
    private String parentCommentId = null; // ID of the comment this is a reply to (null for top-level)
    private boolean deleted = false; // Flag for soft delete
    private int replyCount = 0; // Number of direct replies to this comment
    private int likeCount = 0;    // NEW: Like count
    private int dislikeCount = 0; // NEW: Dislike count
    // --- End New Fields ---


    public Comment() {} // Required for Firestore

    // ... (your constructor) ...
    public Comment(String postId, String authorUid, String authorName, String authorAvatarUrl, String textContent, String parentCommentId) {
        this.postId = postId;
        this.authorUid = authorUid;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.textContent = textContent;
        this.parentCommentId = parentCommentId; // Set parent ID if it's a reply
        // timestamp is set by @ServerTimestamp
        this.deleted = false;
        this.replyCount = 0;
        this.likeCount = 0;      // Initialize NEW
        this.dislikeCount = 0;   // Initialize NEW
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getPostId() { return postId; }
    public String getAuthorUid() { return authorUid; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public String getTextContent() { return deleted ? "[Comment deleted]" : textContent; } // Show placeholder if deleted
    public Timestamp getTimestamp() { return timestamp; }
    public String getParentCommentId() { return parentCommentId; } // Getter for parent ID
    public boolean isDeleted() { return deleted; } // Getter for deleted status
    public int getReplyCount() { return replyCount; } // Getter for reply count
    public int getLikeCount() { return likeCount; }       // NEW Getter
    public int getDislikeCount() { return dislikeCount; } // NEW Getter


    // --- Setters ---
    public void setId(String id) { this.id = id; }
    // Setter for parentCommentId needed for Firestore mapping if structure changes later
    public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; } // Setter for deleted status
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; } // Setter for reply count
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }       // NEW Setter
    public void setDislikeCount(int dislikeCount) { this.dislikeCount = dislikeCount; } // NEW Setter


    // Setter for Firestore timestamp mapping
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    // Helper method to check if it's a reply
    @Exclude // <-- ADD THIS ANNOTATION
    public boolean isReply() {
        return parentCommentId != null && !parentCommentId.isEmpty();
    }
}