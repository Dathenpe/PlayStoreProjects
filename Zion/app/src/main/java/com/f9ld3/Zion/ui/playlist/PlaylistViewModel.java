package com.f9ld3.Zion.ui.playlist;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.f9ld3.Zion.data.Playlist;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistViewModel extends ViewModel {

    private static final String TAG = "PlaylistViewModel";
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<List<Playlist>> _userPlaylists = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Playlist>> getUserPlaylists() { return _userPlaylists; }

    private final MutableLiveData<List<PlayerMedia>> _playlistMedia = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<PlayerMedia>> getPlaylistMedia() { return _playlistMedia; }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    public LiveData<String> getSuccessMessage() { return _successMessage; }

    private ListenerRegistration playlistListener;

    /**
     * Load user's playlists
     */
    public void loadUserPlaylists() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            _userPlaylists.setValue(new ArrayList<>());
            return;
        }

        if (playlistListener != null) {
            playlistListener.remove();
        }

        playlistListener = db.collection("users")
                .document(user.getUid())
                .collection("playlists")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading playlists", error);
                        _errorMessage.setValue("Failed to load playlists");
                        return;
                    }

                    List<Playlist> playlists = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Playlist playlist = doc.toObject(Playlist.class);
                            if (playlist != null) {
                                playlist.setId(doc.getId());
                                playlists.add(playlist);
                            }
                        }
                    }

                    _userPlaylists.setValue(playlists);
                    Log.d(TAG, "Loaded " + playlists.size() + " playlists");
                });
    }

    /**
     * Create new playlist
     */
    public void createPlaylist(String name, String description, boolean isPublic) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            _errorMessage.setValue("You must be logged in to create a playlist");
            return;
        }

        String playlistId = db.collection("users")
                .document(user.getUid())
                .collection("playlists")
                .document().getId();

        Playlist playlist = new Playlist(
                playlistId,
                name,
                description,
                user.getUid(),
                user.getDisplayName() != null ? user.getDisplayName() : "Anonymous",
                isPublic
        );

        db.collection("users")
                .document(user.getUid())
                .collection("playlists")
                .document(playlistId)
                .set(playlist)
                .addOnSuccessListener(aVoid -> {
                    _successMessage.setValue("Playlist created successfully");
                    Log.d(TAG, "Playlist created: " + playlistId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create playlist", e);
                    _errorMessage.setValue("Failed to create playlist: " + e.getMessage());
                });
    }

    /**
     * Add media to playlist
     */
    public void addMediaToPlaylist(String playlistId, PlayerMedia media) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            _errorMessage.setValue("You must be logged in");
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("playlists")
                .document(playlistId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Playlist playlist = documentSnapshot.toObject(Playlist.class);
                    if (playlist != null) {
                        if (playlist.containsMedia(media.getId())) {
                            _errorMessage.setValue("Media already in playlist");
                            return;
                        }

                        playlist.addMedia(media.getId());

                        // Update thumbnail if first item
                        if (playlist.getItemCount() == 1 && media.getThumbnailUrl() != null) {
                            playlist.setThumbnailUrl(media.getThumbnailUrl());
                        }

                        // Update Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("mediaIds", playlist.getMediaIds());
                        updates.put("itemCount", playlist.getItemCount());
                        updates.put("updatedAt", Timestamp.now());
                        if (playlist.getThumbnailUrl() != null) {
                            updates.put("thumbnailUrl", playlist.getThumbnailUrl());
                        }

                        documentSnapshot.getReference().update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    _successMessage.setValue("Added to playlist");
                                    Log.d(TAG, "Media added to playlist");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to add media to playlist", e);
                                    _errorMessage.setValue("Failed to add to playlist");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get playlist", e);
                    _errorMessage.setValue("Failed to access playlist");
                });
    }

    /**
     * Remove media from playlist
     */
    public void removeMediaFromPlaylist(String playlistId, String mediaId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("playlists")
                .document(playlistId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Playlist playlist = documentSnapshot.toObject(Playlist.class);
                    if (playlist != null) {
                        playlist.removeMedia(mediaId);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("mediaIds", playlist.getMediaIds());
                        updates.put("itemCount", playlist.getItemCount());
                        updates.put("updatedAt", Timestamp.now());

                        documentSnapshot.getReference().update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    _successMessage.setValue("Removed from playlist");
                                    loadPlaylistMedia(playlistId); // Refresh
                                })
                                .addOnFailureListener(e -> {
                                    _errorMessage.setValue("Failed to remove from playlist");
                                });
                    }
                });
    }

    /**
     * Load media items in a playlist
     */
    public void loadPlaylistMedia(String playlistId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("playlists")
                .document(playlistId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Playlist playlist = documentSnapshot.toObject(Playlist.class);
                    if (playlist != null && playlist.getMediaIds() != null && !playlist.getMediaIds().isEmpty()) {
                        loadMediaByIds(playlist.getMediaIds());
                    } else {
                        _playlistMedia.setValue(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load playlist", e);
                    _playlistMedia.setValue(new ArrayList<>());
                });
    }

    /**
     * Load media items by their IDs
     */
    private void loadMediaByIds(List<String> mediaIds) {
        if (mediaIds.isEmpty()) {
            _playlistMedia.setValue(new ArrayList<>());
            return;
        }

        // Firestore whereIn limit is 10, so batch if needed
        List<String> batchIds = mediaIds.size() > 10 ? mediaIds.subList(0, 10) : mediaIds;

        db.collection("media")
                .whereIn("id", batchIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<PlayerMedia> mediaList = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        PlayerMedia media = doc.toObject(PlayerMedia.class);
                        if (media != null) {
                            mediaList.add(media);
                        }
                    }
                    _playlistMedia.setValue(mediaList);
                    Log.d(TAG, "Loaded " + mediaList.size() + " media items");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load playlist media", e);
                    _playlistMedia.setValue(new ArrayList<>());
                });
    }

    /**
     * Update playlist details
     */
    public void updatePlaylist(String playlistId, String name, String description, boolean isPublic) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("description", description);
        updates.put("isPublic", isPublic);
        updates.put("updatedAt", Timestamp.now());

        db.collection("users")
                .document(user.getUid())
                .collection("playlists")
                .document(playlistId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    _successMessage.setValue("Playlist updated");
                })
                .addOnFailureListener(e -> {
                    _errorMessage.setValue("Failed to update playlist");
                });
    }

    /**
     * Delete playlist
     */
    public void deletePlaylist(String playlistId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("playlists")
                .document(playlistId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    _successMessage.setValue("Playlist deleted");
                })
                .addOnFailureListener(e -> {
                    _errorMessage.setValue("Failed to delete playlist");
                });
    }

    public void clearMessages() {
        _errorMessage.setValue(null);
        _successMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (playlistListener != null) {
            playlistListener.remove();
        }
    }
}