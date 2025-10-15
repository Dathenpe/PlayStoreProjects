package com.f9ld3.Zion.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MediaDownloadManager {

    private static final String TAG = "MediaDownloadManager";
    private final Context context;
    private final DownloadManager downloadManager;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public MediaDownloadManager(Context context) {
        this.context = context;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    /**
     * Download podcast or video to device
     */
    public void downloadMedia(PlayerMedia media) {
        if (media == null || media.getMediaUrl() == null) {
            Toast.makeText(context, "Cannot download: Invalid media", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Please log in to download", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = sanitizeFileName(media.getTitle());
            String fileExtension = media.getType() == PlayerMedia.TYPE_VIDEO ? ".mp4" : ".mp3";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(media.getMediaUrl()));
            request.setTitle(media.getTitle());
            request.setDescription("Downloading " + (media.getType() == PlayerMedia.TYPE_VIDEO ? "video" : "podcast"));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // Set destination
            request.setDestinationInExternalPublicDir(
                    media.getType() == PlayerMedia.TYPE_VIDEO ?
                            Environment.DIRECTORY_MOVIES :
                            Environment.DIRECTORY_MUSIC,
                    "Zion/" + fileName + fileExtension
            );

            // Allow download over mobile and WiFi
            request.setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI |
                            DownloadManager.Request.NETWORK_MOBILE
            );

            // Start download
            long downloadId = downloadManager.enqueue(request);

            // Save download record to Firestore
            saveDownloadRecord(media, downloadId, user.getUid());

            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Download started for: " + media.getTitle() + " (ID: " + downloadId + ")");

        } catch (Exception e) {
            Log.e(TAG, "Download failed", e);
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Save download record to Firestore for tracking
     */
    private void saveDownloadRecord(PlayerMedia media, long downloadId, String userId) {
        Map<String, Object> downloadData = new HashMap<>();
        downloadData.put("mediaId", media.getId());
        downloadData.put("mediaTitle", media.getTitle());
        downloadData.put("mediaType", media.getType());
        downloadData.put("downloadId", downloadId);
        downloadData.put("downloadedAt", System.currentTimeMillis());
        downloadData.put("mediaUrl", media.getMediaUrl());
        downloadData.put("thumbnailUrl", media.getThumbnailUrl());

        db.collection("users")
                .document(userId)
                .collection("downloads")
                .document(String.valueOf(downloadId))
                .set(downloadData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Download record saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save download record", e));
    }

    /**
     * Check if media is already downloaded
     */
    public void isMediaDownloaded(PlayerMedia media, DownloadCheckListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            listener.onResult(false);
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("downloads")
                .whereEqualTo("mediaId", media.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listener.onResult(!querySnapshot.isEmpty());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking download status", e);
                    listener.onResult(false);
                });
    }

    /**
     * Remove download record from Firestore
     */
    public void removeDownloadRecord(String mediaId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("downloads")
                .whereEqualTo("mediaId", mediaId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    querySnapshot.getDocuments().forEach(doc -> doc.getReference().delete());
                    Toast.makeText(context, "Download record removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove download record", e);
                });
    }

    /**
     * Sanitize filename for safe file system storage
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "media_file";
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    /**
     * Callback interface for download check
     */
    public interface DownloadCheckListener {
        void onResult(boolean isDownloaded);
    }
}