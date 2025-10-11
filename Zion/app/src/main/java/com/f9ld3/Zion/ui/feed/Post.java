// MultipleFiles/Post.java
package com.f9ld3.Zion.ui.feed;

/**
 * Data model for a single blog post or feed item.
 */
public class Post {
    public String id;
    public String title;
    public String description;
    public String imageUrl;
    public String authorName;
    public long timestamp;
    public String type; // e.g., "blog", "sermon", "podcast"
    public String authorUid; // NEW: Add author UID for filtering

    // Required for Firestore automatic object mapping
    public Post() {}

    // Original constructor
    public Post(String id, String title, String description, String imageUrl, String authorName, long timestamp, String type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.authorName = authorName;
        this.timestamp = timestamp;
        this.type = type;
        // authorUid will be set separately or in a more complete constructor
    }

    // NEW: Constructor that includes authorUid
    public Post(String id, String title, String description, String imageUrl, String authorName, long timestamp, String type, String authorUid) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.authorName = authorName;
        this.timestamp = timestamp;
        this.type = type;
        this.authorUid = authorUid;
    }

    // Getters for LiveData observation and binding (important for data access)
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getAuthorName() { return authorName; }
    public long getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public String getAuthorUid() { return authorUid; } // NEW Getter

    public void setId(String id) { this.id = id; }
}