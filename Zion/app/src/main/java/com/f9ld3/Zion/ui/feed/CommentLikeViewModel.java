// main/java/com/f9ld3/Zion/ui/feed/CommentLikeViewModel.java
package com.f9ld3.Zion.ui.feed;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.ui.notifications.NotificationViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class CommentLikeViewModel extends ViewModel {

    private static final String TAG = "CommentLikeViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Key format: "postId_commentId"
    private final ConcurrentHashMap<String, MutableLiveData<Boolean>> likedStatusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MutableLiveData<Boolean>> dislikedStatusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MutableLiveData<Integer>> likeCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MutableLiveData<Integer>> dislikeCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ListenerRegistration> listeners = new ConcurrentHashMap<>();

    // Helper to generate the map key
    private String getKey(String postId, String commentId) {
        // Ensure IDs are not null or empty to prevent invalid Firestore paths/keys
        if (postId == null || postId.isEmpty() || commentId == null || commentId.isEmpty()) {
            Log.e(TAG, "Attempted to generate key with null or empty postId/commentId");
            return null; // Return null to indicate an invalid key
        }
        return postId + "_" + commentId;
    }


    // --- LiveData Getters ---

    public LiveData<Boolean> isLiked(String postId, String commentId) {
        String key = getKey(postId, commentId);
        if (key == null) return new MutableLiveData<>(false); // Return default if key is invalid
        return likedStatusMap.computeIfAbsent(key, k -> {
            MutableLiveData<Boolean> liveData = new MutableLiveData<>(false);
            startLikeStatusListener(postId, commentId, liveData);
            return liveData;
        });
    }

    public LiveData<Boolean> isDisliked(String postId, String commentId) {
        String key = getKey(postId, commentId);
        if (key == null) return new MutableLiveData<>(false); // Return default if key is invalid
        return dislikedStatusMap.computeIfAbsent(key, k -> {
            MutableLiveData<Boolean> liveData = new MutableLiveData<>(false);
            startDislikeStatusListener(postId, commentId, liveData);
            return liveData;
        });
    }

    public LiveData<Integer> getLikeCount(String postId, String commentId) {
        String key = getKey(postId, commentId);
        if (key == null) return new MutableLiveData<>(0); // Return default if key is invalid

        // Ensure dislike LiveData exists if like is requested (count listener updates both)
        if (!dislikeCountMap.containsKey(key)) {
            dislikeCountMap.computeIfAbsent(key, k -> new MutableLiveData<>(0));
        }
        return likeCountMap.computeIfAbsent(key, k -> {
            MutableLiveData<Integer> liveData = new MutableLiveData<>(0);
            // Pass the corresponding dislike LiveData to the count listener
            startCountListener(postId, commentId, liveData, dislikeCountMap.get(k));
            return liveData;
        });
    }

    public LiveData<Integer> getDislikeCount(String postId, String commentId) {
        String key = getKey(postId, commentId);
        if (key == null) return new MutableLiveData<>(0); // Return default if key is invalid

        // Ensure like LiveData exists if dislike is requested first
        if (!likeCountMap.containsKey(key)) {
            getLikeCount(postId, commentId); // This will start the listener that updates both
        }
        // Return the dislike LiveData (it's updated by the listener started in getLikeCount)
        return dislikeCountMap.computeIfAbsent(key, k -> new MutableLiveData<>(0));
    }


    // --- Listeners ---

    private void startLikeStatusListener(String postId, String commentId, MutableLiveData<Boolean> liveData) {
        FirebaseUser user = mAuth.getCurrentUser();
        String key = getKey(postId, commentId);
        if (user == null || key == null) { // Check key validity
            liveData.postValue(false);
            return;
        }
        String listenerKey = key + "_likeStatus";

        stopListener(listenerKey); // Stop previous if exists

        DocumentReference likeRef = db.collection("posts").document(postId)
                .collection("comments").document(commentId)
                .collection("likes").document(user.getUid());

        ListenerRegistration listener = likeRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Like status listener error for " + key, e);
                liveData.postValue(false); // Assume false on error
                return;
            }
            liveData.postValue(snapshot != null && snapshot.exists());
        });
        listeners.put(listenerKey, listener);
    }

    private void startDislikeStatusListener(String postId, String commentId, MutableLiveData<Boolean> liveData) {
        FirebaseUser user = mAuth.getCurrentUser();
        String key = getKey(postId, commentId);
        if (user == null || key == null) { // Check key validity
            liveData.postValue(false);
            return;
        }
        String listenerKey = key + "_dislikeStatus";

        stopListener(listenerKey);

        DocumentReference dislikeRef = db.collection("posts").document(postId)
                .collection("comments").document(commentId)
                .collection("dislikes").document(user.getUid());

        ListenerRegistration listener = dislikeRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Dislike status listener error for " + key, e);
                liveData.postValue(false);
                return;
            }
            liveData.postValue(snapshot != null && snapshot.exists());
        });
        listeners.put(listenerKey, listener);
    }

    // Combined listener for both like and dislike counts from the parent comment document
    private void startCountListener(String postId, String commentId, MutableLiveData<Integer> likeLiveData, MutableLiveData<Integer> dislikeLiveData) {
        String key = getKey(postId, commentId);
        if (key == null) return; // Check key validity

        String listenerKey = key + "_count";

        stopListener(listenerKey);

        DocumentReference commentRef = db.collection("posts").document(postId)
                .collection("comments").document(commentId);

        ListenerRegistration listener = commentRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Count listener error for " + key, e);
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
                Log.d(TAG, "Counts updated for " + key + ": Likes=" + (likes != null ? likes : 0) + ", Dislikes=" + (dislikes != null ? dislikes : 0));
            } else {
                Log.w(TAG, "Comment document not found for counts: " + key);
                likeLiveData.postValue(0);
                if (dislikeLiveData != null) dislikeLiveData.postValue(0);
            }
        });
        listeners.put(listenerKey, listener);
    }

    // Helper to stop and remove a specific listener
    private void stopListener(String listenerKey) {
        ListenerRegistration listener = listeners.remove(listenerKey);
        if (listener != null) {
            listener.remove();
            Log.d(TAG, "Removed listener: " + listenerKey);
        }
    }

    // --- Actions ---

    public void toggleLike(String postId, String commentId, String commentAuthorUid, String commentTextSnippet, String postTextSnippet) {
        FirebaseUser user = mAuth.getCurrentUser();
        String key = getKey(postId, commentId);
        if (user == null || key == null) { // Check key validity
            Log.w(TAG, "toggleLike aborted: Missing user, postId, or commentId.");
            return; // Or show error message via LiveData
        }
        String userId = user.getUid();


        // Get current state from LiveData, default to false if not yet loaded
        boolean currentlyLiked = Boolean.TRUE.equals(likedStatusMap.getOrDefault(key, new MutableLiveData<>(false)).getValue());
        boolean currentlyDisliked = Boolean.TRUE.equals(dislikedStatusMap.getOrDefault(key, new MutableLiveData<>(false)).getValue());

        WriteBatch batch = db.batch();
        DocumentReference commentRef = db.collection("posts").document(postId).collection("comments").document(commentId);
        DocumentReference likeRef = commentRef.collection("likes").document(userId);
        DocumentReference dislikeRef = commentRef.collection("dislikes").document(userId);

        Log.d(TAG, "Toggling Like for " + key + ". Currently Liked: " + currentlyLiked + ", Disliked: " + currentlyDisliked);


        if (currentlyLiked) { // --- Unlike ---
            batch.delete(likeRef);
            batch.update(commentRef, "likeCount", FieldValue.increment(-1));
        } else { // --- Like ---
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("userId", userId);
            likeData.put("timestamp", Timestamp.now());
            batch.set(likeRef, likeData);
            batch.update(commentRef, "likeCount", FieldValue.increment(1));

            // If previously disliked, remove dislike and adjust count
            if (currentlyDisliked) {
                batch.delete(dislikeRef);
                batch.update(commentRef, "dislikeCount", FieldValue.increment(-1));
                Log.d(TAG, "Removing dislike while liking for " + key);
            }

            // --- Send Notification (only on like) ---
            // Ensure author UID is valid and not the current user
            if (commentAuthorUid != null && !commentAuthorUid.isEmpty() && !commentAuthorUid.equals(userId)) {
                sendCommentLikeNotification(commentAuthorUid, user.getDisplayName(), userId, postId, commentId, commentTextSnippet, postTextSnippet);
            } else {
                Log.d(TAG, "Skipping self-notification or invalid author UID for comment like.");
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Toggle like batch successful for " + key))
                .addOnFailureListener(e -> Log.e(TAG, "Toggle like batch failed for " + key, e));
    }

    public void toggleDislike(String postId, String commentId) {
        FirebaseUser user = mAuth.getCurrentUser();
        String key = getKey(postId, commentId);
        if (user == null || key == null) { // Check key validity
            Log.w(TAG, "toggleDislike aborted: Missing user, postId, or commentId.");
            return;
        }
        String userId = user.getUid();


        boolean currentlyLiked = Boolean.TRUE.equals(likedStatusMap.getOrDefault(key, new MutableLiveData<>(false)).getValue());
        boolean currentlyDisliked = Boolean.TRUE.equals(dislikedStatusMap.getOrDefault(key, new MutableLiveData<>(false)).getValue());

        WriteBatch batch = db.batch();
        DocumentReference commentRef = db.collection("posts").document(postId).collection("comments").document(commentId);
        DocumentReference likeRef = commentRef.collection("likes").document(userId);
        DocumentReference dislikeRef = commentRef.collection("dislikes").document(userId);

        Log.d(TAG, "Toggling Dislike for " + key + ". Currently Liked: " + currentlyLiked + ", Disliked: " + currentlyDisliked);

        if (currentlyDisliked) { // --- Remove Dislike ---
            batch.delete(dislikeRef);
            batch.update(commentRef, "dislikeCount", FieldValue.increment(-1));
        } else { // --- Add Dislike ---
            Map<String, Object> dislikeData = new HashMap<>();
            dislikeData.put("userId", userId);
            dislikeData.put("timestamp", Timestamp.now());
            batch.set(dislikeRef, dislikeData);
            batch.update(commentRef, "dislikeCount", FieldValue.increment(1));

            // If previously liked, remove like and adjust count
            if (currentlyLiked) {
                batch.delete(likeRef);
                batch.update(commentRef, "likeCount", FieldValue.increment(-1));
                Log.d(TAG, "Removing like while disliking for " + key);
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Toggle dislike batch successful for " + key))
                .addOnFailureListener(e -> Log.e(TAG, "Toggle dislike batch failed for " + key, e));
    }

    // --- Send Notification Helper ---
    private void sendCommentLikeNotification(String targetUserId, String likerName, String likerId, String postId, String commentId, String commentTextSnippet, String postTextSnippet) {
        Map<String, Object> data = new HashMap<>();
        String name = likerName != null ? likerName : "Someone";
        data.put("likerName", name);
        data.put("likerId", likerId);
        data.put("postId", postId);
        data.put("commentId", commentId); // Identify the specific comment

        // Use helper to create snippets
        data.put("commentTextSnippet", getSnippet(commentTextSnippet, 50));
        data.put("postTextSnippet", getSnippet(postTextSnippet, 50));

        Log.d(TAG, "Sending comment_like notification to " + targetUserId);
        NotificationViewModel.sendNotification(
                targetUserId,
                "comment_like", // Specific type
                "Comment Liked",
                name + " liked your comment.",
                data
        );
    }

    // Helper to create text snippets
    private String getSnippet(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove all active listeners
        listeners.values().forEach(ListenerRegistration::remove);
        listeners.clear();
        // Clear maps
        likedStatusMap.clear();
        dislikedStatusMap.clear();
        likeCountMap.clear();
        dislikeCountMap.clear();
        Log.d(TAG, "CommentLikeViewModel cleared.");
    }
}