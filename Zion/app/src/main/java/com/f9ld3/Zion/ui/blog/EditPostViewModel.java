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
import com.f9ld3.Zion.ui.feed.PollOption;
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
import java.util.Collections; // Import Collections
import java.util.Comparator; // Import Comparator
import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map; // Import Map
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors; // Import Collectors

public class EditPostViewModel extends ViewModel {

    private static final String TAG = "EditPostViewModel";
    public enum UpdateStatus { IDLE, UPDATING, SUCCESS, FAILED }

    private final MutableLiveData<UpdateStatus> _updateStatus = new MutableLiveData<>(UpdateStatus.IDLE);
    public LiveData<UpdateStatus> getUpdateStatus() { return _updateStatus; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    // Flags to track if media items have changed
    private boolean mediaChanged = false;

    public void setMediaChanged(boolean changed) { this.mediaChanged = changed; }
    public boolean getMediaChanged() { return this.mediaChanged; }

    /**
     * Updates an existing post, now handling poll option images.
     * @param post The Post object with potentially updated data (ID must be set).
     * @param currentMediaItems Current list of MediaItems (mix of URLs/URIs) for TYPE_TEXT_MEDIA.
     * @param currentPollOptionImageData Map of option index to Uri (new/changed) or String (existing URL). // <-- NEW PARAM
     * @param context The application context.
     */
    // *** UPDATED Signature ***
    public void updatePost(Post post, List<MediaItem> currentMediaItems, Map<Integer, Object> currentPollOptionImageData, Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || post == null || post.getId() == null) {
            _errorMessage.setValue("Cannot update post. Invalid data or user not logged in.");
            _updateStatus.setValue(UpdateStatus.FAILED);
            return;
        }

        _updateStatus.setValue(UpdateStatus.UPDATING);
        String postId = post.getId();

        // Ensure author details are correct
        post.setAuthorUid(user.getUid());
        post.setAuthorName(user.getDisplayName() != null ? user.getDisplayName() : "Anonymous");
        post.setAuthorAvatarUrl(user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);

        // --- Combined Upload Logic for Edit ---
        List<Task<Uri>> allUploadTasks = new ArrayList<>();
        List<MediaItem> finalMediaItems = new ArrayList<>(); // For TYPE_TEXT_MEDIA
        Map<Integer, String> finalPollImageUrls = new HashMap<>(); // For poll options
        List<StorageReference> oldImagesToDelete = new ArrayList<>(); // Track images to delete

        // 1. Handle Post Media Items (if applicable)
        if (post.getPostType().equals(Post.TYPE_TEXT_MEDIA)) {
            List<MediaItem> itemsToUpload = new ArrayList<>();
            List<MediaItem> existingItemsToKeep = new ArrayList<>();
            for (MediaItem item : currentMediaItems) {
                if (item.getUrl() != null && (item.getUrl().startsWith("content://") || item.getUrl().startsWith("file://"))) {
                    itemsToUpload.add(item);
                } else if (item.getUrl() != null && !item.getUrl().isEmpty()) {
                    existingItemsToKeep.add(item);
                }
            }
            finalMediaItems.addAll(existingItemsToKeep); // Start with items to keep

            // Determine which old media items were removed
            List<String> currentUrls = currentMediaItems.stream()
                    .map(MediaItem::getUrl)
                    .filter(url -> url != null && !(url.startsWith("content://") || url.startsWith("file://")))
                    .collect(Collectors.toList());
            if (post.getMediaItems() != null) {
                for(MediaItem oldItem : post.getMediaItems()) {
                    if (oldItem.getUrl() != null && !currentUrls.contains(oldItem.getUrl())) {
                        try {
                            oldImagesToDelete.add(FirebaseStorage.getInstance().getReferenceFromUrl(oldItem.getUrl()));
                        } catch (IllegalArgumentException e) { Log.w(TAG, "Could not parse old post media URL for deletion: " + oldItem.getUrl()); }
                    }
                }
            }


            // Add upload tasks for new items
            finalMediaItems.addAll(Collections.nCopies(itemsToUpload.size(), null)); // Add placeholders
            for (int i = 0; i < itemsToUpload.size(); i++) {
                MediaItem itemToUpload = itemsToUpload.get(i);
                final int finalIndex = existingItemsToKeep.size() + i; // Index in the final list

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
                    MediaItem uploadedItem = new MediaItem(); // Create new item for the final list
                    uploadedItem.mediaType = mediaType;
                    uploadedItem.url = downloadUrl.toString();
                    uploadedItem.thumbnailUrl = "video".equals(mediaType) ? downloadUrl.toString() : downloadUrl.toString();
                    finalMediaItems.set(finalIndex, uploadedItem); // Set in the correct position
                }).addOnFailureListener(e -> { /* Failed, placeholder remains null */ });
                allUploadTasks.add(urlTask);
            }
        }

        // 2. Handle Poll Option Images (if applicable)
        boolean pollImagesChanged = false; // Track if poll images were added/removed/changed
        if ((post.getPostType().equals(Post.TYPE_POLL) || post.getPostType().equals(Post.TYPE_QUIZ))
                && currentPollOptionImageData != null) {

            List<Map.Entry<Integer, Object>> sortedEntries = new ArrayList<>(currentPollOptionImageData.entrySet());
            Collections.sort(sortedEntries, Comparator.comparingInt(Map.Entry::getKey));

            for (Map.Entry<Integer, Object> entry : sortedEntries) {
                int optionIndex = entry.getKey();
                Object imageData = entry.getValue();

                if (imageData instanceof Uri) { // New or changed image
                    pollImagesChanged = true;
                    Uri imageUri = (Uri) imageData;
                    String fileExtension = ".jpg";

                    // Check if there was an old image URL for this index to delete later
                    if (post.getPollOptions() != null && optionIndex < post.getPollOptions().size()) {
                        String oldImageUrl = post.getPollOptions().get(optionIndex).getImageUrl();
                        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                            try {
                                oldImagesToDelete.add(FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl));
                            } catch (IllegalArgumentException e) { Log.w(TAG, "Could not parse old poll image URL for deletion: " + oldImageUrl); }
                        }
                    }

                    StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                            .child("posts/" + user.getUid() + "/" + postId + "/option_" + optionIndex + "_" + UUID.randomUUID().toString() + fileExtension);

                    UploadTask uploadTask = fileRef.putFile(imageUri);
                    Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                        if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                        return fileRef.getDownloadUrl();
                    }).addOnSuccessListener(downloadUrl -> {
                        finalPollImageUrls.put(optionIndex, downloadUrl.toString());
                    }).addOnFailureListener(e -> { /* Error handled in whenAllComplete */ });
                    allUploadTasks.add(urlTask);

                } else if (imageData instanceof String) { // Existing URL, keep it
                    finalPollImageUrls.put(optionIndex, (String) imageData);
                }
            }
            // Check if images were removed
            if (post.getPollOptions() != null) {
                for (int i = 0; i < post.getPollOptions().size(); i++) {
                    PollOption oldOption = post.getPollOptions().get(i);
                    // If old option had an image URL but it's NOT in the current image data map (neither as URL nor placeholder index)
                    if (oldOption.getImageUrl() != null && !oldOption.getImageUrl().isEmpty() && !currentPollOptionImageData.containsKey(i)) {
                        pollImagesChanged = true;
                        try {
                            oldImagesToDelete.add(FirebaseStorage.getInstance().getReferenceFromUrl(oldOption.getImageUrl()));
                        } catch (IllegalArgumentException e) { Log.w(TAG, "Could not parse removed poll image URL for deletion: " + oldOption.getImageUrl()); }
                    }
                }
            }
        }

        // 3. Wait for all uploads and then save post update
        boolean finalPollImagesChanged = pollImagesChanged;
        Tasks.whenAllComplete(allUploadTasks).addOnCompleteListener(task -> {
            boolean anyFailed = false;
            for (Task<?> completedTask : task.getResult()) {
                if (!completedTask.isSuccessful()) {
                    anyFailed = true;
                    Log.e(TAG, "An upload task failed during edit.", completedTask.getException());
                    break;
                }
            }

            if (anyFailed) {
                _errorMessage.setValue("One or more file uploads failed.");
                _updateStatus.setValue(UpdateStatus.FAILED);
            } else {
                // Update Post object with final URLs
                if (post.getPostType().equals(Post.TYPE_TEXT_MEDIA)) {
                    // Filter out nulls (failed uploads) before setting
                    post.setMediaItems(finalMediaItems.stream().filter(Objects::nonNull).collect(Collectors.toList()));
                } else if (finalPollImagesChanged) { // Only update poll options if images changed
                    List<PollOption> options = post.getPollOptions(); // Get the potentially updated list from savePostChanges
                    if (options != null) {
                        for (int i=0; i < options.size(); i++) {
                            // Set URL if present in final map, otherwise set null (handles removal)
                            options.get(i).setImageUrl(finalPollImageUrls.getOrDefault(i, null));
                        }
                        post.setPollOptions(options); // Set updated list back
                    }
                } // If pollImagesChanged is false, pollOptions remain as they were set in savePostChanges

                savePostUpdate(post); // Save updated metadata

                // Delete old images *after* successful metadata update
                deleteOldStorageFiles(oldImagesToDelete);
            }
        });

        mediaChanged = false; // Reset flag for post media
    }


    private void savePostUpdate(Post post) {
        post.setTimestamp(System.currentTimeMillis()); // Update timestamp on edit

        FirebaseFirestore.getInstance().collection("posts").document(post.getId())
                .set(post, SetOptions.merge()) // Use merge
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

    // Media upload logic (can be refactored)
    // private void uploadMultipleMedia(...) { /* ... */ }

    // *** NEW: Helper to delete old storage files ***
    private void deleteOldStorageFiles(List<StorageReference> refsToDelete) {
        if (refsToDelete.isEmpty()) return;
        Log.d(TAG, "Attempting to delete " + refsToDelete.size() + " old file(s).");
        List<Task<Void>> deleteTasks = new ArrayList<>();
        for (StorageReference ref : refsToDelete) {
            deleteTasks.add(ref.delete().addOnFailureListener(e -> Log.w(TAG, "Failed to delete file: " + ref.getPath(), e)));
        }
        Tasks.whenAllComplete(deleteTasks).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Failed to delete one or more old storage files.", task.getException());
            } else {
                long successfulDeletes = task.getResult().stream().filter(Task::isSuccessful).count();
                Log.d(TAG, "Successfully deleted " + successfulDeletes + " / " + refsToDelete.size() + " old storage files.");
            }
        });
    }
    // *** END NEW ***

    public void clearMessages() {
        _errorMessage.setValue(null);
    }

    public void resetStatus() {
        if (_updateStatus.getValue() == UpdateStatus.SUCCESS || _updateStatus.getValue() == UpdateStatus.FAILED) {
            _updateStatus.setValue(UpdateStatus.IDLE);
        }
    }

    // Interface removed as logic is integrated
}