package com.f9ld3.Zion.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Keep Button import if accessing directly
import android.widget.TextView; // Keep TextView import
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Added
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.data.HistoryItem;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.databinding.FragmentProfileBinding;
import com.f9ld3.Zion.ui.settings.SettingsActivity;
import com.f9ld3.Zion.ui.social.FollowViewModel;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private AuthViewModel authViewModel;
    private FollowViewModel followViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        followViewModel = new ViewModelProvider(this).get(FollowViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Observe LiveData in onViewCreated
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser currentUser = authViewModel.getCurrentUser().getValue();
        if (currentUser != null) {
            profileViewModel.fetchUserProfile(currentUser.getUid());
            followViewModel.loadFollowerCount(currentUser.getUid());
            profileViewModel.fetchUserHistory(currentUser.getUid()); // Fetch history
        } else {
            // Handle logged out state if necessary (e.g., show login prompt)
            binding.buttonLoginSignupPrompt.setVisibility(View.VISIBLE);
            // Hide sensitive elements
            binding.cardMyUploads.setVisibility(View.GONE);
            binding.cardMyBlogs.setVisibility(View.GONE);
            // ... hide other elements as needed
        }


        profileViewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateUiWithProfile);
        profileViewModel.getUserHistory().observe(getViewLifecycleOwner(), this::updateHistoryButtonText); // Observe history

        authViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null) {
                // User logged out, update UI accordingly
                binding.buttonLoginSignupPrompt.setVisibility(View.VISIBLE);
                binding.textUsername.setText(R.string.anonymous_user);
                binding.textHandle.setVisibility(View.GONE);
                binding.textEmail.setText(R.string.login_for_features);
                binding.imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
                binding.channelBanner.setImageResource(R.drawable.ic_banner_placeholder);
                binding.verificationBanner.setVisibility(View.GONE);
                // Hide other elements requiring login
                binding.cardMyUploads.setVisibility(View.GONE);
                binding.cardMyBlogs.setVisibility(View.GONE);
                binding.cardLikes.setVisibility(View.GONE);
                binding.cardHistory.setVisibility(View.GONE);
                binding.cardDownloads.setVisibility(View.GONE);
                binding.cardFollowing.setVisibility(View.GONE);
                binding.buttonPlaylist.setVisibility(View.GONE);
                binding.buttonSettings.setVisibility(View.GONE); // Or keep settings?
                binding.statsLayout.setVisibility(View.GONE);

            } else {
                // User logged in
                binding.buttonLoginSignupPrompt.setVisibility(View.GONE);
                // Make elements visible again
                binding.cardMyUploads.setVisibility(View.VISIBLE);
                binding.cardMyBlogs.setVisibility(View.VISIBLE);
                binding.cardLikes.setVisibility(View.VISIBLE);
                binding.cardHistory.setVisibility(View.VISIBLE);
                binding.cardDownloads.setVisibility(View.VISIBLE);
                binding.cardFollowing.setVisibility(View.VISIBLE);
                binding.buttonPlaylist.setVisibility(View.VISIBLE);
                binding.buttonSettings.setVisibility(View.VISIBLE);
                binding.statsLayout.setVisibility(View.VISIBLE);

                updateVerificationUI(user.isEmailVerified());
                // Re-fetch data if needed upon login state change after initial load
                if (profileViewModel.getUserProfile().getValue() == null) {
                    profileViewModel.fetchUserProfile(user.getUid());
                    followViewModel.loadFollowerCount(user.getUid());
                    profileViewModel.fetchUserHistory(user.getUid());
                }
            }
        });


        authViewModel.getAuthError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && getContext() != null) { // Check context
                Toast.makeText(requireContext(), "Profile Error: " + error, Toast.LENGTH_SHORT).show();
                authViewModel.clearMessages();
            }
        });

        authViewModel.getAuthMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && getContext() != null) { // Check context
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                authViewModel.clearMessages();
                if (message.contains("Verification email re-sent")) {
                    FirebaseUser user = authViewModel.getCurrentUser().getValue();
                    if (user != null) {
                        user.reload().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && user.isEmailVerified()) {
                                updateVerificationUI(true);
                            }
                        });
                    }
                }
            }
        });

        followViewModel.getFollowerCount().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) {
                // Update follower count, potentially format it (e.g., 1.2k)
                binding.textFollowerCount.setText(String.valueOf(count));
            }
        });

        setupInteractiveElements();
    }





    private void setupInteractiveElements() {
        FirebaseUser currentUser = authViewModel.getCurrentUser().getValue();
        boolean isLoggedIn = currentUser != null;

        View.OnClickListener navigationClickListener = v -> {
            if (!isLoggedIn) {
                Toast.makeText(requireContext(), R.string.login_for_features, Toast.LENGTH_SHORT).show();
                // Optionally navigate to login screen
                return;
            }
            int id = v.getId();
            try {
                if (id == R.id.card_likes) {
                    NavHostFragment.findNavController(this).navigate(R.id.navigation_likes);
                } else if (id == R.id.card_history) {
                    NavHostFragment.findNavController(this).navigate(R.id.navigation_history);
                } else if (id == R.id.card_downloads) {
                    NavHostFragment.findNavController(this).navigate(R.id.navigation_downloads);
                } else if (id == R.id.card_my_uploads) {
                    NavHostFragment.findNavController(this).navigate(R.id.navigation_my_uploads);
                } else if (id == R.id.card_my_blogs) {
                    NavHostFragment.findNavController(this).navigate(R.id.navigation_my_posts);
                } else if (id == R.id.card_following) {
                    NavHostFragment.findNavController(this).navigate(R.id.navigation_following);
                } else if (id == R.id.button_playlist) {
                    NavHostFragment.findNavController(this).navigate(R.id.navigation_playlist);
                } else if (id == R.id.button_settings) {
                    startActivity(new Intent(requireActivity(), SettingsActivity.class));
                }
            } catch (IllegalStateException | IllegalArgumentException e) {
                Log.e(TAG, "Navigation error: ", e);
                Toast.makeText(getContext(), "Error navigating", Toast.LENGTH_SHORT).show();
            }
        };

        binding.cardMyUploads.setOnClickListener(navigationClickListener);
        binding.cardMyBlogs.setOnClickListener(navigationClickListener);
        binding.cardLikes.setOnClickListener(navigationClickListener);
        binding.cardHistory.setOnClickListener(navigationClickListener);
        binding.cardDownloads.setOnClickListener(navigationClickListener);
        binding.cardFollowing.setOnClickListener(navigationClickListener);
        binding.buttonPlaylist.setOnClickListener(navigationClickListener);
        binding.buttonSettings.setOnClickListener(navigationClickListener);

        // Handle login/signup prompt click
        binding.buttonLoginSignupPrompt.setOnClickListener(v -> {
            // Navigate to LoginActivity
            Intent intent = new Intent(requireActivity(), com.f9ld3.Zion.auth.LoginActivity.class);
            startActivity(intent);
        });

        // Ensure visibility is correct based on login state (handled in observer)
        binding.buttonLoginSignupPrompt.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
    }


    private void updateUiWithProfile(UserProfile profile) {
        if (profile != null && getContext() != null && binding != null) {
            FirebaseUser currentUser = authViewModel.getCurrentUser().getValue(); // Re-check current user
            boolean isLoggedIn = currentUser != null;

            binding.textUsername.setText(profile.getAccountName());

            if (profile.getUsername() != null && !profile.getUsername().isEmpty()) {
                binding.textHandle.setText(String.format("@%s", profile.getUsername()));
                binding.textHandle.setVisibility(View.VISIBLE);
            } else {
                binding.textHandle.setVisibility(View.GONE);
            }

            binding.textEmail.setText(isLoggedIn ? (currentUser.getEmail() != null ? currentUser.getEmail() : "") : profile.getEmail());


            if (profile.getBio() != null && !profile.getBio().isEmpty()) {
                binding.textBio.setText(profile.getBio());
                binding.textBio.setVisibility(View.VISIBLE);
            } else {
                binding.textBio.setVisibility(View.GONE);
            }

            // Show verification status only if logged in
            binding.textVerificationStatus.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
            if (isLoggedIn) {
                updateVerificationUI(currentUser.isEmailVerified());
            }


            if (profile.getCreatedAt() > 0) {
                binding.textJoinDate.setText(String.format("Joined %s", formatDate(profile.getCreatedAt())));
                binding.textJoinDate.setVisibility(View.VISIBLE);
            } else {
                binding.textJoinDate.setVisibility(View.GONE);
            }

            Glide.with(this)
                    .load(profile.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.imageProfile);

            if (binding.channelBanner != null) {
                Glide.with(this)
                        .load(profile.getBannerImageUrl())
                        .placeholder(R.drawable.ic_banner_placeholder)
                        .error(R.drawable.ic_banner_placeholder)
                        .centerCrop()
                        .into(binding.channelBanner);
            } else {
                Log.w(TAG, "channel_banner ImageView not found in the layout.");
            }
        } else if (authViewModel.getCurrentUser().getValue() == null) {
            // Handle logged out state explicitly if profile is null and user is null
            binding.buttonLoginSignupPrompt.setVisibility(View.VISIBLE);
            // ... reset other UI elements ...
        }

    }


    private void updateHistoryButtonText(List<HistoryItem> history) {
        if (binding == null) return;
        int count = history != null ? history.size() : 0;
        // Use binding directly if text_history_count is part of FragmentProfileBinding
        // Ensure text_history_count exists in fragment_profile.xml
        if (binding.textHistoryCount != null) {
            binding.textHistoryCount.setText(getString(R.string.history_with_count, count));
        } else {
            Log.w(TAG, "text_history_count not found via binding.");
            // Fallback to findViewById if necessary, though binding is preferred
            TextView historyCountText = binding.getRoot().findViewById(R.id.text_history_count);
            if (historyCountText != null) {
                historyCountText.setText(getString(R.string.history_with_count, count));
            }
        }
    }


    private void updateVerificationUI(boolean isVerified) {
        if (binding == null || getContext() == null) return;

        if (isVerified) {
            binding.textVerificationStatus.setText(R.string.email_verified);
            binding.textVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.teal)); // Use teal or success color
            binding.textVerificationStatus.setVisibility(View.VISIBLE); // Ensure it's visible
            // Use binding to access included layout's root and set visibility

        } else {
            binding.textVerificationStatus.setText(R.string.email_not_verified);
            binding.textVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
            binding.textVerificationStatus.setVisibility(View.VISIBLE); // Ensure it's visible
            // Use binding to access included layout's root and set visibility
        }
    }


    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}