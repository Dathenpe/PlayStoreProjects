package com.f9ld3.Zion.ui.feed;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a single choice in a poll or quiz.
 */
public class PollOption implements Serializable {
    private String optionText;
    private int voteCount;

    public PollOption() {}

    public PollOption(String optionText) {
        this.optionText = optionText;
        this.voteCount = 0;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PollOption that = (PollOption) o;
        return voteCount == that.voteCount && Objects.equals(optionText, that.optionText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optionText, voteCount);
    }
}
