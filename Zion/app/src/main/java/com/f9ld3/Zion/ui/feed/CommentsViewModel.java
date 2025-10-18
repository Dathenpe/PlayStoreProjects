// main/java/com/f9ld3/Zion/ui/feed/CommentsViewModel.java
package com.f9ld3.Zion.ui.feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.ui.notifications.NotificationViewModel; // Add Notification import
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // Import ListenerRegistration
import com.google.firebase.firestore.Query;
import android.util.Log; // Add Log import
import java.util.ArrayList; // Import ArrayList
import java.util.HashMap; // Add HashMap import
import java.util.List;
import java.util.Map; // Add Map import


public class CommentsViewModel extends ViewModel {
    private static final String TAG = "CommentsViewModel"; // Added TAG
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance(); // Add FirebaseAuth

    private final MutableLiveData<List<Comment>> _comments = new MutableLiveData<>();
    public LiveData<List<Comment>> getComments() { return _comments; }

    private ListenerRegistration commentsListener; // Keep track of listener

    public void loadComments(String postId) {
        if (postId == null || postId.isEmpty()) {
            Log.w(TAG, "loadComments called with null or empty postId.");
            _comments.setValue(new ArrayList<>()); // Set empty list
            return;
        }
        Log.d(TAG, "Loading comments for post: " + postId); // Log load start
        // Remove previous listener if exists
        if (commentsListener != null) {
            commentsListener.remove();
            Log.d(TAG, "Removed previous comments listener for post: " + postId); // Log listener removal
        }

        commentsListener = db.collection("posts").document(postId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error loading comments for post: " + postId, e);
                        // Optionally set an error LiveData
                        _comments.setValue(new ArrayList<>()); // Set empty list on error
                        return;
                    }
                    if (snapshots != null) {
                        List<Comment> fetchedComments = snapshots.toObjects(Comment.class);
                        Log.d(TAG, "Loaded " + fetchedComments.size() + " comments for post: " + postId); // Log count
                        _comments.setValue(fetchedComments);
                    } else {
                        Log.d(TAG, "Comments snapshot is null for post: " + postId); // Log null snapshot
                        _comments.setValue(new ArrayList<>()); // Set empty list if null
                    }
                });
    }

    public void postComment(String postId, String text, Post postData) { // Accept Post data
        FirebaseUser user = mAuth.getCurrentUser(); // Use member variable
        if (user == null || text.trim().isEmpty() || postData == null) {
            Log.w(TAG, "postComment aborted: User null, empty text, or postData null.");
            return;
        }

        String authorUid = postData.getAuthorUid(); // Get author for notification
        Log.d(TAG, "Posting comment for post: " + postId + " by user: " + user.getUid()); // Log post action

        Comment comment = new Comment(postId, user.getUid(), user.getDisplayName(),
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null, text.trim()); // Trim text

        db.collection("posts").document(postId).collection("comments").add(comment)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Comment posted successfully with ID: " + documentReference.getId()); // Log success
                    // --- Update comment count (If NOT using Cloud Functions) ---
                    // PostLikeViewModel.updateCommentCount(postId, 1); // Increment count

                    // --- Send Notification ---
                    if (authorUid != null && !authorUid.equals(user.getUid())) { // Don't notify self
                        Log.d(TAG, "Sending comment notification to: " + authorUid); // Log notification send
                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("commenterName", user.getDisplayName() != null ? user.getDisplayName() : "Someone");
                        notificationData.put("commenterId", user.getUid());
                        notificationData.put("postId", postId); // Add postId for navigation
                        notificationData.put("commentText", text.trim().length() > 50 ? text.trim().substring(0, 50) + "..." : text.trim());
                        notificationData.put("postTextSnippet", postData.getTextContent() != null && postData.getTextContent().length() > 50 ? postData.getTextContent().substring(0, 50) + "..." : postData.getTextContent());


                        NotificationViewModel.sendNotification(
                                authorUid,
                                "post_comment", // Specific type for post comments
                                "New Comment",
                                (user.getDisplayName() != null ? user.getDisplayName() : "Someone") + " commented on your post.",
                                notificationData
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error posting comment for post: " + postId, e);
                    // Optionally set an error LiveData
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared, removing comments listener."); // Log clear
        // Clean up listener
        if (commentsListener != null) {
            commentsListener.remove();
        }
    }
}