package records;

public class AudioLog {
    private String title;
    private String timestamp;
    private String filePath;

    public AudioLog(String title, String timestamp, String filePath) {
        this.title = title;
        this.timestamp = timestamp;
        this.filePath = filePath;
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

    // Optional: Add setters if needed, but for immutable logs, this is fine.
}