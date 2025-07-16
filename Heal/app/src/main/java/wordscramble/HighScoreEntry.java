package wordscramble;

import java.io.Serializable;

/**
 * Data class to represent a high score entry for the Word Scramble game.
 * It stores the date the score was achieved, the score itself, the time taken,
 * and now also the game mode.
 * Implements Serializable to allow for easy storage (e.g., using Gson with SharedPreferences).
 */
public class HighScoreEntry implements Serializable {
    public String date;
    public int score;
    public long timeTaken; // Time taken in seconds
    public GameMode gameMode; // New: Store the game mode

    /**
     * Constructor for a HighScoreEntry.
     * @param date The date the high score was achieved (e.g., "yyyy-MM-dd").
     * @param score The score achieved in the game.
     * @param timeTaken The time taken in seconds to achieve the score.
     * @param gameMode The GameMode in which the score was achieved.
     */
    public HighScoreEntry(String date, int score, long timeTaken, GameMode gameMode) {
        this.date = date;
        this.score = score;
        this.timeTaken = timeTaken;
        this.gameMode = gameMode;
    }
}
