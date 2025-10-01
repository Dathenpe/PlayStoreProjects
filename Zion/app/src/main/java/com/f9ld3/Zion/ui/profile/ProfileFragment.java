package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.data.HistoryItem; // 🔥 NEW IMPORT
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.databinding.FragmentProfileBinding;

import java.util.List; // 🔥 NEW IMPORT

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 1. Observe Profile Data
        profileViewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateUiWithProfile);

        // 2. Observe History Data to update the button count
        profileViewModel.getUserHistory().observe(getViewLifecycleOwner(), this::updateHistoryCount);

        // 3. Bind menu actions
        binding.buttonMyUploads.setOnClickListener(v -> navigateToUploads());
        binding.buttonLikes.setOnClickListener(v -> navigateToLikes());

        // Assuming 'buttonPlaylists' is repurposed or renamed to 'History' in the layout
        // The text is updated dynamically in updateHistoryCount
        binding.buttonPlaylists.setOnClickListener(v -> navigateToHistory());

        return root;
    }

    /**
     * Updates the text of the History button to show the number of items.
     */
    private void updateHistoryCount(List<HistoryItem> history) {
        if (history != null) {
            Log.d(TAG, "User history count updated: " + history.size());
            binding.buttonPlaylists.setText("History (" + history.size() + ")");
        } else {
            binding.buttonPlaylists.setText("History (0)");
        }
    }


    /**
     * Dynamically updates the UI based on the fetched UserProfile (or lack thereof).
     */
    private void updateUiWithProfile(UserProfile profile) {
        // If the LiveData emits null, it means a sign out event occurred
        if (profile == null) {
            // Should theoretically be handled by the ViewModel providing a Guest profile,
            // but this is a final fail-safe reset.
            Toast.makeText(getContext(), "Signed out successfully.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isGuest = profile.getUsername().equals("Guest User");

        if (isGuest) {
            // --- GUEST STATE ---
            binding.textProfileTitle.setText(profile.getUsername());
            binding.textUserDetails.setText("Status: Guest User\nClick 'Log In / Register' to unlock features.");
            binding.buttonSignOut.setText("Log In / Register");

            // Load default guest icon
            Glide.with(this)
                    .load(R.drawable.ic_person_24dp)
                    .into(binding.imageProfile);

            // Set click listeners to prompt login
            View.OnClickListener loginPrompt = v -> promptLogin();
            binding.imageProfile.setOnClickListener(loginPrompt);
            binding.buttonSignOut.setOnClickListener(loginPrompt);

            // Disable features
            binding.buttonMyUploads.setEnabled(false);
            binding.buttonLikes.setEnabled(false);
            binding.buttonPlaylists.setEnabled(false); // History button disabled for guest
        } else {
            // --- LOGGED-IN STATE ---
            binding.textProfileTitle.setText(profile.getUsername() != null ? profile.getUsername() : "My Profile");
            binding.textUserDetails.setText(
                    "Status: Signed In\nEmail: " + (profile.getEmail() != null ? profile.getEmail() : "N/A") +
                            "\nUser ID: " + (profile.getUserId() != null ? profile.getUserId().substring(0, 8) + "..." : "N/A")
            );
            binding.buttonSignOut.setText("Sign Out");

            // Load actual profile image
            if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(profile.getProfileImageUrl())
                        .placeholder(R.drawable.ic_person_24dp) // Use default icon as placeholder
                        .error(R.drawable.ic_error_24dp) // Use error icon if loading fails
                        .into(binding.imageProfile);
            } else {
                binding.imageProfile.setImageResource(R.drawable.ic_person_24dp);
            }

            // Set sign out logic
            binding.buttonSignOut.setOnClickListener(v -> profileViewModel.signOut());
            binding.imageProfile.setOnClickListener(v -> Toast.makeText(getContext(), "Click Sign Out to switch accounts or edit profile (TODO).", Toast.LENGTH_SHORT).show());

            // Enable features
            binding.buttonMyUploads.setEnabled(true);
            binding.buttonLikes.setEnabled(true);
            binding.buttonPlaylists.setEnabled(true); // History button enabled
        }
    }

    // Placeholder navigation methods
    private void promptLogin() { Toast.makeText(getContext(), "TODO: Navigate to Login/Registration screen.", Toast.LENGTH_LONG).show(); }
    private void navigateToUploads() { Toast.makeText(getContext(), "TODO: Navigate to My Uploads screen.", Toast.LENGTH_SHORT).show(); }
    private void navigateToLikes() { Toast.makeText(getContext(), "TODO: Navigate to Liked Media screen.", Toast.LENGTH_SHORT).show(); }

    /**
     * Action for the History button.
     */
    private void navigateToHistory() {
        List<HistoryItem> history = profileViewModel.getUserHistory().getValue();
        int count = history != null ? history.size() : 0;
        Toast.makeText(getContext(), "TODO: Navigate to History Screen. Viewing " + count + " most recent items.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}