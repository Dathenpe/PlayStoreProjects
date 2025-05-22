// data/MoodEntry.java
package records; // Adjust your package name

import java.io.Serializable; // For easy passing between components if needed


public class MoodEntry implements Serializable {
    private String date;      // e.g., "2025-05-22"
    private String time;      // e.g., "14:30"
    private int moodLevel;    // 1-10
    private String moodDetails; // The text input

    public MoodEntry(String date, String time, int moodLevel, String moodDetails) {
        this.date = date;
        this.time = time;
        this.moodLevel = moodLevel;
        this.moodDetails = moodDetails;
    }

    // Getters
    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public int getMoodLevel() {
        return moodLevel;
    }

    public String getMoodDetails() {
        return moodDetails;
    }

    // You might want a toString() for debugging
    @Override
    public String toString() {
        return "MoodEntry{" +
                "date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", moodLevel=" + moodLevel +
                ", moodDetails='" + moodDetails + '\'' +
                '}';
    }
}