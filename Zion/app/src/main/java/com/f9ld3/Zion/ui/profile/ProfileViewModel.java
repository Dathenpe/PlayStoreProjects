// main/java/com/f9ld3/Zion/ui/profile/ProfileViewModel.java
package com.f9ld3.Zion.ui.profile;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.data.HistoryItem;
import com.f9ld3.Zion.data.Playlist;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // LiveData declarations
    private final MutableLiveData<UserProfile> mUserProfile = new MutableLiveData<>();
    public LiveData<UserProfile> getUserProfile() { return mUserProfile; }

    private final MutableLiveData<List<PlayerMedia>> mUserVideos = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserVideos() { return mUserVideos; }

    private final MutableLiveData<List<PlayerMedia>> mUserPodcasts = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserPodcasts() { return mUserPodcasts; }

    private final MutableLiveData<List<HistoryItem>> mUserHistory = new MutableLiveData<>();
    public LiveData<List<HistoryItem>> getUserHistory() { return mUserHistory; }

    private final MutableLiveData<List<PlayerMedia>> mUserDownloads = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserDownloads() { return mUserDownloads; }

    private final MutableLiveData<List<UserProfile>> mFollowing = new MutableLiveData<>();
    public LiveData<List<UserProfile>> getFollowing() { return mFollowing; }

    private final MutableLiveData<List<Post>> mUserPosts = new MutableLiveData<>();
    public LiveData<List<Post>> getUserPosts() { return mUserPosts; }

    private final MutableLiveData<List<Playlist>> mUserPlaylists = new MutableLiveData<>();
    public LiveData<List<Playlist>> getUserPlaylists() { return mUserPlaylists; }

    private final MutableLiveData<List<Object>> mFollowedContent = new MutableLiveData<>();
    public LiveData<List<Object>> getFollowedContent() { return mFollowedContent; }

    // Specific Following LiveData from your stable version
    private final MutableLiveData<List<UserProfile>> mFollowingChannels = new MutableLiveData<>();
    public LiveData<List<UserProfile>> getFollowingChannels() { return mFollowingChannels; }

    private final MutableLiveData<List<UserProfile>> mFollowingUsers = new MutableLiveData<>();
    public LiveData<List<UserProfile>> getFollowingUsers() { return mFollowingUsers; }


    // Firestore Listeners
    private ListenerRegistration userProfileListener, userHistoryListener, followingChannelsListener, followingUsersListener, userPostsListener, userPlaylistsListener, followedContentListener;

    public ProfileViewModel() {
        mAuth.addAuthStateListener(this::onAuthStateChanged);
    }

    private void onAuthStateChanged(FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            fetchUserProfile(uid);
            fetchUserHistory(uid);
            fetchFollowingChannels(uid);
            fetchFollowingUsers(uid);
            fetchUserPosts(uid);
            fetchUserPlaylists(uid);
            mUserDownloads.setValue(new ArrayList<>());
        } else {
            clearData();
        }
    }

    public void fetchUserProfile(String uid) {
        if (userProfileListener != null) userProfileListener.remove();
        userProfileListener = db.collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        mUserProfile.setValue(snapshot.toObject(UserProfile.class));
                    }
                });
    }

    public void fetchUserVideos(String uid) {
        db.collection("media").whereEqualTo("uploaderUid", uid)
                .whereEqualTo("type", PlayerMedia.TYPE_VIDEO)
                .orderBy("dateCreated", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching user videos", error);
                        mUserVideos.setValue(new ArrayList<>());
                        return;
                    }
                    List<PlayerMedia> videos = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            PlayerMedia item = doc.toObject(PlayerMedia.class);
                            if (item != null) {
                                videos.add(item);
                            }
                        }
                    }
                    mUserVideos.setValue(videos);
                });
    }

    public void fetchUserPodcasts(String uid) {
        db.collection("media").whereEqualTo("uploaderUid", uid)
                .whereEqualTo("type", PlayerMedia.TYPE_PODCAST_SINGLE)
                .orderBy("dateCreated", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching user podcasts", error);
                        mUserPodcasts.setValue(new ArrayList<>());
                        return;
                    }
                    List<PlayerMedia> podcasts = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            PlayerMedia item = doc.toObject(PlayerMedia.class);
                            if (item != null) {
                                podcasts.add(item);
                            }
                        }
                    }
                    mUserPodcasts.setValue(podcasts);
                });
    }


    void fetchUserHistory(String uid) {
        if (userHistoryListener != null) userHistoryListener.remove();
        userHistoryListener = db.collection("users").document(uid).collection("history")
                .orderBy("viewedAt", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((value, error) -> {
                    List<HistoryItem> historyList = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            HistoryItem item = doc.toObject(HistoryItem.class);
                            if (item != null) historyList.add(item);
                        }
                    }
                    mUserHistory.setValue(historyList);
                });
    }

    void fetchFollowingChannels(String uid) {
        if (followingChannelsListener != null) followingChannelsListener.remove();
        followingChannelsListener = db.collection("users").document(uid).collection("following")
                .whereEqualTo("type", "channel")
                .addSnapshotListener((value, error) -> {
                    List<UserProfile> channels = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            UserProfile profile = doc.toObject(UserProfile.class);
                            if (profile != null) channels.add(profile);
                        }
                    }
                    mFollowingChannels.setValue(channels);
                    updateCombinedFollowing();
                });
    }

    void fetchFollowingUsers(String uid) {
        if (followingUsersListener != null) followingUsersListener.remove();
        followingUsersListener = db.collection("users").document(uid).collection("following")
                .whereEqualTo("type", "user")
                .addSnapshotListener((value, error) -> {
                    List<UserProfile> users = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            UserProfile profile = doc.toObject(UserProfile.class);
                            if (profile != null) users.add(profile);
                        }
                    }
                    mFollowingUsers.setValue(users);
                    updateCombinedFollowing();
                });
    }

    private void updateCombinedFollowing() {
        List<UserProfile> allFollowing = new ArrayList<>();
        List<String> followingIds = new ArrayList<>();
        if (mFollowingChannels.getValue() != null) {
            allFollowing.addAll(mFollowingChannels.getValue());
        }
        if (mFollowingUsers.getValue() != null) {
            allFollowing.addAll(mFollowingUsers.getValue());
        }
        mFollowing.setValue(allFollowing);

        for (UserProfile profile : allFollowing) {
            followingIds.add(profile.getUserId());
        }
        fetchFollowedContent(followingIds);
    }

    void fetchUserPosts(String uid) {
        if (userPostsListener != null) userPostsListener.remove();
        userPostsListener = db.collection("posts").whereEqualTo("authorUid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((value, error) -> {
                    // --- FIX: Add error handling and try-catch ---
                    if (error != null) {
                        Log.e(TAG, "Error fetching user posts", error);
                        mUserPosts.setValue(new ArrayList<>());
                        return;
                    }

                    List<Post> posts = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                Post post = doc.toObject(Post.class);
                                if (post != null) {
                                    post.setId(doc.getId());
                                    posts.add(post);
                                }
                            } catch (Exception e) {
                                // This try-catch prevents the Profile fragment from crashing
                                Log.e(TAG, "Error parsing post: " + doc.getId(), e);
                            }
                        }
                    }
                    mUserPosts.setValue(posts);
                    Log.d(TAG, "Successfully processed " + posts.size() + " user posts.");
                    // --- END FIX ---
                });
    }

    private void fetchUserPlaylists(String uid) {
        if (userPlaylistsListener != null) userPlaylistsListener.remove();
        userPlaylistsListener = db.collection("users").document(uid).collection("playlists")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((value, error) -> {
                    List<Playlist> playlists = new ArrayList<>();
                    boolean hasMyList = false;
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Playlist playlist = doc.toObject(Playlist.class);
                            if (playlist != null) {
                                playlist.setId(doc.getId());
                                playlists.add(playlist);
                                if ("My List".equals(playlist.getName())) hasMyList = true;
                            }
                        }
                    }
                    if (!hasMyList) createDefaultMyListPlaylist(uid);
                    mUserPlaylists.setValue(playlists);
                });
    }

    private void createDefaultMyListPlaylist(String uid) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        Map<String, Object> playlistData = new HashMap<>();
        playlistData.put("name", "My List");
        playlistData.put("description", "Your personal collection of saved content.");
        playlistData.put("creatorUid", uid);
        playlistData.put("creatorName", user.getDisplayName());
        playlistData.put("isPublic", false);
        playlistData.put("mediaIds", new ArrayList<>());
        playlistData.put("itemCount", 0);
        playlistData.put("createdAt", Timestamp.now());
        playlistData.put("updatedAt", Timestamp.now());
        db.collection("users").document(uid).collection("playlists").add(playlistData);
    }

    private void fetchFollowedContent(List<String> followingIds) {
        if (followedContentListener != null) followedContentListener.remove();
        if (followingIds == null || followingIds.isEmpty()) {
            mFollowedContent.setValue(new ArrayList<>());
            return;
        }

        List<String> queryIds = followingIds.size() > 10 ? followingIds.subList(0, 10) : followingIds;

        Query query = db.collection("posts")
                .whereIn("authorUid", queryIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50);

        followedContentListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                mFollowedContent.setValue(new ArrayList<>());
                return;
            }
            List<Object> content = new ArrayList<>();
            if (value != null) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    // --- FIX: Add try-catch here as well ---
                    try {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setId(doc.getId());
                            content.add(post);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing followed content post: " + doc.getId(), e);
                    }
                    // --- END FIX ---
                }
            }
            mFollowedContent.setValue(content);
        });
    }

    /**
     * Refresh user profile data (useful after editing profile)
     */
    public void refreshProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            fetchUserProfile(user.getUid());
        }
    }

    /**
     * Sign out and clear all data
     */
    public void signOut() {
        mAuth.signOut();
        clearData();
    }

    /**
     * Clear all data and remove all listeners
     */
    private void clearData() {
        if (userProfileListener != null) userProfileListener.remove();
        if (userHistoryListener != null) userHistoryListener.remove();
        if (followingChannelsListener != null) followingChannelsListener.remove();
        if (followingUsersListener != null) followingUsersListener.remove();
        if (userPostsListener != null) userPostsListener.remove();
        if (userPlaylistsListener != null) userPlaylistsListener.remove();
        if (followedContentListener != null) followedContentListener.remove();

        mUserProfile.setValue(null);
        mUserVideos.setValue(new ArrayList<>());
        mUserPodcasts.setValue(new ArrayList<>());
        mUserHistory.setValue(new ArrayList<>());
        mUserDownloads.setValue(new ArrayList<>());
        mFollowing.setValue(new ArrayList<>());
        mFollowingChannels.setValue(new ArrayList<>());
        mFollowingUsers.setValue(new ArrayList<>());
        mUserPosts.setValue(new ArrayList<>());
        mUserPlaylists.setValue(new ArrayList<>());
        mFollowedContent.setValue(new ArrayList<>());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        clearData();
    }
}