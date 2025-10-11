package com.f9ld3.Zion.data;

public class UserProfile {
    private String userId;
    private String username;
    private String email;
    private String profileImageUrl;
    private long createdAt;
    private long updatedAt; // ADDED: Fix for Firestore warning

    // Required for Firestore
    public UserProfile() {}

    public UserProfile(String userId, String username, String email, String profileImageUrl) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUid() { return userId; } // Alias for compatibility
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; } // ADDED

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setUid(String uid) { this.userId = uid; } // Alias
    public void setUsername(String username) {
        this.username = username;
        this.updatedAt = System.currentTimeMillis();
    }
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = System.currentTimeMillis();
    }
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = System.currentTimeMillis();
    }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; } // ADDED
}