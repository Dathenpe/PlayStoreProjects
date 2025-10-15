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

    public void saveProfile(String newUsername, String newEmail, Uri newImageUri, String currentPassword, Context context) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            mSaveStatus.setValue(SaveStatus.FAILED);
            _uploadError.postValue("User not authenticated. Please log in again.");
            return;
        }
        mSaveStatus.setValue(SaveStatus.LOADING);
        _uploadError.postValue(null);

        String currentUsername = user.getDisplayName() != null ? user.getDisplayName() : "";
        String currentEmail = user.getEmail() != null ? user.getEmail() : "";

        boolean usernameChanged = !currentUsername.equals(newUsername);
        boolean emailChanged = newEmail != null && !newEmail.isEmpty() && !currentEmail.equals(newEmail);
        boolean imageChanged = newImageUri != null;

        if (emailChanged && (currentPassword == null || currentPassword.isEmpty())) {
            _reauthRequired.setValue("Re-authentication is required to change email. Please provide your current password.");
            mSaveStatus.setValue(SaveStatus.IDLE);
            return;
        }

        if (imageChanged) {
            Uri compressedImageUri = compressImage(newImageUri, context);
            uploadProfileImage(compressedImageUri != null ? compressedImageUri : newImageUri, user.getUid(), imageUrl -> {
                updateUserProfile(user, newUsername, newEmail, imageUrl, currentPassword, usernameChanged, emailChanged);
            });
        } else {
            updateUserProfile(user, newUsername, newEmail, null, currentPassword, usernameChanged, emailChanged);
        }
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

    private void uploadProfileImage(Uri imageUri, String userId, OnImageUploadListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            _uploadError.postValue("User not authenticated. Please log in again.");
            mSaveStatus.setValue(SaveStatus.FAILED);
            return;
        }

        String uid = currentUser.getUid();
        if (uid == null || uid.isEmpty()) {
            _uploadError.postValue("Invalid user ID. Please log in again.");
            mSaveStatus.setValue(SaveStatus.FAILED);
            return;
        }

        // CORRECTED PATH
        StorageReference profileImagesRef = storage.getReference()
                .child("profile_images")
                .child(uid)
                .child("profile.jpg");

        Log.d(TAG, "Uploading to path: " + profileImagesRef.getPath());

        profileImagesRef.putFile(imageUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    _uploadProgress.postValue((int) progress);
                    Log.d(TAG, "Upload progress: " + progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully");
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

    private void handleStorageError(Exception e, String defaultMessage) {
        String errorMsg = defaultMessage;
        if (e instanceof StorageException) {
            StorageException storageException = (StorageException) e;
            int errorCode = storageException.getErrorCode();
            switch (errorCode) {
                case -13021: // ERROR_OBJECT_NOT_FOUND
                    errorMsg = "File not found. The upload path may be incorrect.";
                    break;
                case -13010: // ERROR_UNAUTHORIZED
                    errorMsg = "Permission denied. Please check your Firebase Storage rules or contact support.";
                    break;
                case -13000: // ERROR_UNKNOWN
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
            proceedWithUpdates(user, newUsername, newEmail, newImageUrl, usernameChanged, false);
        }
    }

    private void proceedWithUpdates(FirebaseUser user, String newUsername, String newEmail,
                                    String newImageUrl, boolean usernameChanged, boolean emailChanged) {

        UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest.Builder();

        if (usernameChanged) {
            profileUpdatesBuilder.setDisplayName(newUsername);
        }

        if (newImageUrl != null) {
            profileUpdatesBuilder.setPhotoUri(Uri.parse(newImageUrl));
        }

        user.updateProfile(profileUpdatesBuilder.build())
                .addOnCompleteListener(profileTask -> {
                    if (profileTask.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth profile updated");

                        if (emailChanged && newEmail != null) {
                            user.verifyBeforeUpdateEmail(newEmail)
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Log.d(TAG, "Verification email sent to new address.");
                                            _emailUpdateStatus.postValue("Verification link sent to " + newEmail + ". Please check your inbox to confirm the change.");
                                            updateFirestoreProfile(user.getUid(), newUsername, newEmail, newImageUrl);
                                        } else {
                                            Log.e(TAG, "Failed to send verification email", emailTask.getException());
                                            _emailUpdateStatus.postValue("Failed to update email: " +
                                                    (emailTask.getException() != null ? emailTask.getException().getMessage() : "Unknown error"));
                                            mSaveStatus.setValue(SaveStatus.FAILED);
                                        }
                                    });
                        } else {
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
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Firestore profile", e);
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

    public void clearUploadError() {
        _uploadError.setValue(null);
    }

    private interface OnImageUploadListener {
        void onUploadComplete(String imageUrl);
    }
}