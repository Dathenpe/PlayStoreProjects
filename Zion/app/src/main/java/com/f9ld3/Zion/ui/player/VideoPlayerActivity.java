// main/java/com/f9ld3/Zion/ui/player/VideoPlayerActivity.java
package com.f9ld3.Zion.ui.player;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityVideoPlayerBinding;

/**
 * Activity specifically for playing video content using ExoPlayer via BasePlayerActivity.
 * It provides the PlayerView required for video rendering.
 */
@OptIn(markerClass = UnstableApi.class) // For BasePlayerActivity potentially using unstable APIs
public class VideoPlayerActivity extends BasePlayerActivity {

    public static final String EXTRA_MEDIA_ITEM = "extra_media_item";
    private static final String LOG_TAG = "VideoPlayerActivity"; // Specific Log Tag

    private ActivityVideoPlayerBinding binding;

    // --- Implementation of Abstract Methods ---

    /**
     * Provides the PlayerView from this activity's layout to the base class.
     * @return The PlayerView instance.
     */
    @Override
    protected View getPlayerUiView() {
        // Ensure binding is initialized before accessing playerView
        return (binding != null) ? binding.playerView : null;
    }

    /**
     * Extracts the PlayerMedia object passed via the Intent.
     * @return The PlayerMedia object, or null if not found or invalid.
     */
    @Override
    protected PlayerMedia getMediaItemFromIntent() {
        try {
            // Check if the extra exists and is of the correct type
            if (getIntent() != null && getIntent().hasExtra(EXTRA_MEDIA_ITEM)) {
                return (PlayerMedia) getIntent().getSerializableExtra(EXTRA_MEDIA_ITEM);
            } else {
                Log.e(getLogTag(), "Intent or EXTRA_MEDIA_ITEM is missing.");
                return null;
            }
        } catch (ClassCastException e) {
            Log.e(getLogTag(), "Error casting Intent extra to PlayerMedia.", e);
            return null; // Return null if casting fails
        }
    }

    /**
     * Provides the log tag specific to this activity.
     * @return The log tag string.
     */
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Inflate binding before calling super.onCreate which needs the media item
        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());

        // Call super.onCreate AFTER binding inflation but BEFORE setContentView
        // super.onCreate handles media item extraction and potential finish() if invalid
        super.onCreate(savedInstanceState);

        // Check if super.onCreate called finish() due to invalid media
        if (isFinishing() || currentMedia == null) {
            Log.w(getLogTag(), "Finishing activity in onCreate - invalid media or already finishing.");
            return; // Don't proceed if finishing
        }

        // Set the content view using the binding's root
        setContentView(binding.getRoot());

        // Setup Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide default title
        } else {
            Log.w(getLogTag(), "Support ActionBar is null, cannot set up toolbar.");
        }

        // Setup UI elements (Title, Author, Avatar) using currentMedia (set in super.onCreate)
        try {
            binding.videoTitle.setText(currentMedia.getTitle());
            binding.authorName.setText(currentMedia.getAuthorName());

            // Load author avatar using Glide
            Glide.with(this)
                    .load(currentMedia.getUploaderAvatarUrl()) // Use the correct getter
                    .placeholder(R.drawable.ic_profile_placeholder) // Fallback placeholder
                    .error(R.drawable.ic_profile_placeholder)       // Fallback on error
                    .into(binding.authorAvatar);
        } catch (Exception e) {
            Log.e(getLogTag(), "Error setting up UI elements", e);
            // Show a generic error? The activity might still function for playback.
            Toast.makeText(this, "Error loading video details", Toast.LENGTH_SHORT).show();
        }

        // Player initialization is handled by BasePlayerActivity's onStart method
        Log.d(getLogTag(), "onCreate completed.");
    }

    // --- Lifecycle methods (onStart, onResume, onPause, onStop) are handled by BasePlayerActivity ---
    // No need to override them unless specific VideoPlayerActivity logic is required.

    /**
     * Handles action bar item clicks, specifically the Up button.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle presses on the action bar items
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close the activity when the back arrow is pressed
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Cleans up resources, including releasing the ViewBinding.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(getLogTag(), "onDestroy");
        binding = null; // Release the binding
        // Player release is handled in BasePlayerActivity's onStop/onDestroy
    }
}