package com.f9ld3.Zion.ui.likes;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.ui.notifications.NotificationViewModel;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LikesViewModel extends ViewModel {

    private static final String TAG = "LikesViewModel";
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<Boolean> _isLiked = new MutableLiveData<>(false);
    public LiveData<Boolean> isLiked() { return _isLiked; }

    private final MutableLiveData<Integer> _likeCount = new MutableLiveData<>(0);
    public LiveData<Integer> getLikeCount() { return _likeCount; }

    private ListenerRegistration likeCountListener;

    public void checkLikeStatus(String mediaId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || mediaId == null) {
            _isLiked.setValue(false);
            return;
        }

        db.collection("media").document(mediaId).collection("likes").document(user.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        _isLiked.setValue(false);
                        return;
                    }
                    _isLiked.setValue(snapshot != null && snapshot.exists());
                });
    }

    public void loadLikeCount(String mediaId) {
        if (likeCountListener != null) likeCountListener.remove();

        likeCountListener = db.collection("media").document(mediaId).collection("likes")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        _likeCount.setValue(0);
                        return;
                    }
                    if (snapshot != null) {
                        _likeCount.setValue(snapshot.size());
                    }
                });
    }

    public void likeMedia(PlayerMedia media) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        String mediaId = media.getId();

        Map<String, Object> likeData = new HashMap<>();
        likeData.put("mediaId", mediaId);
        likeData.put("mediaTitle", media.getTitle());
        likeData.put("mediaType", media.getType());
        likeData.put("thumbnailUrl", media.getThumbnailUrl());
        likeData.put("authorName", media.getAuthorName());
        likeData.put("likedAt", Timestamp.now());

        db.collection("users").document(userId).collection("likes").document(mediaId).set(likeData)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> mediaLikeData = new HashMap<>();
                    mediaLikeData.put("userId", userId);
                    mediaLikeData.put("username", user.getDisplayName());
                    mediaLikeData.put("likedAt", Timestamp.now());

                    db.collection("media").document(mediaId).collection("likes").document(userId).set(mediaLikeData)
                            .addOnSuccessListener(aVoid2 -> {
                                addToLikedPlaylist(media);
                                sendLikeNotification(media.getUploaderUid(), media.getTitle(), user.getDisplayName(), media.getId());
                            });
                });
    }

    public void unlikeMedia(String mediaId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();

        db.collection("users").document(userId).collection("likes").document(mediaId).delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("media").document(mediaId).collection("likes").document(userId).delete()
                            .addOnSuccessListener(aVoid2 -> {
                                removeFromLikedPlaylist(mediaId);
                            });
                });
    }

    @SuppressWarnings("unchecked")
    private void addToLikedPlaylist(PlayerMedia media) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String playlistName = media.getType() == PlayerMedia.TYPE_VIDEO ? "Liked Videos" : "Liked Podcasts";

        db.collection("users").document(user.getUid()).collection("playlists")
                .whereEqualTo("name", playlistName)
                .whereEqualTo("isDefault", true)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        createDefaultLikedPlaylist(playlistName, media);
                    } else {
                        String playlistId = querySnapshot.getDocuments().get(0).getId();
                        List<String> mediaIds = (List<String>) querySnapshot.getDocuments().get(0).get("mediaIds");
                        if (mediaIds == null) mediaIds = new ArrayList<>();
                        if (!mediaIds.contains(media.getId())) {
                            mediaIds.add(0, media.getId()); // Add to top
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("mediaIds", mediaIds);
                            updates.put("itemCount", mediaIds.size());
                            updates.put("updatedAt", Timestamp.now());
                            updates.put("thumbnailUrl", media.getThumbnailUrl()); // Update thumbnail to latest liked item
                            querySnapshot.getDocuments().get(0).getReference().update(updates);
                        }
                    }
                });
    }

    private void createDefaultLikedPlaylist(String playlistName, PlayerMedia firstMedia) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> playlistData = new HashMap<>();
        playlistData.put("name", playlistName);
        playlistData.put("description", "Your automatically generated " + playlistName.toLowerCase());
        playlistData.put("creatorUid", user.getUid());
        playlistData.put("creatorName", user.getDisplayName());
        playlistData.put("isPublic", false);
        playlistData.put("isDefault", true); // Mark as a default playlist

        List<String> mediaIds = new ArrayList<>();
        mediaIds.add(firstMedia.getId());
        playlistData.put("mediaIds", mediaIds);
        playlistData.put("itemCount", 1);
        playlistData.put("thumbnailUrl", firstMedia.getThumbnailUrl());
        playlistData.put("createdAt", Timestamp.now());
        playlistData.put("updatedAt", Timestamp.now());

        db.collection("users").document(user.getUid()).collection("playlists").add(playlistData);
    }

    @SuppressWarnings("unchecked")
    private void removeFromLikedPlaylist(String mediaId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).collection("playlists")
                .whereEqualTo("isDefault", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        List<String> mediaIds = (List<String>) doc.get("mediaIds");
                        if (mediaIds != null && mediaIds.contains(mediaId)) {
                            mediaIds.remove(mediaId);
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("mediaIds", mediaIds);
                            updates.put("itemCount", mediaIds.size());
                            updates.put("updatedAt", Timestamp.now());
                            doc.getReference().update(updates);
                        }
                    }
                });
    }

    private void sendLikeNotification(String uploaderUid, String mediaTitle, String likerName, String mediaId) {
        Map<String, Object> data = new HashMap<>();
        data.put("mediaTitle", mediaTitle);
        data.put("likerName", likerName != null ? likerName : "Someone");
        data.put("mediaId", mediaId);

        NotificationViewModel.sendNotification(
                uploaderUid,
                "like",
                "New Like",
                (likerName != null ? likerName : "Someone") + " liked your content: " + mediaTitle,
                data
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (likeCountListener != null) {
            likeCountListener.remove();
        }
    }
}