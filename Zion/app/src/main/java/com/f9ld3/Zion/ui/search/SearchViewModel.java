package com.f9ld3.Zion.ui.search;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {

    private static final String TAG = "SearchViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Search results by type
    private final MutableLiveData<List<PlayerMedia>> _videoResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<PlayerMedia>> getVideoResults() { return _videoResults; }

    private final MutableLiveData<List<PlayerMedia>> _podcastResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<PlayerMedia>> getPodcastResults() { return _podcastResults; }

    private final MutableLiveData<List<UserProfile>> _userResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<UserProfile>> getUserResults() { return _userResults; }

    // Combined results (All tab)
    private final MutableLiveData<List<Object>> _allResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Object>> getAllResults() { return _allResults; }

    // Loading and error states
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    // Current search query
    private String currentQuery = "";

    /**
     * Search across all content types
     */
    public void searchAll(String query) {
        if (query == null || query.trim().isEmpty()) {
            clearResults();
            return;
        }

        currentQuery = query.trim().toLowerCase();
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        Log.d(TAG, "Searching for: " + currentQuery);

        // Search all types simultaneously
        searchVideos(currentQuery);
        searchPodcasts(currentQuery);
        searchUsers(currentQuery);
    }

    /**
     * Search for videos only
     */
    public void searchVideos(String query) {
        if (query == null || query.trim().isEmpty()) {
            _videoResults.setValue(new ArrayList<>());
            return;
        }

        String searchQuery = query.trim().toLowerCase();

        db.collection("media")
                .whereEqualTo("type", PlayerMedia.TYPE_VIDEO)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<PlayerMedia> results = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        PlayerMedia media = doc.toObject(PlayerMedia.class);
                        if (media != null && matchesQuery(media, searchQuery)) {
                            results.add(media);
                        }
                    }

                    _videoResults.setValue(results);
                    updateAllResults();
                    _isLoading.setValue(false);

                    Log.d(TAG, "Found " + results.size() + " videos");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching videos", e);
                    _errorMessage.setValue("Failed to search videos: " + e.getMessage());
                    _isLoading.setValue(false);
                });
    }

    /**
     * Search for podcasts only
     */
    public void searchPodcasts(String query) {
        if (query == null || query.trim().isEmpty()) {
            _podcastResults.setValue(new ArrayList<>());
            return;
        }

        String searchQuery = query.trim().toLowerCase();

        db.collection("media")
                .whereIn("type", List.of(PlayerMedia.TYPE_PODCAST_SINGLE, PlayerMedia.TYPE_PODCAST_DUO_CONTAINER))
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<PlayerMedia> results = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        PlayerMedia media = doc.toObject(PlayerMedia.class);
                        if (media != null && matchesQuery(media, searchQuery)) {
                            results.add(media);
                        }
                    }

                    _podcastResults.setValue(results);
                    updateAllResults();
                    _isLoading.setValue(false);

                    Log.d(TAG, "Found " + results.size() + " podcasts");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching podcasts", e);
                    _errorMessage.setValue("Failed to search podcasts: " + e.getMessage());
                    _isLoading.setValue(false);
                });
    }

    /**
     * Search for users/channels
     */
    public void searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            _userResults.setValue(new ArrayList<>());
            return;
        }

        String searchQuery = query.trim().toLowerCase();

        db.collection("users")
                .orderBy("username")
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UserProfile> results = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        UserProfile user = doc.toObject(UserProfile.class);
                        if (user != null && matchesUserQuery(user, searchQuery)) {
                            results.add(user);
                        }
                    }

                    _userResults.setValue(results);
                    updateAllResults();
                    _isLoading.setValue(false);

                    Log.d(TAG, "Found " + results.size() + " users");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching users", e);
                    _errorMessage.setValue("Failed to search users: " + e.getMessage());
                    _isLoading.setValue(false);
                });
    }

    /**
     * Check if media matches search query
     */
    private boolean matchesQuery(PlayerMedia media, String query) {
        if (media.getTitle() != null && media.getTitle().toLowerCase().contains(query)) {
            return true;
        }
        if (media.getDescription() != null && media.getDescription().toLowerCase().contains(query)) {
            return true;
        }
        if (media.getAuthorName() != null && media.getAuthorName().toLowerCase().contains(query)) {
            return true;
        }
        return false;
    }

    /**
     * Check if user matches search query
     */
    private boolean matchesUserQuery(UserProfile user, String query) {
        if (user.getUsername() != null && user.getUsername().toLowerCase().contains(query)) {
            return true;
        }
        if (user.getEmail() != null && user.getEmail().toLowerCase().contains(query)) {
            return true;
        }
        return false;
    }

    /**
     * Combine all search results for "All" tab
     */
    private void updateAllResults() {
        List<Object> combined = new ArrayList<>();

        List<PlayerMedia> videos = _videoResults.getValue();
        List<PlayerMedia> podcasts = _podcastResults.getValue();
        List<UserProfile> users = _userResults.getValue();

        if (videos != null) combined.addAll(videos);
        if (podcasts != null) combined.addAll(podcasts);
        if (users != null) combined.addAll(users);

        _allResults.setValue(combined);
    }

    /**
     * Clear all search results
     */
    public void clearResults() {
        _videoResults.setValue(new ArrayList<>());
        _podcastResults.setValue(new ArrayList<>());
        _userResults.setValue(new ArrayList<>());
        _allResults.setValue(new ArrayList<>());
        _errorMessage.setValue(null);
        currentQuery = "";
    }

    /**
     * Get current search query
     */
    public String getCurrentQuery() {
        return currentQuery;
    }
}