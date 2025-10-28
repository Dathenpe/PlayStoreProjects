// main/java/com/f9ld3/Zion/ui/feed/FullScreenMediaActivity.java
package com.f9ld3.Zion.ui.feed;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityFullScreenMediaBinding;
import java.util.ArrayList;

public class FullScreenMediaActivity extends AppCompatActivity {

    public static final String EXTRA_MEDIA_ITEMS = "extra_media_items";
    public static final String EXTRA_START_POSITION = "extra_start_position";

    private ActivityFullScreenMediaBinding binding;
    private ArrayList<MediaItem> mediaItems;
    private int startPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullScreenMediaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- Make activity fullscreen ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        // --- End Fullscreen setup ---


        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide title
        }

        // Retrieve data from Intent
        mediaItems = (ArrayList<MediaItem>) getIntent().getSerializableExtra(EXTRA_MEDIA_ITEMS);
        startPosition = getIntent().getIntExtra(EXTRA_START_POSITION, 0);

        if (mediaItems == null || mediaItems.isEmpty()) {
            finish(); // Close if no media items
            return;
        }

        setupViewPager();
    }

    private void setupViewPager() {
        FullScreenMediaAdapter adapter = new FullScreenMediaAdapter(this, mediaItems);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setCurrentItem(startPosition, false); // Go to the clicked item without smooth scroll
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close activity on back arrow press
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}