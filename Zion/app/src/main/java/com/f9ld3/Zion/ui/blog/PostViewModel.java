package com.f9ld3.Zion.ui.blog;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull; // Added
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
import com.google.firebase.storage.UploadTask; // Added

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

    public void createPostWithMultipleMedia(String textContent, List<Uri> mediaUris, Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            _errorMessage.setValue("User not authenticated.");
            _uploadStatus.setValue(UploadStatus.FAILED); // Set status to failed
            return;
        }
        // Prevent creating empty posts if both text and media are empty
        if (textContent.isEmpty() && mediaUris.isEmpty()) {
            _errorMessage.setValue("Cannot create an empty post.");
            // Don't change status from IDLE
            return;
        }


        _uploadStatus.setValue(UploadStatus.UPLOADING);
        String postId = FirebaseFirestore.getInstance().collection("posts").document().getId();

        if (mediaUris != null && !mediaUris.isEmpty()) { // Add null check for mediaUris
            uploadMultipleMedia(user, postId, mediaUris, context, mediaItems -> {
                // Check if mediaItems is null (which indicates an upload failure)
                if (mediaItems != null) {
                    savePost(user, postId, textContent, mediaItems);
                } else {
                    // Error already handled in uploadMultipleMedia, just ensure status is FAILED
                    if (_uploadStatus.getValue() != UploadStatus.FAILED) {
                        _uploadStatus.setValue(UploadStatus.FAILED);
                    }
                }
            });
        } else {
            savePost(user, postId, textContent, new ArrayList<>()); // Pass empty list if no media
        }

    }


    private void uploadMultipleMedia(FirebaseUser user, String postId, @NonNull List<Uri> mediaUris, Context context, OnAllUploadsCompleteListener listener) {
        List<Task<Uri>> uploadTasks = new ArrayList<>();
        // Create a list of MediaItem placeholders, size matching mediaUris
        List<MediaItem> mediaItemsPlaceholders = new ArrayList<>(mediaUris.size());
        for (int i = 0; i < mediaUris.size(); i++) {
            mediaItemsPlaceholders.add(new MediaItem()); // Add empty placeholders
        }


        for (int i = 0; i < mediaUris.size(); i++) {
            Uri uri = mediaUris.get(i);
            final int index = i; // final index for use in lambda

            String mimeType = context.getContentResolver().getType(uri);
            String mediaType = "unknown";
            String fileExtension = "";

            // Add null check for mimeType
            if (mimeType != null) {
                if (mimeType.startsWith("image")) {
                    mediaType = "image";
                    fileExtension = ".jpg"; // Or determine more accurately if needed
                } else if (mimeType.startsWith("video")) {
                    mediaType = "video";
                    fileExtension = ".mp4"; // Or determine more accurately
                }
            } else {
                // Handle cases where MIME type cannot be determined (e.g., from file path)
                String path = uri.getPath();
                if (path != null) {
                    if (path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".jpeg") || path.toLowerCase().endsWith(".png")) {
                        mediaType = "image";
                        fileExtension = ".jpg";
                    } else if (path.toLowerCase().endsWith(".mp4") || path.toLowerCase().endsWith(".mov") || path.toLowerCase().endsWith(".avi")) {
                        mediaType = "video";
                        fileExtension = ".mp4";
                    }
                }
                // If still unknown, maybe skip or log a warning
                if (mediaType.equals("unknown")) {
                    Log.w(TAG, "Could not determine media type for URI: " + uri);
                    // Decide how to handle: skip? Default to something? Error out?
                    // For now, let's skip adding the task, but the placeholder remains.
                    continue;
                }

            }

            StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                    .child("posts/" + user.getUid() + "/" + postId + "/" + UUID.randomUUID().toString() + fileExtension);

            // Store media type in the placeholder *before* starting upload
            mediaItemsPlaceholders.get(index).mediaType = mediaType;


            // Use putFile and chain tasks
            UploadTask uploadTask = fileRef.putFile(uri);

            Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                // Continue with getting the download URL
                return fileRef.getDownloadUrl();
            }).addOnSuccessListener(downloadUrl -> {
                // Populate the placeholder with the URL
                mediaItemsPlaceholders.get(index).url = downloadUrl.toString();
                // Simplistic thumbnail = URL for now, could be improved with Cloud Functions
                if ("video".equals(mediaItemsPlaceholders.get(index).mediaType)) {
                    mediaItemsPlaceholders.get(index).thumbnailUrl = downloadUrl.toString();
                }
                Log.d(TAG, "Upload success for index " + index + ": " + downloadUrl.toString());
            }).addOnFailureListener(e -> {
                // Log failure for this specific file
                Log.e(TAG, "Upload failed for index " + index + ", URI: " + uri, e);
                // The overall failure will be caught by Tasks.whenAllSuccess(...).addOnFailureListener
            });

            uploadTasks.add(urlTask); // Add the task to get the URL
        }

        // Wait for all URL retrieval tasks to complete successfully
        Tasks.whenAllSuccess(uploadTasks).addOnSuccessListener(urls -> { // urls is List<Object>, needs casting
            Log.d(TAG, "All uploads finished successfully.");
            // Filter out any placeholders that failed (where URL is still null)
            List<MediaItem> successfulMediaItems = new ArrayList<>();
            for(MediaItem item : mediaItemsPlaceholders) {
                if (item.url != null) {
                    successfulMediaItems.add(item);
                }
            }
            listener.onComplete(successfulMediaItems); // Pass the list of successfully uploaded items
        }).addOnFailureListener(e -> {
            // This catches failure if *any* of the tasks in uploadTasks failed
            Log.e(TAG, "One or more media uploads failed.", e);
            _errorMessage.setValue("Media upload failed: " + e.getMessage());
            _uploadStatus.setValue(UploadStatus.FAILED);
            listener.onComplete(null); // Indicate failure with null
        });

    }


    private void savePost(FirebaseUser user, String postId, String textContent, List<MediaItem> mediaItems) {
        // Ensure user details are not null before creating Post
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Anonymous"; // Provide default
        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

        Post newPost = new Post(
                postId,
                user.getUid(),
                displayName,
                photoUrl,
                textContent,
                mediaItems
        );

        FirebaseFirestore.getInstance().collection("posts").document(postId).set(newPost)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post saved successfully: " + postId);
                    _uploadStatus.setValue(UploadStatus.SUCCESS);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save post: " + postId, e);
                    _errorMessage.setValue("Failed to save post: " + e.getMessage());
                    _uploadStatus.setValue(UploadStatus.FAILED);
                });
    }

    public void clearMessages() {
        _errorMessage.setValue(null);
        // Reset status only if needed, e.g., when dialog is dismissed
        // if (_uploadStatus.getValue() == UploadStatus.FAILED || _uploadStatus.getValue() == UploadStatus.SUCCESS) {
        //     _uploadStatus.setValue(UploadStatus.IDLE);
        // }
    }


    // Consider resetting status to IDLE after success/failure messages are shown
    public void resetStatus() {
        if (_uploadStatus.getValue() == UploadStatus.SUCCESS || _uploadStatus.getValue() == UploadStatus.FAILED) {
            _uploadStatus.setValue(UploadStatus.IDLE);
        }
    }


    interface OnAllUploadsCompleteListener {
        // Pass null to indicate failure
        void onComplete(@Nullable List<MediaItem> mediaItems);
    }

}