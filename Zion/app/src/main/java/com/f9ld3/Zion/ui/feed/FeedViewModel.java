// main/java/com/f9ld3/Zion/ui/feed/FeedViewModel.java
package com.f9ld3.Zion.ui.feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

// --- Add necessary imports ---
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// --- End Add imports ---

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException; // Import Exception

// --- Add Storage imports ---
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
// --- End Storage imports ---


import java.util.ArrayList;
import java.util.List;

public class FeedViewModel extends ViewModel {

    private static final String TAG = "FeedViewModel";
    private final MutableLiveData<List<Post>> mPosts = new MutableLiveData<>();
    public LiveData<List<Post>> getPosts() { return mPosts; }

    private final MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading() { return mIsLoading; }

    // --- NEW: LiveData for status messages ---
    private final MutableLiveData<String> _statusMessage = new MutableLiveData<>();
    public LiveData<String> getStatusMessage() { return _statusMessage; }
    // --- End New ---


    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration firestoreListener;

    private boolean initialLoad = true; // Flag for initial skeleton display

    public FeedViewModel() {
        fetchPosts();
    }

    private void fetchPosts() {
        // ... (fetchPosts implementation remains the same) ...
        Query query = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50); // Limit initial load if desired

        mIsLoading.setValue(true); // Start loading
        Log.d(TAG, "Fetching posts...");

        firestoreListener = query.addSnapshotListener((snapshot, e) -> {
            // Handle Firestore errors
            if (e != null) {
                Log.e(TAG, "Error fetching posts", e); // Log the specific error
                mPosts.setValue(new ArrayList<>()); // Set empty list on error
                mIsLoading.setValue(false); // Stop loading on error
                _statusMessage.setValue("Error loading feed: " + e.getMessage());
                return;
            }

            // Process the snapshot
            if (snapshot != null && !snapshot.isEmpty()) {
                List<Post> postsList = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    try {
                        Post post = doc.toObject(Post.class);
                        post.setId(doc.getId()); // Set the document ID here
                        postsList.add(post);
                    } catch (RuntimeException parseError) {
                        Log.e(TAG, "Error parsing post document: " + doc.getId() + ". Skipping this post.", parseError);
                    }
                }
                mPosts.setValue(postsList); // Update LiveData with successfully parsed posts
                Log.d(TAG, "Successfully processed " + postsList.size() + " posts out of " + snapshot.size());
            } else {
                Log.d(TAG, "Post snapshot is null or empty.");
                mPosts.setValue(new ArrayList<>()); // Set empty list if snapshot is empty
            }

            mIsLoading.setValue(false); // Stop loading when data is received or processed
            if (initialLoad) {
                initialLoad = false;
            }
            Log.d(TAG, "Posts fetch listener update processed. Loading finished.");
        });
    }

    // --- NEW: Delete Post Method ---
    public void deletePost(Post postToDelete) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || postToDelete == null || postToDelete.getId() == null) {
            _statusMessage.setValue("Error: Cannot delete post. Invalid data or not logged in.");
            return;
        }

        // Authorization check (ensure user owns the post)
        if (!currentUser.getUid().equals(postToDelete.getAuthorUid())) {
            _statusMessage.setValue("Error: You do not have permission to delete this post.");
            return;
        }

        String postId = postToDelete.getId();
        String authorUid = postToDelete.getAuthorUid(); // Get author UID for storage path
        Log.d(TAG, "Attempting to delete post: " + postId + " by user: " + authorUid);

        // 1. Delete Firestore Document
        Task<Void> deleteFirestoreTask = db.collection("posts").document(postId).delete();

        // 2. Delete Associated Storage Files (if any)
        List<Task<Void>> deleteStorageTasks = new ArrayList<>();
        if (postToDelete.getMediaItems() != null && !postToDelete.getMediaItems().isEmpty()) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            for (MediaItem item : postToDelete.getMediaItems()) {
                if (item.getUrl() != null && !item.getUrl().isEmpty()) {
                    try {
                        // IMPORTANT: Assumes URL structure allows deriving storage path.
                        // You might need a more robust way if URLs are complex or custom domains are used.
                        StorageReference storageRef = storage.getReferenceFromUrl(item.getUrl());
                        deleteStorageTasks.add(storageRef.delete());
                        Log.d(TAG, "Scheduled deletion for storage file: " + storageRef.getPath());
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Error deriving storage reference from URL: " + item.getUrl(), e);
                        // Decide how to handle this - skip deletion? Log error?
                    }
                }
            }
        }

        // Combine Firestore and Storage deletion tasks
        Task<Void> firestoreDelete = deleteFirestoreTask.addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Firestore document deleted successfully: " + postId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error deleting Firestore document: " + postId, e);
            // Don't set status message here, let the combined task handle it
        });

        // Use Tasks.whenAllSuccess for storage tasks (or whenAllComplete if you need to know about individual failures)
        Task<List<Object>> storageDelete = Tasks.whenAllSuccess(deleteStorageTasks).addOnSuccessListener(results -> {
            Log.d(TAG, "All associated storage files deleted successfully for post: " + postId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error deleting one or more storage files for post: " + postId, e);
            // Consider logging which files failed if needed
        });


        // Wait for both Firestore and Storage deletions
        Tasks.whenAllComplete(firestoreDelete, storageDelete).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "Post and associated storage deleted successfully: " + postId);
                _statusMessage.setValue("Post deleted successfully.");
                // The LiveData listener will automatically remove the post from the UI.
            } else {
                Log.e(TAG, "Post deletion failed (Firestore or Storage error) for post: " + postId, task.getException());
                _statusMessage.setValue("Failed to delete post. Please try again.");
            }
        });

        // IMPORTANT REMINDER: Implement a Cloud Function to delete subcollections
        // (likes, dislikes, comments, votes) associated with the deleted postId.
        // Client-side deletion is unreliable for deeply nested data.
    }
    // --- End Delete Post Method ---

    // --- NEW: Method to clear status messages ---
    public void clearStatusMessage() {
        _statusMessage.setValue(null);
    }
    // --- End New ---


    @Override
    protected void onCleared() {
        super.onCleared();
        if (firestoreListener != null) {
            firestoreListener.remove();
            Log.d(TAG, "Firestore listener removed.");
        }
    }
}