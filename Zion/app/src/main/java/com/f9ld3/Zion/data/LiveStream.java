package com.f9ld3.Zion.data;

import com.google.firebase.Timestamp;

public class LiveStream {

    public static final String STATUS_SCHEDULED = "scheduled";
    public static final String STATUS_LIVE = "live";
    public static final String STATUS_ENDED = "ended";

    private String id;
    private String title;
    private String description;
    private String hostUid;
    private String hostName;
    private String hostAvatarUrl;
    private String streamUrl; // RTMP or HLS URL
    private String thumbnailUrl;
    private String status; // scheduled, live, ended
    private int viewerCount;
    private Timestamp scheduledTime;
    private Timestamp startedAt;
    private Timestamp endedAt;
    private boolean isRecorded; // Save stream for later viewing

    // Required empty constructor for Firestore
    public LiveStream() {}

    public LiveStream(String id, String title, String description, String hostUid, String hostName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.hostUid = hostUid;
        this.hostName = hostName;
        this.status = STATUS_SCHEDULED;
        this.viewerCount = 0;
        this.isRecorded = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getHostUid() { return hostUid; }
    public void setHostUid(String hostUid) { this.hostUid = hostUid; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getHostAvatarUrl() { return hostAvatarUrl; }
    public void setHostAvatarUrl(String hostAvatarUrl) { this.hostAvatarUrl = hostAvatarUrl; }

    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getViewerCount() { return viewerCount; }
    public void setViewerCount(int viewerCount) { this.viewerCount = viewerCount; }

    public Timestamp getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(Timestamp scheduledTime) { this.scheduledTime = scheduledTime; }

    public Timestamp getStartedAt() { return startedAt; }
    public void setStartedAt(Timestamp startedAt) { this.startedAt = startedAt; }

    public Timestamp getEndedAt() { return endedAt; }
    public void setEndedAt(Timestamp endedAt) { this.endedAt = endedAt; }

    public boolean isRecorded() { return isRecorded; }
    public void setRecorded(boolean recorded) { isRecorded = recorded; }

    // Helper methods
    public boolean isLive() {
        return STATUS_LIVE.equals(status);
    }

    public boolean isScheduled() {
        return STATUS_SCHEDULED.equals(status);
    }

    public boolean hasEnded() {
        return STATUS_ENDED.equals(status);
    }
}