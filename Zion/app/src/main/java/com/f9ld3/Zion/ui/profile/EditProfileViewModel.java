package com.f9ld3.Zion.ui.profile;

import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.data.UserProfile;
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

    public void saveProfile(String newUsername, Uri newImageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            mSaveStatus.setValue(SaveStatus.FAILED);
            return;
        }
        mSaveStatus.setValue(SaveStatus.LOADING);
        // Image update logic is complex and omitted for brevity.
        // TODO: Handle image upload to Firebase Storage if newImageUri is not null.
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);
        db.collection("users").document(user.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> mSaveStatus.setValue(SaveStatus.SUCCESS))
                .addOnFailureListener(e -> mSaveStatus.setValue(SaveStatus.FAILED));
    }
}