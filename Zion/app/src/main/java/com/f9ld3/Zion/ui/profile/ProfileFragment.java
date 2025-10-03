package com.f9ld3.Zion.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.data.HistoryItem;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.databinding.FragmentProfileBinding;

import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        profileViewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateUiWithProfile);
        profileViewModel.getUserHistory().observe(getViewLifecycleOwner(), this::updateHistoryButtonText);

        // Bind all buttons to launch their respective Activities
        binding.buttonSignOut.setOnClickListener(v -> profileViewModel.signOut());
        binding.buttonEditProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));

        // --- UPDATED: Launch MyUploadsTabbedActivity for uploads ---
        binding.buttonMyUploads.setOnClickListener(v -> startActivity(new Intent(getActivity(), MyUploadsTabbedActivity.class)));
        // --- NEW: Following button (assuming FollowingActivity exists) ---
        binding.buttonFollowing.setOnClickListener(v -> startActivity(new Intent(getActivity(), FollowingActivity.class)));

        binding.buttonLikes.setOnClickListener(v -> startActivity(new Intent(getActivity(), LikesActivity.class)));
        binding.buttonHistory.setOnClickListener(v -> startActivity(new Intent(getActivity(), HistoryActivity.class)));
        binding.buttonDownloads.setOnClickListener(v -> startActivity(new Intent(getActivity(), DownloadsActivity.class)));

        // --- NEW: My Blogs button ---
        binding.buttonMyBlogs.setOnClickListener(v -> startActivity(new Intent(getActivity(), MyBlogsActivity.class)));

        // --- NEW: Settings button ---
        binding.buttonSettings.setOnClickListener(v -> startActivity(new Intent(getActivity(), com.f9ld3.Zion.ui.settings.SettingsActivity.class)));


        return root;
    }

    private void updateUiWithProfile(UserProfile profile) {
        if (profile != null && getContext() != null) {
            binding.textUsername.setText(profile.getUsername());
            binding.textEmail.setText(profile.getEmail());
            Glide.with(this)
                    .load(profile.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.imageProfile);
        } else {
            binding.textUsername.setText(R.string.anonymous_user);
            binding.textEmail.setText(R.string.login_for_features);
            binding.imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void updateHistoryButtonText(List<HistoryItem> history) {
        int count = history != null ? history.size() : 0;
        binding.buttonHistory.setText(getString(R.string.history_with_count, count));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}