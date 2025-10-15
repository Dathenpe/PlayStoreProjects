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
import java.util.HashMap;
import java.util.Map;

public class FollowViewModel extends ViewModel {

    private static final String TAG = "FollowViewModel";
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<Boolean> _isFollowing = new MutableLiveData<>(false);
    public LiveData<Boolean> isFollowing() { return _isFollowing; }

    private final MutableLiveData<Integer> _followerCount = new MutableLiveData<>(0);
    public LiveData<Integer> getFollowerCount() { return _followerCount; }

    private final MutableLiveData<String> _message = new MutableLiveData<>();
    public LiveData<String> getMessage() { return _message; }

    public void checkFollowStatus(String targetUserId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            _isFollowing.setValue(false);
            return;
        }

        db.collection("users").document(currentUser.getUid()).collection("following").document(targetUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> _isFollowing.setValue(documentSnapshot.exists()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking follow status", e);
                    _isFollowing.setValue(false);
                });
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
                                _isFollowing.setValue(true);
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
                                _isFollowing.setValue(false);
                                _message.setValue("Unfollowed");
                            });
                })
                .addOnFailureListener(e -> _message.setValue("Failed to unfollow"));
    }

    public void clearMessage() {
        _message.setValue(null);
    }
}
