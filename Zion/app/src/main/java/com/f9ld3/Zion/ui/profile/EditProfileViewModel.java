// MultipleFiles/EditProfileViewModel.java
package com.f9ld3.Zion.ui.profile;

import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.data.UserProfile;
import com.google.firebase.auth.AuthCredential; // NEW
import com.google.firebase.auth.EmailAuthProvider; // NEW
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class EditProfileViewModel extends ViewModel {
    private static final String TAG = "EditProfileViewModel";
    public enum SaveStatus { IDLE, LOADING, SUCCESS, FAILED }

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<UserProfile> mUserProfile = new MutableLiveData<>();
    public LiveData<UserProfile> getUserProfile() { return mUserProfile; }

    private final MutableLiveData<SaveStatus> mSaveStatus = new MutableLiveData<>(SaveStatus.IDLE);
    public LiveData<SaveStatus> getSaveStatus() { return mSaveStatus; }

    // NEW: LiveData to signal re-authentication is needed for email change
    private final MutableLiveData<String> _reauthRequired = new MutableLiveData<>();
    public LiveData<String> getReauthRequired() { return _reauthRequired; }

    // NEW: LiveData for email update status messages
    private final MutableLiveData<String> _emailUpdateStatus = new MutableLiveData<>();
    public LiveData<String> getEmailUpdateStatus() { return _emailUpdateStatus; }


    public EditProfileViewModel() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> mUserProfile.setValue(doc.toObject(UserProfile.class)))
                .addOnFailureListener(e -> Log.w(TAG, "Error loading user profile", e));
    }

    // Modified saveProfile to handle email change and re-authentication
    public void saveProfile(String newUsername, String newEmail, Uri newImageUri, String currentPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            mSaveStatus.setValue(SaveStatus.FAILED);
            return;
        }
        mSaveStatus.setValue(SaveStatus.LOADING);

        // Image update logic is complex and omitted for brevity.
        // TODO: Handle image upload to Firebase Storage if newImageUri is not null.

        Map<String, Object> updates = new HashMap<>();
        boolean usernameChanged = !user.getDisplayName().equals(newUsername);
        boolean emailChanged = newEmail != null && !newEmail.isEmpty() && !user.getEmail().equals(newEmail);

        if (usernameChanged) {
            updates.put("username", newUsername);
        }

        if (emailChanged) {
            if (currentPassword == null || currentPassword.isEmpty()) {
                _reauthRequired.setValue("Re-authentication is required to change email. Please provide your current password.");
                mSaveStatus.setValue(SaveStatus.IDLE); // Reset status
                return;
            }

            // Re-authenticate user before changing email
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential)
                    .addOnCompleteListener(reauthTask -> {
                        if (reauthTask.isSuccessful()) {
                            user.updateEmail(newEmail)
                                    .addOnCompleteListener(emailUpdateTask -> {
                                        if (emailUpdateTask.isSuccessful()) {
                                            Log.d(TAG, "User email address updated.");
                                            _emailUpdateStatus.setValue("Email updated. Please verify your new email.");
                                            // Send verification email for the new email
                                            user.sendEmailVerification();
                                            // Update Firestore profile email
                                            updates.put("email", newEmail);
                                            updateFirestoreProfile(user.getUid(), updates); // Proceed to update Firestore
                                        } else {
                                            Log.e(TAG, "Error updating email.", emailUpdateTask.getException());
                                            _emailUpdateStatus.setValue("Failed to update email: " + (emailUpdateTask.getException() != null ? emailUpdateTask.getException().getMessage() : "Unknown error."));
                                            mSaveStatus.setValue(SaveStatus.FAILED);
                                        }
                                    });
                        } else {
                            Log.e(TAG, "Re-authentication failed.", reauthTask.getException());
                            _reauthRequired.setValue("Re-authentication failed. Incorrect password.");
                            mSaveStatus.setValue(SaveStatus.FAILED);
                        }
                    });
        } else {
            // Only update Firestore if username changed or no email change was attempted
            if (!updates.isEmpty()) {
                updateFirestoreProfile(user.getUid(), updates);
            } else {
                mSaveStatus.setValue(SaveStatus.SUCCESS); // Nothing to save, consider it a success
            }
        }
    }

    // Helper method to update Firestore profile
    private void updateFirestoreProfile(String uid, Map<String, Object> updates) {
        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> mSaveStatus.setValue(SaveStatus.SUCCESS))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Firestore profile.", e);
                    mSaveStatus.setValue(SaveStatus.FAILED);
                });
    }

    // NEW: Clear re-authentication required status
    public void clearReauthRequired() { _reauthRequired.setValue(null); }
    // NEW: Clear email update status
    public void clearEmailUpdateStatus() { _emailUpdateStatus.setValue(null); }
}