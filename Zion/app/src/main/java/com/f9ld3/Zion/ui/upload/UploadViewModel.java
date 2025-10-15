package com.f9ld3.Zion.ui.upload;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class UploadViewModel extends ViewModel {

    private static final String TAG = "UploadViewModel";

    public enum UploadStatus { IDLE, UPLOADING, SUCCESS, FAILED }

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private final MutableLiveData<UploadStatus> _uploadStatus = new MutableLiveData<>(UploadStatus.IDLE);
    public LiveData<UploadStatus> getUploadStatus() { return _uploadStatus; }

    private final MutableLiveData<Integer> _uploadProgress = new MutableLiveData<>(0);
    public LiveData<Integer> getUploadProgress() { return _uploadProgress; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    /**
     * Upload podcast with audio file and thumbnail
     */
    public void uploadPodcast(String title, String description, Uri audioUri, Uri thumbnailUri, long duration, Context context) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            _errorMessage.setValue("You must be logged in to upload.");
            _uploadStatus.setValue(UploadStatus.FAILED);
            return;
        }

        _uploadStatus.setValue(UploadStatus.UPLOADING);
        _uploadProgress.setValue(0);

        String userId = user.getUid();
        String podcastId = db.collection("media").document().getId();

        uploadThumbnail(thumbnailUri, userId, podcastId, thumbnailUrl -> {
            uploadAudioFile(audioUri, userId, podcastId, audioUrl -> {
                savePodcastMetadata(podcastId, title, description, audioUrl, thumbnailUrl, userId, duration);
            });
        });
    }

    /**
     * Upload video with video file and thumbnail
     */
    public void uploadVideo(String title, String description, Uri videoUri, Uri thumbnailUri, long duration, Context context) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            _errorMessage.setValue("You must be logged in to upload.");
            _uploadStatus.setValue(UploadStatus.FAILED);
            return;
        }

        _uploadStatus.setValue(UploadStatus.UPLOADING);
        _uploadProgress.setValue(0);

        String userId = user.getUid();
        String videoId = db.collection("media").document().getId();

        uploadVideoThumbnail(thumbnailUri, userId, videoId, thumbnailUrl -> {
            uploadVideoFile(videoUri, userId, videoId, videoUrl -> {
                saveVideoMetadata(videoId, title, description, videoUrl, thumbnailUrl, userId, duration);
            });
        });
    }

    /**
     * Upload podcast thumbnail
     */
    private void uploadThumbnail(Uri thumbnailUri, String userId, String podcastId, OnUploadCompleteListener listener) {
        // CORRECTED PATH
        StorageReference thumbnailRef = storage.getReference()
                .child("podcasts/" + userId + "/" + podcastId + "/thumbnail.jpg");

        thumbnailRef.putFile(thumbnailUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()) * 0.30;
                    _uploadProgress.postValue((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> thumbnailRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Thumbnail uploaded: " + uri.toString());
                    listener.onUploadComplete(uri.toString());
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get thumbnail URL", e);
                    _errorMessage.postValue("Failed to get thumbnail URL");
                    _uploadStatus.postValue(UploadStatus.FAILED);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Thumbnail upload failed", e);
                    _errorMessage.postValue("Thumbnail upload failed: " + e.getMessage());
                    _uploadStatus.postValue(UploadStatus.FAILED);
                });
    }

    /**
     * Upload podcast audio file
     */
    private void uploadAudioFile(Uri audioUri, String userId, String podcastId, OnUploadCompleteListener listener) {
        // CORRECTED PATH
        StorageReference audioRef = storage.getReference()
                .child("podcasts/" + userId + "/" + podcastId + "/audio.mp3");

        audioRef.putFile(audioUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = 30 + (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()) * 0.60;
                    _uploadProgress.postValue((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Audio file uploaded: " + uri.toString());
                    listener.onUploadComplete(uri.toString());
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get audio URL", e);
                    _errorMessage.postValue("Failed to get audio URL");
                    _uploadStatus.postValue(UploadStatus.FAILED);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Audio upload failed", e);
                    _errorMessage.postValue("Audio upload failed: " + e.getMessage());
                    _uploadStatus.postValue(UploadStatus.FAILED);
                });
    }

    /**
     * Upload video thumbnail
     */
    private void uploadVideoThumbnail(Uri thumbnailUri, String userId, String videoId, OnUploadCompleteListener listener) {
        // CORRECTED PATH
        StorageReference thumbnailRef = storage.getReference()
                .child("videos/" + userId + "/" + videoId + "/thumbnail.jpg");

        thumbnailRef.putFile(thumbnailUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()) * 0.20; // 20% for thumb
                    _uploadProgress.postValue((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> thumbnailRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Video thumbnail uploaded: " + uri.toString());
                    listener.onUploadComplete(uri.toString());
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get video thumbnail URL", e);
                    _errorMessage.postValue("Failed to get thumbnail URL");
                    _uploadStatus.postValue(UploadStatus.FAILED);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Video thumbnail upload failed", e);
                    _errorMessage.postValue("Thumbnail upload failed: " + e.getMessage());
                    _uploadStatus.postValue(UploadStatus.FAILED);
                });
    }

    /**
     * Upload video file
     */
    private void uploadVideoFile(Uri videoUri, String userId, String videoId, OnUploadCompleteListener listener) {
        // CORRECTED PATH
        StorageReference videoRef = storage.getReference()
                .child("videos/" + userId + "/" + videoId + "/video.mp4");

        videoRef.putFile(videoUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = 20 + (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()) * 0.70; // 70% for video
                    _uploadProgress.postValue((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> videoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Video file uploaded: " + uri.toString());
                    listener.onUploadComplete(uri.toString());
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get video URL", e);
                    _errorMessage.postValue("Failed to get video URL");
                    _uploadStatus.postValue(UploadStatus.FAILED);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Video upload failed", e);
                    _errorMessage.postValue("Video upload failed: " + e.getMessage());
                    _uploadStatus.postValue(UploadStatus.FAILED);
                });
    }

    private void saveMetadata(String mediaId, String title, String description, String mediaUrl, String thumbnailUrl, String userId, int mediaType, long duration) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        _uploadProgress.postValue(95);

        PlayerMedia mediaData = new PlayerMedia(
                mediaId,
                mediaType,
                title,
                description,
                mediaUrl,
                thumbnailUrl,
                user.getDisplayName() != null ? user.getDisplayName() : "Anonymous",
                duration,
                userId,
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null
        );

        db.collection("media").document(mediaId)
                .set(mediaData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Media metadata saved");
                    _uploadProgress.postValue(100);
                    _uploadStatus.postValue(UploadStatus.SUCCESS);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save media metadata", e);
                    _errorMessage.postValue("Failed to save media data: " + e.getMessage());
                    _uploadStatus.postValue(UploadStatus.FAILED);
                });
    }

    private void savePodcastMetadata(String podcastId, String title, String description, String audioUrl, String thumbnailUrl, String userId, long duration) {
        saveMetadata(podcastId, title, description, audioUrl, thumbnailUrl, userId, PlayerMedia.TYPE_PODCAST_SINGLE, duration);
    }

    private void saveVideoMetadata(String videoId, String title, String description, String videoUrl, String thumbnailUrl, String userId, long duration) {
        saveMetadata(videoId, title, description, videoUrl, thumbnailUrl, userId, PlayerMedia.TYPE_VIDEO, duration);
    }


    public void clearError() {
        _errorMessage.setValue(null);
    }

    public void resetStatus() {
        _uploadStatus.setValue(UploadStatus.IDLE);
    }

    private interface OnUploadCompleteListener {
        void onUploadComplete(String url);
    }
}