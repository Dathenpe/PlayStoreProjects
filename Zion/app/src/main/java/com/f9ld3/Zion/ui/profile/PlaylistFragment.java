package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding;
import com.f9ld3.Zion.ui.player.PlayerMedia; // Assuming playlists contain media
import com.f9ld3.Zion.ui.player.PlayerPostAdapter; // Reuse or create PlaylistAdapter if needed

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fragment to display the user's playlists.
 */
public class PlaylistFragment extends Fragment implements PlayerPostAdapter.OnMediaClickListener {

    private FragmentFullPageListBinding binding;
    private ProfileViewModel profileViewModel;
    private PlayerPostAdapter playlistAdapter; // Update to handle playlist items if needed

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

        // FIXED: Use direct binding accessors
        // Customize empty state for playlists
        binding.textPlaceholder.setText(getString(R.string.playlists_empty_text)); // e.g., "No playlists created"
        binding.iconPlaceholder.setImageResource(R.drawable.ic_playlist_play_24dp); // Playlist-specific icon

        // Observe user playlists (adjust getter if your ViewModel uses getUser Playlists() or similar)
        profileViewModel.getUserMedia().observe(getViewLifecycleOwner(), mediaList -> { // Or getUser Playlists() if separate
            if (mediaList != null && !mediaList.isEmpty()) {
                // Assuming playlists are derived from media; filter or map as needed
                List<PlayerMedia> playlists = mediaList.stream()
                        .filter(media -> media.getType() == PlayerMedia.TYPE_PLAYLIST) // Adjust type if exists
                        .collect(Collectors.toList());

                if (playlists.isEmpty()) {
                    // FIXED: Use correct container for visibility
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.emptyStateContainer.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerView.setVisibility(View.VISIBLE);
                    binding.emptyStateContainer.setVisibility(View.GONE);
                    playlistAdapter.submitList(playlists);
                }
            } else {
                // No data: show empty state
                binding.recyclerView.setVisibility(View.GONE);
                binding.emptyStateContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupRecyclerView() {
        playlistAdapter = new PlayerPostAdapter(this); // 'this' for OnMediaClickListener
        // If playlists need a custom layout (e.g., item_playlist.xml), create a PlaylistAdapter
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(playlistAdapter);
    }

    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        // TODO: Navigate to playlist details/player
        // Example: Toast.makeText(getContext(), "Opening playlist: " + mediaItem.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}