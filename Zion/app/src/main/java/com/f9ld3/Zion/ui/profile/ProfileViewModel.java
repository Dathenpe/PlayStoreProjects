package com.f9ld3.Zion.ui.profile;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.data.HistoryItem; // 🔥 NEW IMPORT
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    // LiveData for user's uploaded media (My Uploads feature)
    private final MutableLiveData<List<PlayerMedia>> mUserMedia = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getUserMedia() { return mUserMedia; }

    // 🔥 NEW LiveData for user's history
    private final MutableLiveData<List<HistoryItem>> mUserHistory = new MutableLiveData<>();
    public LiveData<List<HistoryItem>> getUserHistory() { return mUserHistory; }

    private ListenerRegistration userMediaListener;
    private ListenerRegistration userHistoryListener; // 🔥 NEW LISTENER

    public ProfileViewModel() {
        // Start observing authentication state immediately
        mAuth.addAuthStateListener(this::onAuthStateChanged);
    }

    private void onAuthStateChanged(FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            fetchUserProfile(user);
            fetchUserMedia(user.getUid());
            fetchUserHistory(user.getUid()); // 🔥 NEW: Fetch history when user is logged in
        } else {
            // User signed out or is anonymous, reset state
            mUserProfile.setValue(new UserProfile(user != null ? user.getUid() : "guest_id", "Guest User", "anonymous", "")); // Provide default guest profile
            mUserMedia.setValue(new ArrayList<>());
            mUserHistory.setValue(new ArrayList<>()); // 🔥 Reset history

            if (userMediaListener != null) {
                userMediaListener.remove();
                userMediaListener = null;
            }
            if (userHistoryListener != null) { // 🔥 Clear history listener
                userHistoryListener.remove();
                userHistoryListener = null;
            }
        }
    }

    private void fetchUserProfile(FirebaseUser user) {
        if (user.isAnonymous()) {
            // Default profile for the guest user is set in onAuthStateChanged
            return;
        }

        // Fetch the custom profile from Firestore
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                    if (profile != null) {
                        mUserProfile.setValue(profile);
                    } else {
                        // User signed up with Firebase Auth but no custom profile exists in Firestore.
                        // Create a default profile.
                        String username = user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : "My Profile";
                        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

                        UserProfile defaultProfile = new UserProfile(user.getUid(), username, user.getEmail(), photoUrl);
                        mUserProfile.setValue(defaultProfile);

                        // Optionally save the default profile to Firestore
                        db.collection("users").document(user.getUid()).set(defaultProfile)
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to save default user profile.", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch user profile from Firestore.", e));
    }

    private void fetchUserMedia(String uid) {
        // Clear previous listener if it exists
        if (userMediaListener != null) userMediaListener.remove();

        // Fetch media uploaded by this user, ordered by creation date
        userMediaListener = db.collection("media")
                .whereEqualTo("uploaderUid", uid)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user media.", error);
                        mUserMedia.setValue(new ArrayList<>());
                        return;
                    }

                    List<PlayerMedia> mediaList = new ArrayList<>();
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            PlayerMedia media = doc.toObject(PlayerMedia.class);
                            if (media != null) {
                                mediaList.add(media);
                            }
                        }
                    }
                    mUserMedia.setValue(mediaList);
                });
    }

    /**
     * 🔥 NEW: Fetches the user's recent viewing history from a sub-collection.
     */
    private void fetchUserHistory(String uid) {
        if (userHistoryListener != null) userHistoryListener.remove();

        userHistoryListener = db.collection("users").document(uid).collection("history")
                .orderBy("viewedAt", Query.Direction.DESCENDING) // Order by most recent view
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user history.", error);
                        mUserHistory.setValue(new ArrayList<>());
                        return;
                    }

                    List<HistoryItem> historyList = new ArrayList<>();
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
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


    /**
     * Signs out the current user. MainActivity should handle signing them back in anonymously if needed.
     */
    public void signOut() {
        mAuth.signOut();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove listeners to prevent memory leaks
        if (userMediaListener != null) {
            userMediaListener.remove();
        }
        if (userHistoryListener != null) {
            userHistoryListener.remove();
        }
        // Remove auth state listener to prevent leaks
        mAuth.removeAuthStateListener(this::onAuthStateChanged);
    }
}