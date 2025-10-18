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
import com.f9ld3.Zion.databinding.FragmentUserVideosBinding;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;
import com.f9ld3.Zion.ui.player.VideoPlayerActivity; // Added

import java.util.List;

public class UserVideosFragment extends Fragment {

    private FragmentUserVideosBinding binding;
    private ProfileViewModel profileViewModel;
    private PlayerPostAdapter videoAdapter;
    private String userId;

    public static UserVideosFragment newInstance(String userId) {
        UserVideosFragment fragment = new UserVideosFragment();
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
        binding = FragmentUserVideosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Use requireActivity() for potentially shared ViewModel
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        setupRecyclerView();
        profileViewModel.fetchUserVideos(userId); // Fetch for the specific user
        profileViewModel.getUserVideos().observe(getViewLifecycleOwner(), this::updateVideos);
    }

    private void setupRecyclerView() {
        // Implement the click listener
        videoAdapter = new PlayerPostAdapter(media -> {
            if (getContext() != null && media.getType() == PlayerMedia.TYPE_VIDEO) {
                // Navigate to VideoPlayerActivity
                Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_MEDIA_ITEM, media);
                startActivity(intent);
            } else if (getContext() != null) {
                Toast.makeText(getContext(), "Cannot play this media type", Toast.LENGTH_SHORT).show();
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(videoAdapter);
    }

    private void updateVideos(List<PlayerMedia> videos) {
        // Ensure binding is not null
        if (binding == null) return;

        if (videos == null || videos.isEmpty()) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
            videoAdapter.submitList(videos);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}