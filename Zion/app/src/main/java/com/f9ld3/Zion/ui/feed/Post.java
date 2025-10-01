package com.f9ld3.Zion.ui.feed;

/**
 * Data model for a single blog post or feed item.
 * NOTE: This is an inner class for simplicity, but could be moved to a separate file (model/Post.java).
 */
 public class Post {
    public String id;
    public String title;
    public String description;
    public String imageUrl;
    public String authorName;
    public long timestamp;
    public String type; // e.g., "blog", "sermon", "podcast"

    // Required for Firestore automatic object mapping
    public Post() {}

    public Post(String id, String title, String description, String imageUrl, String authorName, long timestamp, String type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.authorName = authorName;
        this.timestamp = timestamp;
        this.type = type;
    }

    // Getters for LiveData observation and binding (important for data access)
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getAuthorName() { return authorName; }
    public long getTimestamp() { return timestamp; }
    public String getType() { return type; }
}
