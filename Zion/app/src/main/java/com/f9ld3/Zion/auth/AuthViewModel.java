// MultipleFiles/AuthViewModel.java
package com.f9ld3.Zion.auth;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.f9ld3.Zion.R;  // For string resources (add if needed for messages)
import com.f9ld3.Zion.data.UserProfile;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser  ;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthViewModel extends ViewModel {

    private static final String TAG = "AuthViewModel";

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private final MutableLiveData<FirebaseUser  > _currentUser   = new MutableLiveData<>();
    public LiveData<FirebaseUser  > getCurrentUser  () {
        return _currentUser  ;
    }

    private final MutableLiveData<Boolean> _isAuthenticated = new MutableLiveData<>();
    public LiveData<Boolean> isAuthenticated() {
        return _isAuthenticated;
    }

    private final MutableLiveData<String> _authError = new MutableLiveData<>();
    public LiveData<String> getAuthError() {
        return _authError;
    }

    // Dedicated LiveData for success/info messages (e.g., verification prompts)
    private final MutableLiveData<String> _authMessage = new MutableLiveData<>();
    public LiveData<String> getAuthMessage() {
        return _authMessage;
    }

    // LiveData for email verification status
    private final MutableLiveData<Boolean> _isEmailVerified = new MutableLiveData<>();
    public LiveData<Boolean> isEmailVerified() {
        return _isEmailVerified;
    }

    // LiveData for password reset status
    private final MutableLiveData<Boolean> _passwordResetSent = new MutableLiveData<>();
    public LiveData<Boolean> getPasswordResetSent() {
        return _passwordResetSent;
    }

    // Requires non-anonymous and verified email
    public boolean canPerformAuthenticatedAction() {
        FirebaseUser   user = mAuth.getCurrentUser  ();
        return user != null && !user.isAnonymous() && user.isEmailVerified();
    }

    public AuthViewModel() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mAuth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser   user = firebaseAuth.getCurrentUser  ();
            if (user != null && user.isAnonymous()) {
                // Force sign out for anonymous users (no anonymous access allowed)
                Log.w(TAG, "Anonymous user detected. Signing out to force registration.");
                mAuth.signOut();
                _currentUser  .setValue(null);
                _isAuthenticated.setValue(false);
                _isEmailVerified.setValue(false);
                return;
            }
            _currentUser  .setValue(user);
            _isAuthenticated.setValue(user != null && !user.isAnonymous()); // Only non-anonymous is authenticated
            if (user != null && !user.isAnonymous()) {
                Log.d(TAG, "Auth state changed: User " + user.getUid() + " is authenticated");
                _isEmailVerified.setValue(user.isEmailVerified()); // Update verification status
                // Ensure user profile exists in Firestore for registered users
                checkAndCreateUserProfile(user);
            } else {
                Log.d(TAG, "Auth state changed: No registered user signed in.");
                _isEmailVerified.setValue(false); // Reset on sign out
            }
        });
    }

    // REMOVED: signInAnonymously() method - No longer supported

    public void signInWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmailAndPassword:success");
                        // AuthStateListener will handle updating LiveData
                    } else {
                        Log.w(TAG, "signInWithEmailAndPassword:failure", task.getException());
                        _authError.setValue(task.getException() != null ? task.getException().getMessage() : "Sign-in failed.");
                    }
                });
    }

    public void loginWithEmailAndPassword(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            _authError.setValue("Email and password are required.");
            return;
        }
        signInWithEmailAndPassword(email.trim(), password);
    }

    // Proper camelCase method name
    public void createUserWithEmailAndPassword(String email, String password, String username) {
        if (username == null || username.trim().isEmpty() || username.length() < 3) {
            _authError.setValue("Username must be at least 3 characters.");
            return;
        }
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty() || password.length() < 6) {
            _authError.setValue("Valid email and password (6+ characters) are required.");
            return;
        }

        // Check username uniqueness in Firestore before creating account
        db.collection("usernames").whereEqualTo("username", username.trim()).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        _authError.setValue("Username '" + username + "' is already taken. Please choose another.");
                        return;
                    }
                    // Proceed with account creation if unique
                    proceedWithAccountCreation(email.trim(), password, username.trim());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check username uniqueness", e);
                    _authError.setValue("Failed to check username availability. Please try again.");
                });
    }

    // Helper method to proceed after uniqueness check
    private void proceedWithAccountCreation(String email, String password, String username) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "createUser  WithEmailAndPassword:success");
                FirebaseUser   user = mAuth.getCurrentUser  ();
                if (user != null) {
                    // Send email verification with improved error handling
                    sendEmailVerification(user, () -> {
                        // On verification send success/fail, create profile
                        createUserProfile(user, username, email);
                    });
                }
                // AuthStateListener will handle updating LiveData
            } else {
                Log.w(TAG, "createUser  WithEmailAndPassword:failure", task.getException());
                _authError.setValue(task.getException() != null ? task.getException().getMessage() : "Account creation failed.");
            }
        });
    }

    // UPDATED: Separated verification send with callback for profile creation
    private void sendEmailVerification(FirebaseUser   user, Runnable onComplete) {
        if (user == null) {
            Log.e(TAG, "Cannot send verification: User is null.");
            _authError.setValue("Invalid user state. Please try registering again.");
            if (onComplete != null) onComplete.run();
            return;
        }

        try {
            // Optional: Configure for dynamic links (requires Firebase setup)
            ActionCodeSettings settings = ActionCodeSettings.newBuilder()  // FIXED: Use static factory method
                    .setUrl("https://yourapp.page.link/verify") // TODO: Replace with your actual dynamic link domain
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName("com.f9ld3.Zion", true, null) // Your package
                    .build();

            user.sendEmailVerification(settings)
                    .addOnCompleteListener(verificationTask -> {
                        if (verificationTask.isSuccessful()) {
                            Log.d(TAG, "Verification email sent to: " + user.getEmail());
                            _authMessage.setValue("Account created. Please check your email for verification.");
                        } else {
                            Log.e(TAG, "Failed to send verification email.", verificationTask.getException());
                            // Don't block account creation; just notify
                            _authMessage.setValue("Account created, but verification email failed to send. You can resend from your profile.");
                        }
                        if (onComplete != null) onComplete.run(); // Proceed to profile creation
                    });
        } catch (Exception e) { // Network/offline handling
            Log.e(TAG, "Network error sending verification", e);
            _authError.setValue("Network error. Please check connection and try again.");
            if (onComplete != null) onComplete.run(); // Still create profile
        }
    }

    // UPDATED: Method to resend verification email (UI-friendly)
    public void resendVerificationEmail() {
        FirebaseUser   user = mAuth.getCurrentUser  ();
        if (user == null || user.isAnonymous()) {
            _authError.setValue("You must be logged in with an email account to resend verification.");
            return;
        }
        if (user.isEmailVerified()) {
            _authMessage.setValue("Your email is already verified.");
            return;
        }

        try {
            ActionCodeSettings settings = ActionCodeSettings.newBuilder()  // FIXED: Use static factory method
                    .setUrl("https://yourapp.page.link/verify") // TODO: Replace with your actual dynamic link domain
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName("com.f9ld3.Zion", true, null)
                    .build();

            user.sendEmailVerification(settings)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Verification email resent.");
                            _authMessage.setValue("Verification email resent. Check your inbox.");
                        } else {
                            Log.e(TAG, "Failed to resend verification email.", task.getException());
                            _authError.setValue("Failed to resend: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error."));
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Network error resending verification", e);
            _authError.setValue("Network error. Please check connection.");
        }
    }

    // NEW: Method to refresh user and check verification with error handling (for UI calls)
    public void refreshVerificationStatus() {
        FirebaseUser   user = mAuth.getCurrentUser  ();
        if (user == null || user.isAnonymous()) {
            _authError.setValue("No registered user logged in.");
            return;
        }
        user.reload()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        _isEmailVerified.setValue(user.isEmailVerified());
                        Log.d(TAG, "User   refreshed. Verified: " + user.isEmailVerified());
                    } else {
                        Log.e(TAG, "Failed to refresh user.", task.getException());
                        _authError.setValue("Failed to refresh user data. Please try again.");
                        // Don't block; status remains as-is until next auth change
                    }
                });
    }

    // Proper camelCase method name
    private void createUserProfile(FirebaseUser   user, String username, String email) {
        UserProfile newProfile = new UserProfile(user.getUid(), username, email, null);
        db.collection("users").document(user.getUid()).set(newProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User   profile created in Firestore: " + username);
                    // Add to usernames collection for uniqueness
                    Map<String, Object> usernameDoc = new HashMap<>();
                    usernameDoc.put("username", username);
                    usernameDoc.put("uid", user.getUid());
                    db.collection("usernames").document(username).set(usernameDoc)
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to index username", e));
                    // Update Firebase Auth display name
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build();
                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User   display name updated.");
                                } else {
                                    Log.e(TAG, "Failed to update display name.", task.getException());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create user profile", e);
                    _authError.setValue("Profile creation failed: " + e.getMessage());
                });
    }

    // Proper camelCase method name - UPDATED: Skip for anonymous (but anonymous is now forbidden)
    private void checkAndCreateUserProfile(FirebaseUser   user) {
        if (user == null || user.isAnonymous()) return; // No profile for anonymous
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        String username = user.getDisplayName();
                        if (username == null || username.isEmpty()) {
                            username = user.getEmail() != null ? user.getEmail().split("@")[0] : "User "; // Fallback for registered users
                        }
                        String email = user.getEmail();
                        createUserProfile(user, username, email);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to check user profile", e));
    }

    public void sendPasswordResetEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            _authError.setValue("Email is required.");
            return;
        }
        mAuth.sendPasswordResetEmail(email.trim())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent.");
                        _passwordResetSent.setValue(true);
                        _authMessage.setValue("Password reset email sent. Check your inbox.");
                    } else {
                        Log.w(TAG, "Failed to send password reset email.", task.getException());
                        _authError.setValue(task.getException() != null ? task.getException().getMessage() : "Failed to send reset email.");
                    }
                });
    }

    public void signOut() {
        mAuth.signOut();
        // AuthStateListener will handle updating LiveData
        clearMessages();
    }

    // Clear messages/errors after display
    public void clearMessages() {
        _authError.setValue(null);
        _authMessage.setValue(null);
        _passwordResetSent.setValue(false);
    }

    // For compatibility (calls clearMessages)
    public void clearAuthError() {
        clearMessages();
    }
}