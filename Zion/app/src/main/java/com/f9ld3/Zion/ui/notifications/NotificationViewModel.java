package com.f9ld3.Zion.ui.notifications;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NotificationViewModel extends ViewModel {

    private static final String TAG = "NotificationViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<Integer> _unreadCount = new MutableLiveData<>(0);
    public LiveData<Integer> getUnreadCount() { return _unreadCount; }

    // Placeholder for a list of notifications if you build a notification screen
    // private final MutableLiveData<List<Notification>> _notifications = new MutableLiveData<>();
    // public LiveData<List<Notification>> getNotifications() { return _notifications; }

    public NotificationViewModel() {
        loadNotifications();
    }

    public void loadNotifications() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            _unreadCount.setValue(0);
            return;
        }

        db.collection("notifications")
                .whereEqualTo("targetUserId", currentUser.getUid())
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        _unreadCount.setValue(0);
                        return;
                    }

                    if (snapshots != null) {
                        _unreadCount.setValue(snapshots.size());
                        Log.d(TAG, "Unread notification count: " + snapshots.size());
                    } else {
                        _unreadCount.setValue(0);
                    }
                });
    }

    /**
     * A static helper method to send notifications from any part of the app.
     * This centralizes the creation of notification documents in Firestore.
     *
     * @param targetUserId The UID of the user who should receive the notification.
     * @param type         The type of notification (e.g., "follow", "like", "comment").
     * @param title        The title of the notification.
     * @param message      The main message body of the notification.
     * @param data         A map of additional data related to the notification (e.g., mediaId, followerName).
     */
    public static void sendNotification(String targetUserId, String type, String title, String message, Map<String, Object> data) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Prevent users from sending notifications to themselves
        if (targetUserId.equals(currentUserId)) {
            Log.d(TAG, "Skipping self-notification.");
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("targetUserId", targetUserId);
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", Timestamp.now());
        notification.put("read", false);

        // Add any extra data provided
        if (data != null) {
            notification.putAll(data);
        }

        firestore.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Notification sent successfully with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error sending notification", e));
    }
}
