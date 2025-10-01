package com.f9ld3.Zion.ui.player;

import com.google.firebase.Timestamp;

// Note: Using the name PlayerMedia to avoid confusion with the Feed's Post class.
public class PlayerMedia {
    // Media types for the adapter to distinguish
    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_PODCAST_SINGLE = 2; // Represents one item in a duo
    public static final int TYPE_PODCAST_DUO_CONTAINER = 3; // The wrapper item for the duo layout

    public String id;
    public int type; // Should be one of the TYPE_ constants
    public String title;
    public String description;
    public String mediaUrl; // URL for the video or audio file
    public String thumbnailUrl; // Thumbnail image URL
    public String authorName;
    public long durationSeconds;
    public Timestamp dateCreated; // 🔥 Added for Firestore ordering and History logging

    // 🔥 Fields for UPLOADER PROFILE INTEGRATION
    public String uploaderUid;
    public String uploaderAvatarUrl;

    // Optional: Only used if type == TYPE_PODCAST_DUO_CONTAINER
    public PlayerMedia podcastOne;
    public PlayerMedia podcastTwo;

    // Required for Firestore automatic object mapping
    public PlayerMedia() {}

    // Constructor for a single video/podcast item (TYPE_VIDEO or TYPE_PODCAST_SINGLE)
    public PlayerMedia(String id, int type, String title, String description, String mediaUrl, String thumbnailUrl,
                       String authorName, long durationSeconds, String uploaderUid, String uploaderAvatarUrl) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.mediaUrl = mediaUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.authorName = authorName;
        this.durationSeconds = durationSeconds;
        this.uploaderUid = uploaderUid;
        this.uploaderAvatarUrl = uploaderAvatarUrl;
        this.dateCreated = Timestamp.now(); // Date is set explicitly or mapped by Firestore
    }

    // Original constructor for backwards compatibility (can be removed later)
    public PlayerMedia(String id, int type, String title, String description, String mediaUrl, String thumbnailUrl, String authorName, long durationSeconds) {
        this(id, type, title, description, mediaUrl, thumbnailUrl, authorName, durationSeconds, null, null);
    }

    // 🔥 Constructor for the DUO container (used in ViewModel)
    public PlayerMedia(PlayerMedia podcastOne, PlayerMedia podcastTwo) {
        this.type = TYPE_PODCAST_DUO_CONTAINER;
        this.podcastOne = podcastOne;
        this.podcastTwo = podcastTwo;
    }

    // Getters (Important for data access and Firestore)
    public String getId() {
        // For the DUO container, return the ID of the first podcast, or generate a temporary ID
        return (type == TYPE_PODCAST_DUO_CONTAINER && podcastOne != null) ? podcastOne.getId() + "_duo" : id;
    }
    public int getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getMediaUrl() { return mediaUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getAuthorName() { return authorName; }
    public long getDurationSeconds() { return durationSeconds; }
    public Timestamp getDateCreated() { return dateCreated; }

    // 🔥 NEW GETTERS
    public String getUploaderUid() { return uploaderUid; }
    public String getUploaderAvatarUrl() { return uploaderAvatarUrl; }

    public PlayerMedia getPodcastOne() { return podcastOne; }
    public PlayerMedia getPodcastTwo() { return podcastTwo; }
}