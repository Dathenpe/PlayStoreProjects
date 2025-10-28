// main/java/com/f9ld3/Zion/ui/feed/PollOption.java
package com.f9ld3.Zion.ui.feed;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a single choice in a poll or quiz, now with optional image.
 */
public class PollOption implements Serializable {
    private String optionText;
    private int voteCount;
    private String imageUrl; // <-- NEW: Optional image URL for the option

    // Required empty constructor for Firestore
    public PollOption() {}

    // Constructor with only text (initial creation)
    public PollOption(String optionText) {
        this.optionText = optionText;
        this.voteCount = 0; // Default vote count to 0
        this.imageUrl = null; // Default image URL to null
    }

    // Constructor with text and existing vote count (used in EditPostActivity)
    public PollOption(String optionText, int voteCount) {
        this.optionText = optionText;
        this.voteCount = voteCount; // Set the provided vote count
        this.imageUrl = null; // Default image URL to null
    }

    // --- NEW Constructor with Image ---
    public PollOption(String optionText, String imageUrl) {
        this.optionText = optionText;
        this.voteCount = 0;
        this.imageUrl = imageUrl;
    }


    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    // --- NEW Getter and Setter for imageUrl ---
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    // --- End New ---


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PollOption that = (PollOption) o;
        return voteCount == that.voteCount &&
                Objects.equals(optionText, that.optionText) &&
                Objects.equals(imageUrl, that.imageUrl); // <-- Include imageUrl
    }

    @Override
    public int hashCode() {
        return Objects.hash(optionText, voteCount, imageUrl); // <-- Include imageUrl
    }
}