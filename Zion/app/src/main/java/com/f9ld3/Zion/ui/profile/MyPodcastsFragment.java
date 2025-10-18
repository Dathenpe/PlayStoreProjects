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
import com.f9ld3.Zion.databinding.FragmentListNoToolbarBinding;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MyPodcastsFragment extends Fragment implements PlayerPostAdapter.OnMediaClickListener {

    private FragmentListNoToolbarBinding binding;
    private ProfileViewModel profileViewModel;
    private PlayerPostAdapter podcastAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListNoToolbarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Explicitly fetch podcasts for the current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            profileViewModel.fetchUserPodcasts(currentUser.getUid());
        }

        setupRecyclerView();

        binding.textPlaceholder.setText(getString(R.string.podcasts_empty_text));
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_mic_24dp, 0, 0);

        profileViewModel.getUserPodcasts().observe(getViewLifecycleOwner(), podcasts -> {
            if (podcasts != null && !podcasts.isEmpty()) {
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.textPlaceholder.setVisibility(View.GONE);
                podcastAdapter.submitList(podcasts);
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupRecyclerView() {
        podcastAdapter = new PlayerPostAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(podcastAdapter);
    }

    @Override
    public void onMediaClick(PlayerMedia media) {
        // TODO: Implement navigation for the podcast.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}