package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.util.Log; // Added for logging
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // Added for user feedback if needed
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.databinding.FragmentChannelBinding;
import com.f9ld3.Zion.ui.social.FollowViewModel;
import com.google.android.material.tabs.TabLayoutMediator;

public class ChannelFragment extends Fragment {

    private static final String TAG = "ChannelFragment"; // Added for logging
    private FragmentChannelBinding binding;
    private ProfileViewModel profileViewModel;
    private FollowViewModel followViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChannelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).popBackStack()) {
                // If popping back stack failed (e.g., at start destination), finish activity
                requireActivity().finish();
            }
        });

        // Get user ID from arguments
        String channelId = null;
        if (getArguments() != null) {
            channelId = getArguments().getString("channelId");
        }

        if (channelId == null) {
            Log.e(TAG, "Channel ID is null. Cannot load profile.");
            Toast.makeText(getContext(), "Error: User ID not found.", Toast.LENGTH_SHORT).show();
            // Optionally navigate back or finish activity
            NavHostFragment.findNavController(this).popBackStack();
            return; // Stop further execution
        }

        // Initialize ViewModels
        // Use 'this' as owner for Fragment-specific ViewModels like FollowViewModel
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        followViewModel = new ViewModelProvider(this).get(FollowViewModel.class);


        // Fetch profile data
        profileViewModel.fetchUserProfile(channelId);
        profileViewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateUI);

        // Fetch and display follower count
        followViewModel.loadFollowerCount(channelId);
        followViewModel.getFollowerCount().observe(getViewLifecycleOwner(), count -> {
            binding.followerCount.setText(String.format("%s followers", formatCount(count)));
        });
        // You might want to add following count as well if needed in your ViewModel/UI

        // Setup ViewPager
        MyUploadsFragment.ViewPagerAdapter adapter = new MyUploadsFragment.ViewPagerAdapter(getChildFragmentManager(), getLifecycle());
        adapter.addFragment(UserPostsFragment.newInstance(channelId), "Posts");
        adapter.addFragment(UserVideosFragment.newInstance(channelId), "Videos");
        adapter.addFragment(UserPodcastsFragment.newInstance(channelId), "Podcasts");
        adapter.addFragment(UserLiveFragment.newInstance(channelId), "Live");
        binding.viewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(adapter.getPageTitle(position))
        ).attach();

        // TODO: Implement follow/unfollow button logic using followViewModel
        // followViewModel.checkFollowStatus(channelId);
        // followViewModel.isFollowing().observe(getViewLifecycleOwner(), isFollowing -> {
        //     binding.buttonFollow.setText(isFollowing ? "Unfollow" : "Follow");
        // });
        // binding.buttonFollow.setOnClickListener(v -> {
        //    if (followViewModel.isFollowing().getValue() == Boolean.TRUE) {
        //        followViewModel.unfollowUser(channelId);
        //    } else {
        //        UserProfile profile = profileViewModel.getUserProfile().getValue();
        //        if (profile != null) {
        //            followViewModel.followUser(channelId, profile.getAccountName()); // Use accountName
        //        }
        //    }
        // });

    }

    private void updateUI(UserProfile userProfile) {
        if (userProfile != null && getContext() != null && binding != null) { // Added null check for binding
            binding.collapsingToolbar.setTitle(userProfile.getAccountName());
            binding.toolbar.setTitle(" "); // Keep collapsing title behavior
            binding.channelName.setText(userProfile.getAccountName());

            // Handle custom username (^username)
            if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {
                binding.channelHandle.setText("^" + userProfile.getUsername());
                binding.channelHandle.setVisibility(View.VISIBLE);
            } else {
                binding.channelHandle.setVisibility(View.GONE); // Hide if no custom username
            }

            // Set Bio text
            if (userProfile.getBio() != null && !userProfile.getBio().isEmpty()) {
                binding.channelBio.setText(userProfile.getBio());
                binding.channelBio.setVisibility(View.VISIBLE);
            } else {
                binding.channelBio.setVisibility(View.GONE); // Hide bio if empty
            }

            // Load Profile Image
            Glide.with(getContext())
                    .load(userProfile.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder) // Standard profile placeholder
                    .error(R.drawable.ic_profile_placeholder)       // Show placeholder on error
                    .into(binding.channelAvatar);

            // Load Banner Image with Placeholder
            Glide.with(getContext())
                    .load(userProfile.getBannerImageUrl())
                    .placeholder(R.drawable.ic_banner_placeholder) // Use banner placeholder
                    .error(R.drawable.ic_banner_placeholder)       // Use banner placeholder on error
                    .centerCrop()
                    .into(binding.channelBanner);

        } else if (userProfile == null) {
            Log.w(TAG, "UserProfile data is null in updateUI");
            // Optionally show an error state or clear UI elements
        } else if (getContext() == null) {
            Log.w(TAG, "Context is null in updateUI, cannot load images.");
        } else {
            Log.w(TAG, "Binding is null in updateUI");
        }
    }

    // Formatting count (k, M, etc.)
    private String formatCount(long count) {
        if (count < 1000) return String.valueOf(count);
        try {
            int exp = (int) (Math.log(count) / Math.log(1000));
            // Ensure exp-1 is within bounds for the charAt index
            if (exp > 0 && exp <= "kMGTPE".length()) {
                String value = String.format("%.1f%c",
                        count / Math.pow(1000, exp),
                        "kMGTPE".charAt(exp - 1));
                return value.replace(".0", ""); // Remove .0 for whole numbers (e.g., 1k instead of 1.0k)
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting count: " + count, e);
        }
        return String.valueOf(count); // Fallback to plain number if formatting fails
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important: Nullify the binding
    }
}