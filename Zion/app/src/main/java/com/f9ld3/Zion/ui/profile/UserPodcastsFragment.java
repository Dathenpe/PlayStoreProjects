package com.f9ld3.Zion.ui.profile;

import android.content.Intent; // Added
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // Added
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentUserPodcastsBinding;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;
import com.f9ld3.Zion.ui.player.PodcastPlayerActivity; // Added

import java.util.List;

public class UserPodcastsFragment extends Fragment {

    private FragmentUserPodcastsBinding binding;
    private ProfileViewModel profileViewModel;
    private PlayerPostAdapter podcastAdapter;
    private String userId;

    public static UserPodcastsFragment newInstance(String userId) {
        UserPodcastsFragment fragment = new UserPodcastsFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserPodcastsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // *** FIX: Get the ViewModel from the PARENT fragment (ChannelFragment) ***
        profileViewModel = new ViewModelProvider(requireParentFragment()).get(ProfileViewModel.class);
        setupRecyclerView();
        profileViewModel.fetchUserPodcasts(userId); // Fetch for the specific user
        profileViewModel.getUserPodcasts().observe(getViewLifecycleOwner(), this::updatePodcasts);
    }

    private void setupRecyclerView() {
        // Implement the click listener
        podcastAdapter = new PlayerPostAdapter(media -> {
            if (getContext() != null) {
                // Navigate to PodcastPlayerActivity
                Intent intent = new Intent(requireContext(), PodcastPlayerActivity.class);
                intent.putExtra(PodcastPlayerActivity.EXTRA_MEDIA_ITEM, media);
                startActivity(intent);
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(podcastAdapter);
    }

    private void updatePodcasts(List<PlayerMedia> podcasts) {
        // Ensure binding is not null
        if (binding == null) return;

        if (podcasts == null || podcasts.isEmpty()) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
            podcastAdapter.submitList(podcasts);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}