package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding; // Reusing generic list layout
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment to display content (e.g., recent uploads) from a specific followed CHANNEL.
 */
public class FollowedContentFragment extends Fragment implements PlayerPostAdapter.OnMediaClickListener {

    private static final String ARG_FOLLOWED_CHANNEL_ID = "followed_channel_id";
    private String followedChannelId;

    private FragmentFullPageListBinding binding;
    private ProfileViewModel profileViewModel; // Or a dedicated ViewModel for channel content
    private PlayerPostAdapter mediaAdapter;

    public static FollowedContentFragment newInstance(String followedChannelId) {
        FollowedContentFragment fragment = new FollowedContentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FOLLOWED_CHANNEL_ID, followedChannelId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            followedChannelId = getArguments().getString(ARG_FOLLOWED_CHANNEL_ID);
        }
    }

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

        // Observe media for the specific followed channel
        profileViewModel.getMediaForChannel(followedChannelId).observe(getViewLifecycleOwner(), mediaList -> {
            if (mediaList != null && !mediaList.isEmpty()) {
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.textPlaceholder.setVisibility(View.GONE);
                mediaAdapter.submitList(mediaList);
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
                binding.textPlaceholder.setText("No content found for this channel."); // More specific empty state
            }
        });
    }

    private void setupRecyclerView() {
        mediaAdapter = new PlayerPostAdapter(this); // 'this' refers to OnMediaClickListener
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(mediaAdapter);
    }

    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        // TODO: Implement navigation to play the media item
        // Example: Toast.makeText(getContext(), "Playing media from channel: " + mediaItem.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}