package com.f9ld3.Zion.data;

public class UserProfile {
    private String userId;
    private String accountName; // Renamed from username
    private String username; // This will be the customizable ^username
    private String email;
    private String profileImageUrl;
    private String bannerImageUrl; // New field for banner image
    private String bio; // New field for bio
    private long createdAt;
    private long updatedAt;

    // Required for Firestore
    public UserProfile() {}

    public UserProfile(String userId, String accountName, String email, String profileImageUrl) {
        this.userId = userId;
        this.accountName = accountName;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUid() { return userId; } // Alias for compatibility
    public String getAccountName() { return accountName; } // Renamed
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getBannerImageUrl() { return bannerImageUrl; } // New getter
    public String getBio() { return bio; } // New getter
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setUid(String uid) { this.userId = uid; } // Alias
    public void setAccountName(String accountName) { // Renamed
        this.accountName = accountName;
        this.updatedAt = System.currentTimeMillis();
    }
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
    public void setBannerImageUrl(String bannerImageUrl) { // New setter
        this.bannerImageUrl = bannerImageUrl;
        this.updatedAt = System.currentTimeMillis();
    }
    public void setBio(String bio) { // New setter
        this.bio = bio;
        this.updatedAt = System.currentTimeMillis();
    }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}