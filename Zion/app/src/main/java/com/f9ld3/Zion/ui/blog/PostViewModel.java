// main/java/com/f9ld3/Zion/ui/blog/PostViewModel.java
package com.f9ld3.Zion.ui.blog;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.ui.feed.MediaItem;
import com.f9ld3.Zion.ui.feed.Post;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PostViewModel extends ViewModel {

    private static final String TAG = "PostViewModel";
    public enum UploadStatus { IDLE, UPLOADING, SUCCESS, FAILED }

    private final MutableLiveData<UploadStatus> _uploadStatus = new MutableLiveData<>(UploadStatus.IDLE);
    public LiveData<UploadStatus> getUploadStatus() { return _uploadStatus; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    /**
     * Creates a new post of any type (media, poll, or quiz).
     * @param post The Post object pre-filled with type, text, and poll options.
     * @param mediaUris A list of media URIs to upload (for media posts only).
     * @param context The application context.
     */
    public void createPost(Post post, List<Uri> mediaUris, Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            _errorMessage.setValue("User not authenticated.");
            _uploadStatus.setValue(UploadStatus.FAILED);
            return;
        }

        _uploadStatus.setValue(UploadStatus.UPLOADING);
        String postId = FirebaseFirestore.getInstance().collection("posts").document().getId();

        post.setId(postId);
        post.setAuthorUid(user.getUid());
        post.setAuthorName(user.getDisplayName() != null ? user.getDisplayName() : "Anonymous");
        post.setAuthorAvatarUrl(user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        // Timestamp will be set manually in savePost()

        if (mediaUris != null && !mediaUris.isEmpty() && post.getPostType().equals(Post.TYPE_TEXT_MEDIA)) {
            uploadMultipleMedia(user, postId, mediaUris, context, mediaItems -> {
                if (mediaItems != null) {
                    post.setMediaItems(mediaItems);
                    savePost(post);
                } else {
                    // Error message is set within uploadMultipleMedia
                    if (_uploadStatus.getValue() != UploadStatus.FAILED) {
                        _uploadStatus.setValue(UploadStatus.FAILED);
                    }
                }
            });
        } else {
            savePost(post);
        }
    }

    private void savePost(Post post) {
        // --- FIX: Set timestamp as Long (milliseconds) ---
        if (post.getTimestamp() == null) {
            Log.w(TAG, "Timestamp not set, using client time.");
            post.setTimestamp(System.currentTimeMillis()); // <-- SET AS LONG
        }
        // --- END FIX ---

        FirebaseFirestore.getInstance().collection("posts").document(post.getId()).set(post)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post saved successfully: " + post.getId());
                    _uploadStatus.setValue(UploadStatus.SUCCESS);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save post: " + post.getId(), e);
                    _errorMessage.setValue("Failed to save post: " + e.getMessage());
                    _uploadStatus.setValue(UploadStatus.FAILED);
                });
    }


    private void uploadMultipleMedia(FirebaseUser user, String postId, @NonNull List<Uri> mediaUris, Context context, OnAllUploadsCompleteListener listener) {
        List<Task<Uri>> uploadTasks = new ArrayList<>();
        // Create placeholders with correct size immediately
        List<MediaItem> mediaItemsPlaceholders = new ArrayList<>(mediaUris.size());
        for (int i = 0; i < mediaUris.size(); i++) {
            mediaItemsPlaceholders.add(new MediaItem()); // Add empty MediaItem
        }

        for (int i = 0; i < mediaUris.size(); i++) {
            Uri uri = mediaUris.get(i);
            final int index = i; // Final index for use in lambdas

            String mimeType = context.getContentResolver().getType(uri);
            String mediaType = "unknown";
            String fileExtension = "";

            // --- Robust Media Type Determination ---
            if (mimeType != null) {
                if (mimeType.startsWith("image")) {
                    mediaType = "image";
                    fileExtension = ".jpg"; // Assume jpg for simplicity or determine actual type
                } else if (mimeType.startsWith("video")) {
                    mediaType = "video";
                    fileExtension = ".mp4"; // Assume mp4 for simplicity
                }
            } else {
                // Fallback: Check file extension from URI path if MIME type is null
                String path = uri.getPath();
                if (path != null) {
                    if (path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".jpeg") || path.toLowerCase().endsWith(".png")) {
                        mediaType = "image";
                        fileExtension = ".jpg";
                    } else if (path.toLowerCase().endsWith(".mp4") || path.toLowerCase().endsWith(".mov") || path.toLowerCase().endsWith(".avi") || path.toLowerCase().endsWith(".mkv")) {
                        mediaType = "video";
                        fileExtension = ".mp4";
                    }
                }
            }

            if (mediaType.equals("unknown")) {
                Log.w(TAG, "Could not determine media type for URI: " + uri + ". Skipping file.");
                // Set placeholder to null to indicate failure for this specific item
                mediaItemsPlaceholders.set(index, null);
                continue; // Skip this file
            }
            // --- End Media Type Determination ---

            mediaItemsPlaceholders.get(index).mediaType = mediaType; // Set media type on placeholder
            StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                    .child("posts/" + user.getUid() + "/" + postId + "/" + UUID.randomUUID().toString() + fileExtension);

            UploadTask uploadTask = fileRef.putFile(uri);

            // Chain tasks to get download URL
            Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Upload failed for index " + index, task.getException());
                    // Set placeholder to null on failure
                    mediaItemsPlaceholders.set(index, null);
                    throw task.getException(); // Propagate exception
                }
                // File uploaded successfully, now get download URL
                return fileRef.getDownloadUrl();
            }).addOnSuccessListener(downloadUrl -> {
                // Update the correct placeholder with the URL and potentially thumbnail
                MediaItem currentItem = mediaItemsPlaceholders.get(index);
                if (currentItem != null) { // Check if it wasn't already marked as failed
                    currentItem.url = downloadUrl.toString();
                    if ("video".equals(currentItem.mediaType)) {
                        // Ideally, generate a real thumbnail using Cloud Functions.
                        // For now, using the video URL itself or a placeholder.
                        currentItem.thumbnailUrl = downloadUrl.toString(); // Placeholder thumbnail
                    }
                    Log.d(TAG, "Upload success for index " + index + ": " + downloadUrl.toString());
                }
            }).addOnFailureListener(e -> {
                // Already logged in continueWithTask, just ensure placeholder is null
                mediaItemsPlaceholders.set(index, null);
                Log.e(TAG, "Failed to get download URL for index " + index, e);
            });

            uploadTasks.add(urlTask);
        }

        // Wait for all URL retrieval tasks (including potential failures)
        Tasks.whenAllComplete(uploadTasks).addOnCompleteListener(allTasks -> {
            List<MediaItem> successfulMediaItems = new ArrayList<>();
            boolean anyFailed = false;
            for (MediaItem item : mediaItemsPlaceholders) {
                if (item != null && item.url != null) { // Check if item exists and has a URL
                    successfulMediaItems.add(item);
                } else {
                    anyFailed = true; // Mark if any item is null or lacks a URL
                }
            }

            if (anyFailed) {
                Log.e(TAG, "One or more media uploads failed during processing.");
                // Only set error if not already set by a specific upload failure
                if (_errorMessage.getValue() == null) {
                    _errorMessage.setValue("Some media files failed to upload.");
                }
                _uploadStatus.setValue(UploadStatus.FAILED);
                listener.onComplete(null); // Indicate overall failure
            } else if (successfulMediaItems.isEmpty() && !mediaUris.isEmpty()) {
                // Case where all uploads failed or were skipped
                Log.e(TAG, "All media uploads failed or were skipped.");
                if (_errorMessage.getValue() == null) {
                    _errorMessage.setValue("All media files failed to upload.");
                }
                _uploadStatus.setValue(UploadStatus.FAILED);
                listener.onComplete(null);
            } else {
                Log.d(TAG, "All uploads finished. Successful items: " + successfulMediaItems.size());
                listener.onComplete(successfulMediaItems); // Pass the list of successfully uploaded items
            }
        });
    }

    public void clearMessages() {
        _errorMessage.setValue(null);
    }

    public void resetStatus() {
        if (_uploadStatus.getValue() == UploadStatus.SUCCESS || _uploadStatus.getValue() == UploadStatus.FAILED) {
            _uploadStatus.setValue(UploadStatus.IDLE);
        }
    }

    // Interface for callback after all uploads are attempted
    interface OnAllUploadsCompleteListener {
        /**
         * Called when all media upload attempts are complete.
         * @param mediaItems List of successfully uploaded MediaItems, or null if any critical error occurred.
         */
        void onComplete(@Nullable List<MediaItem> mediaItems);
    }
}