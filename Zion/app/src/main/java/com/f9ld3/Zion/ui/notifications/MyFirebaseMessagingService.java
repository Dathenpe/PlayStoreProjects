package com.f9ld3.Zion.ui.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.f9ld3.Zion.MainActivity;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.ui.feed.PostDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging; // <-- ADDED IMPORT
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = null;
        String body = null;

        // 1. Check for Notification payload (most common place for title/body)
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Message has Notification payload: Title=" + title + ", Body=" + body);
        }

        // 2. Check for Data payload
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            Log.d(TAG, "Message has Data payload: " + data);
            // 3. Override title/body IF they are also in the data payload
            //    (This is common for data-only messages)
            if (data.containsKey("title")) {
                title = data.get("title");
            }
            if (data.containsKey("body")) {
                body = data.get("body");
            }
        }

        // 4. Set defaults if title/body are still null
        if (title == null) {
            title = getString(R.string.app_name);
        }
        if (body == null) {
            body = "You have a new notification.";
        }

        // 5. Always build the PendingIntent from the data payload for deep-linking
        PendingIntent pendingIntent = createPendingIntentFromData(data);

        // 6. Send the notification
        sendNotification(title, body, pendingIntent);
    }

    /**
     * Creates a deep-link PendingIntent based on the notification's data payload.
     */
    private PendingIntent createPendingIntentFromData(Map<String, String> data) {
        Intent intent;
        String type = data.get("type");
        Log.d(TAG, "Creating PendingIntent for type: " + type);

        // Create a deep-link intent based on the notification type
        if ("post_like".equals(type) || "post_comment".equals(type) || "comment_like".equals(type) || "comment_reply".equals(type)) {
            String postId = data.get("postId");
            if (postId != null) {
                intent = new Intent(this, PostDetailActivity.class);
                intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);

                // Add logic to highlight the specific comment if ID is provided
                String commentId = data.get("commentId");
                if (commentId != null && ("post_comment".equals(type) || "comment_reply".equals(type) || "comment_like".equals(type))) {
                    intent.putExtra(PostDetailActivity.EXTRA_HIGHLIGHT_COMMENT_ID, commentId);
                    Log.d(TAG, "Adding highlight commentId: " + commentId);
                }
            } else {
                // Fallback to MainActivity if postId is missing
                intent = new Intent(this, MainActivity.class);
            }
        } else if ("follow".equals(type)) {
            String followerId = data.get("followerId");
            if (followerId != null) {
                intent = new Intent(this, com.f9ld3.Zion.ui.channel.ChannelActivity.class);
                intent.putExtra(com.f9ld3.Zion.ui.channel.ChannelActivity.EXTRA_CHANNEL_ID, followerId);
                intent.putExtra(com.f9ld3.Zion.ui.channel.ChannelActivity.EXTRA_CHANNEL_NAME, data.get("followerName"));
            } else {
                // Fallback to MainActivity
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            // Default: open MainActivity
            intent = new Intent(this, MainActivity.class);
        }

        // Add flags to open the app
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create a unique request code for each notification
        int requestCode = (int) System.currentTimeMillis();

        return PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Creates and shows a simple notification using the received text.
     * This method is now called by onMessageReceived with the correct title, body, and intent.
     */
    private void sendNotification(String title, String messageBody, @NonNull PendingIntent pendingIntent) {
        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.logo) // Use your logo drawable
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH); // Make it a heads-up notification

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since Android 8.0 (API level 26) and above, notification channels are required.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_HIGH); // Set Importance to HIGH
            notificationManager.createNotificationChannel(channel);
        }

        // Use a unique ID for each notification to show multiple
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // This is the line you must implement:
        sendRegistrationToServer(token);
    }

    // --- NEW STATIC METHOD ---
    /**
     * Proactively fetches the current FCM token and saves it to Firestore.
     * Call this after a user logs in.
     */
    public static void updateFCMToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String token = task.getResult();
                    Log.d(TAG, "Proactive token fetch successful: " + token);
                    sendRegistrationToServer(token); // Call your existing method
                } else {
                    Log.w(TAG, "Fetching FCM token failed", task.getException());
                }
            });
        }
    }

    // --- MADE STATIC ---
    private static void sendRegistrationToServer(String token) {
        // Get the current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Create a map for the token
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("token", token);
            tokenData.put("timestamp", com.google.firebase.Timestamp.now());

            // Save the token to a new subcollection for that user
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("fcmTokens") // This is the collection the extension will look for
                    .document(token) // Use the token itself as the document ID to prevent duplicates
                    .set(tokenData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token saved to Firestore for user: " + userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
        } else {
            Log.w(TAG, "Cannot send token to server, user is not logged in.");
        }
    }
}