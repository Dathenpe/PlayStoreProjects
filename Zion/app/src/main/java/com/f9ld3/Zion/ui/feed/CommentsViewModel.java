// main/java/com/f9ld3/Zion/ui/feed/CommentsViewModel.java
package com.f9ld3.Zion.ui.feed;

import com.f9ld3.Zion.ui.notifications.NotificationViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Import Objects

public class CommentsViewModel extends ViewModel {
    private static final String TAG = "CommentsViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<Comment>> _comments = new MutableLiveData<>();
    public LiveData<List<Comment>> getComments() { return _comments; }

    private final MutableLiveData<Comment> _parentComment = new MutableLiveData<>();
    public LiveData<Comment> getParentComment() { return _parentComment; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    public LiveData<String> getSuccessMessage() { return _successMessage; }

    private ListenerRegistration commentsListener;
    private ListenerRegistration parentCommentListener;

    // --- Loading Top-Level Comments ---
    public void loadComments(String postId) {
        if (postId == null || postId.isEmpty()) {
            Log.w(TAG, "loadComments called with null or empty postId.");
            _comments.setValue(new ArrayList<>());
            return;
        }
        Log.d(TAG, "Loading TOP-LEVEL comments for post: " + postId);
        stopListeners(); // Stop both listeners before starting new ones

        commentsListener = db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("parentCommentId", null) // Only fetch top-level comments
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error loading top-level comments for post: " + postId, e);
                        _errorMessage.setValue("Failed to load comments.");
                        _comments.setValue(new ArrayList<>());
                        return;
                    }

                    List<Comment> fetchedComments = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Comment comment = doc.toObject(Comment.class);
                                comment.setId(doc.getId()); // Set the document ID
                                fetchedComments.add(comment);
                            } catch (Exception parseError) {
                                Log.e(TAG, "Error parsing comment: " + doc.getId(), parseError);
                            }
                        }
                        Log.d(TAG, "Loaded " + fetchedComments.size() + " top-level comments for post: " + postId);
                    } else {
                        Log.d(TAG, "Top-level comments snapshot is null for post: " + postId);
                    }
                    _comments.setValue(fetchedComments);
                });
    }

    // --- Loading Comment Thread ---
    public void loadCommentThread(String postId, String parentCommentId) {
        if (postId == null || parentCommentId == null) {
            Log.e(TAG, "loadCommentThread failed: null IDs");
            _errorMessage.setValue("Error loading thread.");
            _comments.setValue(new ArrayList<>()); // Clear replies
            _parentComment.setValue(null); // Clear parent
            return;
        }
        Log.d(TAG, "Loading thread for parent comment: " + parentCommentId + " on post: " + postId);
        stopListeners(); // Stop previous listeners

        // 1. Load the Parent Comment
        parentCommentListener = db.collection("posts").document(postId).collection("comments").document(parentCommentId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error loading parent comment: " + parentCommentId, e);
                        _errorMessage.setValue("Failed to load parent comment.");
                        _parentComment.setValue(null);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            Comment parent = snapshot.toObject(Comment.class);
                            if (parent != null) {
                                parent.setId(snapshot.getId()); // Set the document ID
                                _parentComment.setValue(parent);
                            } else {
                                _parentComment.setValue(null); // Parsing failed
                                Log.e(TAG, "Failed to parse parent comment: " + parentCommentId);
                            }
                        } catch (Exception parseError) {
                            Log.e(TAG, "Error parsing parent comment: " + snapshot.getId(), parseError);
                            _parentComment.setValue(null);
                        }

                    } else {
                        Log.w(TAG, "Parent comment not found: " + parentCommentId);
                        _parentComment.setValue(null); // Explicitly set null if not found
                    }
                });

        // 2. Load the Replies (using the *same* _comments LiveData)
        commentsListener = db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("parentCommentId", parentCommentId) // Only fetch replies for THIS parent
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error loading replies for comment: " + parentCommentId, e);
                        _errorMessage.setValue("Failed to load replies.");
                        _comments.setValue(new ArrayList<>()); // Set empty list on error
                        return;
                    }

                    List<Comment> fetchedReplies = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Comment reply = doc.toObject(Comment.class);
                                reply.setId(doc.getId()); // Set the document ID
                                fetchedReplies.add(reply);
                            } catch (Exception parseError) {
                                Log.e(TAG, "Error parsing reply comment: " + doc.getId(), parseError);
                            }
                        }
                        Log.d(TAG, "Loaded " + fetchedReplies.size() + " replies for comment: " + parentCommentId);
                    } else {
                        Log.d(TAG, "Replies snapshot is null for comment: " + parentCommentId);
                    }
                    _comments.setValue(fetchedReplies); // Post the list of replies
                });
    }

    // --- Post Comment or Reply ---
    // Now accepts notification-related data directly
    public void postCommentOrReply(String postId, String text, @Nullable String parentCommentId,
                                   String postAuthorUid, @Nullable String postTextSnippet) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || text.trim().isEmpty()) {
            Log.w(TAG, "postCommentOrReply aborted: User null or empty text.");
            _errorMessage.setValue("Cannot post empty comment.");
            return;
        }

        String action = (parentCommentId == null) ? "Posting comment" : "Posting reply to " + parentCommentId;
        Log.d(TAG, action + " for post: " + postId + " by user: " + user.getUid());

        Comment comment = new Comment(
                postId,
                user.getUid(),
                user.getDisplayName() != null ? user.getDisplayName() : "Anonymous",
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null,
                text.trim(),
                parentCommentId // Pass parentCommentId here
        );

        DocumentReference newCommentRef = db.collection("posts").document(postId).collection("comments").document();
        WriteBatch batch = db.batch();

        // 1. Add the new comment/reply (timestamp set by @ServerTimestamp)
        batch.set(newCommentRef, comment);

        // 2. Increment the post's comment count atomically
        DocumentReference postRef = db.collection("posts").document(postId);
        batch.update(postRef, "commentCount", FieldValue.increment(1));

        // 3. Increment the parent comment's reply count (if it's a reply)
        DocumentReference parentCommentRef = null;
        if (parentCommentId != null) {
            parentCommentRef = db.collection("posts").document(postId).collection("comments").document(parentCommentId);
            batch.update(parentCommentRef, "replyCount", FieldValue.increment(1));
        }
        final DocumentReference finalParentCommentRef = parentCommentRef; // Final for lambda

        // Commit batch
        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Comment/Reply posted successfully with ID: " + newCommentRef.getId());
            _successMessage.setValue("Comment posted.");

            // --- Send Notification ---
            String notificationType = (finalParentCommentRef != null) ? "comment_reply" : "post_comment";
            String notificationTitle = (finalParentCommentRef != null) ? "New Reply" : "New Comment";
            String commenterName = user.getDisplayName() != null ? user.getDisplayName() : "Someone";
            String notificationMessage = commenterName +
                    ((finalParentCommentRef != null) ? " replied to your comment." : " commented on your post.");

            // Shorten snippets for notification
            String shortCommentText = getSnippet(text.trim(), 50);
            String shortPostText = getSnippet(postTextSnippet, 50);

            if (finalParentCommentRef != null) {
                // It's a reply. Fetch parent comment's author.
                getCommentAuthorId(finalParentCommentRef).addOnSuccessListener(targetUserId -> {
                    if (targetUserId != null && !targetUserId.equals(user.getUid())) { // Don't notify self
                        sendCommentNotification(targetUserId, notificationType, notificationTitle, notificationMessage,
                                commenterName, user.getUid(), postId, newCommentRef.getId(), shortCommentText, shortPostText);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting parent comment author for notification", e);
                });
            } else {
                // It's a top-level comment. Notify the post author.
                if (postAuthorUid != null && !postAuthorUid.equals(user.getUid())) { // Don't notify self
                    sendCommentNotification(postAuthorUid, notificationType, notificationTitle, notificationMessage,
                            commenterName, user.getUid(), postId, newCommentRef.getId(), shortCommentText, shortPostText);
                }
            }

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error posting comment/reply for post: " + postId, e);
            _errorMessage.setValue("Failed to post comment.");
        });
    }

    // Helper to create text snippets
    private String getSnippet(@Nullable String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    // Fetch parent comment's author ID
    private Task<String> getCommentAuthorId(DocumentReference commentRef) {
        return commentRef.get().continueWith(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot != null && snapshot.exists()) {
                    return snapshot.getString("authorUid");
                }
            }
            Log.e(TAG, "Error getting comment author for notification", task.getException());
            return null; // Return null on failure or if not found
        });
    }

    // --- Delete Comment (Soft Delete) ---
    public void deleteComment(Comment commentToDelete, String postAuthorUid) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || commentToDelete == null || commentToDelete.getId() == null) {
            _errorMessage.setValue("Cannot delete comment. Invalid data.");
            return;
        }

        // Check permission: User must be comment author OR post author
        if (!currentUser.getUid().equals(commentToDelete.getAuthorUid()) && !currentUser.getUid().equals(postAuthorUid)) {
            _errorMessage.setValue("You don't have permission to delete this comment.");
            return;
        }

        Log.d(TAG, "Attempting to soft delete comment: " + commentToDelete.getId() + " by user: " + currentUser.getUid());

        DocumentReference commentRef = db.collection("posts").document(commentToDelete.getPostId())
                .collection("comments").document(commentToDelete.getId());

        // Update the 'deleted' flag
        commentRef.update("deleted", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Comment soft deleted successfully: " + commentToDelete.getId());
                    _successMessage.setValue("Comment deleted.");
                    // Optionally: Update counts via Cloud Function or another mechanism
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error soft deleting comment: " + commentToDelete.getId(), e);
                    _errorMessage.setValue("Failed to delete comment.");
                });
    }


    // --- Report Comment ---
    public void reportComment(Comment commentToReport, String reason) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || commentToReport == null) {
            _errorMessage.setValue("Cannot report comment. Please log in.");
            return;
        }

        if (currentUser.getUid().equals(commentToReport.getAuthorUid())) {
            _errorMessage.setValue("You cannot report your own comment.");
            return;
        }

        Log.d(TAG, "Reporting comment: " + commentToReport.getId() + " by user: " + currentUser.getUid());

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("postId", commentToReport.getPostId());
        reportData.put("commentId", commentToReport.getId());
        reportData.put("commentText", getSnippet(commentToReport.getTextContent(), 100)); // Report snippet
        reportData.put("commentAuthorUid", commentToReport.getAuthorUid());
        reportData.put("reporterUid", currentUser.getUid());
        reportData.put("reporterName", currentUser.getDisplayName()); // Add reporter name
        reportData.put("reason", reason);
        reportData.put("timestamp", Timestamp.now());
        reportData.put("status", "pending"); // Initial status

        db.collection("reports").add(reportData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Comment reported successfully: " + commentToReport.getId() + ", Report ID: " + documentReference.getId());
                    _successMessage.setValue("Comment reported. Thank you.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error reporting comment: " + commentToReport.getId(), e);
                    _errorMessage.setValue("Failed to report comment.");
                });
    }

    // --- Send Notification Helper ---
    private void sendCommentNotification(String targetUserId, String type, String title, String message,
                                         String commenterName, String commenterId, String postId, String commentId,
                                         @Nullable String commentTextSnippet, @Nullable String postTextSnippet) {

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("commenterName", commenterName != null ? commenterName : "Someone");
        notificationData.put("commenterId", commenterId);
        notificationData.put("postId", postId);
        notificationData.put("commentId", commentId); // Include comment ID for navigation
        if (commentTextSnippet != null) {
            notificationData.put("commentTextSnippet", commentTextSnippet);
        }
        if (postTextSnippet != null) {
            notificationData.put("postTextSnippet", postTextSnippet);
        }

        NotificationViewModel.sendNotification(
                targetUserId,
                type,
                title,
                message,
                notificationData
        );
    }


    // --- Utility and Lifecycle ---

    public void clearMessages() {
        _errorMessage.setValue(null);
        _successMessage.setValue(null);
    }

    private void stopListeners() {
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
            Log.d(TAG,"Comments listener removed.");
        }
        if (parentCommentListener != null) {
            parentCommentListener.remove();
            parentCommentListener = null;
            Log.d(TAG,"Parent comment listener removed.");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared, removing all listeners.");
        stopListeners();
    }
}