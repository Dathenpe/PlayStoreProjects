package com.f9ld3.Zion.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.data.HistoryItem;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.databinding.FragmentProfileBinding;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseUser ;

import java.util.List;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Observe ProfileViewModel for profile and history
        profileViewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateUiWithProfile);
        profileViewModel.getUserHistory().observe(getViewLifecycleOwner(), this::updateHistoryButtonText);

        // Observe AuthViewModel for verification status
        authViewModel.isEmailVerified().observe(getViewLifecycleOwner(), isVerified -> {
            if (isVerified != null) {
                updateVerificationUI(isVerified);
            }
        });

        // Observe auth errors/messages
        authViewModel.getAuthError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Profile Error: " + error, Toast.LENGTH_SHORT).show();
                authViewModel.clearAuthError();
            }
        });
        authViewModel.getAuthMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                authViewModel.clearMessages();
            }
        });

        // Setup resend verification (nested in banner)
        setupResendVerification();

        // Bind all buttons/cards to launch their respective Activities (disabled if unverified)
        setupInteractiveElements();

        // Initial refresh
        authViewModel.refreshVerificationStatus();

        return root;
    }

    private void setupResendVerification() {
        if (binding.verificationBanner != null) {
            Button resendButton = binding.verificationBanner.findViewById(R.id.resend_button);
            if (resendButton != null) {
                resendButton.setOnClickListener(v -> {
                    authViewModel.resendVerificationEmail();
                    authViewModel.refreshVerificationStatus(); // Refresh after resend
                });
            }
        }
    }

    private void setupInteractiveElements() {
        // Grid cards - Matches XML IDs, using navigation intents
        MaterialCardView cardMyUploads = binding.getRoot().findViewById(R.id.card_my_uploads);
        MaterialCardView cardMyBlogs = binding.getRoot().findViewById(R.id.card_my_blogs);
        MaterialCardView cardLikes = binding.getRoot().findViewById(R.id.card_likes);
        MaterialCardView cardHistory = binding.getRoot().findViewById(R.id.card_history);
        MaterialCardView cardDownloads = binding.getRoot().findViewById(R.id.card_downloads);
        MaterialCardView cardFollowing = binding.getRoot().findViewById(R.id.card_following);

        // Buttons - Matches XML
        com.google.android.material.button.MaterialButton buttonPlaylist = binding.buttonPlaylist;
        com.google.android.material.button.MaterialButton buttonSettings = binding.buttonSettings;

        // Common click handler with navigation intents (from your code)
        View.OnClickListener authenticatedClickListener = v -> {
            int id = v.getId();
            if (id == R.id.card_my_uploads) {
                startActivity(new Intent(getActivity(), MyUploadsTabbedActivity.class));
            } else if (id == R.id.card_my_blogs) {
                startActivity(new Intent(getActivity(), MyBlogsActivity.class));
            } else if (id == R.id.card_likes) {
                startActivity(new Intent(getActivity(), LikesActivity.class));
            } else if (id == R.id.card_history) {
                startActivity(new Intent(getActivity(), HistoryActivity.class));
            } else if (id == R.id.card_downloads) {
                startActivity(new Intent(getActivity(), DownloadsActivity.class));
            } else if (id == R.id.card_following) {
                startActivity(new Intent(getActivity(), FollowingActivity.class));
            } else if (id == R.id.button_playlist) {
                startActivity(new Intent(getActivity(), PlaylistActivity.class)); // Assumed; adjust if different
            } else if (id == R.id.button_settings) {
                startActivity(new Intent(getActivity(), com.f9ld3.Zion.ui.settings.SettingsActivity.class));
            }
        };

        // Set listeners (will be enabled/disabled later based on verification)
        if (cardMyUploads != null) cardMyUploads.setOnClickListener(authenticatedClickListener);
        if (cardMyBlogs != null) cardMyBlogs.setOnClickListener(authenticatedClickListener);
        if (cardLikes != null) cardLikes.setOnClickListener(authenticatedClickListener);
        if (cardHistory != null) cardHistory.setOnClickListener(authenticatedClickListener);
        if (cardDownloads != null) cardDownloads.setOnClickListener(authenticatedClickListener);
        if (cardFollowing != null) cardFollowing.setOnClickListener(authenticatedClickListener);
        if (buttonPlaylist != null) buttonPlaylist.setOnClickListener(authenticatedClickListener);
        if (buttonSettings != null) buttonSettings.setOnClickListener(authenticatedClickListener);

        // Hide login prompt since registration is forced
        if (binding.buttonLoginSignupPrompt != null) {
            binding.buttonLoginSignupPrompt.setVisibility(View.GONE);
        }

        // TODO: If adding buttonEditProfile/buttonSignOut to XML, uncomment:
        // binding.buttonEditProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));
        // binding.buttonSignOut.setOnClickListener(v -> profileViewModel.signOut());
    }

    private void updateUiWithProfile(UserProfile profile) {
        if (profile != null && getContext() != null && binding != null) {
            binding.textUsername.setText(profile.getUsername());
            binding.textEmail.setText(profile.getEmail());
            Glide.with(this)
                    .load(profile.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.imageProfile);
        } else {
            // No anonymous fallback - Log error and show placeholder (user should be authenticated)
            Log.w(TAG, "Profile data unavailable - ensure user is authenticated.");
            if (binding != null) {
                binding.textUsername.setText("Loading...");
                binding.textEmail.setText("Loading...");
                binding.imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }
    }

    private void updateHistoryButtonText(List<HistoryItem> history) {
        if (binding == null) return;
        int count = history != null ? history.size() : 0;
        TextView historyCountText = binding.getRoot().findViewById(R.id.text_history_count); // Matches XML in card_history
        if (historyCountText != null) {
            historyCountText.setText(getString(R.string.history_with_count, count)); // e.g., "History (5)"
        }
    }

    private void updateVerificationUI(boolean isVerified) {
        if (binding == null) return;

        // Update status text - Matches XML: text_verification_status
        if (isVerified) {
            binding.textVerificationStatus.setText("Email Verified ✓");
            binding.textVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            binding.textVerificationStatus.setVisibility(View.VISIBLE);
            if (binding.verificationBanner != null) {
                binding.verificationBanner.setVisibility(View.GONE);
            }
        } else {
            binding.textVerificationStatus.setText("Email Not Verified");
            binding.textVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            binding.textVerificationStatus.setVisibility(View.VISIBLE);
            if (binding.verificationBanner != null) {
                binding.verificationBanner.setVisibility(View.VISIBLE);
            }
            Toast.makeText(requireContext(), "Please verify your email to unlock full features.", Toast.LENGTH_LONG).show();
        }

        // Enable/disable interactions based on verification
        setInteractiveElementsEnabled(isVerified);
    }

    // Enable/disable actual XML elements (cards and buttons)
    private void setInteractiveElementsEnabled(boolean enabled) {
        // Grid cards
        MaterialCardView[] cards = {
                binding.getRoot().findViewById(R.id.card_my_uploads),
                binding.getRoot().findViewById(R.id.card_my_blogs),
                binding.getRoot().findViewById(R.id.card_likes),
                binding.getRoot().findViewById(R.id.card_history),
                binding.getRoot().findViewById(R.id.card_downloads),
                binding.getRoot().findViewById(R.id.card_following)
        };

        for (MaterialCardView card : cards) {
            if (card != null) {
                card.setClickable(enabled);
                card.setFocusable(enabled);
                card.setAlpha(enabled ? 1.0f : 0.5f);
            }
        }

        // Buttons
        if (binding.buttonPlaylist != null) {
            binding.buttonPlaylist.setEnabled(enabled);
            binding.buttonPlaylist.setAlpha(enabled ? 1.0f : 0.5f);
        }
        if (binding.buttonSettings != null) {
            binding.buttonSettings.setEnabled(enabled);
            binding.buttonSettings.setAlpha(enabled ? 1.0f : 0.5f);
        }

        // On disable, add click listener to prompt verification
        if (!enabled) {
            View.OnClickListener promptListener = v -> Toast.makeText(requireContext(), "Verify your email to access this feature.", Toast.LENGTH_SHORT).show();
            for (MaterialCardView card : cards) {
                if (card != null) card.setOnClickListener(promptListener);
            }
            if (binding.buttonPlaylist != null) binding.buttonPlaylist.setOnClickListener(promptListener);
            if (binding.buttonSettings != null) binding.buttonSettings.setOnClickListener(promptListener);
        }

        Log.d(TAG, "Profile interactions enabled: " + enabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}