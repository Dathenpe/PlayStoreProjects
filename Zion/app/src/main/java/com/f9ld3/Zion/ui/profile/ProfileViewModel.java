package com.f9ld3.Zion.ui.profile;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.data.HistoryItem;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<UserProfile> mUserProfile = new MutableLiveData<>();
    public LiveData<UserProfile> getUserProfile() { return mUserProfile; }

    private final MutableLiveData<List<PlayerMedia>> mUserMedia = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserMedia() { return mUserMedia; }

    private final MutableLiveData<List<HistoryItem>> mUserHistory = new MutableLiveData<>();
    public LiveData<List<HistoryItem>> getUserHistory() { return mUserHistory; }

    // LiveData for the Downloads section (Placeholder for now)
    private final MutableLiveData<List<PlayerMedia>> mUserDownloads = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserDownloads() { return mUserDownloads; }

    // LiveData for the list of followed CHANNELS (users = channels)
    private final MutableLiveData<List<UserProfile>> mFollowingChannels = new MutableLiveData<>();
    public LiveData<List<UserProfile>> getFollowingChannels() { return mFollowingChannels; }

    // NEW: LiveData for user's blog posts
    private final MutableLiveData<List<com.f9ld3.Zion.ui.feed.Post>> mUserBlogs = new MutableLiveData<>();
    public LiveData<List<com.f9ld3.Zion.ui.feed.Post>> getUserBlogs() { return mUserBlogs; }


    private ListenerRegistration userProfileListener;
    private ListenerRegistration userMediaListener;
    private ListenerRegistration userHistoryListener;
    private ListenerRegistration followingChannelsListener;
    private ListenerRegistration userBlogsListener; // NEW listener for blogs

    public ProfileViewModel() {
        // Start listening for auth state changes immediately
        mAuth.addAuthStateListener(this::onAuthStateChanged);
    }

    private void onAuthStateChanged(FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // User is signed in (may be anonymous)
            Log.d(TAG, "Auth state changed. User UID: " + user.getUid());
            fetchUserProfile(user.getUid());
            fetchUserMedia(user.getUid());
            fetchUserHistory(user.getUid());
            fetchFollowingChannels(user.getUid()); // Fetch following channels (users)
            fetchUserBlogs(user.getUid()); // NEW: Fetch user's blog posts
            // Placeholder: Initialize Downloads data
            mUserDownloads.setValue(new ArrayList<>());
        } else {
            // User is signed out
            Log.d(TAG, "User signed out or null.");
            clearData();
        }
    }

    private void fetchUserProfile(String uid) {
        // Fetch public profile data from the 'users' collection
        userProfileListener = db.collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for user profile.", e);
                        mUserProfile.setValue(null);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        UserProfile profile = snapshot.toObject(UserProfile.class);
                        // Ensure a profile is always available for the fragment to display the UID/Email
                        if (profile == null) {
                            profile = new UserProfile(uid, "Anonymous", mAuth.getCurrentUser().getEmail(), null);
                        }
                        mUserProfile.setValue(profile);
                    } else {
                        // User exists but no profile document found (e.g., first anonymous login)
                        mUserProfile.setValue(new UserProfile(uid, "Anonymous", mAuth.getCurrentUser().getEmail(), null));
                    }
                });
    }

    private void fetchUserMedia(String uid) {
        // Fetch media uploaded by this user, ordered by date
        userMediaListener = db.collection("media")
                .whereEqualTo("uploaderUid", uid)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(20) // Limit the number of media items for the profile view
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user media.", error);
                        mUserMedia.setValue(new ArrayList<>());
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
                    mUserMedia.setValue(mediaList);
                    Log.d(TAG, "Fetched " + mediaList.size() + " user media items for user " + uid);
                });
    }

    private void fetchUserHistory(String uid) {
        // Fetch viewing history, ordered by viewedAt timestamp
        userHistoryListener = db.collection("users")
                .document(uid)
                .collection("history")
                .orderBy("viewedAt", Query.Direction.DESCENDING)
                .limit(20) // Limit the number of history items
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user history.", error); // Fixed: use 'error'
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
                    Log.d(TAG, "Fetched " + historyList.size() + " history items for user " + uid);
                });
    }

    // Fetch the list of followed CHANNELS (other users, since users = channels)
    private void fetchFollowingChannels(String uid) {
        // Subcollection "following" under the user document, where each doc ID is followed user ID,
        // and doc contains UserProfile of the followed user (channel).
        followingChannelsListener = db.collection("users")
                .document(uid)
                .collection("following")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for following channels list.", error);
                        mFollowingChannels.setValue(new ArrayList<>());
                        return;
                    }

                    List<UserProfile> followedChannels = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            UserProfile profile = doc.toObject(UserProfile.class);
                            if (profile != null) {
                                followedChannels.add(profile);
                            }
                        }
                    }
                    mFollowingChannels.setValue(followedChannels);
                    Log.d(TAG, "Fetched " + followedChannels.size() + " followed channels for user " + uid);
                });
    }

    // NEW: Fetch user's blog posts
    private void fetchUserBlogs(String uid) {
        userBlogsListener = db.collection("posts")
                .whereEqualTo("authorUid", uid) // Assuming 'authorUid' field exists in Post
                .whereEqualTo("type", "blog")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user blogs.", error);
                        mUserBlogs.setValue(new ArrayList<>());
                        return;
                    }

                    List<com.f9ld3.Zion.ui.feed.Post> blogList = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            com.f9ld3.Zion.ui.feed.Post post = doc.toObject(com.f9ld3.Zion.ui.feed.Post.class);
                            if (post != null) {
                                post.id = doc.getId(); // Ensure ID is set
                                blogList.add(post);
                            }
                        }
                    }
                    mUserBlogs.setValue(blogList);
                    Log.d(TAG, "Fetched " + blogList.size() + " user blog posts for user " + uid);
                });
    }


    // Method to get media for a specific channel (used by FollowedContentFragment)
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


    public void signOut() {
        mAuth.signOut();
        clearData();
    }

    private void clearData() {
        if (userProfileListener != null) userProfileListener.remove();
        if (userMediaListener != null) userMediaListener.remove();
        if (userHistoryListener != null) userHistoryListener.remove();
        if (followingChannelsListener != null) followingChannelsListener.remove();
        if (userBlogsListener != null) userBlogsListener.remove(); // NEW: Remove blogs listener

        mUserProfile.setValue(null);
        mUserMedia.setValue(new ArrayList<>());
        mUserHistory.setValue(new ArrayList<>());
        mUserDownloads.setValue(new ArrayList<>());
        mFollowingChannels.setValue(new ArrayList<>());
        mUserBlogs.setValue(new ArrayList<>()); // NEW: Clear blogs data
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        clearData();
    }
}