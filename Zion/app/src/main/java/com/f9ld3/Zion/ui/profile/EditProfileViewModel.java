package com.f9ld3.Zion.ui.profile;

import android.content.Context;
import android.graphics.Bitmap;
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

    public void saveProfile(String newAccountName, String newUsername, String newEmail, String newBio,
                            Uri newImageUri, Uri newBannerUri, String currentPassword, Context context) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            mSaveStatus.setValue(SaveStatus.FAILED);
            _uploadError.postValue("User not authenticated. Please log in again.");
            return;
        }
        mSaveStatus.setValue(SaveStatus.LOADING);
        _uploadError.postValue(null);

        String currentAccountName = user.getDisplayName() != null ? user.getDisplayName() : "";
        String currentEmail = user.getEmail() != null ? user.getEmail() : "";
        UserProfile currentUserProfile = mUserProfile.getValue();
        String currentUsername = currentUserProfile != null ? currentUserProfile.getUsername() : "";

        boolean accountNameChanged = !currentAccountName.equals(newAccountName);
        boolean usernameChanged = newUsername != null && !newUsername.equals(currentUsername);
        boolean emailChanged = newEmail != null && !newEmail.isEmpty() && !currentEmail.equals(newEmail);

        boolean imageRemoved = "REMOVE_IMAGE".equals(newImageUri != null ? newImageUri.toString() : "");
        boolean bannerRemoved = "REMOVE_BANNER".equals(newBannerUri != null ? newBannerUri.toString() : "");
        boolean imageChanged = newImageUri != null && !imageRemoved;
        boolean bannerChanged = newBannerUri != null && !bannerRemoved;


        if (emailChanged && (currentPassword == null || currentPassword.isEmpty())) {
            _reauthRequired.setValue("Re-authentication is required to change email. Please provide your current password.");
            mSaveStatus.setValue(SaveStatus.IDLE);
            return;
        }

        // Image Upload/Removal Logic
        uploadProfileImage(imageChanged ? newImageUri : null, imageRemoved, user.getUid(), context, imageUrl -> {
            // Banner Upload/Removal Logic
            uploadBannerImage(bannerChanged ? newBannerUri : null, bannerRemoved, user.getUid(), context, bannerUrl -> {
                // Once both images are handled, update the profile
                updateUserProfile(user, newAccountName, newUsername, newEmail, newBio, imageUrl, bannerUrl,
                        currentPassword, accountNameChanged, usernameChanged, emailChanged, imageRemoved, bannerRemoved);
            });
        });
    }

    private Uri compressImage(Uri imageUri, Context context) {
        try {
            Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            if (width > 1024 || height > 1024) {
                float scale = Math.min(1024f / width, 1024f / height);
                width = Math.round(width * scale);
                height = Math.round(height * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            File compressedFile = new File(context.getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(compressedFile);
            fos.write(data);
            fos.close();

            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }

            return Uri.fromFile(compressedFile);
        } catch (IOException e) {
            Log.e(TAG, "Image compression failed", e);
            return null;
        }
    }

    private void uploadProfileImage(Uri imageUri, boolean removeImage, String userId, Context context, OnImageUploadListener listener) {
        if (removeImage) {
            listener.onUploadComplete(""); // Pass empty string to signify removal
            return;
        }
        if (imageUri == null) {
            listener.onUploadComplete(null); // No change
            return;
        }
        Uri compressedUri = compressImage(imageUri, context);
        if (compressedUri == null) {
            handleStorageError(new IOException("Compression failed"), "Could not prepare image for upload.");
            return;
        }


        StorageReference profileImagesRef = storage.getReference().child("profile_images/" + userId + "/profile.jpg");

        profileImagesRef.putFile(compressedUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    _uploadProgress.postValue((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> profileImagesRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> listener.onUploadComplete(uri.toString()))
                        .addOnFailureListener(e -> handleStorageError(e, "Failed to retrieve image URL.")))
                .addOnFailureListener(e -> handleStorageError(e, "Image upload failed."));
    }

    private void uploadBannerImage(Uri imageUri, boolean removeBanner, String userId, Context context, OnImageUploadListener listener) {
        if (removeBanner) {
            listener.onUploadComplete(""); // Pass empty string for removal
            return;
        }
        if (imageUri == null) {
            listener.onUploadComplete(null); // No change
            return;
        }

        Uri compressedUri = compressImage(imageUri, context);
        if (compressedUri == null) {
            handleStorageError(new IOException("Compression failed"), "Could not prepare banner for upload.");
            return;
        }

        StorageReference bannerImagesRef = storage.getReference().child("banner_images/" + userId + "/banner.jpg");

        bannerImagesRef.putFile(compressedUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    _uploadProgress.postValue((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> bannerImagesRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> listener.onUploadComplete(uri.toString()))
                        .addOnFailureListener(e -> handleStorageError(e, "Failed to retrieve banner URL.")))
                .addOnFailureListener(e -> handleStorageError(e, "Banner upload failed."));
    }


    private void handleStorageError(Exception e, String defaultMessage) {
        String errorMsg = defaultMessage;
        if (e instanceof StorageException) {
            StorageException storageException = (StorageException) e;
            int errorCode = storageException.getErrorCode();
            switch (errorCode) {
                case -13021: errorMsg = "File not found."; break;
                case -13010: errorMsg = "Permission denied. Check storage rules."; break;
                default: errorMsg = defaultMessage + " (Error code: " + errorCode + ")"; break;
            }
        } else {
            errorMsg = defaultMessage + ": " + (e != null ? e.getMessage() : "Unknown error");
        }
        _uploadError.postValue(errorMsg);
        mSaveStatus.setValue(SaveStatus.FAILED);
    }

    private void updateUserProfile(FirebaseUser user, String newAccountName, String newUsername, String newEmail, String newBio,
                                   String newImageUrl, String newBannerUrl, String currentPassword,
                                   boolean accountNameChanged, boolean usernameChanged, boolean emailChanged,
                                   boolean imageRemoved, boolean bannerRemoved) {

        if (emailChanged) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential)
                    .addOnCompleteListener(reauthTask -> {
                        if (reauthTask.isSuccessful()) {
                            proceedWithUpdates(user, newAccountName, newUsername, newEmail, newBio, newImageUrl, newBannerUrl, accountNameChanged, usernameChanged, true, imageRemoved, bannerRemoved);
                        } else {
                            _reauthRequired.postValue("Re-authentication failed. Incorrect password.");
                            mSaveStatus.setValue(SaveStatus.FAILED);
                        }
                    });
        } else {
            proceedWithUpdates(user, newAccountName, newUsername, newEmail, newBio, newImageUrl, newBannerUrl, accountNameChanged, usernameChanged, false, imageRemoved, bannerRemoved);
        }
    }

    private void proceedWithUpdates(FirebaseUser user, String newAccountName, String newUsername, String newEmail, String newBio,
                                    String newImageUrl, String newBannerUrl, boolean accountNameChanged, boolean usernameChanged, boolean emailChanged,
                                    boolean imageRemoved, boolean bannerRemoved) {

        UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest.Builder();

        if (accountNameChanged) {
            profileUpdatesBuilder.setDisplayName(newAccountName);
        }

        if (newImageUrl != null || imageRemoved) {
            profileUpdatesBuilder.setPhotoUri(newImageUrl != null && !newImageUrl.isEmpty() ? Uri.parse(newImageUrl) : null);
        }

        user.updateProfile(profileUpdatesBuilder.build())
                .addOnCompleteListener(profileTask -> {
                    if (profileTask.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth profile updated");

                        if (emailChanged && newEmail != null) {
                            user.verifyBeforeUpdateEmail(newEmail)
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            _emailUpdateStatus.postValue("Verification link sent to " + newEmail + ".");
                                            updateFirestoreProfile(user.getUid(), newAccountName, newUsername, newEmail, newBio, newImageUrl, newBannerUrl, imageRemoved, bannerRemoved);
                                        } else {
                                            _emailUpdateStatus.postValue("Failed to update email: " +
                                                    (emailTask.getException() != null ? emailTask.getException().getMessage() : "Unknown error"));
                                            mSaveStatus.setValue(SaveStatus.FAILED);
                                        }
                                    });
                        } else {
                            updateFirestoreProfile(user.getUid(), newAccountName, newUsername, newEmail, newBio, newImageUrl, newBannerUrl, imageRemoved, bannerRemoved);
                        }
                    } else {
                        Log.e(TAG, "Failed to update Firebase Auth profile", profileTask.getException());
                        mSaveStatus.setValue(SaveStatus.FAILED);
                        _uploadError.postValue("Failed to update profile: " + (profileTask.getException() != null ? profileTask.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void updateFirestoreProfile(String uid, String accountName, String username, String email, String bio, String imageUrl, String bannerUrl, boolean imageRemoved, boolean bannerRemoved) {
        Map<String, Object> updates = new HashMap<>();

        if (accountName != null && !accountName.isEmpty()) updates.put("accountName", accountName);
        updates.put("username", username);
        if (email != null && !email.isEmpty()) updates.put("email", email);
        updates.put("bio", bio);
        if (imageUrl != null || imageRemoved) updates.put("profileImageUrl", imageUrl);
        if (bannerUrl != null || bannerRemoved) updates.put("bannerImageUrl", bannerUrl);
        updates.put("updatedAt", System.currentTimeMillis());


        if (updates.size() <= 1) { // Only updatedAt
            mSaveStatus.setValue(SaveStatus.SUCCESS);
            return;
        }

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore profile updated successfully");
                    mSaveStatus.setValue(SaveStatus.SUCCESS);
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Firestore profile", e);
                    mSaveStatus.setValue(SaveStatus.FAILED);
                    _uploadError.postValue("Failed to save profile to database: " + e.getMessage());
                });
    }


    public void clearReauthRequired() {
        _reauthRequired.setValue(null);
    }

    public void clearEmailUpdateStatus() {
        _emailUpdateStatus.setValue(null);
    }

    public void clearUploadError() {
        _uploadError.setValue(null);
    }

    private interface OnImageUploadListener {
        void onUploadComplete(String imageUrl);
    }
}
