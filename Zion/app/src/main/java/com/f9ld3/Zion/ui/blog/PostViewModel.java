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
import com.f9ld3.Zion.ui.feed.PollOption;
import com.f9ld3.Zion.ui.feed.Post;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

// --- IMPORT ADDED ---
import com.google.firebase.Timestamp;
// --- END IMPORT ---

import java.util.ArrayList;
import java.util.Collections; // Import Collections
import java.util.Comparator; // Import Comparator
import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map; // Import Map
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors; // Import Collectors

public class PostViewModel extends ViewModel {

    private static final String TAG = "PostViewModel";
    public enum UploadStatus { IDLE, UPLOADING, SUCCESS, FAILED }

    private final MutableLiveData<UploadStatus> _uploadStatus = new MutableLiveData<>(UploadStatus.IDLE);
    public LiveData<UploadStatus> getUploadStatus() { return _uploadStatus; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    /**
     * Creates a new post, now handling poll option images.
     * @param post The Post object pre-filled with type, text, and poll options (without image URLs yet).
     * @param mediaItems A list of MediaItem objects to upload (for media posts only).
     * @param pollOptionImageUris Map of option index to local image Uri (for poll/quiz posts). // <-- NEW PARAM
     * @param context The application context.
     */
    // *** UPDATED Signature ***
    public void createPost(Post post, List<MediaItem> mediaItems, Map<Integer, Uri> pollOptionImageUris, Context context) {
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

        // --- Combined Upload Logic ---
        List<Task<Uri>> allUploadTasks = new ArrayList<>();
        List<MediaItem> finalMediaItems; // For TYPE_TEXT_MEDIA
        Map<Integer, String> finalPollImageUrls = new HashMap<>(); // For poll options


        // 1. Handle Post Media Items (if applicable)
        if (post.getPostType().equals(Post.TYPE_TEXT_MEDIA) && mediaItems != null && !mediaItems.isEmpty()) {
            finalMediaItems = new ArrayList<>(Collections.nCopies(mediaItems.size(), null)); // Initialize with nulls

            for (int i = 0; i < mediaItems.size(); i++) {
                MediaItem itemToUpload = mediaItems.get(i);
                final int index = i; // Final index for use in lambdas

                Uri uri = Uri.parse(itemToUpload.getUrl());
                String mediaType = itemToUpload.getMediaType();
                String fileExtension = mediaType.equals("image") ? ".jpg" : (mediaType.equals("video") ? ".mp4" : ".unknown");
                if (fileExtension.equals(".unknown")) continue;

                StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                        .child("posts/" + user.getUid() + "/" + postId + "/" + UUID.randomUUID().toString() + fileExtension);

                UploadTask uploadTask = fileRef.putFile(uri);
                Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                    return fileRef.getDownloadUrl();
                }).addOnSuccessListener(downloadUrl -> {
                    // Create the final MediaItem here
                    MediaItem uploadedItem = new MediaItem();
                    uploadedItem.mediaType = mediaType;
                    uploadedItem.url = downloadUrl.toString();
                    uploadedItem.thumbnailUrl = "video".equals(mediaType) ? downloadUrl.toString() : downloadUrl.toString(); // Simple thumbnail for now
                    finalMediaItems.set(index, uploadedItem); // Place in the correct index
                    Log.d(TAG, "Post Media Upload success index " + index + ": " + downloadUrl);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Post Media Upload failed index " + index, e);
                    // Placeholder remains null
                });
                allUploadTasks.add(urlTask);
            }
        } else {
            finalMediaItems = new ArrayList<>();
        }

        // 2. Handle Poll Option Images (if applicable)
        if ((post.getPostType().equals(Post.TYPE_POLL) || post.getPostType().equals(Post.TYPE_QUIZ))
                && pollOptionImageUris != null && !pollOptionImageUris.isEmpty()) {

            // Sort entries by index to ensure order
            List<Map.Entry<Integer, Uri>> sortedEntries = new ArrayList<>(pollOptionImageUris.entrySet());
            Collections.sort(sortedEntries, Comparator.comparingInt(Map.Entry::getKey));

            for (Map.Entry<Integer, Uri> entry : sortedEntries) {
                int optionIndex = entry.getKey();
                Uri imageUri = entry.getValue();
                String fileExtension = ".jpg"; // Assuming poll images are jpg

                StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                        .child("posts/" + user.getUid() + "/" + postId + "/option_" + optionIndex + "_" + UUID.randomUUID().toString() + fileExtension);

                UploadTask uploadTask = fileRef.putFile(imageUri);
                Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                    return fileRef.getDownloadUrl();
                }).addOnSuccessListener(downloadUrl -> {
                    finalPollImageUrls.put(optionIndex, downloadUrl.toString()); // Map index to URL
                    Log.d(TAG, "Poll Option Image Upload success index " + optionIndex + ": " + downloadUrl);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Poll Option Image Upload failed index " + optionIndex, e);
                    // Don't add to finalPollImageUrls if failed
                });
                allUploadTasks.add(urlTask);
            }
        }

        // 3. Wait for all uploads and then save post
        if (!allUploadTasks.isEmpty()) {
            Tasks.whenAllComplete(allUploadTasks).addOnCompleteListener(task -> {
                // Check if *any* task failed
                boolean anyFailed = false;
                for (Task<?> completedTask : task.getResult()) {
                    if (!completedTask.isSuccessful()) {
                        anyFailed = true;
                        Log.e(TAG, "An upload task failed.", completedTask.getException());
                        break;
                    }
                }

                if (anyFailed) {
                    _errorMessage.setValue("One or more file uploads failed.");
                    _uploadStatus.setValue(UploadStatus.FAILED);
                    // Optionally: Delete already uploaded files for this post? (Complex)
                } else {
                    // Update Post object with final URLs
                    if (post.getPostType().equals(Post.TYPE_TEXT_MEDIA)) {
                        // Filter out nulls (failed uploads) before setting
                        post.setMediaItems(finalMediaItems.stream().filter(Objects::nonNull).collect(Collectors.toList()));
                    } else if (!finalPollImageUrls.isEmpty()) {
                        List<PollOption> options = post.getPollOptions();
                        if (options != null) {
                            for (Map.Entry<Integer, String> entry : finalPollImageUrls.entrySet()) {
                                if (entry.getKey() < options.size()) {
                                    options.get(entry.getKey()).setImageUrl(entry.getValue());
                                }
                            }
                            post.setPollOptions(options); // Set updated list back
                        }
                    }
                    savePost(post); // Save post metadata now with URLs
                }
            });
        } else {
            // No files to upload, just save post metadata
            savePost(post);
        }
    }


    private void savePost(Post post) {
        if (post.getTimestamp() == null) {
            // --- THIS IS THE FIX ---
            post.setTimestamp(Timestamp.now()); // Set timestamp just before saving
            // --- END FIX ---
        }

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

    public void clearMessages() {
        _errorMessage.setValue(null);
    }

    public void resetStatus() {
        if (_uploadStatus.getValue() == UploadStatus.SUCCESS || _uploadStatus.getValue() == UploadStatus.FAILED) {
            _uploadStatus.setValue(UploadStatus.IDLE);
        }
    }

    // Interface removed as logic is integrated
}