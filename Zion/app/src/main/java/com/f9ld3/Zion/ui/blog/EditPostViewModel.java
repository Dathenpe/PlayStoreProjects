// main/java/com/f9ld3/Zion/ui/blog/EditPostViewModel.java
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
import com.google.firebase.firestore.SetOptions; // Import SetOptions for merging
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class EditPostViewModel extends ViewModel {

    private static final String TAG = "EditPostViewModel";
    // *** NEW ENUM ***
    public enum UpdateStatus { IDLE, UPDATING, SUCCESS, FAILED }

    // *** USE NEW ENUM ***
    private final MutableLiveData<UpdateStatus> _updateStatus = new MutableLiveData<>(UpdateStatus.IDLE);
    public LiveData<UpdateStatus> getUpdateStatus() { return _updateStatus; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    // Flag to track if media items have changed
    private boolean mediaChanged = false;

    public void setMediaChanged(boolean changed) {
        this.mediaChanged = changed;
    }

    /**
     * Updates an existing post.
     * @param post The Post object with updated data (text, poll options, etc.). ID must be set.
     * @param currentMediaItems The current list of MediaItems displayed in the UI (mix of old URLs and new local URIs).
     * @param context The application context.
     */
    public void updatePost(Post post, List<MediaItem> currentMediaItems, Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || post == null || post.getId() == null) {
            _errorMessage.setValue("Cannot update post. Invalid data or user not logged in.");
            _updateStatus.setValue(UpdateStatus.FAILED);
            return;
        }

        _updateStatus.setValue(UpdateStatus.UPDATING);
        String postId = post.getId();

        // Ensure author details are correct (shouldn't change, but good practice)
        post.setAuthorUid(user.getUid());
        post.setAuthorName(user.getDisplayName() != null ? user.getDisplayName() : "Anonymous");
        post.setAuthorAvatarUrl(user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);

        // --- Media Handling Logic ---
        List<MediaItem> itemsToUpload = new ArrayList<>();
        List<MediaItem> existingItemsToKeep = new ArrayList<>();

        if (post.getPostType().equals(Post.TYPE_TEXT_MEDIA)) {
            for (MediaItem item : currentMediaItems) {
                // Check if the URL is a local URI (content:// or file://) indicating a new file
                if (item.getUrl() != null && (item.getUrl().startsWith("content://") || item.getUrl().startsWith("file://"))) {
                    itemsToUpload.add(item);
                } else if (item.getUrl() != null && !item.getUrl().isEmpty()) {
                    // Assume it's an existing Firestore URL
                    existingItemsToKeep.add(item);
                }
            }
        }

        // Only upload if there are new items or if media was explicitly marked as changed (e.g., items removed)
        if (!itemsToUpload.isEmpty() || (mediaChanged && post.getPostType().equals(Post.TYPE_TEXT_MEDIA))) {
            uploadMultipleMedia(user, postId, itemsToUpload, context, uploadedNewItems -> {
                if (uploadedNewItems != null) {
                    // Combine existing kept items with newly uploaded items
                    List<MediaItem> finalMediaItems = new ArrayList<>(existingItemsToKeep);
                    finalMediaItems.addAll(uploadedNewItems);
                    post.setMediaItems(finalMediaItems);
                    savePostUpdate(post); // Save post with updated media list
                } else {
                    // Error occurred during upload
                    if (_updateStatus.getValue() != UpdateStatus.FAILED) {
                        _updateStatus.setValue(UpdateStatus.FAILED);
                    }
                    // Optionally: Delete already uploaded files for this update attempt? (More complex)
                }
            });
        } else {
            // If no new media to upload AND media hasn't changed, keep the original list if it's a media post
            if (post.getPostType().equals(Post.TYPE_TEXT_MEDIA)) {
                post.setMediaItems(existingItemsToKeep);
            } else {
                post.setMediaItems(new ArrayList<>()); // Clear media if switching away from media post
            }
            savePostUpdate(post); // Save directly if no media changes required
        }
        mediaChanged = false; // Reset flag after update attempt
    }


    /**
     * Saves the updated post data to Firestore using merge option.
     * @param post The Post object with all updated fields.
     */
    private void savePostUpdate(Post post) {
        // Set the timestamp for the update
        post.setTimestamp(System.currentTimeMillis()); // Update timestamp on edit

        // Use SetOptions.merge() to only update fields present in the 'post' object
        // NOTE: This assumes your Post object only contains fields you want to update.
        // If it contains old fields you DON'T want to overwrite, create a Map instead.
        FirebaseFirestore.getInstance().collection("posts").document(post.getId())
                .set(post, SetOptions.merge()) // Use merge to avoid overwriting fields not included
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post updated successfully: " + post.getId());
                    _updateStatus.setValue(UpdateStatus.SUCCESS);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update post: " + post.getId(), e);
                    _errorMessage.setValue("Failed to save changes: " + e.getMessage());
                    _updateStatus.setValue(UpdateStatus.FAILED);
                });
    }

    // --- Media Upload Logic (Mostly identical to PostViewModel, can be refactored) ---
    private void uploadMultipleMedia(FirebaseUser user, String postId, @NonNull List<MediaItem> itemsToUpload, Context context, PostViewModel.OnAllUploadsCompleteListener listener) {
        // --- This logic is identical to the one in PostViewModel ---
        // You should consider extracting this into a separate utility class or service
        // to avoid code duplication.

        List<Task<Uri>> uploadTasks = new ArrayList<>();
        List<MediaItem> uploadedMediaItems = new ArrayList<>(itemsToUpload.size());
        for (int i = 0; i < itemsToUpload.size(); i++) {
            uploadedMediaItems.add(new MediaItem());
        }

        for (int i = 0; i < itemsToUpload.size(); i++) {
            MediaItem itemToUpload = itemsToUpload.get(i);
            final int index = i;

            Uri uri = Uri.parse(itemToUpload.getUrl());
            String mediaType = itemToUpload.getMediaType();
            String fileExtension = "";

            if ("image".equals(mediaType)) fileExtension = ".jpg";
            else if ("video".equals(mediaType)) fileExtension = ".mp4";
            else {
                Log.w(TAG, "Unknown media type: " + mediaType + ". Skipping.");
                uploadedMediaItems.set(index, null);
                continue;
            }

            uploadedMediaItems.get(index).mediaType = mediaType;

            StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                    .child("posts/" + user.getUid() + "/" + postId + "/" + UUID.randomUUID().toString() + fileExtension);

            UploadTask uploadTask = fileRef.putFile(uri);

            Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    uploadedMediaItems.set(index, null);
                    throw Objects.requireNonNull(task.getException());
                }
                return fileRef.getDownloadUrl();
            }).addOnSuccessListener(downloadUrl -> {
                MediaItem currentItem = uploadedMediaItems.get(index);
                if (currentItem != null) {
                    currentItem.url = downloadUrl.toString();
                    // Set thumbnailUrl (same logic as create)
                    currentItem.thumbnailUrl = "video".equals(currentItem.mediaType) ? downloadUrl.toString() : downloadUrl.toString(); // Placeholder or actual thumb
                }
            }).addOnFailureListener(e -> {
                uploadedMediaItems.set(index, null);
            });
            uploadTasks.add(urlTask);
        }

        Tasks.whenAllComplete(uploadTasks).addOnCompleteListener(allTasks -> {
            List<MediaItem> successfulItems = uploadedMediaItems.stream()
                    .filter(item -> item != null && item.url != null)
                    .collect(Collectors.toList());
            boolean anyFailed = successfulItems.size() != itemsToUpload.stream().filter(Objects::nonNull).count(); // Check against original valid items

            if (anyFailed) {
                Log.e(TAG, "One or more media uploads failed during edit.");
                if (_errorMessage.getValue() == null) {
                    _errorMessage.setValue("Some media files failed to upload.");
                }
                // Don't set status to FAILED here, let savePostUpdate handle the overall status
                listener.onComplete(null);
            } else {
                listener.onComplete(successfulItems);
            }
        });
    }

    public void clearMessages() {
        _errorMessage.setValue(null);
    }

    public void resetStatus() {
        if (_updateStatus.getValue() == UpdateStatus.SUCCESS || _updateStatus.getValue() == UpdateStatus.FAILED) {
            _updateStatus.setValue(UpdateStatus.IDLE);
        }
    }

    // Interface for callback (can reuse PostViewModel's interface or keep it separate)
    interface OnAllUploadsCompleteListener {
        void onComplete(@Nullable List<MediaItem> mediaItems);
    }
}