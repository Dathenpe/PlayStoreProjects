package com.f9ld3.Zion.ui.search;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchViewModel extends ViewModel {

    private static final String TAG = "SearchViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<List<PlayerMedia>> _videoResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<PlayerMedia>> getVideoResults() { return _videoResults; }
    private final MutableLiveData<List<PlayerMedia>> _podcastResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<PlayerMedia>> getPodcastResults() { return _podcastResults; }
    private final MutableLiveData<List<Post>> _postResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Post>> getPostResults() { return _postResults; }
    private final MutableLiveData<List<UserProfile>> _userResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<UserProfile>> getUserResults() { return _userResults; }
    private final MutableLiveData<List<Object>> _allResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Object>> getAllResults() { return _allResults; }
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    private String currentQuery = "";

    public void searchAll(String query) {
        if (query == null || query.trim().isEmpty()) {
            clearResults();
            return;
        }
        currentQuery = query.trim().toLowerCase();
        _isLoading.setValue(true);
        searchVideos(currentQuery);
        searchPodcasts(currentQuery);
        searchPosts(currentQuery);
        searchUsers(currentQuery);
    }

    private void searchVideos(String query) {
        db.collection("media").whereEqualTo("type", PlayerMedia.TYPE_VIDEO)
                .orderBy("dateCreated", Query.Direction.DESCENDING).limit(50).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<PlayerMedia> results = queryDocumentSnapshots.toObjects(PlayerMedia.class).stream()
                            .filter(media -> matchesQuery(media.getTitle(), query) || matchesQuery(media.getDescription(), query))
                            .collect(Collectors.toList());
                    _videoResults.setValue(results);
                    updateAllResults();
                });
    }

    private void searchPodcasts(String query) {
        db.collection("media").whereEqualTo("type", PlayerMedia.TYPE_PODCAST_SINGLE)
                .orderBy("dateCreated", Query.Direction.DESCENDING).limit(50).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<PlayerMedia> results = queryDocumentSnapshots.toObjects(PlayerMedia.class).stream()
                            .filter(media -> matchesQuery(media.getTitle(), query) || matchesQuery(media.getDescription(), query))
                            .collect(Collectors.toList());
                    _podcastResults.setValue(results);
                    updateAllResults();
                });
    }

    public void searchPosts(String query) {
        db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Post> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Post post = doc.toObject(Post.class);
                        if (post.getTextContent() != null && post.getTextContent().toLowerCase().contains(query)) {
                            post.setId(doc.getId());
                            results.add(post);
                        }
                    }
                    _postResults.setValue(results);
                    updateAllResults();
                });
    }

    private void searchUsers(String query) {
        db.collection("users").orderBy("username").startAt(query).endAt(query + "\uf8ff").limit(20).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserProfile> results = queryDocumentSnapshots.toObjects(UserProfile.class);
                    _userResults.setValue(results);
                    updateAllResults();
                });
    }

    private void updateAllResults() {
        List<Object> combined = new ArrayList<>();
        if (_postResults.getValue() != null) combined.addAll(_postResults.getValue());
        if (_videoResults.getValue() != null) combined.addAll(_videoResults.getValue());
        if (_podcastResults.getValue() != null) combined.addAll(_podcastResults.getValue());
        if (_userResults.getValue() != null) combined.addAll(_userResults.getValue());
        _allResults.setValue(combined);
        _isLoading.setValue(false);
    }

    public void clearResults() {
        _videoResults.setValue(new ArrayList<>());
        _podcastResults.setValue(new ArrayList<>());
        _postResults.setValue(new ArrayList<>());
        _userResults.setValue(new ArrayList<>());
        _allResults.setValue(new ArrayList<>());
        currentQuery = "";
    }

    private boolean matchesQuery(String text, String query) {
        return text != null && text.toLowerCase().contains(query);
    }
}