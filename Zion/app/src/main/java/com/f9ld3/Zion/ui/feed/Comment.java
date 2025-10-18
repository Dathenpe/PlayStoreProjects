package com.f9ld3.Zion.ui.feed;

import com.google.firebase.Timestamp;

public class Comment {
    private String id;
    private String postId;
    private String authorUid;
    private String authorName;
    private String authorAvatarUrl;
    private String textContent;
    private Timestamp timestamp;

    public Comment() {} // Required for Firestore

    public Comment(String postId, String authorUid, String authorName, String authorAvatarUrl, String textContent) {
        this.postId = postId;
        this.authorUid = authorUid;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.textContent = textContent;
        this.timestamp = Timestamp.now();
    }

    // Getters
    public String getId() { return id; }
    public String getPostId() { return postId; }
    public String getAuthorUid() { return authorUid; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public String getTextContent() { return textContent; }
    public Timestamp getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }
}