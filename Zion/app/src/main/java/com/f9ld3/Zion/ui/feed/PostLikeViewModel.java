// main/java/com/f9ld3/Zion/ui/feed/PostLikeViewModel.java
package com.f9ld3.Zion.ui.feed;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import com.f9ld3.Zion.ui.notifications.NotificationViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot; // Add this
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PostLikeViewModel extends ViewModel {

    private static final String TAG = "PostLikeViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Maps to hold LiveData for each post's like/dislike status
    private final ConcurrentHashMap<String, MutableLiveData<Boolean>> likedStatusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MutableLiveData<Boolean>> dislikedStatusMap = new ConcurrentHashMap<>(); // <<< ADDED Dislike Status Map

    // --- NEW: Maps for Live Counts ---
    private final ConcurrentHashMap<String, MutableLiveData<Integer>> likeCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MutableLiveData<Integer>> dislikeCountMap = new ConcurrentHashMap<>();
    // --- END NEW ---

    // Map to hold listeners (key includes type, e.g., "postId_like")
    private final ConcurrentHashMap<String, ListenerRegistration> listeners = new ConcurrentHashMap<>();

    /**
     * Returns a LiveData for the LIKE status of a specific post.
     */
    public LiveData<Boolean> isLiked(String postId) {
        // Handle null postId explicitly
        if (postId == null) {
            Log.w(TAG, "isLiked called with null postId.");
            return new MutableLiveData<>(false); // Return a non-null default
        }
        return likedStatusMap.computeIfAbsent(postId, id -> {
            MutableLiveData<Boolean> liveData = new MutableLiveData<>(false);
            startLikeStatusListener(id, liveData);
            return liveData;
        });
    }

    /**
     * Returns a LiveData for the DISLIKE status of a specific post.
     */
    public LiveData<Boolean> isDisliked(String postId) { // <<< ADDED Method
        // Handle null postId explicitly
        if (postId == null) {
            Log.w(TAG, "isDisliked called with null postId.");
            return new MutableLiveData<>(false); // Return a non-null default
        }
        return dislikedStatusMap.computeIfAbsent(postId, id -> {
            MutableLiveData<Boolean> liveData = new MutableLiveData<>(false);
            startDislikeStatusListener(id, liveData); // <<< ADDED Listener Start
            return liveData;
        });
    }

    // --- NEW: LiveData Getters for Counts ---
    public LiveData<Integer> getLikeCount(String postId) {
        if (postId == null) {
            Log.w(TAG, "getLikeCount called with null postId.");
            return new MutableLiveData<>(0); // Return a non-null default
        }
        // Ensure dislike count LiveData exists if like is requested first
        if (!dislikeCountMap.containsKey(postId)) {
            dislikeCountMap.computeIfAbsent(postId, k -> new MutableLiveData<>(0));
        }
        return likeCountMap.computeIfAbsent(postId, id -> {
            MutableLiveData<Integer> liveData = new MutableLiveData<>(0);
            startPostCountListener(id, liveData, dislikeCountMap.get(id)); // Pass dislike LiveData
            return liveData;
        });
    }

    public LiveData<Integer> getDislikeCount(String postId) {
        if (postId == null) {
            Log.w(TAG, "getDislikeCount called with null postId.");
            return new MutableLiveData<>(0); // Return a non-null default
        }
        // Ensure like count LiveData exists if dislike is requested first
        // Calling getLikeCount will start the listener that updates both
        if (!likeCountMap.containsKey(postId)) {
            getLikeCount(postId);
        }
        // Return the existing LiveData, updated by the shared listener
        return dislikeCountMap.computeIfAbsent(postId, k -> new MutableLiveData<>(0));
    }
    // --- END NEW ---

    // --- Listener Setup ---

    private void startLikeStatusListener(String postId, MutableLiveData<Boolean> liveData) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || postId == null) { // Check postId again just in case
            liveData.postValue(false);
            return;
        }
        String listenerKey = postId + "_like"; // Key specific to like listener
        stopListener(listenerKey); // Stop previous listener for this specific key

        DocumentReference likeRef = db.collection("posts").document(postId)
                .collection("likes").document(user.getUid());

        ListenerRegistration listener = likeRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening for like status on post " + postId, e);
                liveData.postValue(false);
                return;
            }
            liveData.postValue(snapshot != null && snapshot.exists());
        });
        listeners.put(listenerKey, listener); // Store with specific key
    }

    private void startDislikeStatusListener(String postId, MutableLiveData<Boolean> liveData) { // <<< ADDED Method
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || postId == null) { // Check postId again
            liveData.postValue(false);
            return;
        }
        String listenerKey = postId + "_dislike"; // Key specific to dislike listener
        stopListener(listenerKey); // Stop previous listener for this specific key

        DocumentReference dislikeRef = db.collection("posts").document(postId)
                .collection("dislikes").document(user.getUid()); // <<< Check "dislikes" collection

        ListenerRegistration listener = dislikeRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening for dislike status on post " + postId, e);
                liveData.postValue(false);
                return;
            }
            liveData.postValue(snapshot != null && snapshot.exists());
        });
        listeners.put(listenerKey, listener); // Store with specific key
    }


    // --- NEW: Listener for Post Counts ---
    private void startPostCountListener(String postId, MutableLiveData<Integer> likeLiveData, MutableLiveData<Integer> dislikeLiveData) {
        if (postId == null) return; // Check postId validity
        String listenerKey = postId + "_count"; // Key specific to count listener
        stopListener(listenerKey); // Stop previous count listener for this post

        DocumentReference postRef = db.collection("posts").document(postId);

        ListenerRegistration listener = postRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening for post counts on post " + postId, e);
                likeLiveData.postValue(0);
                if (dislikeLiveData != null) dislikeLiveData.postValue(0);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Long likes = snapshot.getLong("likeCount");
                Long dislikes = snapshot.getLong("dislikeCount");
                likeLiveData.postValue(likes != null ? likes.intValue() : 0);
                if (dislikeLiveData != null) {
                    dislikeLiveData.postValue(dislikes != null ? dislikes.intValue() : 0);
                }
                Log.d(TAG, "Post counts updated for " + postId + ": Likes=" + (likes != null ? likes : 0) + ", Dislikes=" + (dislikes != null ? dislikes : 0));
            } else {
                Log.w(TAG, "Post document not found for counts listener: " + postId);
                likeLiveData.postValue(0);
                if (dislikeLiveData != null) dislikeLiveData.postValue(0);
            }
        });
        listeners.put(listenerKey, listener); // Store with specific key
    }
    // --- END NEW ---

    private void stopListener(String listenerKey) { // <<< Modified to accept specific key
        ListenerRegistration listener = listeners.remove(listenerKey);
        if (listener != null) {
            listener.remove();
        }
    }

    // --- Action Methods ---

    /**
     * Toggles the LIKE state for a post. Removes dislike if present.
     */
    public void toggleLike(String postId, Post postData) { // <<< Refactored
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || postId == null || postData == null) {
            Log.w(TAG, "toggleLike aborted: Missing user, postId, or postData.");
            return;
        }
        String userId = user.getUid();
        String postAuthorUid = postData.getAuthorUid();

        boolean currentlyLiked = Boolean.TRUE.equals(likedStatusMap.getOrDefault(postId, new MutableLiveData<>(false)).getValue());
        boolean currentlyDisliked = Boolean.TRUE.equals(dislikedStatusMap.getOrDefault(postId, new MutableLiveData<>(false)).getValue());

        WriteBatch batch = db.batch();
        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(userId);
        DocumentReference dislikeRef = postRef.collection("dislikes").document(userId);

        Log.d(TAG, "Toggling LIKE for post: " + postId + ". Currently Liked: " + currentlyLiked + ", Disliked: " + currentlyDisliked);

        if (currentlyLiked) { // --- Unlike ---
            batch.delete(likeRef);
            batch.update(postRef, "likeCount", FieldValue.increment(-1));
        } else { // --- Like ---
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("userId", userId);
            likeData.put("timestamp", Timestamp.now());
            if (user.getDisplayName() != null) likeData.put("username", user.getDisplayName());
            batch.set(likeRef, likeData);
            batch.update(postRef, "likeCount", FieldValue.increment(1));

            // If previously disliked, remove dislike and adjust count
            if (currentlyDisliked) {
                batch.delete(dislikeRef);
                batch.update(postRef, "dislikeCount", FieldValue.increment(-1));
                Log.d(TAG, "Removing dislike while liking post: " + postId);
            }

            // Send notification only when liking
            sendLikeNotification(user, postAuthorUid, postId, postData);
        }

        commitBatch(batch, postId, "like");
    }

    /**
     * Toggles the DISLIKE state for a post. Removes like if present.
     */
    public void toggleDislike(String postId, Post postData) { // <<< ADDED Method
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || postId == null || postData == null) {
            Log.w(TAG, "toggleDislike aborted: Missing user, postId, or postData.");
            return;
        }
        String userId = user.getUid();

        boolean currentlyLiked = Boolean.TRUE.equals(likedStatusMap.getOrDefault(postId, new MutableLiveData<>(false)).getValue());
        boolean currentlyDisliked = Boolean.TRUE.equals(dislikedStatusMap.getOrDefault(postId, new MutableLiveData<>(false)).getValue());

        WriteBatch batch = db.batch();
        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(userId);
        DocumentReference dislikeRef = postRef.collection("dislikes").document(userId);

        Log.d(TAG, "Toggling DISLIKE for post: " + postId + ". Currently Liked: " + currentlyLiked + ", Disliked: " + currentlyDisliked);

        if (currentlyDisliked) { // --- Remove Dislike ---
            batch.delete(dislikeRef);
            batch.update(postRef, "dislikeCount", FieldValue.increment(-1));
        } else { // --- Add Dislike ---
            Map<String, Object> dislikeData = new HashMap<>();
            dislikeData.put("userId", userId);
            dislikeData.put("timestamp", Timestamp.now());
            batch.set(dislikeRef, dislikeData);
            batch.update(postRef, "dislikeCount", FieldValue.increment(1));

            // If previously liked, remove like and adjust count
            if (currentlyLiked) {
                batch.delete(likeRef);
                batch.update(postRef, "likeCount", FieldValue.increment(-1));
                Log.d(TAG, "Removing like while disliking post: " + postId);
            }
            // Note: Typically no notification is sent for a dislike.
        }

        commitBatch(batch, postId, "dislike");
    }

    // --- Helper Methods ---

    private void commitBatch(WriteBatch batch, String postId, String actionType) {
        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Toggle " + actionType + " batch committed successfully for post: " + postId);
            // Listeners will update LiveData
        }).addOnFailureListener(e -> {
            // *** Enhanced Logging ***
            Log.e(TAG, "Toggle " + actionType + " batch commit FAILED for post: " + postId + ", Error: ", e);
            // Handle failure (e.g., show error message via LiveData)
        });
    }


    private void sendLikeNotification(FirebaseUser user, String postAuthorUid, String postId, Post postData) {
        if (postAuthorUid != null && !postAuthorUid.equals(user.getUid())) { // Don't notify self
            Log.d(TAG, "Sending like notification to: " + postAuthorUid);
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("likerName", user.getDisplayName() != null ? user.getDisplayName() : "Someone");
            notificationData.put("likerId", user.getUid());
            notificationData.put("postId", postId);
            notificationData.put("postTextSnippet", postData.getTextContent() != null && postData.getTextContent().length() > 50 ? postData.getTextContent().substring(0, 50) + "..." : postData.getTextContent());

            NotificationViewModel.sendNotification(
                    postAuthorUid,
                    "post_like",
                    "New Post Like",
                    (user.getDisplayName() != null ? user.getDisplayName() : "Someone") + " liked your post.",
                    notificationData
            );
        }
    }


    // --- Cleanup ---

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared, removing all listeners.");
        // Remove all listeners when ViewModel is cleared
        listeners.values().forEach(ListenerRegistration::remove);
        listeners.clear();
        likedStatusMap.clear();
        dislikedStatusMap.clear(); // <<< Clear dislike map too
        likeCountMap.clear(); // <<< Clear count maps
        dislikeCountMap.clear(); // <<< Clear count maps
    }

    // Helper to get ViewModel instance
    public static PostLikeViewModel getInstance(ViewModelStoreOwner owner) {
        return new ViewModelProvider(owner).get(PostLikeViewModel.class);
    }

    // Keep updateCommentCount method if you are NOT using Cloud Functions
    public static void updateCommentCount(String postId, int change) {
        if (postId == null || postId.isEmpty()) { // Add null/empty check
            Log.w(TAG, "updateCommentCount aborted: postId is null or empty.");
            return;
        }
        Log.d(TAG, "Updating comment count for post " + postId + " by " + change); // Log count update
        FirebaseFirestore.getInstance().collection("posts").document(postId)
                .update("commentCount", FieldValue.increment(change))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Comment count updated successfully for post: " + postId))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating comment count for post: " + postId, e));
    }
}