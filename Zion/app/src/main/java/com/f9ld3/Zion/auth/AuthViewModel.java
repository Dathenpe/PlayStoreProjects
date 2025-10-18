package com.f9ld3.Zion.auth;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    private final MutableLiveData<Boolean> _accountDeleted = new MutableLiveData<>();
    public LiveData<Boolean> getAccountDeleted() { return _accountDeleted; }

    public AuthViewModel() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mAuth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            _currentUser.postValue(user);
            _isAuthenticated.postValue(user != null && user.isEmailVerified());
        });
    }

    public void deleteAccount(String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            _authError.postValue("User not signed in or email is missing.");
            return;
        }

        String uid = user.getUid();
        String username = user.getDisplayName();
        String email = user.getEmail();

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                // Start a batch to delete all user-related data
                WriteBatch batch = db.batch();

                // 1. Delete user document
                batch.delete(db.collection("users").document(uid));

                // 2. Delete username document
                if (username != null && !username.isEmpty()) {
                    batch.delete(db.collection("usernames").document(username));
                }

                // 3. Delete email document
                if (email != null && !email.isEmpty()) {
                    batch.delete(db.collection("emails").document(email));
                }


                // Commit the batch
                batch.commit().addOnCompleteListener(batchTask -> {
                    if(batchTask.isSuccessful()){
                        // Finally delete the auth user
                        user.delete().addOnCompleteListener(deleteTask -> {
                            if (deleteTask.isSuccessful()) {
                                Log.d(TAG, "User account and all data deleted.");
                                _accountDeleted.postValue(true);
                            } else {
                                handleAuthException(deleteTask.getException(), "Failed to delete Firebase Auth user.");
                            }
                        });
                    } else {
                        _authError.postValue("Failed to delete user data from Firestore.");
                    }
                });

            } else {
                handleAuthException(reauthTask.getException(), "Authentication failed. Could not delete account.");
            }
        });
    }

    public boolean isEmailValid(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public boolean isUsernameValid(String username) {
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                mAuth.getCurrentUser().reload().addOnCompleteListener(reloadTask -> {
                    if (reloadTask.isSuccessful()) {
                        if (mAuth.getCurrentUser().isEmailVerified()) {
                            _isAuthenticated.postValue(true);
                        } else {
                            _authError.postValue("Login successful, but your email is not verified.");
                        }
                    } else {
                        _authError.postValue("An error occurred while verifying your session.");
                    }
                });
            } else {
                handleAuthException(task.getException(), "Login failed.");
            }
        });
    }

    public void signUp(String email, String password, String accountName) {
        if (!isUsernameValid(accountName)) {
            _authError.postValue("Invalid Account Name format.");
            return;
        }

        // Check for unique email first
        db.collection("emails").document(email).get().addOnSuccessListener(emailDoc -> {
            if (emailDoc.exists()) {
                _authError.postValue("This email is already in use.");
                return;
            }

            // Then check for unique username
            db.collection("usernames").document(accountName).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    _authError.postValue("This account name is already taken.");
                } else {
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                        if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            sendVerificationEmail(user);
                            updateUserProfile(user, accountName, email);
                            _authMessage.postValue("Registration successful! A verification email has been sent.");
                        } else {
                            handleAuthException(task.getException(), "Registration failed.");
                        }
                    });
                }
            }).addOnFailureListener(e -> _authError.postValue("Network error. Please try again."));
        }).addOnFailureListener(e -> _authError.postValue("Network error checking email. Please try again."));
    }


    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification().addOnFailureListener(e -> _authError.postValue("Failed to send verification email."));
    }

    public void resendVerificationFromLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                if (!mAuth.getCurrentUser().isEmailVerified()) {
                    sendVerificationEmail(mAuth.getCurrentUser());
                    _authMessage.postValue("Verification email re-sent.");
                } else {
                    _authMessage.postValue("Your email is already verified!");
                    _isAuthenticated.postValue(true);
                }
            } else {
                handleAuthException(task.getException(), "Could not resend verification.");
            }
        });
    }

    public void resendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            sendVerificationEmail(user);
            _authMessage.postValue("Verification email re-sent.");
        } else if (user != null) {
            _authMessage.postValue("Your email is already verified.");
        }
    }

    public void changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            _authError.postValue("User not logged in properly.");
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential).addOnCompleteListener(reauth -> {
            if (reauth.isSuccessful()) {
                user.updatePassword(newPassword).addOnCompleteListener(update -> {
                    if (update.isSuccessful()) {
                        _authMessage.postValue("Password updated successfully!");
                    } else {
                        handleAuthException(update.getException(), "Failed to update password.");
                    }
                });
            } else {
                handleAuthException(reauth.getException(), "Current password is incorrect.");
            }
        });
    }

    public void sendPasswordResetEmail(String email) {
        if (!isEmailValid(email)) {
            _authError.postValue("Invalid email format.");
            return;
        }
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                _passwordResetSent.postValue(true);
            } else {
                handleAuthException(task.getException(), "Could not send password reset link.");
            }
        });
    }

    public void updateUsername(String newUsername, OnCompleteCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || !isUsernameValid(newUsername)) {
            if (callback != null) callback.onError("Invalid username or user not logged in.");
            return;
        }

        String oldUsername = user.getDisplayName();
        if (newUsername.equals(oldUsername)) {
            if (callback != null) callback.onSuccess("Username is unchanged.");
            return;
        }

        db.collection("usernames").document(newUsername).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (callback != null) callback.onError("This username is already taken.");
            } else {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(newUsername).build();
                user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Task<Void> updateUserDoc = db.collection("users").document(user.getUid()).update("username", newUsername);
                        Map<String, Object> usernameMap = new HashMap<>();
                        usernameMap.put("uid", user.getUid());
                        Task<Void> createUsernameDoc = db.collection("usernames").document(newUsername).set(usernameMap);
                        Task<Void> deleteOldUsernameDoc = (oldUsername != null && !oldUsername.isEmpty())
                                ? db.collection("usernames").document(oldUsername).delete()
                                : Tasks.forResult(null);

                        Tasks.whenAll(updateUserDoc, createUsernameDoc, deleteOldUsernameDoc).addOnCompleteListener(firestoreTask -> {
                            if (firestoreTask.isSuccessful()) {
                                if (callback != null) callback.onSuccess("Username updated successfully!");
                            } else {
                                if (callback != null) callback.onError("Failed to update username in database.");
                            }
                        });
                    } else {
                        if (callback != null) callback.onError("Failed to update authentication profile.");
                    }
                });
            }
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError("Network error while checking username.");
        });
    }


    public interface OnCompleteCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    private void updateUserProfile(FirebaseUser user, String accountName, String email) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(accountName).build();
        user.updateProfile(profileUpdates);

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("uid", user.getUid());
        userProfile.put("accountName", accountName);
        userProfile.put("email", email);
        userProfile.put("createdAt", System.currentTimeMillis());
        db.collection("users").document(user.getUid()).set(userProfile);

        Map<String, Object> usernameDoc = new HashMap<>();
        usernameDoc.put("uid", user.getUid());
        db.collection("usernames").document(accountName).set(usernameDoc);

        Map<String, Object> emailDoc = new HashMap<>();
        emailDoc.put("uid", user.getUid());
        db.collection("emails").document(email).set(emailDoc);
    }

    private void handleAuthException(Exception exception, String defaultMessage) {
        String errorMsg = defaultMessage;
        if (exception instanceof FirebaseAuthException) {
            errorMsg = ((FirebaseAuthException) exception).getLocalizedMessage();
        }
        _authError.postValue(errorMsg);
    }

    public void signOut() {
        mAuth.signOut();
    }

    public void clearMessages() {
        _authError.postValue(null);
        _authMessage.postValue(null);
        _passwordResetSent.postValue(false);
        _accountDeleted.postValue(null);
    }
}
