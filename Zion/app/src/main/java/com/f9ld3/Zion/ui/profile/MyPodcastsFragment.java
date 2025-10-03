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
 * Fragment to display the user's uploaded podcast media items.
 */
public class MyPodcastsFragment extends Fragment implements PlayerPostAdapter.OnMediaClickListener {

    private FragmentFullPageListBinding binding;
    private ProfileViewModel profileViewModel;
    private PlayerPostAdapter podcastAdapter;

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
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_mic_24dp, 0, 0);

        profileViewModel.getUserMedia().observe(getViewLifecycleOwner(), mediaList -> {
            if (mediaList != null && !mediaList.isEmpty()) {
                List<PlayerMedia> podcasts = mediaList.stream()
                        .filter(media -> media.getType() == PlayerMedia.TYPE_PODCAST_SINGLE || media.getType() == PlayerMedia.TYPE_PODCAST_DUO_CONTAINER)
                        .collect(Collectors.toList());

                if (podcasts.isEmpty()) {
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.textPlaceholder.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerView.setVisibility(View.VISIBLE);
                    binding.textPlaceholder.setVisibility(View.GONE);
                    podcastAdapter.submitList(podcasts);
                }
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupRecyclerView() {
        podcastAdapter = new PlayerPostAdapter(this); // 'this' refers to OnMediaClickListener
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(podcastAdapter);
    }

    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        // TODO: Implement navigation to play the podcast
        // Example: Toast.makeText(getContext(), "Playing podcast: " + mediaItem.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}