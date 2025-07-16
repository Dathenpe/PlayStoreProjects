package wordscramble;

import androidx.annotation.NonNull;

/**
 * Enum representing different game modes for the Word Scramble game.
 */
public enum GameMode {
    ENDLESS("Endless"),
    TEN_ROUNDS("10 Rounds"),
    TWENTY_ROUNDS("20 Rounds");

    public final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    @NonNull
    @Override
    public String toString() {
        return displayName;
    }
}
