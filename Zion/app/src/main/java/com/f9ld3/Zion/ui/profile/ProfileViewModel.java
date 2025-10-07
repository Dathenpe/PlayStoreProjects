// MultipleFiles/ProfileViewModel.java
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

    private final MutableLiveData<List<PlayerMedia>> mUserDownloads = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserDownloads() { return mUserDownloads; }

    private final MutableLiveData<List<UserProfile>> mFollowingChannels = new MutableLiveData<>();
    public LiveData<List<UserProfile>> getFollowingChannels() { return mFollowingChannels; }

    private final MutableLiveData<List<com.f9ld3.Zion.ui.feed.Post>> mUserBlogs = new MutableLiveData<>();
    public LiveData<List<com.f9ld3.Zion.ui.feed.Post>> getUserBlogs() { return mUserBlogs; }


    private ListenerRegistration userProfileListener;
    private ListenerRegistration userMediaListener;
    private ListenerRegistration userHistoryListener;
    private ListenerRegistration followingChannelsListener;
    private ListenerRegistration userBlogsListener;

    public ProfileViewModel() {
        mAuth.addAuthStateListener(this::onAuthStateChanged);
    }

    private void onAuthStateChanged(FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            Log.d(TAG, "Auth state changed. User UID: " + user.getUid());
            fetchUserProfile(user.getUid());
            fetchUserMedia(user.getUid());
            fetchUserHistory(user.getUid());
            fetchFollowingChannels(user.getUid());
            fetchUserBlogs(user.getUid()); // NEW: Fetch user blogs
            mUserDownloads.setValue(new ArrayList<>()); // Placeholder
        } else {
            Log.d(TAG, "User signed out or null.");
            clearData();
        }
    }

    private void fetchUserProfile(String uid) {
        if (userProfileListener != null) userProfileListener.remove(); // Remove previous listener
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
                            // Fallback for anonymous or newly created users without full profile data
                            String email = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "N/A";
                            profile = new UserProfile(uid, "Anonymous", email, null);
                        }
                        mUserProfile.setValue(profile);
                    } else {
                        // User exists but no profile document found (e.g., first anonymous login)
                        String email = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "N/A";
                        mUserProfile.setValue(new UserProfile(uid, "Anonymous", email, null));
                    }
                });
    }

    private void fetchUserMedia(String uid) {
        if (userMediaListener != null) userMediaListener.remove();
        userMediaListener = db.collection("media")
                .whereEqualTo("uploaderUid", uid)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(20)
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
        if (userHistoryListener != null) userHistoryListener.remove();
        userHistoryListener = db.collection("users")
                .document(uid)
                .collection("history")
                .orderBy("viewedAt", Query.Direction.DESCENDING)
                .limit(20)
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
                    Log.d(TAG, "Fetched " + historyList.size() + " history items for user " + uid);
                });
    }

    private void fetchFollowingChannels(String uid) {
        if (followingChannelsListener != null) followingChannelsListener.remove();
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

    private void fetchUserBlogs(String uid) {
        if (userBlogsListener != null) userBlogsListener.remove();
        userBlogsListener = db.collection("posts")
                .whereEqualTo("authorUid", uid)
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
                                post.id = doc.getId();
                                blogList.add(post);
                            }
                        }
                    }
                    mUserBlogs.setValue(blogList);
                    Log.d(TAG, "Fetched " + blogList.size() + " user blog posts for user " + uid);
                });
    }

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
        if (userBlogsListener != null) userBlogsListener.remove();

        mUserProfile.setValue(null);
        mUserMedia.setValue(new ArrayList<>());
        mUserHistory.setValue(new ArrayList<>());
        mUserDownloads.setValue(new ArrayList<>());
        mFollowingChannels.setValue(new ArrayList<>());
        mUserBlogs.setValue(new ArrayList<>());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        clearData();
    }
}