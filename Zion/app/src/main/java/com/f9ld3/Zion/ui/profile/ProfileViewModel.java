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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

    // Mixed Content (Posts + Media)
    private final MutableLiveData<List<Object>> mFollowedContent = new MutableLiveData<>();
    public LiveData<List<Object>> getFollowedContent() { return mFollowedContent; }

    // Map for individual user mixed content (key: userId)
    private final ConcurrentHashMap<String, MutableLiveData<List<Object>>> userMixedContentMap = new ConcurrentHashMap<>();

    private List<String> localFollowingChannelIds = new ArrayList<>();
    private List<String> localFollowingUserIds = new ArrayList<>();
    private List<String> currentFollowedContentIds = new ArrayList<>();

    private final MutableLiveData<List<UserProfile>> mFollowingChannels = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<UserProfile>> getFollowingChannels() { return mFollowingChannels; }

    private final MutableLiveData<List<UserProfile>> mFollowingUsers = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<UserProfile>> getFollowingUsers() { return mFollowingUsers; }

    // Listeners
    private ListenerRegistration userProfileListener, userHistoryListener, followingChannelsListener, followingUsersListener, userPostsListener, userPlaylistsListener;
    // Removed single followedContentListener in favor of manual refresh or specific handling to avoid flashing
    // For this implementation, we will use one-shot queries for mixed content to allow easier merging,
    // or parallel listeners if real-time updates are strictly required.
    // Given "no flashing" requirement, re-fetching on change is safer than complex merging of streams.

    public ProfileViewModel() {
        mUserVideos.setValue(new ArrayList<>());
        mUserPodcasts.setValue(new ArrayList<>());
        mUserHistory.setValue(new ArrayList<>());
        mUserDownloads.setValue(new ArrayList<>());
        mFollowing.setValue(new ArrayList<>());
        mUserPosts.setValue(new ArrayList<>());
        mUserPlaylists.setValue(new ArrayList<>());
        mFollowedContent.setValue(new ArrayList<>());
    }

    public void loadDataForCurrentUser(FirebaseUser user) {
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

    public void handleSignOut() {
        clearData();
    }

    public void fetchUserProfile(String uid) {
        if (uid == null || uid.isEmpty()) { mUserProfile.setValue(null); return; }
        if (userProfileListener != null) userProfileListener.remove();
        userProfileListener = db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) { mUserProfile.setValue(null); return; }
            mUserProfile.setValue(snapshot.toObject(UserProfile.class));
        });
    }

    public void fetchUserVideos(String uid) {
        db.collection("media").whereEqualTo("uploaderUid", uid).whereEqualTo("type", PlayerMedia.TYPE_VIDEO)
                .orderBy("dateCreated", Query.Direction.DESCENDING).limit(50).addSnapshotListener((value, error) -> {
                    if (error != null) { mUserVideos.setValue(new ArrayList<>()); return; }
                    List<PlayerMedia> videos = new ArrayList<>();
                    if (value != null) { for (DocumentSnapshot doc : value.getDocuments()) { PlayerMedia item = doc.toObject(PlayerMedia.class); if (item != null) videos.add(item); } }
                    mUserVideos.setValue(videos);
                });
    }

    public void fetchUserPodcasts(String uid) {
        db.collection("media").whereEqualTo("uploaderUid", uid).whereEqualTo("type", PlayerMedia.TYPE_PODCAST_SINGLE)
                .orderBy("dateCreated", Query.Direction.DESCENDING).limit(50).addSnapshotListener((value, error) -> {
                    if (error != null) { mUserPodcasts.setValue(new ArrayList<>()); return; }
                    List<PlayerMedia> podcasts = new ArrayList<>();
                    if (value != null) { for (DocumentSnapshot doc : value.getDocuments()) { PlayerMedia item = doc.toObject(PlayerMedia.class); if (item != null) podcasts.add(item); } }
                    mUserPodcasts.setValue(podcasts);
                });
    }

    void fetchUserHistory(String uid) {
        if (userHistoryListener != null) userHistoryListener.remove();
        userHistoryListener = db.collection("users").document(uid).collection("history")
                .orderBy("viewedAt", Query.Direction.DESCENDING).limit(50).addSnapshotListener((value, error) -> {
                    List<HistoryItem> historyList = new ArrayList<>();
                    if (value != null) { for (DocumentSnapshot doc : value.getDocuments()) { HistoryItem item = doc.toObject(HistoryItem.class); if (item != null) historyList.add(item); } }
                    mUserHistory.setValue(historyList);
                });
    }

    void fetchUserPosts(String uid) {
        if (userPostsListener != null) userPostsListener.remove();
        userPostsListener = db.collection("posts").whereEqualTo("authorUid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(50).addSnapshotListener((value, error) -> {
                    if (error != null) { mUserPosts.setValue(new ArrayList<>()); return; }
                    List<Post> posts = new ArrayList<>();
                    if (value != null) { for (DocumentSnapshot doc : value.getDocuments()) { try { Post post = doc.toObject(Post.class); if (post != null) { post.setId(doc.getId()); posts.add(post); } } catch (Exception e) { Log.e(TAG, "Error parsing post", e); } } }
                    mUserPosts.setValue(posts);
                });
    }

    // --- NEW: Fetch Mixed Content for Specific User ---
    public LiveData<List<Object>> getUserMixedContent(String uid) {
        return userMixedContentMap.computeIfAbsent(uid, k -> new MutableLiveData<>());
    }

    public void fetchUserMixedContent(String uid) {
        if (uid == null) return;

        // Task to fetch Posts
        Task<List<Post>> postsTask = db.collection("posts")
                .whereEqualTo("authorUid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return new ArrayList<>();
                    List<Post> posts = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        Post p = doc.toObject(Post.class);
                        if (p != null) { p.setId(doc.getId()); posts.add(p); }
                    }
                    return posts;
                });

        // Task to fetch Media (Videos/Podcasts)
        Task<List<PlayerMedia>> mediaTask = db.collection("media")
                .whereEqualTo("uploaderUid", uid)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return new ArrayList<>();
                    List<PlayerMedia> media = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        PlayerMedia m = doc.toObject(PlayerMedia.class);
                        if (m != null) media.add(m);
                    }
                    return media;
                });

        // Combine Results
        Tasks.whenAllSuccess(postsTask, mediaTask).addOnSuccessListener(results -> {
            List<Object> combined = new ArrayList<>();
            List<Post> posts = (List<Post>) results.get(0);
            List<PlayerMedia> media = (List<PlayerMedia>) results.get(1);

            combined.addAll(posts);
            combined.addAll(media);

            // Sort combined list by date
            Collections.sort(combined, (o1, o2) -> {
                long t1 = getTime(o1);
                long t2 = getTime(o2);
                return Long.compare(t2, t1); // Descending
            });

            // Update LiveData
            MutableLiveData<List<Object>> liveData = userMixedContentMap.get(uid);
            if (liveData != null) {
                liveData.setValue(combined);
            }
        });
    }

    private void fetchUserPlaylists(String uid) {
        if (userPlaylistsListener != null) userPlaylistsListener.remove();
        userPlaylistsListener = db.collection("users").document(uid).collection("playlists")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(50).addSnapshotListener((value, error) -> {
                    List<Playlist> playlists = new ArrayList<>();
                    if (value != null) { for (DocumentSnapshot doc : value.getDocuments()) { Playlist playlist = doc.toObject(Playlist.class); if (playlist != null) { playlist.setId(doc.getId()); playlists.add(playlist); } } }
                    mUserPlaylists.setValue(playlists);
                });
    }

    private void createDefaultMyListPlaylist(String uid) { /* ... */ }

    void fetchFollowingChannels(String uid) {
        if (followingChannelsListener != null) followingChannelsListener.remove();
        followingChannelsListener = db.collection("users").document(uid).collection("following")
                .whereEqualTo("type", "channel")
                .orderBy("followedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    localFollowingChannelIds.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) { localFollowingChannelIds.add(doc.getId()); }
                    }
                    updateCombinedFollowing();
                });
    }

    void fetchFollowingUsers(String uid) {
        if (followingUsersListener != null) followingUsersListener.remove();
        followingUsersListener = db.collection("users").document(uid).collection("following")
                .whereEqualTo("type", "user")
                .orderBy("followedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    localFollowingUserIds.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) { localFollowingUserIds.add(doc.getId()); }
                    }
                    updateCombinedFollowing();
                });
    }

    private void updateCombinedFollowing() {
        List<String> allFollowingIds = new ArrayList<>();
        allFollowingIds.addAll(localFollowingUserIds);
        allFollowingIds.addAll(localFollowingChannelIds);
        fetchFollowedUserProfiles(allFollowingIds);
        fetchFollowedContent(allFollowingIds);
    }

    private void fetchFollowedUserProfiles(List<String> followingIds) {
        // ... (Existing implementation kept as is) ...
        if (followingIds == null || followingIds.isEmpty()) {
            mFollowing.setValue(new ArrayList<>());
            mFollowingChannels.setValue(new ArrayList<>());
            mFollowingUsers.setValue(new ArrayList<>());
            return;
        }
        List<String> queryIds = followingIds.size() > 10 ? followingIds.subList(0, 10) : followingIds;
        db.collection("users").whereIn(FieldPath.documentId(), queryIds).get()
                .addOnSuccessListener(value -> {
                    List<UserProfile> all = new ArrayList<>();
                    List<UserProfile> channels = new ArrayList<>();
                    List<UserProfile> users = new ArrayList<>();
                    if (value != null) {
                        all = value.toObjects(UserProfile.class);
                        for (UserProfile p : all) {
                            if (localFollowingUserIds.contains(p.getUserId())) users.add(p);
                            if (localFollowingChannelIds.contains(p.getUserId())) channels.add(p);
                        }
                    }
                    mFollowing.setValue(all);
                    mFollowingUsers.setValue(users);
                    mFollowingChannels.setValue(channels);
                });
    }

    // --- UPDATED: Merged Fetch for "All" Tab ---
    private void fetchFollowedContent(List<String> followingIds) {
        if (followingIds == null || followingIds.isEmpty()) {
            mFollowedContent.setValue(new ArrayList<>());
            currentFollowedContentIds.clear();
            return;
        }

        // Limit ids
        List<String> queryIds = followingIds.size() > 10 ? followingIds.subList(0, 10) : followingIds;

        // Check if IDs changed to avoid unnecessary re-fetch
        if (currentFollowedContentIds.equals(queryIds)) return;
        currentFollowedContentIds = new ArrayList<>(queryIds);

        // 1. Fetch Posts
        Task<List<Post>> postsTask = db.collection("posts")
                .whereIn("authorUid", queryIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return new ArrayList<>();
                    List<Post> posts = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        try {
                            Post p = doc.toObject(Post.class);
                            if (p != null) { p.setId(doc.getId()); posts.add(p); }
                        } catch(Exception e) { Log.e(TAG, "Post parse error", e); }
                    }
                    return posts;
                });

        // 2. Fetch Media
        Task<List<PlayerMedia>> mediaTask = db.collection("media")
                .whereIn("uploaderUid", queryIds)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return new ArrayList<>();
                    List<PlayerMedia> media = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        try {
                            PlayerMedia m = doc.toObject(PlayerMedia.class);
                            if (m != null) media.add(m);
                        } catch(Exception e) { Log.e(TAG, "Media parse error", e); }
                    }
                    return media;
                });

        // 3. Merge & Sort
        Tasks.whenAllSuccess(postsTask, mediaTask).addOnSuccessListener(results -> {
            List<Object> combined = new ArrayList<>();
            combined.addAll((List<Post>) results.get(0));
            combined.addAll((List<PlayerMedia>) results.get(1));

            Collections.sort(combined, (o1, o2) -> Long.compare(getTime(o2), getTime(o1)));

            mFollowedContent.setValue(combined);
            Log.d(TAG, "Followed content updated. Total items: " + combined.size());
        });
    }

    private long getTime(Object o) {
        if (o instanceof Post) {
            Post p = (Post) o;
            return p.getTimestamp() != null ? p.getTimestamp().toDate().getTime() : 0;
        } else if (o instanceof PlayerMedia) {
            PlayerMedia m = (PlayerMedia) o;
            return m.getDateCreated() != null ? m.getDateCreated().toDate().getTime() : 0;
        }
        return 0;
    }

    private void clearData() {
        if (userProfileListener != null) userProfileListener.remove();
        if (userHistoryListener != null) userHistoryListener.remove();
        if (followingChannelsListener != null) followingChannelsListener.remove();
        if (followingUsersListener != null) followingUsersListener.remove();
        if (userPostsListener != null) userPostsListener.remove();
        if (userPlaylistsListener != null) userPlaylistsListener.remove();

        mUserProfile.setValue(null);
        mUserVideos.setValue(new ArrayList<>());
        mUserPodcasts.setValue(new ArrayList<>());
        mUserHistory.setValue(new ArrayList<>());
        mFollowing.setValue(new ArrayList<>());
        mFollowingChannels.setValue(new ArrayList<>());
        mFollowingUsers.setValue(new ArrayList<>());
        mUserPosts.setValue(new ArrayList<>());
        mUserPlaylists.setValue(new ArrayList<>());
        mFollowedContent.setValue(new ArrayList<>());

        localFollowingChannelIds.clear();
        localFollowingUserIds.clear();
        currentFollowedContentIds.clear();
        userMixedContentMap.clear();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        clearData();
    }
}