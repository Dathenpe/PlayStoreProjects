package com.f9ld3.Zion.auth;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AuthViewModel extends ViewModel {

    private static final String TAG = "AuthViewModel";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,20}$");

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private final MutableLiveData<FirebaseUser> _currentUser = new MutableLiveData<>();
    public LiveData<FirebaseUser> getCurrentUser() { return _currentUser; }

    private final MutableLiveData<Boolean> _isAuthenticated = new MutableLiveData<>();
    public LiveData<Boolean> isAuthenticated() { return _isAuthenticated; }

    private final MutableLiveData<String> _authError = new MutableLiveData<>();
    public LiveData<String> getAuthError() { return _authError; }

    private final MutableLiveData<String> _authMessage = new MutableLiveData<>();
    public LiveData<String> getAuthMessage() { return _authMessage; }

    private final MutableLiveData<Boolean> _passwordResetSent = new MutableLiveData<>();
    public LiveData<Boolean> getPasswordResetSent() { return _passwordResetSent; }

    // Track previous auth state to prevent redundant updates
    private String previousUserId = null;
    private boolean previousVerificationStatus = false;
    private Boolean previousAuthState = null;
    private boolean isInitialized = false;

    public AuthViewModel() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize with current state IMMEDIATELY (synchronously)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            boolean isVerified = currentUser.isEmailVerified();
            boolean isAuth = !currentUser.isAnonymous() && isVerified;

            // Set initial values WITHOUT triggering observers
            _currentUser.setValue(currentUser);
            _isAuthenticated.setValue(isAuth);

            previousUserId = currentUser.getUid();
            previousVerificationStatus = isVerified;
            previousAuthState = isAuth;

            Log.d(TAG, "Initial auth state - User: " + currentUser.getEmail() +
                    ", Verified: " + isVerified + ", isAuthenticated: " + isAuth);
        } else {
            _currentUser.setValue(null);
            _isAuthenticated.setValue(false);
            previousAuthState = false;
            Log.d(TAG, "Initial auth state - No user");
        }

        isInitialized = true;

        // Now set up the listener for future changes
        mAuth.addAuthStateListener(firebaseAuth -> {
            if (!isInitialized) return; // Safety check

            FirebaseUser user = firebaseAuth.getCurrentUser();

            // Get current state
            String currentUserId = user != null ? user.getUid() : null;
            boolean currentVerificationStatus = user != null && user.isEmailVerified();
            boolean isFullyAuthenticated = user != null && !user.isAnonymous() && currentVerificationStatus;

            // Check if user changed (login/logout)
            boolean userChanged = !isEqual(previousUserId, currentUserId);

            // Check if verification status changed
            boolean verificationChanged = previousVerificationStatus != currentVerificationStatus;

            // Check if overall auth state changed
            boolean authStateChanged = previousAuthState == null || previousAuthState != isFullyAuthenticated;

            // Only update LiveData if something actually changed
            if (userChanged) {
                Log.d(TAG, "User changed: " + previousUserId + " -> " + currentUserId);
                _currentUser.setValue(user);
                previousUserId = currentUserId;
            }

            if (authStateChanged) {
                Log.d(TAG, "Auth state changed - User: " + (user != null ? user.getEmail() : "null") +
                        ", Verified: " + currentVerificationStatus +
                        ", isAuthenticated: " + isFullyAuthenticated +
                        ", Previous: " + previousAuthState);
                _isAuthenticated.setValue(isFullyAuthenticated);
                previousAuthState = isFullyAuthenticated;
                previousVerificationStatus = currentVerificationStatus;
            }
        });
    }

    // Helper to safely compare strings (handles nulls)
    private boolean isEqual(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // --- Validation Helpers ---

    public boolean isEmailValid(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public boolean isUsernameValid(String username) {
        return USERNAME_PATTERN.matcher(username).matches();
    }

    // --- Core Authentication Methods ---

    public void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Force reload to get latest email verification status
                            user.reload().addOnCompleteListener(reloadTask -> {
                                if (reloadTask.isSuccessful()) {
                                    // CRITICAL FIX: Check verification status after reload
                                    if (user.isEmailVerified()) {
                                        Log.d(TAG, "signIn:success - email verified");
                                        // Update LiveData immediately to trigger navigation
                                        _currentUser.postValue(user);
                                        _isAuthenticated.postValue(true);
                                        // Auth state listener will also fire, but this ensures immediate update
                                    } else {
                                        Log.d(TAG, "signIn:success - but email NOT verified");
                                        _authError.postValue("Login successful, but your email is not verified. Please check your inbox or click 'Resend'.");
                                    }
                                } else {
                                    Log.e(TAG, "Failed to reload user", reloadTask.getException());
                                    _authError.postValue("An error occurred. Please try again.");
                                }
                            });
                        }
                    } else {
                        handleAuthException(task.getException(), "Login failed. Please check your credentials.");
                    }
                });
    }

    public void signUp(String email, String password, String username) {
        if (!isUsernameValid(username)) {
            _authError.postValue("Username must be 3-20 characters long and contain only letters, numbers, '.', '_', or '-'.");
            return;
        }

        db.collection("usernames").document(username).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        _authError.postValue("This username is already taken. Please choose another.");
                    } else {
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        if (user != null) {
                                            sendVerificationEmail(user);
                                            updateUserProfile(user, username, email);
                                            _authMessage.postValue("Registration successful! A verification email has been sent to " + email + ". Please check your inbox.");
                                        }
                                    } else {
                                        handleAuthException(task.getException(), "Registration failed.");
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore check failed: ", e);
                    _authError.postValue("A network error occurred. Please check your connection and try again.");
                });
    }

    // --- Email Verification Methods ---

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Verification email sent."))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send verification email.", e);
                    _authError.postValue("Failed to send verification email. Please try again later.");
                });
    }

    public void resendVerificationFromLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.reload().addOnCompleteListener(reloadTask -> {
                                if (reloadTask.isSuccessful()) {
                                    if (!user.isEmailVerified()) {
                                        sendVerificationEmail(user);
                                        _authMessage.postValue("Verification email re-sent successfully. Check your inbox.");
                                    } else {
                                        _authMessage.postValue("Your email is already verified!");
                                        // Update auth state immediately
                                        _isAuthenticated.postValue(true);
                                    }
                                } else {
                                    Log.e(TAG, "Failed to reload user", reloadTask.getException());
                                    _authError.postValue("An error occurred. Please try again.");
                                }
                            });
                        }
                    } else {
                        handleAuthException(task.getException(), "Could not resend verification. Please check your credentials.");
                    }
                });
    }

    public void resendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            sendVerificationEmail(user);
            _authMessage.postValue("Verification email re-sent. Please check your inbox.");
        } else if (user == null) {
            _authError.postValue("You are not signed in.");
        } else {
            _authMessage.postValue("Your email is already verified.");
        }
    }

    // --- Password Management Methods ---

    public void changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            _authError.postValue("User not logged in. Please log in again.");
            return;
        }
        if (newPassword.length() < 6) {
            _authError.postValue("New password must be at least 6 characters long.");
            return;
        }

        String email = user.getEmail();
        if (email == null) {
            _authError.postValue("Unable to retrieve user email.");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        _authMessage.postValue("Password updated successfully!");
                                        Log.d(TAG, "User password updated.");
                                    } else {
                                        handleAuthException(updateTask.getException(), "Failed to update password.");
                                    }
                                });
                    } else {
                        handleAuthException(task.getException(), "Current password is incorrect. Password not changed.");
                    }
                });
    }

    public void sendPasswordResetEmail(String email) {
        if (!isEmailValid(email)) {
            _authError.postValue("Invalid email format.");
            return;
        }
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        _authMessage.postValue("Password reset link sent to " + email + ". Check your inbox.");
                        _passwordResetSent.postValue(true);
                    } else {
                        handleAuthException(task.getException(), "Could not send password reset link. Please ensure the email is correct.");
                    }
                });
    }

    // --- Profile Management Methods ---

    public void updateUsername(String newUsername, OnCompleteCallback callback) {
        if (!isUsernameValid(newUsername)) {
            if (callback != null) {
                callback.onError("Username must be 3-20 characters long and contain only letters, numbers, '.', '_', or '-'.");
            }
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onError("User not logged in. Please log in again.");
            }
            return;
        }

        String oldUsername = user.getDisplayName();

        // Check if username is available
        db.collection("usernames").document(newUsername).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && !newUsername.equals(oldUsername)) {
                        if (callback != null) {
                            callback.onError("This username is already taken. Please choose another.");
                        }
                    } else {
                        // Update Firebase Auth profile
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(newUsername)
                                .build();

                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Update Firestore
                                        db.collection("users").document(user.getUid())
                                                .update("username", newUsername)
                                                .addOnSuccessListener(aVoid -> {
                                                    // Update username mapping
                                                    if (oldUsername != null && !oldUsername.equals(newUsername)) {
                                                        db.collection("usernames").document(oldUsername).delete();
                                                    }

                                                    Map<String, Object> usernameDoc = new HashMap<>();
                                                    usernameDoc.put("uid", user.getUid());
                                                    db.collection("usernames").document(newUsername).set(usernameDoc)
                                                            .addOnSuccessListener(aVoid2 -> {
                                                                Log.d(TAG, "Username updated successfully");
                                                                if (callback != null) {
                                                                    callback.onSuccess("Username updated successfully!");
                                                                }
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Log.e(TAG, "Failed to update username mapping", e);
                                                                if (callback != null) {
                                                                    callback.onError("Failed to update username. Please try again.");
                                                                }
                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Failed to update Firestore", e);
                                                    if (callback != null) {
                                                        callback.onError("Failed to update profile. Please try again.");
                                                    }
                                                });
                                    } else {
                                        Log.e(TAG, "Failed to update profile", task.getException());
                                        if (callback != null) {
                                            callback.onError("Failed to update username. Please try again.");
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check username availability", e);
                    if (callback != null) {
                        callback.onError("A network error occurred. Please check your connection and try again.");
                    }
                });
    }

    // Callback interface for async operations
    public interface OnCompleteCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // --- Profile/Firestore Methods ---

    private void updateUserProfile(FirebaseUser user, String username, String email) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated.");
                    } else {
                        Log.e(TAG, "Failed to update profile", task.getException());
                    }
                });

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("uid", user.getUid());
        userProfile.put("username", username);
        userProfile.put("email", email);
        userProfile.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(user.getUid()).set(userProfile)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create user profile", e));

        Map<String, Object> usernameDoc = new HashMap<>();
        usernameDoc.put("uid", user.getUid());
        db.collection("usernames").document(username).set(usernameDoc)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create username mapping", e));
    }

    private void handleAuthException(Exception exception, String defaultMessage) {
        String errorMsg = defaultMessage;
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            switch (errorCode) {
                case "ERROR_USER_NOT_FOUND":
                    errorMsg = "No account found with this email.";
                    break;
                case "ERROR_WRONG_PASSWORD":
                    errorMsg = "Incorrect password.";
                    break;
                case "ERROR_USER_DISABLED":
                    errorMsg = "This account has been disabled.";
                    break;
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    errorMsg = "An account with this email already exists.";
                    break;
                case "ERROR_WEAK_PASSWORD":
                    errorMsg = "Password is too weak. Must be at least 6 characters.";
                    break;
                case "ERROR_INVALID_EMAIL":
                    errorMsg = "The email address is not valid.";
                    break;
                case "ERROR_INVALID_CREDENTIAL":
                    errorMsg = "Invalid credentials. Please try again.";
                    break;
                case "ERROR_NETWORK_REQUEST_FAILED":
                    errorMsg = "Network error. Please check your connection and try again.";
                    break;
                case "ERROR_TOO_MANY_REQUESTS":
                    errorMsg = "Too many attempts. Please try again later.";
                    break;
                case "ERROR_OPERATION_NOT_ALLOWED":
                    errorMsg = "This operation is not allowed. Please contact support.";
                    break;
                case "ERROR_REQUIRES_RECENT_LOGIN":
                    errorMsg = "This operation requires recent authentication. Please log in again.";
                    break;
                default:
                    errorMsg = "An error occurred. Please try again.";
                    Log.e(TAG, "Unhandled Firebase Auth error: " + errorCode + " - " + exception.getMessage());
                    break;
            }
        } else if (exception != null) {
            // Generic error handling for non-Firebase exceptions
            String message = exception.getMessage();
            if (message != null) {
                if (message.contains("network")) {
                    errorMsg = "Network error. Please check your connection and try again.";
                } else if (message.contains("timeout")) {
                    errorMsg = "Request timed out. Please try again.";
                } else {
                    errorMsg = "An unexpected error occurred. Please try again.";
                }
            }
            Log.e(TAG, "Auth error: " + exception.getMessage(), exception);
        }
        _authError.postValue(errorMsg);
    }

    public void signOut() {
        mAuth.signOut();
        // Reset tracking variables
        previousUserId = null;
        previousVerificationStatus = false;
        previousAuthState = null;
    }

    public void clearMessages() {
        _authError.postValue(null);
        _authMessage.postValue(null);
        _passwordResetSent.postValue(false);
    }
}