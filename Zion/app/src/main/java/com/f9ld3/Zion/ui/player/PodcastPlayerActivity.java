// main/java/com/f9ld3/Zion/ui/player/PodcastPlayerActivity.java
package com.f9ld3.Zion.ui.player;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
// Removed PlayerView import as it's not directly used here
import androidx.media3.ui.PlayerControlView;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityPodcastPlayerBinding;

/**
 * Activity for playing podcast (audio-only) content using ExoPlayer via BasePlayerActivity.
 * It provides the PlayerControlView for audio controls.
 */
@OptIn(markerClass = UnstableApi.class) // For BasePlayerActivity potentially using unstable APIs
public class PodcastPlayerActivity extends BasePlayerActivity {

    public static final String EXTRA_MEDIA_ITEM = "extra_media_item";
    private static final String LOG_TAG = "PodcastPlayerActivity"; // Specific Log Tag

    private ActivityPodcastPlayerBinding binding;

    // --- Implementation of Abstract Methods ---

    /**
     * Provides the PlayerControlView from this activity's layout to the base class.
     * @return The PlayerControlView instance.
     */
    @Override
    protected View getPlayerUiView() {
        // Return the PlayerControlView, as this layout doesn't have a PlayerView
        return (binding != null) ? binding.playerControlView : null;
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
        // Inflate binding before calling super.onCreate
        binding = ActivityPodcastPlayerBinding.inflate(getLayoutInflater());

        // Call super.onCreate AFTER binding inflation but BEFORE setContentView
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

        // Setup UI elements (Title, Author, Thumbnail) using currentMedia
        try {
            binding.podcastTitle.setText(currentMedia.getTitle());
            binding.authorName.setText(currentMedia.getAuthorName());

            // Load podcast thumbnail using Glide
            Glide.with(this)
                    .load(currentMedia.getThumbnailUrl())
                    .placeholder(R.drawable.ic_mic_24dp) // Podcast specific placeholder
                    .error(R.drawable.ic_mic_24dp)       // Podcast specific error placeholder
                    .into(binding.podcastThumbnail);
        } catch (Exception e) {
            Log.e(getLogTag(), "Error setting up UI elements", e);
            Toast.makeText(this, "Error loading podcast details", Toast.LENGTH_SHORT).show();
        }

        // Player initialization is handled by BasePlayerActivity's onStart method.
        // The PlayerControlView will be linked automatically by the base class logic.
        Log.d(getLogTag(), "onCreate completed.");
    }

    // --- Lifecycle methods (onStart, onResume, onPause, onStop) are handled by BasePlayerActivity ---
    // No need to override initializePlayer or releasePlayer here, as the base class
    // correctly handles linking/unlinking PlayerControlView based on getPlayerUiView().

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