// main/java/com/f9ld3/Zion/ui/feed/Post.java
package com.f9ld3.Zion.ui.feed;

import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.io.IOException; // Import IOException
import java.io.ObjectInputStream; // Import ObjectInputStream
import java.io.ObjectOutputStream; // Import ObjectOutputStream
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class Post implements Serializable {
    // Post Types
    public static final String TYPE_TEXT_MEDIA = "TEXT_MEDIA";
    public static final String TYPE_POLL = "POLL";
    public static final String TYPE_QUIZ = "QUIZ";

    // Media Types (Legacy)
    public static final int MEDIA_TYPE_TEXT = 1;
    public static final int MEDIA_TYPE_IMAGE = 2;
    public static final int MEDIA_TYPE_VIDEO = 3;

    // --- Fields ---
    private String id;
    private String authorUid;
    private String authorName;
    private String authorAvatarUrl;

    // *** FIX 1: Mark Timestamp as transient so default serialization skips it ***
    private transient Timestamp timestamp;

    private String textContent;
    private List<MediaItem> mediaItems = new ArrayList<>();
    private int likeCount = 0;
    private int dislikeCount = 0;
    private int commentCount = 0;

    // New fields for Poll/Quiz posts
    private String postType = TYPE_TEXT_MEDIA;
    private List<PollOption> pollOptions = new ArrayList<>();
    private int quizCorrectOptionIndex = -1;
    private int totalVotes = 0;
    private Integer pollDurationHours = null;

    // Legacy fields
    private int mediaType = 0;
    private String mediaUrl;

    // --- Constructor ---
    public Post() {}

    // *** FIX 2: Custom serialization methods ***
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject(); // Write all non-transient fields automatically
        // Convert Timestamp to Date (which IS Serializable) and write it
        out.writeObject(timestamp != null ? timestamp.toDate() : null);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject(); // Read all non-transient fields automatically
        // Read the Date object and convert it back to Timestamp
        Date date = (Date) in.readObject();
        timestamp = date != null ? new Timestamp(date) : null;
    }
    // *** END FIX ***

    // --- GETTERS ---
    public String getId() { return id; }
    public String getAuthorUid() { return authorUid; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }

    public Timestamp getTimestamp() { return timestamp; }

    public String getTextContent() { return textContent; }
    public List<MediaItem> getMediaItems() { return mediaItems; }
    public int getLikeCount() { return likeCount; }
    public int getDislikeCount() { return dislikeCount; }
    public int getCommentCount() { return commentCount; }
    public String getPostType() { return postType; }
    public List<PollOption> getPollOptions() { return pollOptions; }
    public int getQuizCorrectOptionIndex() { return quizCorrectOptionIndex; }
    public int getTotalVotes() { return totalVotes; }
    public Integer getPollDurationHours() { return pollDurationHours; }

    // --- SETTERS ---
    public void setId(String id) { this.id = id; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }

    public void setTimestamp(Object timestampObj) {
        if (timestampObj instanceof Timestamp) {
            this.timestamp = (Timestamp) timestampObj;
        } else if (timestampObj instanceof Long) {
            this.timestamp = new Timestamp(new Date((Long) timestampObj));
        } else if (timestampObj == null) {
            this.timestamp = null;
        } else {
            Log.e("Post.java", "Unknown type for timestamp field: " + timestampObj.getClass().getName());
            this.timestamp = null;
        }
    }

    public void setTextContent(String textContent) { this.textContent = textContent; }
    public void setMediaItems(List<MediaItem> mediaItems) { this.mediaItems = mediaItems; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setDislikeCount(int dislikeCount) { this.dislikeCount = dislikeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setPostType(String postType) { this.postType = postType; }
    public void setPollOptions(List<PollOption> pollOptions) { this.pollOptions = pollOptions; }
    public void setQuizCorrectOptionIndex(int quizCorrectOptionIndex) { this.quizCorrectOptionIndex = quizCorrectOptionIndex; }
    public void setTotalVotes(int totalVotes) { this.totalVotes = totalVotes; }
    public void setPollDurationHours(Integer pollDurationHours) { this.pollDurationHours = pollDurationHours; }

    public void setMediaType(int mediaType) { this.mediaType = mediaType; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    // --- Excluded Helper Methods ---
    @Exclude
    public int getLegacyMediaType() {
        if (mediaItems == null || mediaItems.isEmpty()) return MEDIA_TYPE_TEXT;
        MediaItem firstItem = mediaItems.get(0);
        if ("video".equals(firstItem.getMediaType())) return MEDIA_TYPE_VIDEO;
        if ("image".equals(firstItem.getMediaType())) return MEDIA_TYPE_IMAGE;
        return MEDIA_TYPE_TEXT;
    }

    @Exclude
    public String getLegacyMediaUrl() {
        if (mediaItems != null && !mediaItems.isEmpty()) return mediaItems.get(0).getUrl();
        return mediaUrl;
    }

    @Exclude
    public String getThumbnailUrl() {
        if (mediaItems != null && !mediaItems.isEmpty()) {
            MediaItem firstItem = mediaItems.get(0);
            if (firstItem.getThumbnailUrl() != null && !firstItem.getThumbnailUrl().isEmpty()) {
                return firstItem.getThumbnailUrl();
            }
            return firstItem.getUrl();
        }
        return null;
    }

    // --- equals() and hashCode() ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return likeCount == post.likeCount &&
                dislikeCount == post.dislikeCount &&
                commentCount == post.commentCount &&
                quizCorrectOptionIndex == post.quizCorrectOptionIndex &&
                totalVotes == post.totalVotes &&
                Objects.equals(id, post.id) &&
                Objects.equals(authorUid, post.authorUid) &&
                Objects.equals(authorName, post.authorName) &&
                Objects.equals(authorAvatarUrl, post.authorAvatarUrl) &&
                Objects.equals(timestamp, post.timestamp) &&
                Objects.equals(textContent, post.textContent) &&
                Objects.equals(mediaItems, post.mediaItems) &&
                Objects.equals(postType, post.postType) &&
                Objects.equals(pollOptions, post.pollOptions) &&
                Objects.equals(pollDurationHours, post.pollDurationHours);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, authorUid, authorName, authorAvatarUrl, timestamp, textContent,
                mediaItems, likeCount, dislikeCount, commentCount, postType, pollOptions,
                quizCorrectOptionIndex, totalVotes, pollDurationHours);
    }
}