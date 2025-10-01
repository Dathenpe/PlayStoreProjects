package com.f9ld3.Zion.ui.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class PlayerViewModel extends ViewModel {

    private static final String TAG = "PlayerViewModel";

    // LiveData for the single, interleaved feed
    private final MutableLiveData<List<PlayerMedia>> mMediaFeed = new MutableLiveData<>();
    public LiveData<List<PlayerMedia>> getMediaFeed() { return mMediaFeed; }

    // Firestore members
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration mediaFeedListener;

    public PlayerViewModel() {
        fetchMediaFeed();
    }

    private void fetchMediaFeed() {
        // Fetch a large number of items to ensure enough videos and podcasts for interleaving
        // A limit of 100 should be sufficient for 30 podcasts (15 duos) + 45 videos.
        Query query = db.collection("media")
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(100);

        mediaFeedListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(TAG, "Listen failed for media feed. (Ensure Firestore database is created)", error);
                mMediaFeed.setValue(new ArrayList<>());
                return;
            }

            if (value != null && !value.isEmpty()) {
                List<PlayerMedia> allMedia = value.toObjects(PlayerMedia.class);

                // 1. Separate the media into two distinct lists
                List<PlayerMedia> videos = new ArrayList<>();
                List<PlayerMedia> singlePodcasts = new ArrayList<>();

                for (PlayerMedia media : allMedia) {
                    if (media.getType() == PlayerMedia.TYPE_VIDEO) {
                        videos.add(media);
                    } else if (media.getType() == PlayerMedia.TYPE_PODCAST_SINGLE) {
                        singlePodcasts.add(media);
                    }
                    // Ignore TYPE_PODCAST_DUO_CONTAINER items if they exist in the DB,
                    // as we are creating the containers here.
                }

                // 2. Pair up single podcasts into Duo Containers (side-by-side items)
                List<PlayerMedia> podcastDuos = new ArrayList<>();
                for (int i = 0; i < singlePodcasts.size(); i += 2) {
                    PlayerMedia podcastOne = singlePodcasts.get(i);
                    PlayerMedia podcastTwo = null;

                    // Ensure we don't go out of bounds when pairing
                    if (i + 1 < singlePodcasts.size()) {
                        podcastTwo = singlePodcasts.get(i + 1);
                    }

                    // Create the Duo Container item
                    PlayerMedia duoContainer = new PlayerMedia(podcastOne, podcastTwo);
                    podcastDuos.add(duoContainer);

                    // The last odd podcast is effectively ignored to maintain the Duo format.
                }

                // 3. 🔥 CRITICAL LOGIC: Interleave Videos and Podcast Duos (3 Videos, 1 Podcast Duo)
                List<PlayerMedia> interleavedFeed = new ArrayList<>();
                int videoIndex = 0;
                int podcastIndex = 0;

                // Continue until both the video and podcast lists are fully consumed
                while (videoIndex < videos.size() || podcastIndex < podcastDuos.size()) {
                    // Add up to 3 videos
                    for (int i = 0; i < 3 && videoIndex < videos.size(); i++) {
                        interleavedFeed.add(videos.get(videoIndex++));
                    }

                    // Add 1 podcast duo (if available)
                    if (podcastIndex < podcastDuos.size()) {
                        interleavedFeed.add(podcastDuos.get(podcastIndex++));
                    }
                }

                mMediaFeed.setValue(interleavedFeed);
                Log.d(TAG, "Updated interleaved feed: " + interleavedFeed.size() + " total items (" + podcastDuos.size() + " duos, " + videos.size() + " videos).");

            } else {
                Log.d(TAG, "Current media feed data is empty or null.");
                mMediaFeed.setValue(new ArrayList<>());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mediaFeedListener != null) {
            mediaFeedListener.remove();
            Log.d(TAG, "Firestore listener removed.");
        }
    }
}