package records;

import java.util.Locale;

public class AudioLog {
    private String title;
    private String timestamp;
    private String filePath;
    private long durationMillis; // Duration in milliseconds

    public AudioLog(String title, String timestamp, String filePath, long durationMillis) {
        this.title = title;
        this.timestamp = timestamp;
        this.filePath = filePath;
        this.durationMillis = durationMillis;
    }

    // Overloaded constructor for cases where duration might not be immediately available
    public AudioLog(String title, String timestamp, String filePath) {
        this(title, timestamp, filePath, 0);
    }

    public String getTitle() {
        return title;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "--:--";
        }
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60)) % 24;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    public String getFormattedDuration() {
        return formatDuration(this.durationMillis);
    }
}
