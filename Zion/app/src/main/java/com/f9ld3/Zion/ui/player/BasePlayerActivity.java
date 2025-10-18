// main/java/com/f9ld3/Zion/ui/player/BasePlayerActivity.java
package com.f9ld3.Zion.ui.player;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

// Using OptIn for UnstableApi related to Media3 UI components if needed
@OptIn(markerClass = UnstableApi.class)
public abstract class BasePlayerActivity extends AppCompatActivity {

    protected ExoPlayer player;
    protected PlayerMedia currentMedia;
    // Keep references to both potential UI views
    protected PlayerView playerView;
    protected PlayerControlView playerControlView;


    private static final String STATE_PLAYER_POSITION = "player_position";
    private static final String STATE_PLAYER_PLAY_WHEN_READY = "player_play_when_ready";

    private long startPosition = 0;
    private boolean startPlayWhenReady = true;

    /**
     * Abstract method for subclasses to provide the primary player UI view
     * (either a PlayerView or a PlayerControlView).
     */
    protected abstract View getPlayerUiView();
    /**
     * Abstract method for subclasses to extract the PlayerMedia object from the Intent.
     */
    protected abstract PlayerMedia getMediaItemFromIntent();
    /**
     * Abstract method for subclasses to provide a specific log tag.
     */
    protected abstract String getLogTag();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore state if available
        if (savedInstanceState != null) {
            startPosition = savedInstanceState.getLong(STATE_PLAYER_POSITION, 0);
            startPlayWhenReady = savedInstanceState.getBoolean(STATE_PLAYER_PLAY_WHEN_READY, true);
            Log.d(getLogTag(), "Restoring state: position=" + startPosition + ", playWhenReady=" + startPlayWhenReady);
        }

        // Get media item from intent
        currentMedia = getMediaItemFromIntent();
        if (currentMedia == null || currentMedia.getMediaUrl() == null || currentMedia.getMediaUrl().isEmpty()) {
            Log.e(getLogTag(), "Error: Invalid or missing media data in Intent.");
            Toast.makeText(this, "Error: Could not load media", Toast.LENGTH_SHORT).show();
            finish(); // Finish activity if media data is invalid
            return;
        }
        Log.d(getLogTag(), "Media item loaded: " + currentMedia.getTitle());
    }

    /**
     * Initializes the ExoPlayer instance and links it to the appropriate UI view.
     */
    @OptIn(markerClass = UnstableApi.class)
    protected void initializePlayer() {
        if (player != null) {
            Log.d(getLogTag(), "Player already initialized.");
            return; // Avoid re-initialization
        }

        View playerUiView = getPlayerUiView(); // Get the UI view from the subclass
        if (playerUiView == null) {
            Log.e(getLogTag(), "getPlayerUiView() returned null. Cannot initialize player.");
            Toast.makeText(this, "Playback Error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Identify if the provided view is PlayerView or PlayerControlView
        if (playerUiView instanceof PlayerView) {
            playerView = (PlayerView) playerUiView;
            playerControlView = null; // Ensure the other is null
            Log.d(getLogTag(), "UI View identified as PlayerView.");
        } else if (playerUiView instanceof PlayerControlView) {
            playerControlView = (PlayerControlView) playerUiView;
            playerView = null; // Ensure the other is null
            Log.d(getLogTag(), "UI View identified as PlayerControlView.");
        } else {
            // This case should ideally not happen if subclasses return the correct view
            Log.e(getLogTag(), "getPlayerUiView() returned an unexpected view type: " + playerUiView.getClass().getName());
            Toast.makeText(this, "Playback UI Error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            player = new ExoPlayer.Builder(this).build();

            // Link player to the identified view
            if (playerView != null) {
                playerView.setPlayer(player);
                Log.d(getLogTag(), "Linked player to PlayerView.");
            } else { // Must be playerControlView
                playerControlView.setPlayer(player);
                Log.d(getLogTag(), "Linked player to PlayerControlView.");
            }

            player.addListener(playerListener); // Add listener for errors and state changes

            // Prepare the player with the media item
            MediaItem mediaItem = MediaItem.fromUri(currentMedia.getMediaUrl());
            player.setMediaItem(mediaItem);
            player.setPlayWhenReady(startPlayWhenReady);
            player.seekTo(startPosition);
            player.prepare();
            Log.d(getLogTag(), "Player initialized and prepared. playWhenReady=" + startPlayWhenReady + ", startPosition=" + startPosition);

        } catch (Exception e) {
            Log.e(getLogTag(), "Error during player initialization", e);
            Toast.makeText(this, "Error initializing player", Toast.LENGTH_SHORT).show();
            releasePlayer(); // Clean up if initialization failed
            finish();
        }
    }

    /**
     * Releases the ExoPlayer instance and unlinks it from UI views.
     */
    protected void releasePlayer() {
        if (player != null) {
            // Save state before releasing
            startPosition = player.getCurrentPosition();
            startPlayWhenReady = player.getPlayWhenReady();
            Log.d(getLogTag(), "Releasing player. Saving state: position=" + startPosition + ", playWhenReady=" + startPlayWhenReady);

            player.removeListener(playerListener);
            player.release(); // Release ExoPlayer resources
            player = null;

            // Unlink from views
            if (playerView != null) {
                playerView.setPlayer(null);
                playerView = null;
            }
            if (playerControlView != null) {
                playerControlView.setPlayer(null);
                playerControlView = null;
            }
            Log.d(getLogTag(), "Player released and unlinked.");
        } else {
            Log.d(getLogTag(), "Player already null in releasePlayer.");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Use current player state if available, otherwise use last known state
        long positionToSave = (player != null) ? player.getCurrentPosition() : startPosition;
        boolean playWhenReadyToSave = (player != null) ? player.getPlayWhenReady() : startPlayWhenReady;

        Log.d(getLogTag(), "Saving state: position=" + positionToSave + ", playWhenReady=" + playWhenReadyToSave);
        outState.putLong(STATE_PLAYER_POSITION, positionToSave);
        outState.putBoolean(STATE_PLAYER_PLAY_WHEN_READY, playWhenReadyToSave);
    }

    // --- Lifecycle Management ---
    // Using onStart/onStop for player init/release as recommended for API 24+

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(getLogTag(), "onStart");
        // Initialize player if it hasn't been initialized (e.g., first start or after being stopped)
        initializePlayer();
        // Resume PlayerView's rendering if it exists
        if (playerView != null) {
            playerView.onResume();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(getLogTag(), "onResume");
        // No player initialization needed here if done in onStart
        // PlayerView resume is handled in onStart
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(getLogTag(), "onPause");
        // Pause PlayerView's rendering if it exists
        // Player pause itself happens in onStop before release
        if (playerView != null) {
            playerView.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(getLogTag(), "onStop");
        // Release the player when the activity is no longer visible
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(getLogTag(), "onDestroy");
        // Ensure player is released if somehow missed in onStop
        releasePlayer();
    }

    // --- Helper to get reason string ---
    private String getPlaybackSuppressionReasonString(@Player.PlaybackSuppressionReason int reason) {
        switch (reason) {
            case Player.PLAYBACK_SUPPRESSION_REASON_NONE:
                return "None";
            case Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS:
                return "Transient Audio Focus Loss";
            // Add other cases from Player.PlaybackSuppressionReason if needed
            default:
                return "Unknown (" + reason + ")";
        }
    }

    // --- Player Event Listener ---
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Log.e(getLogTag(), "Player Error: " + error.getErrorCodeName() + ", Message: " + error.getMessage(), error);
            Toast.makeText(BasePlayerActivity.this, "Playback Error: " + error.getErrorCodeName(), Toast.LENGTH_LONG).show();
            // Consider more robust error handling: maybe finish(), show dialog, retry?
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            String stateString;
            switch (playbackState) {
                case Player.STATE_IDLE: stateString = "Idle"; break;
                case Player.STATE_BUFFERING: stateString = "Buffering"; break;
                case Player.STATE_READY: stateString = "Ready"; break;
                case Player.STATE_ENDED: stateString = "Ended"; break;
                default: stateString = "UNKNOWN (" + playbackState + ")"; break;
            }
            Log.d(getLogTag(), "Player state changed to: " + stateString);
            // Update UI based on state if needed (e.g., show/hide loading indicator)
        }

        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            // *** FIX: Use the helper method or switch statement here ***
            Log.d(getLogTag(), "PlayWhenReady changed: " + playWhenReady + ", Reason: " + getPlaybackSuppressionReasonString(reason));
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Log.d(getLogTag(), "IsPlaying changed: " + isPlaying);
            // Keep screen on only while playing
            View playerUi = getPlayerUiView(); // Get the root UI view
            if (playerUi != null) {
                playerUi.setKeepScreenOn(isPlaying);
            } else {
                Log.w(getLogTag(), "Could not set keepScreenOn: playerUiView is null");
            }
        }
    };
}