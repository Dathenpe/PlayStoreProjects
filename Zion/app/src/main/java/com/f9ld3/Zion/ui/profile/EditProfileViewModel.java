package com.f9ld3.Zion.ui.profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.data.UserProfile;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditProfileViewModel extends ViewModel {
    private static final String TAG = "EditProfileViewModel";
    public enum SaveStatus { IDLE, LOADING, SUCCESS, FAILED }

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private final MutableLiveData<UserProfile> mUserProfile = new MutableLiveData<>();
    public LiveData<UserProfile> getUserProfile() { return mUserProfile; }

    private final MutableLiveData<SaveStatus> mSaveStatus = new MutableLiveData<>(SaveStatus.IDLE);
    public LiveData<SaveStatus> getSaveStatus() { return mSaveStatus; }

    private final MutableLiveData<String> _reauthRequired = new MutableLiveData<>();
    public LiveData<String> getReauthRequired() { return _reauthRequired; }

    private final MutableLiveData<String> _emailUpdateStatus = new MutableLiveData<>();
    public LiveData<String> getEmailUpdateStatus() { return _emailUpdateStatus; }

    private final MutableLiveData<Integer> _uploadProgress = new MutableLiveData<>();
    public LiveData<Integer> getUploadProgress() { return _uploadProgress; }

    // NEW: For upload-specific errors
    private final MutableLiveData<String> _uploadError = new MutableLiveData<>();
    public LiveData<String> getUploadError() { return _uploadError; }

    public EditProfileViewModel() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        mUserProfile.setValue(doc.toObject(UserProfile.class));
                    } else {
                        // Create profile from auth data if Firestore doc doesn't exist
                        UserProfile profile = new UserProfile(
                                user.getUid(),
                                user.getDisplayName() != null ? user.getDisplayName() : "Anonymous",
                                user.getEmail(),
                                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null
                        );
                        mUserProfile.setValue(profile);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading user profile", e);
                    // Fallback to auth data
                    if (user != null) {
                        UserProfile profile = new UserProfile(
                                user.getUid(),
                                user.getDisplayName() != null ? user.getDisplayName() : "Anonymous",
                                user.getEmail(),
                                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null
                        );
                        mUserProfile.setValue(profile);
                    }
                });
    }

    /**
     * Save profile with optional image upload
     * @param newUsername New username
     * @param newEmail New email (nullable - if null, email won't be changed)
     * @param newImageUri New profile image URI (nullable - if null, image won't be changed)
     * @param currentPassword Current password (required only if email is being changed)
     * @param context Context for image compression (if image is provided)
     */
    public void saveProfile(String newUsername, String newEmail, Uri newImageUri, String currentPassword, Context context) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            mSaveStatus.setValue(SaveStatus.FAILED);
            _uploadError.postValue("User not authenticated. Please log in again.");
            return;
        }
        mSaveStatus.setValue(SaveStatus.LOADING);

        // Clear previous errors
        _uploadError.postValue(null);

        // Determine what needs to be updated
        String currentUsername = user.getDisplayName() != null ? user.getDisplayName() : "";
        String currentEmail = user.getEmail() != null ? user.getEmail() : "";

        boolean usernameChanged = !currentUsername.equals(newUsername);
        boolean emailChanged = newEmail != null && !newEmail.isEmpty() && !currentEmail.equals(newEmail);
        boolean imageChanged = newImageUri != null;

        // If email is changing, require password
        if (emailChanged && (currentPassword == null || currentPassword.isEmpty())) {
            _reauthRequired.setValue("Re-authentication is required to change email. Please provide your current password.");
            mSaveStatus.setValue(SaveStatus.IDLE);
            return;
        }

        // Start the update chain
        if (imageChanged) {
            // Compress image if needed and upload first, then update profile
            Uri compressedImageUri = compressImage(newImageUri, context);
            uploadProfileImage(compressedImageUri != null ? compressedImageUri : newImageUri, user.getUid(), imageUrl -> {
                updateUserProfile(user, newUsername, newEmail, imageUrl, currentPassword, usernameChanged, emailChanged);
            });
        } else {
            // No image to upload, proceed with profile update
            updateUserProfile(user, newUsername, newEmail, null, currentPassword, usernameChanged, emailChanged);
        }
    }

    // NEW: Image compression method (optional performance boost)
    private Uri compressImage(Uri imageUri, Context context) {
        try {
            Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            // Scale down if larger than 1024x1024
            if (width > 1024 || height > 1024) {
                float scale = Math.min(1024f / width, 1024f / height);
                width = Math.round(width * scale);
                height = Math.round(height * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            // Compress to JPEG with 80% quality
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            // Create a temp file for upload
            File compressedFile = new File(context.getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(compressedFile);
            fos.write(data);
            fos.close();

            // Clean up bitmap to free memory
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }

            return Uri.fromFile(compressedFile);
        } catch (IOException e) {
            Log.e(TAG, "Image compression failed", e);
            return null;  // Fall back to original URI
        }
    }

    private void uploadProfileImage(Uri imageUri, String userId, OnImageUploadListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            _uploadError.postValue("User not authenticated. Please log in again.");
            mSaveStatus.setValue(SaveStatus.FAILED);
            return;
        }

        // Use current user's UID for consistency and security
        String uid = currentUser.getUid();
        if (uid == null || uid.isEmpty()) {
            _uploadError.postValue("Invalid user ID. Please log in again.");
            mSaveStatus.setValue(SaveStatus.FAILED);
            return;
        }

        StorageReference profileImagesRef = storage.getReference()
                .child("profile_images")
                .child(uid + ".jpg");

        Log.d(TAG, "Uploading to path: " + profileImagesRef.getPath());

        profileImagesRef.putFile(imageUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    _uploadProgress.postValue((int) progress);
                    Log.d(TAG, "Upload progress: " + progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully");
                    // Get download URL
                    profileImagesRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                Log.d(TAG, "Download URL: " + uri.toString());
                                listener.onUploadComplete(uri.toString());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to get download URL", e);
                                handleStorageError(e, "Failed to retrieve image URL after upload.");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed", e);
                    handleStorageError(e, "Failed to upload image. Check your permissions and internet connection.");
                });
    }

    // NEW: Helper method for Storage-specific errors
    // UPDATED: Helper method for Storage-specific errors (using integer codes for compatibility)
    private void handleStorageError(Exception e, String defaultMessage) {
        String errorMsg = defaultMessage;
        if (e instanceof StorageException) {
            StorageException storageException = (StorageException) e;
            int errorCode = storageException.getErrorCode();
            switch (errorCode) {
                case 1:  // ERROR_CODE_UNAUTHENTICATED
                    errorMsg = "Not authenticated. Please log in again.";
                    break;
                case 2:  // ERROR_CODE_PERMISSION_DENIED
                    errorMsg = "Permission denied. Please check your Firebase Storage rules or contact support.";
                    break;
                case 5:  // ERROR_CODE_OBJECT_NOT_FOUND (e.g., if trying to access a non-existent file)
                    errorMsg = "File not found. The upload path may be incorrect.";
                    break;
                case 6:  // ERROR_CODE_BUCKET_NOT_FOUND (if available; otherwise falls to default)
                    errorMsg = "Storage bucket not found. Contact support.";
                    break;
                case 7:  // ERROR_CODE_PROJECT_NOT_FOUND (if available; otherwise falls to default)
                    errorMsg = "Project not found. Check your Firebase configuration.";
                    break;
                case 4:  // ERROR_CODE_NETWORK_ERROR
                    errorMsg = "Network error. Please check your internet connection and try again.";
                    break;
                case -1:  // ERROR_CODE_UNKNOWN
                default:
                    errorMsg = defaultMessage + " (Error code: " + errorCode + ")";
                    break;
            }
        } else {
            errorMsg = defaultMessage + ": " + (e != null ? e.getMessage() : "Unknown error");
        }
        _uploadError.postValue(errorMsg);
        mSaveStatus.setValue(SaveStatus.FAILED);
    }

    private void updateUserProfile(FirebaseUser user, String newUsername, String newEmail,
                                   String newImageUrl, String currentPassword,
                                   boolean usernameChanged, boolean emailChanged) {

        if (emailChanged) {
            // Re-authenticate first if email is changing
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential)
                    .addOnCompleteListener(reauthTask -> {
                        if (reauthTask.isSuccessful()) {
                            Log.d(TAG, "Re-authentication successful");
                            proceedWithUpdates(user, newUsername, newEmail, newImageUrl, usernameChanged, true);
                        } else {
                            Log.e(TAG, "Re-authentication failed", reauthTask.getException());
                            _reauthRequired.postValue("Re-authentication failed. Incorrect password.");
                            mSaveStatus.setValue(SaveStatus.FAILED);
                        }
                    });
        } else {
            // No email change, proceed directly
            proceedWithUpdates(user, newUsername, newEmail, newImageUrl, usernameChanged, false);
        }
    }

    private void proceedWithUpdates(FirebaseUser user, String newUsername, String newEmail,
                                    String newImageUrl, boolean usernameChanged, boolean emailChanged) {

        // Build Firebase Auth profile update
        UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest.Builder();

        if (usernameChanged) {
            profileUpdatesBuilder.setDisplayName(newUsername);
        }

        if (newImageUrl != null) {
            profileUpdatesBuilder.setPhotoUri(Uri.parse(newImageUrl));
        }

        // Update Firebase Auth profile first
        user.updateProfile(profileUpdatesBuilder.build())
                .addOnCompleteListener(profileTask -> {
                    if (profileTask.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth profile updated");

                        // If email needs updating, do it now
                        if (emailChanged && newEmail != null) {
                            user.updateEmail(newEmail)
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Log.d(TAG, "Email updated successfully");
                                            _emailUpdateStatus.postValue("Email updated. Please verify your new email.");
                                            // Send verification email
                                            user.sendEmailVerification();
                                            // Update Firestore
                                            updateFirestoreProfile(user.getUid(), newUsername, newEmail, newImageUrl);
                                        } else {
                                            Log.e(TAG, "Failed to update email", emailTask.getException());
                                            _emailUpdateStatus.postValue("Failed to update email: " +
                                                    (emailTask.getException() != null ? emailTask.getException().getMessage() : "Unknown error"));
                                            mSaveStatus.setValue(SaveStatus.FAILED);
                                        }
                                    });
                        } else {
                            // No email change, just update Firestore
                            updateFirestoreProfile(user.getUid(), newUsername, newEmail, newImageUrl);
                        }
                    } else {
                        Log.e(TAG, "Failed to update Firebase Auth profile", profileTask.getException());
                        mSaveStatus.setValue(SaveStatus.FAILED);
                        _uploadError.postValue("Failed to update profile: " + (profileTask.getException() != null ? profileTask.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void updateFirestoreProfile(String uid, String username, String email, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();

        if (username != null && !username.isEmpty()) {
            updates.put("username", username);
        }

        if (email != null && !email.isEmpty()) {
            updates.put("email", email);
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            updates.put("profileImageUrl", imageUrl);
        }

        updates.put("updatedAt", System.currentTimeMillis());

        if (updates.isEmpty() || updates.size() == 1) { // Only updatedAt
            mSaveStatus.setValue(SaveStatus.SUCCESS);
            return;
        }

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore profile updated successfully");
                    mSaveStatus.setValue(SaveStatus.SUCCESS);
                    // Reload profile to reflect changes
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Firestore profile", e);
                    // Try to create the document if it doesn't exist
                    db.collection("users").document(uid).set(updates)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Firestore profile created successfully");
                                mSaveStatus.setValue(SaveStatus.SUCCESS);
                                loadUserProfile();
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Failed to create Firestore profile", e2);
                                mSaveStatus.setValue(SaveStatus.FAILED);
                                _uploadError.postValue("Failed to save profile to database: " + (e2 != null ? e2.getMessage() : "Unknown error"));
                            });
                });
    }

    public void clearReauthRequired() {
        _reauthRequired.setValue(null);
    }

    public void clearEmailUpdateStatus() {
        _emailUpdateStatus.setValue(null);
    }

    // NEW: Clear upload error
    public void clearUploadError() {
        _uploadError.setValue(null);
    }

    // Interface for image upload callback
    private interface OnImageUploadListener {
        void onUploadComplete(String imageUrl);
    }
}