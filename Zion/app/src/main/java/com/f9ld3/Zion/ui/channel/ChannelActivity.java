// main/java/com/f9ld3/Zion/ui/channel/ChannelActivity.java
package com.f9ld3.Zion.ui.channel;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View; // Import View
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider; // Import ViewModelProvider
import androidx.viewpager2.widget.ViewPager2; // Import ViewPager2

import com.bumptech.glide.Glide; // Import Glide
import com.f9ld3.Zion.R;
// *** Use the correct binding for the duplicated layout ***
import com.f9ld3.Zion.databinding.ActivityChannelBinding; // Assuming this is your activity layout binding name
import com.f9ld3.Zion.data.UserProfile; // Import UserProfile
import com.f9ld3.Zion.ui.profile.ProfileViewModel; // Import ProfileViewModel
import com.f9ld3.Zion.ui.profile.MyUploadsFragment; // Import adapter from here
import com.f9ld3.Zion.ui.profile.UserPostsFragment;
import com.f9ld3.Zion.ui.profile.UserVideosFragment;
import com.f9ld3.Zion.ui.profile.UserPodcastsFragment;
import com.f9ld3.Zion.ui.profile.UserLiveFragment;
import com.f9ld3.Zion.ui.social.FollowViewModel; // Import FollowViewModel
import com.google.android.material.tabs.TabLayoutMediator; // Import TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth; // <-- IMPORTED
import com.google.firebase.auth.FirebaseUser; // <-- IMPORTED

/**
 * Standalone activity to host Channel UI directly (layout duplicated from fragment).
 */
public class ChannelActivity extends AppCompatActivity {

    public static final String EXTRA_CHANNEL_ID = "channelId";
    public static final String EXTRA_CHANNEL_NAME = "channelName";
    private static final String TAG = "ChannelActivity";

    // *** Use the Activity's binding ***
    private ActivityChannelBinding binding;
    // *** Add ViewModels ***
    private ProfileViewModel profileViewModel;
    private FollowViewModel followViewModel;
    // *** Store Channel ID ***
    private String channelId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // *** Inflate the Activity layout ***
        binding = ActivityChannelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // *** Setup the Toolbar from the Activity's layout ***
        setSupportActionBar(binding.toolbar); // Use the toolbar ID from your layout
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // CollapsingToolbar handles title
        }

        // Get channel data from intent
        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID); // Store channelId
        String channelName = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);

        // Basic validation
        if (channelId == null || channelId.isEmpty()) {
            Log.e(TAG, "Channel ID is missing in Intent extras.");
            Toast.makeText(this, "Error: Could not load profile.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if ID is missing
            return;
        }

        // *** Initialize ViewModels (scoped to the Activity) ***
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        followViewModel = new ViewModelProvider(this).get(FollowViewModel.class);


        // *** Remove Fragment Transaction Logic ***
        // No longer adding ChannelFragment


        // *** Setup UI elements directly ***
        setupViewPagerAndTabs();
        setupObservers(); // <-- Call this before fetching data

        // *** Fetch data using ViewModels ***
        profileViewModel.fetchUserProfile(channelId);
        followViewModel.loadFollowerCount(channelId);

        // --- START OF FOLLOW LOGIC ---
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // Hide follow button if it's the user's own profile
        if (currentUser != null && currentUser.getUid().equals(channelId)) {
            binding.buttonFollow.setVisibility(View.GONE);
        } else {
            binding.buttonFollow.setVisibility(View.VISIBLE);
            // Check follow status
            followViewModel.checkFollowStatus(channelId);
            // Set click listener
            binding.buttonFollow.setOnClickListener(v -> handleFollowClick());
        }
        // --- END OF FOLLOW LOGIC ---
    }

    // *** NEW: Setup ViewPager and Tabs directly in Activity ***
    private void setupViewPagerAndTabs() {
        if (channelId == null) return; // Need channelId for fragments

        // Use the adapter defined in MyUploadsFragment (or create a similar one here)
        // Pass 'this' instead of getChildFragmentManager()
        MyUploadsFragment.ViewPagerAdapter adapter = new MyUploadsFragment.ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        adapter.addFragment(UserPostsFragment.newInstance(channelId), "Posts");
        adapter.addFragment(UserVideosFragment.newInstance(channelId), "Videos");
        adapter.addFragment(UserPodcastsFragment.newInstance(channelId), "Podcasts");
        adapter.addFragment(UserLiveFragment.newInstance(channelId), "Live");
        binding.viewPager.setAdapter(adapter); // Use binding's viewPager

        // Link tabs using binding's tabLayout and viewPager
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(adapter.getPageTitle(position))
        ).attach();
    }


    // *** NEW: Setup Observers to update UI ***
    private void setupObservers() {
        profileViewModel.getUserProfile().observe(this, this::updateProfileUI);

        followViewModel.getFollowerCount().observe(this, count -> {
            if (binding != null) { // Check binding
                binding.followerCount.setText(String.format("%s followers", formatCount(count)));
            }
        });

        // Observe follow status using the channelId
        followViewModel.isFollowing(channelId).observe(this, isFollowing -> { // <-- UPDATED
            if (binding != null) {
                binding.buttonFollow.setText(isFollowing ? "Unfollow" : "Follow");
            }
        });

        // Observe messages from FollowViewModel (e.g., "Followed", "Unfollowed")
        followViewModel.getMessage().observe(this, message -> {
            if (message != null && !message.isEmpty() && !isFinishing()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                followViewModel.clearMessage(); // Clear message after showing
            }
        });
    }

    // --- NEW: Handle Follow Button Click ---
    private void handleFollowClick() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            Toast.makeText(this, R.string.login_for_features, Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfile profile = profileViewModel.getUserProfile().getValue();
        Boolean following = followViewModel.isFollowing(channelId).getValue(); // <-- UPDATED

        if (profile != null && following != null && channelId != null) {
            if (following) {
                // --- UNFOLLOW ---
                followViewModel.unfollowUser(channelId);
            } else {
                // --- FOLLOW (FIXED: Use fallback for account name) ---
                String nameToFollow = profile.getAccountName() != null && !profile.getAccountName().isEmpty()
                        ? profile.getAccountName()
                        : profile.getUsername();
                if (nameToFollow == null || nameToFollow.isEmpty()) nameToFollow = "user"; // Extra fallback
                followViewModel.followUser(channelId, nameToFollow);
            }
        } else {
            Toast.makeText(this, "Could not perform action. Try again.", Toast.LENGTH_SHORT).show();
        }
    }
    // --- END NEW ---

    // *** NEW: Method to update profile UI elements (similar to fragment's updateUI) ***
    private void updateProfileUI(UserProfile userProfile) {
        if (userProfile != null && binding != null) {
            binding.collapsingToolbar.setTitle(userProfile.getAccountName()); // Use binding
            binding.toolbar.setTitle(" "); // Keep collapsing title behavior

            binding.channelName.setText(userProfile.getAccountName());

            if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {
                binding.channelHandle.setText("^" + userProfile.getUsername());
                binding.channelHandle.setVisibility(View.VISIBLE);
            } else {
                binding.channelHandle.setVisibility(View.GONE);
            }

            if (userProfile.getBio() != null && !userProfile.getBio().isEmpty()) {
                binding.channelBio.setText(userProfile.getBio());
                binding.channelBio.setVisibility(View.VISIBLE);
            } else {
                binding.channelBio.setVisibility(View.GONE);
            }

            Glide.with(this)
                    .load(userProfile.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.channelAvatar);

            Glide.with(this)
                    .load(userProfile.getBannerImageUrl())
                    .placeholder(R.drawable.ic_banner_placeholder)
                    .error(R.drawable.ic_banner_placeholder)
                    .centerCrop()
                    .into(binding.channelBanner);

        } else if (binding == null) {
            Log.w(TAG, "Binding is null in updateProfileUI");
        } else {
            Log.w(TAG, "UserProfile data is null in updateProfileUI");
        }
    }

    // *** NEW: Formatting count (copied from fragment) ***
    private String formatCount(long count) {
        if (count < 1000) return String.valueOf(count);
        try {
            int exp = (int) (Math.log(count) / Math.log(1000));
            if (exp > 0 && exp <= "kMGTPE".length()) {
                String value = String.format("%.1f%c",
                        count / Math.pow(1000, exp),
                        "kMGTPE".charAt(exp - 1));
                return value.replace(".0", "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting count: " + count, e);
        }
        return String.valueOf(count);
    }

    // This method correctly handles the back press for the Activity.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Clean up binding
        Log.d(TAG, "ChannelActivity onDestroy");
    }
}