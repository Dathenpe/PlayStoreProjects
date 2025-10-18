// main/java/com/f9ld3/Zion/ui/feed/PostLikeViewModel.java
package com.f9ld3.Zion.ui.feed;

import android.util.Log; // Add Log import
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import com.f9ld3.Zion.ui.notifications.NotificationViewModel; // For sending notifications
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue; // For atomic increments
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch; // For batch writes

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // For thread-safe map

public class PostLikeViewModel extends ViewModel {

    private static final String TAG = "PostLikeViewModel"; // Added TAG
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Use a Map to hold LiveData for each post's like status
    // ConcurrentHashMap for thread safety if accessed from multiple threads
    private final ConcurrentHashMap<String, MutableLiveData<Boolean>> likedStatusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ListenerRegistration> listeners = new ConcurrentHashMap<>();

    // Optional: LiveData for like count if needed dynamically in UI separate from Post object
    // private final ConcurrentHashMap<String, MutableLiveData<Integer>> likeCountMap = new ConcurrentHashMap<>();

    /**
     * Returns a LiveData object for the like status of a specific post.
     * Creates and starts listening if it doesn't exist.
     */
    public LiveData<Boolean> isLiked(String postId) {
        // ComputeIfAbsent ensures thread-safe creation and retrieval
        return likedStatusMap.computeIfAbsent(postId, id -> {
            MutableLiveData<Boolean> liveData = new MutableLiveData<>(false); // Default to false
            startListening(id, liveData);
            return liveData;
        });
    }

    private void startListening(String postId, MutableLiveData<Boolean> liveData) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || postId == null) {
            liveData.postValue(false); // Post value for background thread safety if needed
            return;
        }

        // Remove existing listener first to avoid duplicates
        stopListening(postId);

        ListenerRegistration listener = db.collection("posts").document(postId)
                .collection("likes").document(user.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for like status on post " + postId, e); // Log error
                        liveData.postValue(false); // Assume not liked on error
                        return;
                    }
                    liveData.postValue(snapshot != null && snapshot.exists());
                });
        listeners.put(postId, listener);
    }

    private void stopListening(String postId) {
        ListenerRegistration listener = listeners.remove(postId);
        if (listener != null) {
            listener.remove();
        }
    }


    public void toggleLike(String postId, Post postData) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || postId == null || postData == null) {
            Log.w(TAG, "toggleLike aborted: User null, postId null, or postData null."); // Log warning
            return; // Need postData for notification
        }

        String userId = user.getUid();
        String postAuthorUid = postData.getAuthorUid();

        MutableLiveData<Boolean> currentStatus = likedStatusMap.get(postId);
        boolean currentlyLiked = currentStatus != null && Boolean.TRUE.equals(currentStatus.getValue());

        // Reference to the specific like document and the main post document
        var likeRef = db.collection("posts").document(postId).collection("likes").document(userId);
        var postRef = db.collection("posts").document(postId);

        // Use a WriteBatch for atomic operations on multiple documents if needed,
        // or use FieldValue for atomic increments/decrements on a single document.
        WriteBatch batch = db.batch();

        if (currentlyLiked) {
            // --- Unlike ---
            Log.d(TAG, "Unliking post: " + postId + " by user: " + userId); // Log action
            batch.delete(likeRef);
            // ** Use Cloud Function for count OR uncomment below **
            // batch.update(postRef, "likeCount", FieldValue.increment(-1));

        } else {
            // --- Like ---
            Log.d(TAG, "Liking post: " + postId + " by user: " + userId); // Log action
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("userId", userId);
            likeData.put("timestamp", com.google.firebase.Timestamp.now());
            if (user.getDisplayName() != null) {
                likeData.put("username", user.getDisplayName());
            }
            batch.set(likeRef, likeData);
            // ** Use Cloud Function for count OR uncomment below **
            // batch.update(postRef, "likeCount", FieldValue.increment(1));

            // Send notification only when liking
            if (postAuthorUid != null && !postAuthorUid.equals(userId)) { // Don't notify self
                Log.d(TAG, "Sending like notification to: " + postAuthorUid); // Log notification send
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("likerName", user.getDisplayName() != null ? user.getDisplayName() : "Someone");
                notificationData.put("likerId", userId);
                notificationData.put("postId", postId); // Add postId for navigation
                notificationData.put("postTextSnippet", postData.getTextContent() != null && postData.getTextContent().length() > 50 ? postData.getTextContent().substring(0, 50) + "..." : postData.getTextContent());

                NotificationViewModel.sendNotification(
                        postAuthorUid,
                        "post_like", // Specific type for post likes
                        "New Post Like",
                        (user.getDisplayName() != null ? user.getDisplayName() : "Someone") + " liked your post.",
                        notificationData
                );
            }
        }

        // Commit the batch
        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Like/Unlike batch committed successfully for post: " + postId);
            // Firestore listener will update the LiveData automatically
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Like/Unlike batch commit failed for post: " + postId, e);
            // Handle failure, maybe revert UI state or show error
        });
    }

    // --- Add method for updating comment count atomically (if NOT using Cloud Functions) ---
    public static void updateCommentCount(String postId, int change) {
        Log.d(TAG, "Updating comment count for post " + postId + " by " + change); // Log count update
        FirebaseFirestore.getInstance().collection("posts").document(postId)
                .update("commentCount", FieldValue.increment(change))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Comment count updated successfully for post: " + postId))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating comment count for post: " + postId, e));
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared, removing listeners."); // Log clear
        // Remove all listeners when ViewModel is cleared
        listeners.values().forEach(ListenerRegistration::remove);
        listeners.clear();
        likedStatusMap.clear();
    }

    // Helper to get ViewModel instance, useful in Adapter
    public static PostLikeViewModel getInstance(ViewModelStoreOwner owner) {
        return new ViewModelProvider(owner).get(PostLikeViewModel.class);
    }
}