package com.f9ld3.Zion.ui.notifications;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch; // Import WriteBatch

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NotificationViewModel extends ViewModel {

    private static final String TAG = "NotificationViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<Integer> _unreadCount = new MutableLiveData<>(0);
    public LiveData<Integer> getUnreadCount() { return _unreadCount; }

    private final MutableLiveData<List<Notification>> _notifications = new MutableLiveData<>();
    public LiveData<List<Notification>> getNotifications() { return _notifications; }

    private ListenerRegistration unreadCountListener;
    private ListenerRegistration notificationListListener;

    public NotificationViewModel() {
        mAuth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                loadNotifications(user.getUid());
            } else {
                clearListenersAndData();
            }
        });
    }

    public void loadNotifications(String userId) {
        if (userId == null) {
            clearListenersAndData();
            return;
        }

        // --- UNREAD COUNT LISTENER ---
        if (unreadCountListener != null) unreadCountListener.remove();
        unreadCountListener = db.collection("notifications")
                .whereEqualTo("targetUserId", userId)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Unread count listen failed.", e);
                        _unreadCount.setValue(0);
                        return;
                    }
                    _unreadCount.setValue(snapshots != null ? snapshots.size() : 0);
                    Log.d(TAG, "Unread notification count: " + (snapshots != null ? snapshots.size() : 0));
                });

        // --- NOTIFICATION LIST LISTENER ---
        if (notificationListListener != null) notificationListListener.remove();
        notificationListListener = db.collection("notifications")
                .whereEqualTo("targetUserId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50) // Load recent 50
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Notification list listen failed.", e);
                        _notifications.setValue(new ArrayList<>());
                        return;
                    }
                    List<Notification> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Notification notification = doc.toObject(Notification.class);
                                notification.setId(doc.getId()); // Set document ID
                                list.add(notification);
                            } catch (Exception parseError) {
                                Log.e(TAG, "Error parsing notification: " + doc.getId(), parseError);
                            }
                        }
                    }
                    _notifications.setValue(list);
                    Log.d(TAG, "Loaded " + list.size() + " notifications.");
                });
    }

    public void markNotificationAsRead(Notification notification) {
        if (notification == null || notification.getId() == null || notification.isRead()) {
            return; // No need to update
        }

        Log.d(TAG, "Marking notification as read: " + notification.getId());
        db.collection("notifications").document(notification.getId())
                .update("read", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as read: " + notification.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error marking notification as read: " + notification.getId(), e));
    }

    // --- NEW METHOD: Mark All As Read ---
    public void markAllAsRead() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "markAllAsRead failed: user is null");
            return;
        }
        String userId = user.getUid();
        Log.d(TAG, "Marking all notifications as read for user: " + userId);

        // 1. Find all unread notifications for this user (limit to 500 to be safe, as batches are limited)
        db.collection("notifications")
                .whereEqualTo("targetUserId", userId)
                .whereEqualTo("read", false)
                .limit(500)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.d(TAG, "No unread notifications to mark.");
                        return;
                    }

                    // 2. Create a batch write
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        batch.update(doc.getReference(), "read", true);
                    }

                    // 3. Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Batch mark all as read successful. Updated " + querySnapshot.size() + " items."))
                            .addOnFailureListener(e -> Log.e(TAG, "Batch mark all as read failed", e));

                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get unread notifications for batch update", e));
    }
    // --- END NEW METHOD ---


    public static void sendNotification(String targetUserId, String type, String title, String message, Map<String, Object> data) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "Cannot send notification, user is not logged in.");
            return;
        }
        String currentUserId = currentUser.getUid();

        if (targetUserId == null || targetUserId.equals(currentUserId)) {
            Log.d(TAG, "Skipping self-notification or null targetUserId.");
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("targetUserId", targetUserId);
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", Timestamp.now());
        notification.put("read", false);

        // --- FIX: Put the data map *inside* the 'data' field ---
        // if (data != null) {
        //    notification.putAll(data); // <-- This was the problem
        // }
        if (data != null) {
            notification.put("data", data); // <-- This is the correct way
        }
        // --- END FIX ---

        firestore.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Notification sent successfully with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error sending notification", e));
    }

    private void clearListenersAndData() {
        if (unreadCountListener != null) {
            unreadCountListener.remove();
            unreadCountListener = null;
        }
        if (notificationListListener != null) {
            notificationListListener.remove();
            notificationListListener = null;
        }
        _unreadCount.setValue(0);
        _notifications.setValue(new ArrayList<>());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        clearListenersAndData();
        Log.d(TAG, "NotificationViewModel cleared.");
    }
}