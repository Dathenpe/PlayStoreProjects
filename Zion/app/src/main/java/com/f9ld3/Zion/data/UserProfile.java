package com.f9ld3.Zion.data;

/**
 * Data model for a user profile, used for both ProfileFragment and media attribution.
 */
public class UserProfile {
    public String userId;
    public String username;
    public String email;
    public String profileImageUrl;

    // Required for Firebase (Firestore/Realtime DB)
    public UserProfile() {}

    public UserProfile(String userId, String username, String email, String profileImageUrl) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters for Firebase and usage
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getProfileImageUrl() { return profileImageUrl; }
}