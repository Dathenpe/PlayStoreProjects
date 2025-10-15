package com.f9ld3.Zion.ui.blog;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.f9ld3.Zion.ui.feed.Post;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class BlogViewModel extends ViewModel {

    private static final String TAG = "BlogViewModel";

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private final MutableLiveData<Boolean> _uploading = new MutableLiveData<>();
    public LiveData<Boolean> getUploading() { return _uploading; }

    private final MutableLiveData<String> _uploadError = new MutableLiveData<>();
    public LiveData<String> getUploadError() { return _uploadError; }

    private final MutableLiveData<Boolean> _uploadSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getUploadSuccess() { return _uploadSuccess; }

    public void createBlogPost(String title, String description, Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            _uploadError.setValue("You must be logged in to create a blog post.");
            return;
        }
        if (title.isEmpty() || description.isEmpty()) {
            _uploadError.setValue("Title and description cannot be empty.");
            return;
        }

        _uploading.setValue(true);
        String postId = db.collection("posts").document().getId(); // Generate ID upfront

        if (imageUri != null) {
            uploadImageAndCreatePost(postId, user, title, description, imageUri);
        } else {
            savePostToFirestore(postId, user, title, description, null);
        }
    }

    private void uploadImageAndCreatePost(String postId, FirebaseUser user, String title, String description, Uri imageUri) {
        // CORRECTED PATH
        StorageReference imageRef = storage.getReference().child("blog_images/" + user.getUid() + "/" + postId);
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            Log.d(TAG, "Image uploaded: " + downloadUri.toString());
                            savePostToFirestore(postId, user, title, description, downloadUri.toString());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL for image.", e);
                            _uploadError.setValue("Failed to get image URL: " + e.getMessage());
                            _uploading.setValue(false);
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image.", e);
                    _uploadError.setValue("Failed to upload image: " + e.getMessage());
                    _uploading.setValue(false);
                });
    }

    private void savePostToFirestore(String postId, FirebaseUser user, String title, String description, String imageUrl) {
        Post newPost = new Post(
                postId,
                title,
                description,
                imageUrl,
                user.getDisplayName() != null ? user.getDisplayName() : "Anonymous",
                Timestamp.now().getSeconds(),
                "blog",
                user.getUid() // Set authorUid
        );

        db.collection("posts").document(postId).set(newPost)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Blog post created successfully.");
                    _uploadSuccess.setValue(true);
                    _uploading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create blog post.", e);
                    _uploadError.setValue("Failed to create blog post: " + e.getMessage());
                    _uploading.setValue(false);
                });
    }

    public void clearUploadStatus() {
        _uploadSuccess.setValue(null);
        _uploadError.setValue(null);
    }

    // TODO: Implement methods for editing and deleting posts
    public void updateBlogPost(Post post) {
        // Example:
        // db.collection("posts").document(post.getId()).set(post)
        //    .addOnSuccessListener(...)
        //    .addOnFailureListener(...);
    }

    public void deleteBlogPost(String postId) {
        // Example:
        // db.collection("posts").document(postId).delete()
        //    .addOnSuccessListener(...)
        //    .addOnFailureListener(...);
        // Also delete image from storage if imageUrl exists
    }
}