// main/java/com/f9ld3/Zion/ui/feed/Post.java
package com.f9ld3.Zion.ui.feed;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
// import com.google.firebase.firestore.ServerTimestamp; // <-- REMOVED

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class Post implements Serializable {
    // Post Types
    public static final String TYPE_TEXT_MEDIA = "TEXT_MEDIA";
    public static final String TYPE_POLL = "POLL";
    public static final String TYPE_QUIZ = "QUIZ";

    // Media Types (Legacy, can be deprecated or used for simple checks)
    public static final int MEDIA_TYPE_TEXT = 1;
    public static final int MEDIA_TYPE_IMAGE = 2;
    public static final int MEDIA_TYPE_VIDEO = 3;

    // --- Fields ---
    private String id; // Use private fields with getters/setters
    private String authorUid;
    private String authorName;
    private String authorAvatarUrl;

    // --- FIX 1: Field is now Long ---
    // @ServerTimestamp // <-- REMOVED
    private Long timestamp; // <-- CHANGED FROM Timestamp

    private String textContent;
    private List<MediaItem> mediaItems = new ArrayList<>();
    private int likeCount = 0;
    private int commentCount = 0;

    // New fields for Poll/Quiz posts
    private String postType = TYPE_TEXT_MEDIA; // Default to old post type
    private List<PollOption> pollOptions = new ArrayList<>();
    private int quizCorrectOptionIndex = -1; // -1 indicates not a quiz or no correct answer set
    private int totalVotes = 0; // Total votes for a poll/quiz

    // --- FIX 2: Add legacy fields for compatibility ---
    private int mediaType = 0;
    private String mediaUrl;
    // --- End Fix 2 ---


    // --- Constructor ---
    public Post() {} // Required empty constructor for Firestore

    // --- GETTERS ---
    public String getId() { return id; }
    public String getAuthorUid() { return authorUid; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }

    // --- FIX 3: Getter returns Long ---
    public Long getTimestamp() { return timestamp; } // <-- CHANGED FROM Timestamp

    public String getTextContent() { return textContent; }
    public List<MediaItem> getMediaItems() { return mediaItems; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public String getPostType() { return postType; }
    public List<PollOption> getPollOptions() { return pollOptions; }
    public int getQuizCorrectOptionIndex() { return quizCorrectOptionIndex; }
    public int getTotalVotes() { return totalVotes; }

    // --- SETTERS ---
    public void setId(String id) { this.id = id; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }

    // --- FIX 4: ONLY ONE setter for Long ---
    public void setTimestamp(Long timestamp) { // <-- This is now the only setter
        this.timestamp = timestamp;
    }

    public void setTextContent(String textContent) { this.textContent = textContent; }
    public void setMediaItems(List<MediaItem> mediaItems) { this.mediaItems = mediaItems; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setPostType(String postType) { this.postType = postType; }
    public void setPollOptions(List<PollOption> pollOptions) { this.pollOptions = pollOptions; }
    public void setQuizCorrectOptionIndex(int quizCorrectOptionIndex) { this.quizCorrectOptionIndex = quizCorrectOptionIndex; }
    public void setTotalVotes(int totalVotes) { this.totalVotes = totalVotes; }

    // --- FIX 5: Add legacy setters to prevent other parsing errors ---
    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }
    // --- End Fix 5 ---


    // --- Excluded Helper Methods ---
    @Exclude // Exclude from Firestore serialization
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

    @Exclude // Exclude from Firestore serialization
    public String getMediaUrl() {
        if (mediaItems != null && !mediaItems.isEmpty()) {
            return mediaItems.get(0).getUrl();
        }
        // Fallback to legacy field if new one is empty
        if (mediaUrl != null) {
            return mediaUrl;
        }
        return null;
    }

    @Exclude // Exclude from Firestore serialization
    public String getThumbnailUrl() {
        if (mediaItems != null && !mediaItems.isEmpty()) {
            MediaItem firstItem = mediaItems.get(0);
            // Prioritize specific thumbnail URL if available (especially for videos)
            if (firstItem.getThumbnailUrl() != null && !firstItem.getThumbnailUrl().isEmpty()) {
                return firstItem.getThumbnailUrl();
            }
            // Fallback to the main URL (useful for images or if no specific thumbnail)
            return firstItem.getUrl();
        }
        return null;
    }

    // --- equals() and hashCode() ---
    // Updated to include new fields and handle Long Timestamp
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return likeCount == post.likeCount &&
                commentCount == post.commentCount &&
                quizCorrectOptionIndex == post.quizCorrectOptionIndex &&
                totalVotes == post.totalVotes &&
                Objects.equals(id, post.id) &&
                Objects.equals(authorUid, post.authorUid) &&
                Objects.equals(authorName, post.authorName) &&
                Objects.equals(authorAvatarUrl, post.authorAvatarUrl) &&
                Objects.equals(timestamp, post.timestamp) && // Compare Longs
                Objects.equals(textContent, post.textContent) &&
                Objects.equals(mediaItems, post.mediaItems) &&
                Objects.equals(postType, post.postType) &&
                Objects.equals(pollOptions, post.pollOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, authorUid, authorName, authorAvatarUrl, timestamp, textContent,
                mediaItems, likeCount, commentCount, postType, pollOptions,
                quizCorrectOptionIndex, totalVotes);
    }
}