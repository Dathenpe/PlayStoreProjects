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
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding; // Reusing this layout
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter; // Reusing PlayerPostAdapter

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fragment to display the user's uploaded video media items.
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

        // Customize the empty state for this page
        binding.textPlaceholder.setText(getString(R.string.uploads_empty_text)); // Can be more specific if desired
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_live_tv_24dp, 0, 0);

        profileViewModel.getUserMedia().observe(getViewLifecycleOwner(), mediaList -> {
            if (mediaList != null && !mediaList.isEmpty()) {
                List<PlayerMedia> videos = mediaList.stream()
                        .filter(media -> media.getType() == PlayerMedia.TYPE_VIDEO)
                        .collect(Collectors.toList());

                if (videos.isEmpty()) {
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.textPlaceholder.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerView.setVisibility(View.VISIBLE);
                    binding.textPlaceholder.setVisibility(View.GONE);
                    videoAdapter.submitList(videos);
                }
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupRecyclerView() {
        videoAdapter = new PlayerPostAdapter(this); // 'this' refers to OnMediaClickListener
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(videoAdapter);
    }

    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        // TODO: Implement navigation to play the video
        // Example: Toast.makeText(getContext(), "Playing video: " + mediaItem.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}