package com.f9ld3.Zion.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.data.HistoryItem;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.databinding.FragmentProfileBinding;
import com.f9ld3.Zion.ui.settings.SettingsActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseUser;
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

        profileViewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateUiWithProfile);
        profileViewModel.getUserHistory().observe(getViewLifecycleOwner(), this::updateHistoryButtonText);

        // Observe the current user to get verification status and refresh token
        authViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // If user object changes (e.g., token refresh), update the UI immediately
                updateVerificationUI(user.isEmailVerified());
            }
        });

        // REMOVED: authViewModel.getVerificationStatus() observation. Relying on authViewModel.getAuthMessage().

        authViewModel.getAuthError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Profile Error: " + error, Toast.LENGTH_SHORT).show();
                // FIXED: ensure clearMessages() is called
                authViewModel.clearMessages();
            }
        });

        // Observe for generic messages (including successful resend verification)
        authViewModel.getAuthMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                authViewModel.clearMessages();
                // If the message is about verification success, manually refresh the user state
                if (message.contains("Verification email re-sent")) {
                    FirebaseUser user = authViewModel.getCurrentUser().getValue();
                    if (user != null) {
                        // Crucial step: tells Firebase to fetch the latest status from the server
                        user.reload().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && user.isEmailVerified()) {
                                updateVerificationUI(true);
                            }
                        });
                    }
                }
            }
        });

        setupResendVerification();
        setupInteractiveElements();

        // REMOVED: authViewModel.refreshVerificationStatus(); as current user observer handles it
        // Firebase Auth automatically refreshes user state when needed, but explicit reload() is better after resend.

        return root;
    }

    private void setupResendVerification() {
        if (binding.verificationBanner != null) {
            Button resendButton = binding.verificationBanner.findViewById(R.id.resend_button);
            if (resendButton != null) {
                resendButton.setOnClickListener(v -> {
                    // Call the public method in AuthViewModel (requires no params if user is logged in)
                    authViewModel.resendVerificationEmail();
                });
            }
        }
    }

    private void setupInteractiveElements() {
        MaterialCardView cardMyUploads = binding.getRoot().findViewById(R.id.card_my_uploads);
        MaterialCardView cardMyBlogs = binding.getRoot().findViewById(R.id.card_my_blogs);
        MaterialCardView cardLikes = binding.getRoot().findViewById(R.id.card_likes);
        MaterialCardView cardHistory = binding.getRoot().findViewById(R.id.card_history);
        MaterialCardView cardDownloads = binding.getRoot().findViewById(R.id.card_downloads);
        MaterialCardView cardFollowing = binding.getRoot().findViewById(R.id.card_following);
        com.google.android.material.button.MaterialButton buttonPlaylist = binding.buttonPlaylist;
        com.google.android.material.button.MaterialButton buttonSettings = binding.buttonSettings;

        View.OnClickListener authenticatedClickListener = v -> {
            int id = v.getId();
            // Use local variables to define the target Intent class for clarity
            Class<?> targetActivity = null;

            if (id == R.id.card_my_uploads) targetActivity = MyUploadsTabbedActivity.class;
            else if (id == R.id.card_my_blogs) targetActivity = MyBlogsActivity.class;
            else if (id == R.id.card_likes) targetActivity = LikesActivity.class;
            else if (id == R.id.card_history) targetActivity = HistoryActivity.class;
            else if (id == R.id.card_downloads) targetActivity = DownloadsActivity.class;
            else if (id == R.id.card_following) targetActivity = FollowingActivity.class;
            else if (id == R.id.button_playlist) targetActivity = PlaylistActivity.class;
            else if (id == R.id.button_settings) targetActivity = SettingsActivity.class;

            if (targetActivity != null) {
                startActivity(new Intent(requireActivity(), targetActivity));
            }
        };

        if (cardMyUploads != null) cardMyUploads.setOnClickListener(authenticatedClickListener);
        if (cardMyBlogs != null) cardMyBlogs.setOnClickListener(authenticatedClickListener);
        if (cardLikes != null) cardLikes.setOnClickListener(authenticatedClickListener);
        if (cardHistory != null) cardHistory.setOnClickListener(authenticatedClickListener);
        if (cardDownloads != null) cardDownloads.setOnClickListener(authenticatedClickListener);
        if (cardFollowing != null) cardFollowing.setOnClickListener(authenticatedClickListener);
        if (buttonPlaylist != null) buttonPlaylist.setOnClickListener(authenticatedClickListener);
        if (buttonSettings != null) buttonSettings.setOnClickListener(authenticatedClickListener);

        if (binding.buttonLoginSignupPrompt != null) {
            binding.buttonLoginSignupPrompt.setVisibility(View.GONE);
        }
    }

    private void updateUiWithProfile(UserProfile profile) {
        if (profile != null && getContext() != null && binding != null) {
            // Use DisplayName (username) from Firebase Auth first, then fallback to Firestore data
            FirebaseUser currentUser = authViewModel.getCurrentUser().getValue();
            String username = (currentUser != null && currentUser.getDisplayName() != null) ? currentUser.getDisplayName() : profile.getUsername();
            String email = (currentUser != null) ? currentUser.getEmail() : profile.getEmail(); // Use auth email if available

            binding.textUsername.setText(username);
            binding.textEmail.setText(email);
            Glide.with(this)
                    .load(profile.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.imageProfile);
        }
    }

    private void updateHistoryButtonText(List<HistoryItem> history) {
        if (binding == null) return;
        int count = history != null ? history.size() : 0;
        TextView historyCountText = binding.getRoot().findViewById(R.id.text_history_count);
        if (historyCountText != null) {
            // Note: R.string.history_with_count must be defined to accept an integer argument
            historyCountText.setText(getString(R.string.history_with_count, count));
        }
    }

    private void updateVerificationUI(boolean isVerified) {
        if (binding == null || getContext() == null) return;

        if (isVerified) {
            binding.textVerificationStatus.setText("Email Verified ✓");
            binding.textVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            if (binding.verificationBanner != null) {
                binding.verificationBanner.setVisibility(View.GONE);
            }
        } else {
            binding.textVerificationStatus.setText("Email Not Verified");
            binding.textVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            if (binding.verificationBanner != null) {
                binding.verificationBanner.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
