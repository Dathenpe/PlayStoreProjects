// main/java/com/f9ld3/Zion/ui/social/FollowViewModel.java
package com.f9ld3.Zion.ui.social;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.ui.notifications.NotificationViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // <-- IMPORT
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // <-- IMPORT

public class FollowViewModel extends ViewModel {

    private static final String TAG = "FollowViewModel";
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // --- REFACTORED: Use a map to track status for multiple users ---
    private final ConcurrentHashMap<String, MutableLiveData<Boolean>> followingStatusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ListenerRegistration> statusListeners = new ConcurrentHashMap<>();
    // --- END REFACTOR ---

    private final MutableLiveData<Integer> _followerCount = new MutableLiveData<>(0);
    public LiveData<Integer> getFollowerCount() { return _followerCount; }

    private final MutableLiveData<String> _message = new MutableLiveData<>();
    public LiveData<String> getMessage() { return _message; }

    // --- REFACTORED: This method now returns the correct LiveData from the map ---
    public LiveData<Boolean> isFollowing(String targetUserId) {
        if (targetUserId == null) {
            return new MutableLiveData<>(false);
        }
        return followingStatusMap.computeIfAbsent(targetUserId, id -> {
            MutableLiveData<Boolean> liveData = new MutableLiveData<>(false);
            startFollowStatusListener(id, liveData); // Start a specific listener
            return liveData;
        });
    }

    // --- REFACTORED: This just starts the listener now ---
    public void checkFollowStatus(String targetUserId) {
        // Simply call isFollowing to ensure the listener is started
        isFollowing(targetUserId);
    }

    // --- NEW: Listener method ---
    private void startFollowStatusListener(String targetUserId, MutableLiveData<Boolean> liveData) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            liveData.postValue(false);
            return;
        }

        // Stop existing listener for this user
        stopListener(targetUserId);

        ListenerRegistration listener = db.collection("users").document(currentUser.getUid()).collection("following").document(targetUserId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error checking follow status for " + targetUserId, e);
                        liveData.postValue(false);
                        return;
                    }
                    liveData.postValue(documentSnapshot != null && documentSnapshot.exists());
                });

        statusListeners.put(targetUserId, listener); // Store the listener
    }

    // --- NEW: Helper to stop a single listener ---
    private void stopListener(String listenerKey) {
        ListenerRegistration listener = statusListeners.remove(listenerKey);
        if (listener != null) {
            listener.remove();
        }
    }

    public void loadFollowerCount(String userId) {
        db.collection("users").document(userId).collection("followers")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error loading follower count", e);
                        _followerCount.setValue(0);
                        return;
                    }
                    if (snapshot != null) {
                        _followerCount.setValue(snapshot.size());
                    }
                });
    }

    public void followUser(String targetUserId, String targetUsername) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            _message.setValue("Please log in to follow users");
            return;
        }

        String currentUserId = currentUser.getUid();
        String currentUsername = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous";

        Map<String, Object> followingData = new HashMap<>();
        followingData.put("userId", targetUserId);
        followingData.put("username", targetUsername);
        followingData.put("type", "user");
        followingData.put("followedAt", Timestamp.now());

        db.collection("users").document(currentUserId).collection("following").document(targetUserId)
                .set(followingData)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> followerData = new HashMap<>();
                    followerData.put("userId", currentUserId);
                    followerData.put("username", currentUsername);
                    followerData.put("followedAt", Timestamp.now());

                    db.collection("users").document(targetUserId).collection("followers").document(currentUserId)
                            .set(followerData)
                            .addOnSuccessListener(aVoid2 -> {
                                // --- REFACTORED: No longer set _isFollowing. The listener will auto-update. ---
                                _message.setValue("Following " + targetUsername);

                                // Use centralized NotificationViewModel
                                Map<String, Object> notificationData = new HashMap<>();
                                notificationData.put("followerName", currentUsername);
                                notificationData.put("followerId", currentUserId);
                                NotificationViewModel.sendNotification(
                                        targetUserId,
                                        "follow",
                                        "New Follower",
                                        currentUsername + " started following you",
                                        notificationData
                                );
                            })
                            .addOnFailureListener(e -> _message.setValue("Failed to follow"));
                })
                .addOnFailureListener(e -> _message.setValue("Failed to follow"));
    }

    public void unfollowUser(String targetUserId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();

        db.collection("users").document(currentUserId).collection("following").document(targetUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(targetUserId).collection("followers").document(currentUserId)
                            .delete()
                            .addOnSuccessListener(aVoid2 -> {
                                // --- REFACTORED: No longer set _isFollowing. The listener will auto-update. ---
                                _message.setValue("Unfollowed");
                            });
                })
                .addOnFailureListener(e -> _message.setValue("Failed to unfollow"));
    }

    public void clearMessage() {
        _message.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // --- NEW: Clear all listeners ---
        statusListeners.values().forEach(ListenerRegistration::remove);
        statusListeners.clear();
        followingStatusMap.clear();
        Log.d(TAG, "FollowViewModel cleared, all status listeners removed.");
    }
}