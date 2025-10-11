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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewModel for the Profile tab - handles loading and observing user data
 * Separate from EditProfileViewModel which handles editing
 */
public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // User Profile
    private final MutableLiveData<UserProfile> mUserProfile = new MutableLiveData<>();
    public LiveData<UserProfile> getUserProfile() { return mUserProfile; }

    // Media: Videos and Podcasts
    private final MutableLiveData<List<PlayerMedia>> mUserVideos = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserVideos() { return mUserVideos; }

    private final MutableLiveData<List<PlayerMedia>> mUserPodcasts = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserPodcasts() { return mUserPodcasts; }

    private final MutableLiveData<List<PlayerMedia>> mUserMedia = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserMedia() { return mUserMedia; }

    // History
    private final MutableLiveData<List<HistoryItem>> mUserHistory = new MutableLiveData<>();
    public LiveData<List<HistoryItem>> getUserHistory() { return mUserHistory; }

    // Downloads (placeholder for now)
    private final MutableLiveData<List<PlayerMedia>> mUserDownloads = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserDownloads() { return mUserDownloads; }

    // Following
    private final MutableLiveData<List<UserProfile>> mFollowingChannels = new MutableLiveData<>();
    public LiveData<List<UserProfile>> getFollowingChannels() { return mFollowingChannels; }

    private final MutableLiveData<List<UserProfile>> mFollowingUsers = new MutableLiveData<>();
    public LiveData<List<UserProfile>> getFollowingUsers() { return mFollowingUsers; }

    private final MutableLiveData<List<UserProfile>> mFollowing = new MutableLiveData<>();
    public LiveData<List<UserProfile>> getFollowing() { return mFollowing; }

    // Blogs
    private final MutableLiveData<List<Post>> mUserBlogs = new MutableLiveData<>();
    public LiveData<List<Post>> getUserBlogs() { return mUserBlogs; }

    // Likes
    private final MutableLiveData<List<Post>> mUserLikes = new MutableLiveData<>();
    public LiveData<List<Post>> getUserLikes() { return mUserLikes; }

    // Playlists
    private final MutableLiveData<List<Playlist>> mUserPlaylists = new MutableLiveData<>();
    public LiveData<List<Playlist>> getUserPlaylists() { return mUserPlaylists; }

    // Followed Content
    private final MutableLiveData<List<Post>> mFollowedContent = new MutableLiveData<>();
    public LiveData<List<Post>> getFollowedContent() { return mFollowedContent; }

    // Firestore Listeners
    private ListenerRegistration userProfileListener;
    private ListenerRegistration userMediaListener;
    private ListenerRegistration userHistoryListener;
    private ListenerRegistration followingChannelsListener;
    private ListenerRegistration followingUsersListener;
    private ListenerRegistration userBlogsListener;
    private ListenerRegistration userLikesListener;
    private ListenerRegistration userPlaylistsListener;
    private ListenerRegistration followedContentListener;

    public ProfileViewModel() {
        mAuth.addAuthStateListener(this::onAuthStateChanged);
    }

    private void onAuthStateChanged(FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            Log.d(TAG, "Auth state changed. User UID: " + user.getUid());
            String uid = user.getUid();

            // Fetch all user data
            fetchUserProfile(uid);
            fetchUserMedia(uid);
            fetchUserHistory(uid);
            fetchFollowingChannels(uid);
            fetchFollowingUsers(uid);
            fetchUserBlogs(uid);
            fetchUserLikes(uid);
            fetchUserPlaylists(uid);
            fetchFollowedContent(uid);

            // Initialize empty downloads (implement later with local storage)
            mUserDownloads.setValue(new ArrayList<>());
        } else {
            Log.d(TAG, "User signed out or null.");
            clearData();
        }
    }

    private void fetchUserProfile(String uid) {
        if (userProfileListener != null) userProfileListener.remove();

        userProfileListener = db.collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for user profile.", e);
                        mUserProfile.setValue(null);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        UserProfile profile = snapshot.toObject(UserProfile.class);
                        if (profile == null) {
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            String email = currentUser != null ? currentUser.getEmail() : "N/A";
                            profile = new UserProfile(uid, "Anonymous", email, null);
                        }
                        mUserProfile.setValue(profile);
                    } else {
                        // Create from Firebase Auth if Firestore doc doesn't exist
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            String email = currentUser.getEmail();
                            String displayName = currentUser.getDisplayName();
                            String photoUrl = currentUser.getPhotoUrl() != null ?
                                    currentUser.getPhotoUrl().toString() : null;

                            UserProfile profile = new UserProfile(
                                    uid,
                                    displayName != null ? displayName : "Anonymous",
                                    email,
                                    photoUrl
                            );
                            mUserProfile.setValue(profile);
                        }
                    }
                });
    }

    private void fetchUserMedia(String uid) {
        if (userMediaListener != null) userMediaListener.remove();

        userMediaListener = db.collection("media")
                .whereEqualTo("uploaderUid", uid)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user media.", error);
                        mUserMedia.setValue(new ArrayList<>());
                        mUserVideos.setValue(new ArrayList<>());
                        mUserPodcasts.setValue(new ArrayList<>());
                        return;
                    }

                    List<PlayerMedia> allMedia = new ArrayList<>();
                    List<PlayerMedia> videos = new ArrayList<>();
                    List<PlayerMedia> podcasts = new ArrayList<>();

                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            PlayerMedia item = doc.toObject(PlayerMedia.class);
                            if (item != null) {
                                allMedia.add(item);
                                // FIX: Change 'String' to 'int' and compare with the integer constants
                                int type = item.getType();
                                if (type == PlayerMedia.TYPE_VIDEO) {
                                    videos.add(item);
                                } else if (type == PlayerMedia.TYPE_PODCAST_SINGLE || type == PlayerMedia.TYPE_PODCAST_DUO_CONTAINER) {
                                    podcasts.add(item);
                                }
                            }
                        }
                    }

                    mUserMedia.setValue(allMedia);
                    mUserVideos.setValue(videos);
                    mUserPodcasts.setValue(podcasts);
                    Log.d(TAG, "Fetched " + allMedia.size() + " user media (videos: " +
                            videos.size() + ", podcasts: " + podcasts.size() + ")");
                });
    }


    private void fetchUserHistory(String uid) {
        if (userHistoryListener != null) userHistoryListener.remove();

        userHistoryListener = db.collection("users")
                .document(uid)
                .collection("history")
                .orderBy("viewedAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user history.", error);
                        mUserHistory.setValue(new ArrayList<>());
                        return;
                    }

                    List<HistoryItem> historyList = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            HistoryItem item = doc.toObject(HistoryItem.class);
                            if (item != null) {
                                historyList.add(item);
                            }
                        }
                    }
                    mUserHistory.setValue(historyList);
                    Log.d(TAG, "Fetched " + historyList.size() + " history items");
                });
    }

    private void fetchFollowingChannels(String uid) {
        if (followingChannelsListener != null) followingChannelsListener.remove();

        followingChannelsListener = db.collection("users")
                .document(uid)
                .collection("following")
                .whereEqualTo("type", "channel")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for following channels.", error);
                        mFollowingChannels.setValue(new ArrayList<>());
                        return;
                    }

                    List<UserProfile> channels = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            UserProfile profile = doc.toObject(UserProfile.class);
                            if (profile != null) {
                                channels.add(profile);
                            }
                        }
                    }
                    mFollowingChannels.setValue(channels);
                    Log.d(TAG, "Fetched " + channels.size() + " followed channels");
                    updateCombinedFollowing();
                });
    }

    private void fetchFollowingUsers(String uid) {
        if (followingUsersListener != null) followingUsersListener.remove();

        followingUsersListener = db.collection("users")
                .document(uid)
                .collection("following")
                .whereEqualTo("type", "user")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for following users.", error);
                        mFollowingUsers.setValue(new ArrayList<>());
                        return;
                    }

                    List<UserProfile> users = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            UserProfile profile = doc.toObject(UserProfile.class);
                            if (profile != null) {
                                users.add(profile);
                            }
                        }
                    }
                    mFollowingUsers.setValue(users);
                    Log.d(TAG, "Fetched " + users.size() + " followed users");
                    updateCombinedFollowing();
                });
    }

    private void updateCombinedFollowing() {
        List<UserProfile> allFollowing = new ArrayList<>();
        if (mFollowingChannels.getValue() != null) {
            allFollowing.addAll(mFollowingChannels.getValue());
        }
        if (mFollowingUsers.getValue() != null) {
            allFollowing.addAll(mFollowingUsers.getValue());
        }
        mFollowing.setValue(allFollowing);
    }

    private void fetchUserBlogs(String uid) {
        if (userBlogsListener != null) userBlogsListener.remove();

        userBlogsListener = db.collection("posts")
                .whereEqualTo("authorUid", uid)
                .whereEqualTo("type", "blog")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user blogs.", error);
                        mUserBlogs.setValue(new ArrayList<>());
                        return;
                    }

                    List<Post> blogs = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setId(doc.getId());
                                blogs.add(post);
                            }
                        }
                    }
                    mUserBlogs.setValue(blogs);
                    Log.d(TAG, "Fetched " + blogs.size() + " user blogs");
                });
    }

    private void fetchUserLikes(String uid) {
        if (userLikesListener != null) userLikesListener.remove();

        userLikesListener = db.collection("users")
                .document(uid)
                .collection("likes")
                .orderBy("likedAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user likes.", error);
                        mUserLikes.setValue(new ArrayList<>());
                        return;
                    }

                    List<Post> likes = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setId(doc.getId());
                                likes.add(post);
                            }
                        }
                    }
                    mUserLikes.setValue(likes);
                    Log.d(TAG, "Fetched " + likes.size() + " user likes");
                });
    }

    private void fetchUserPlaylists(String uid) {
        if (userPlaylistsListener != null) userPlaylistsListener.remove();

        userPlaylistsListener = db.collection("users")
                .document(uid)
                .collection("playlists")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user playlists.", error);
                        mUserPlaylists.setValue(new ArrayList<>());
                        return;
                    }

                    List<Playlist> playlists = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Playlist playlist = doc.toObject(Playlist.class);
                            if (playlist != null) {
                                playlist.setId(doc.getId());
                                playlists.add(playlist);
                            }
                        }
                    }
                    mUserPlaylists.setValue(playlists);
                    Log.d(TAG, "Fetched " + playlists.size() + " user playlists");
                });
    }

    private void fetchFollowedContent(String uid) {
        if (followedContentListener != null) followedContentListener.remove();

        List<String> followingUids = getFollowingUids();
        if (followingUids.isEmpty()) {
            mFollowedContent.setValue(new ArrayList<>());
            return;
        }

        // Firestore whereIn limit is 10
        if (followingUids.size() > 10) {
            followingUids = followingUids.subList(0, 10);
        }

        followedContentListener = db.collection("posts")
                .whereIn("authorUid", followingUids)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for followed content.", error);
                        mFollowedContent.setValue(new ArrayList<>());
                        return;
                    }

                    List<Post> content = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setId(doc.getId());
                                content.add(post);
                            }
                        }
                    }
                    mFollowedContent.setValue(content);
                    Log.d(TAG, "Fetched " + content.size() + " followed content items");
                });
    }

    private List<String> getFollowingUids() {
        List<UserProfile> following = mFollowing.getValue();
        if (following == null || following.isEmpty()) {
            return new ArrayList<>();
        }
        return following.stream()
                .map(profile -> {
                    String uid = profile.getUserId();
                    return uid != null ? uid : profile.getUid();
                })
                .filter(uid -> uid != null)
                .collect(Collectors.toList());
    }

    /**
     * Get media for a specific channel (used in FollowingActivity)
     */
    public LiveData<List<PlayerMedia>> getMediaForChannel(String channelId) {
        MutableLiveData<List<PlayerMedia>> channelMedia = new MutableLiveData<>();

        db.collection("media")
                .whereEqualTo("uploaderUid", channelId)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for channel media: " + channelId, error);
                        channelMedia.setValue(new ArrayList<>());
                        return;
                    }

                    List<PlayerMedia> mediaList = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            PlayerMedia item = doc.toObject(PlayerMedia.class);
                            if (item != null) {
                                mediaList.add(item);
                            }
                        }
                    }
                    channelMedia.setValue(mediaList);
                    Log.d(TAG, "Fetched " + mediaList.size() + " media items for channel " + channelId);
                });

        return channelMedia;
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
        // Remove all listeners
        if (userProfileListener != null) userProfileListener.remove();
        if (userMediaListener != null) userMediaListener.remove();
        if (userHistoryListener != null) userHistoryListener.remove();
        if (followingChannelsListener != null) followingChannelsListener.remove();
        if (followingUsersListener != null) followingUsersListener.remove();
        if (userBlogsListener != null) userBlogsListener.remove();
        if (userLikesListener != null) userLikesListener.remove();
        if (userPlaylistsListener != null) userPlaylistsListener.remove();
        if (followedContentListener != null) followedContentListener.remove();

        // Clear all LiveData
        mUserProfile.setValue(null);
        mUserMedia.setValue(new ArrayList<>());
        mUserVideos.setValue(new ArrayList<>());
        mUserPodcasts.setValue(new ArrayList<>());
        mUserHistory.setValue(new ArrayList<>());
        mUserDownloads.setValue(new ArrayList<>());
        mFollowing.setValue(new ArrayList<>());
        mFollowingChannels.setValue(new ArrayList<>());
        mFollowingUsers.setValue(new ArrayList<>());
        mUserBlogs.setValue(new ArrayList<>());
        mUserLikes.setValue(new ArrayList<>());
        mUserPlaylists.setValue(new ArrayList<>());
        mFollowedContent.setValue(new ArrayList<>());

        Log.d(TAG, "All data cleared");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        clearData();
        Log.d(TAG, "ProfileViewModel cleared");
    }
}