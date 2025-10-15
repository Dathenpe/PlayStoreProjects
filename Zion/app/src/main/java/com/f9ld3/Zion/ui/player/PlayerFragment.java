package com.f9ld3.Zion.ui.player;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentPlayerBinding;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter.OnMediaClickListener;
import com.f9ld3.Zion.ui.upload.UploadPodcastActivity;
import com.f9ld3.Zion.ui.upload.UploadVideoActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PlayerFragment extends Fragment implements OnMediaClickListener {

    private static final String TAG = "PlayerFragment";
    private FragmentPlayerBinding binding;
    private PlayerPostAdapter playerAdapter;

    private boolean isFabMenuOpen = false;
    private Animation fabOpen, fabClose, rotateForward, rotateBackward;

    private HistoryLogger historyLogger;

    public interface HistoryLogger {
        void logMediaView(PlayerMedia mediaItem);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HistoryLogger) {
            historyLogger = (HistoryLogger) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement PlayerFragment.HistoryLogger");
        }
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        PlayerViewModel playerViewModel =
                new ViewModelProvider(this).get(PlayerViewModel.class);

        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupAnimations();
        setupFabListeners();
        setupRecyclerView();

        playerViewModel.getMediaFeed().observe(getViewLifecycleOwner(), mediaList -> {
            if (mediaList != null) {
                playerAdapter.submitList(mediaList);
                Log.d(TAG, "New media feed of " + mediaList.size() + " items submitted.");
            }
        });

        return root;
    }

    private void setupAnimations() {
        fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_backward);
    }

    private void setupFabListeners() {
        binding.fabMainMenu.setOnClickListener(v -> animateFab());
        binding.fabUploadVideo.setOnClickListener(v -> handleUploadClick("video"));
        binding.fabUploadPodcast.setOnClickListener(v -> handleUploadClick("podcast"));
        binding.fabGoLive.setOnClickListener(v -> handleUploadClick("live"));
        binding.fabMenuOverlay.setOnClickListener(v -> {
            if (isFabMenuOpen) {
                animateFab();
            }
        });
    }

    private void handleUploadClick(String type) {
        if (!isFabMenuOpen) return;

        animateFab(); // Close the menu

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            // Use a short delay to allow the close animation to start
            binding.getRoot().postDelayed(() -> startUpload(type), 200);
        } else {
            Toast.makeText(getContext(), "You must be logged in to upload content.", Toast.LENGTH_LONG).show();
        }
    }


    private void startUpload(String type) {
        if (getContext() == null) return;
        switch (type) {
            case "video":
                startActivity(new Intent(requireContext(), UploadVideoActivity.class));
                break;
            case "podcast":
                startActivity(new Intent(requireContext(), UploadPodcastActivity.class));
                break;
            case "live":
                Toast.makeText(getContext(), "Live streaming is coming soon!", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    private void setupRecyclerView() {
        playerAdapter = new PlayerPostAdapter(this);
        binding.playerRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.playerRecyclerView.setAdapter(playerAdapter);
    }

    private void animateFab() {
        if (isFabMenuOpen) {
            // Close menu
            binding.fabMainMenu.startAnimation(rotateBackward);
            binding.fabMenuOverlay.setVisibility(View.GONE);

            binding.fabGoLive.startAnimation(fabClose);
            binding.fabUploadPodcast.startAnimation(fabClose);
            binding.fabUploadVideo.startAnimation(fabClose);

            // Hide labels as well
            binding.labelGoLive.setVisibility(View.GONE);
            binding.labelUploadPodcast.setVisibility(View.GONE);
            binding.labelUploadVideo.setVisibility(View.GONE);


            binding.fabGoLive.setClickable(false);
            binding.fabUploadPodcast.setClickable(false);
            binding.fabUploadVideo.setClickable(false);
            isFabMenuOpen = false;
        } else {
            // Open menu
            binding.fabMainMenu.startAnimation(rotateForward);
            binding.fabMenuOverlay.setVisibility(View.VISIBLE);

            // Make FABs and labels visible before animating
            binding.fabGoLive.setVisibility(View.VISIBLE);
            binding.fabUploadPodcast.setVisibility(View.VISIBLE);
            binding.fabUploadVideo.setVisibility(View.VISIBLE);
            binding.labelGoLive.setVisibility(View.VISIBLE);
            binding.labelUploadPodcast.setVisibility(View.VISIBLE);
            binding.labelUploadVideo.setVisibility(View.VISIBLE);

            binding.fabGoLive.startAnimation(fabOpen);
            binding.fabUploadPodcast.startAnimation(fabOpen);
            binding.fabUploadVideo.startAnimation(fabOpen);

            binding.fabGoLive.setClickable(true);
            binding.fabUploadPodcast.setClickable(true);
            binding.fabUploadVideo.setClickable(true);
            isFabMenuOpen = true;
        }
    }

    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        Log.i(TAG, "Media item clicked: " + mediaItem.getTitle());
        if (historyLogger != null) {
            historyLogger.logMediaView(mediaItem);
        }
        // TODO: Navigate to a dedicated player screen
        Toast.makeText(getContext(), "Playing: " + mediaItem.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        historyLogger = null;
    }
}
