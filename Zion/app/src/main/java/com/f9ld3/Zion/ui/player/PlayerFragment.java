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
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentPlayerBinding;
import com.f9ld3.Zion.ui.live.LiveChooserBottomSheet;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter.OnMediaClickListener;
import com.f9ld3.Zion.ui.upload.UploadPodcastActivity;
import com.f9ld3.Zion.ui.upload.UploadVideoActivity;
import com.f9ld3.Zion.ui.common.SkeletonAdapter; // Added
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PlayerFragment extends Fragment implements OnMediaClickListener {

    private static final String TAG = "PlayerFragment";
    private FragmentPlayerBinding binding;
    private PlayerPostAdapter playerAdapter;
    private SkeletonAdapter skeletonAdapter;
    private PlayerViewModel playerViewModel; // Make ViewModel a member variable

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
            // Optional: Don't throw exception if history logging isn't strictly required
            Log.w(TAG, context.toString() + " does not implement PlayerFragment.HistoryLogger");
            // throw new RuntimeException(context.toString() + " must implement PlayerFragment.HistoryLogger");
        }
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class); // Initialize here

        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupAnimations();
        setupFabListeners();
        setupRecyclerView();
        setupSkeletonView();

        // Observe loading state FIRST
        playerViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "isLoading changed: " + isLoading);
            if (isLoading != null && isLoading) { // Null check added
                binding.skeletonScrollView.setVisibility(View.VISIBLE);
                binding.contentScrollView.setVisibility(View.GONE);
            } else {
                binding.skeletonScrollView.setVisibility(View.GONE);
                binding.contentScrollView.setVisibility(View.VISIBLE);
            }
        });
        // Observe media feed data
        playerViewModel.getMediaFeed().observe(getViewLifecycleOwner(), mediaList -> {
            if (mediaList != null) {
                playerAdapter.submitList(mediaList);
                Log.d(TAG, "New media feed of " + mediaList.size() + " items submitted.");
            }
        });

        return root;
    }

    private void setupAnimations() {
        if(getContext() == null) return;
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
            // Delay starting the activity slightly to allow FAB animation to finish
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
                LiveChooserBottomSheet bottomSheet = new LiveChooserBottomSheet();
                bottomSheet.show(getParentFragmentManager(), LiveChooserBottomSheet.TAG);
                break;
        }
    }


    private void setupRecyclerView() {
        playerAdapter = new PlayerPostAdapter(this);
        binding.playerRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.playerRecyclerView.setAdapter(playerAdapter);
    }

    private void setupSkeletonView() {
        skeletonAdapter = new SkeletonAdapter(R.layout.item_video_m3_skeleton, 5); // Show 5 skeleton items
        binding.skeletonRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.skeletonRecyclerView.setAdapter(skeletonAdapter);
    }

    private void animateFab() {
        if (fabOpen == null) setupAnimations(); // Ensure animations are loaded

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

    // --- Updated onMediaClick ---
    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        Log.i(TAG, "Media item clicked: " + mediaItem.getTitle() + " Type: " + mediaItem.getType());
        if (historyLogger != null) {
            historyLogger.logMediaView(mediaItem);
        }

        Intent intent = null;
        if (mediaItem.getType() == PlayerMedia.TYPE_VIDEO) {
            // Create VideoPlayerActivity (using ExoPlayer recommended)
            intent = new Intent(requireContext(), VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_MEDIA_ITEM, mediaItem); // Pass the whole object
        } else if (mediaItem.getType() == PlayerMedia.TYPE_PODCAST_SINGLE) {
            // Create PodcastPlayerActivity (using ExoPlayer or MediaPlayer)
            intent = new Intent(requireContext(), PodcastPlayerActivity.class);
            intent.putExtra(PodcastPlayerActivity.EXTRA_MEDIA_ITEM, mediaItem); // Pass the whole object
        }

        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Cannot play this media type yet.", Toast.LENGTH_SHORT).show();
        }
    }
    // --- End Update ---

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