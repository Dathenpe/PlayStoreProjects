// main/java/com/f9ld3/Zion/ui/feed/FullScreenMediaActivity.java
package com.f9ld3.Zion.ui.feed;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
// --- ExoPlayer Imports ---
import androidx.media3.common.MediaItem; // <-- Use androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
// --- End ExoPlayer Imports ---

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityFullScreenMediaBinding;
import java.util.ArrayList;

// --- Implement the new interface ---
public class FullScreenMediaActivity extends AppCompatActivity implements FullScreenMediaAdapter.OnPlayClickListener {

    public static final String EXTRA_MEDIA_ITEMS = "extra_media_items";
    public static final String EXTRA_START_POSITION = "extra_start_position";
    private static final String TAG = "FullScreenMediaActivity"; // <-- Add TAG

    private ActivityFullScreenMediaBinding binding;
    private ArrayList<com.f9ld3.Zion.ui.feed.MediaItem> mediaItems; // Use correct MediaItem type
    private int startPosition = 0;

    // --- Player Members ---
    private ExoPlayer player;
    private PlayerView playerView;
    private boolean isPlayerPlaying = false;
    private long playerStartPosition = 0;
    private boolean playerPlayWhenReady = true;
    private static final String STATE_PLAYER_POSITION = "player_position_fs";
    private static final String STATE_PLAYER_PLAY_WHEN_READY = "player_play_when_ready_fs";
    private OnBackPressedCallback onBackPressedCallback;
    // --- End Player Members ---


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullScreenMediaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- Restore Player State ---
        if (savedInstanceState != null) {
            playerStartPosition = savedInstanceState.getLong(STATE_PLAYER_POSITION, 0);
            playerPlayWhenReady = savedInstanceState.getBoolean(STATE_PLAYER_PLAY_WHEN_READY, true);
        }
        // --- End Restore Player State ---

        makeActivityFullscreen(); // Refactored fullscreen setup

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide title
        }

        // --- Initialize PlayerView ---
        playerView = binding.playerViewFullscreen; // Get reference from binding
        // --- End Initialize PlayerView ---

        // Retrieve data from Intent
        mediaItems = (ArrayList<com.f9ld3.Zion.ui.feed.MediaItem>) getIntent().getSerializableExtra(EXTRA_MEDIA_ITEMS);
        startPosition = getIntent().getIntExtra(EXTRA_START_POSITION, 0);

        if (mediaItems == null || mediaItems.isEmpty()) {
            finish(); // Close if no media items
            return;
        }

        setupViewPager();
        setupBackPressHandling(); // Setup modern back press handling
    }

    private void makeActivityFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
    }


    private void setupViewPager() {
        // --- Pass 'this' as the playClickListener ---
        FullScreenMediaAdapter adapter = new FullScreenMediaAdapter(this, mediaItems, this);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setCurrentItem(startPosition, false); // Go to the clicked item without smooth scroll
    }

    // --- Implementation of OnPlayClickListener ---
    @Override
    public void onPlayVideo(com.f9ld3.Zion.ui.feed.MediaItem mediaItem) {
        Log.d(TAG, "onPlayVideo triggered for URL: " + mediaItem.getUrl());
        if (mediaItem.getUrl() != null && !mediaItem.getUrl().isEmpty()) {
            initializePlayer(); // Initialize if not already done
            if (player != null) {
                // Prepare player with the new media item
                androidx.media3.common.MediaItem exoMediaItem = androidx.media3.common.MediaItem.fromUri(mediaItem.getUrl());
                player.setMediaItem(exoMediaItem);
                player.setPlayWhenReady(true); // Start playing immediately
                player.prepare();

                // Switch UI visibility
                binding.viewPager.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);
                binding.toolbar.setVisibility(View.GONE); // Optionally hide toolbar during playback
                isPlayerPlaying = true;
                onBackPressedCallback.setEnabled(true); // Enable custom back press handling
            } else {
                Log.e(TAG, "Player is null, cannot play video.");
                Toast.makeText(this, "Error starting playback", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "Cannot play video, URL is null or empty.");
            Toast.makeText(this, "Invalid video source", Toast.LENGTH_SHORT).show();
        }
    }
    // --- End Implementation ---

    // --- Player Initialization (moved from BasePlayerActivity concept) ---
    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            player.addListener(playerListener); // Add listener for state changes/errors
            Log.d(TAG, "ExoPlayer initialized.");
        }
    }
    // --- End Player Initialization ---

    // --- Player Release (moved from BasePlayerActivity concept) ---
    private void releasePlayer() {
        if (player != null) {
            // Save state before releasing
            playerStartPosition = player.getCurrentPosition();
            playerPlayWhenReady = player.getPlayWhenReady();
            Log.d(TAG, "Releasing player. Saving state: position=" + playerStartPosition + ", playWhenReady=" + playerPlayWhenReady);

            player.removeListener(playerListener);
            player.release();
            player = null;
            playerView.setPlayer(null);
            isPlayerPlaying = false; // Reset flag
            if(onBackPressedCallback != null) {
                onBackPressedCallback.setEnabled(false); // Disable custom back press handling
            }
            Log.d(TAG, "ExoPlayer released.");
        }
    }
    // --- End Player Release ---

    // --- Player Listener ---
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                Log.d(TAG, "Playback ended.");
                stopPlaybackAndShowPager(); // Show pager again when video ends
            }
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Log.e(TAG, "Player Error: " + error.getMessage(), error);
            Toast.makeText(FullScreenMediaActivity.this, "Playback Error", Toast.LENGTH_SHORT).show();
            stopPlaybackAndShowPager(); // Show pager on error
        }
    };
    // --- End Player Listener ---

    // --- Modern Back Press Handling ---
    private void setupBackPressHandling() {
        onBackPressedCallback = new OnBackPressedCallback(false /* disabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // This is triggered only when isPlayerPlaying is true
                stopPlaybackAndShowPager();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private void stopPlaybackAndShowPager() {
        if (player != null) {
            player.stop();
            // We don't release the player here to allow for state saving and potential resume.
            // It will be released in onStop().
        }
        binding.viewPager.setVisibility(View.VISIBLE);
        playerView.setVisibility(View.GONE);
        binding.toolbar.setVisibility(View.VISIBLE); // Show toolbar again
        isPlayerPlaying = false;
        onBackPressedCallback.setEnabled(false); // Disable this callback, fall back to default behavior
        // Re-enable fullscreen in case player controls brought back system bars
        makeActivityFullscreen();
    }
    // --- End Back Press Handling ---

    // --- Lifecycle Management for Player ---
    @Override
    protected void onStart() {
        super.onStart();
        // Initialize player only if resuming playback state requires it
        // Or if you want playback to start immediately on entering the screen
        // For now, initialization happens on play click.
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // If player was playing before pause, resume it
        if (isPlayerPlaying && player != null) {
            // Re-hide system bars if they reappeared
            makeActivityFullscreen();
            player.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        // Pause player if it's playing
        if (isPlayerPlaying && player != null) {
            player.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // Release player when activity is not visible
        releasePlayer();
    }
    // --- End Lifecycle Management ---

    // --- Save Player State ---
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        long positionToSave = (player != null) ? player.getCurrentPosition() : playerStartPosition;
        boolean playWhenReadyToSave = (player != null) ? player.getPlayWhenReady() : playerPlayWhenReady;
        outState.putLong(STATE_PLAYER_POSITION, positionToSave);
        outState.putBoolean(STATE_PLAYER_PLAY_WHEN_READY, playWhenReadyToSave);
        Log.d(TAG, "onSaveInstanceState: pos=" + positionToSave + ", playReady=" + playWhenReadyToSave);
    }
    // --- End Save Player State ---


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // The back press dispatcher will handle the logic, so we can just trigger it.
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer(); // Ensure release on destroy
        binding = null;
    }
}
