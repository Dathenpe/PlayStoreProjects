package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;

/**
 * Fragment to display the user's uploaded videos.
 */
public class MyVideosFragment extends Fragment implements PlayerPostAdapter.OnMediaClickListener {

    private FragmentFullPageListBinding binding;
    private ProfileViewModel profileViewModel;
    private PlayerPostAdapter videoAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFullPageListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        setupRecyclerView();

        // Customize the empty state for this page (using template)
        // Note: Removed old references to emptyStateContainer and icon_placeholder
        binding.textPlaceholder.setText(getString(R.string.videos_empty_text));
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_videocam_24dp, 0, 0);

        profileViewModel.getUserVideos().observe(getViewLifecycleOwner(), videos -> {
            if (videos != null && !videos.isEmpty()) {
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.textPlaceholder.setVisibility(View.GONE);
                videoAdapter.submitList(videos);
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupRecyclerView() {
        videoAdapter = new PlayerPostAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(videoAdapter);
    }

    @Override
    public void onMediaClick(PlayerMedia media) {
        // TODO: Implement navigation to video player.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}