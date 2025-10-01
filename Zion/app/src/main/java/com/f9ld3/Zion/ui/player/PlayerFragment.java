package com.f9ld3.Zion.ui.player;

import android.content.Context;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PlayerFragment extends Fragment implements OnMediaClickListener {

    private static final String TAG = "PlayerFragment";
    private FragmentPlayerBinding binding;
    private PlayerPostAdapter playerAdapter;

    // FAB State and Animations
    private Boolean isFabMenuOpen = false;
    private Animation fabOpen, fabClose, rotateForward, rotateBackward;

    // Interface to communicate with host Activity for history logging
    private HistoryLogger historyLogger;

    /**
     * Interface for logging media view history in the host activity.
     */
    public interface HistoryLogger {
        void logMediaView(PlayerMedia mediaItem);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure the host activity implements the HistoryLogger interface
        if (context instanceof HistoryLogger) {
            historyLogger = (HistoryLogger) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement PlayerFragment.HistoryLogger");
        }
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Initialize ViewModel
        PlayerViewModel playerViewModel =
                new ViewModelProvider(this).get(PlayerViewModel.class);

        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 1. Setup Animations and FABs
        setupAnimations();
        setupFabListeners();

        // 2. Setup RecyclerView
        setupRecyclerView();

        // 3. Observe LiveData
        playerViewModel.getMediaFeed().observe(getViewLifecycleOwner(), mediaList -> {
            if (mediaList != null) {
                playerAdapter.submitList(mediaList);
                Log.d(TAG, "New media feed of " + mediaList.size() + " items submitted.");
            }
        });

        return root;
    }

    private void setupAnimations() {
        // Load animations from resources (R.anim.*)
        fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        fabOpen.setDuration(150);
        fabClose.setDuration(150);

        rotateForward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_backward);
        rotateForward.setDuration(150);
        rotateBackward.setDuration(150);
    }

    private void setupFabListeners() {
        binding.fabMainMenu.setOnClickListener(v -> animateFab());
        binding.fabUploadVideo.setOnClickListener(v -> handleUploadClick("video"));
        binding.fabUploadPodcast.setOnClickListener(v -> handleUploadClick("podcast"));
        binding.fabGoLive.setOnClickListener(v -> handleUploadClick("live"));

        // 🔥 NEW: Set click listener for the overlay to close the menu
        binding.fabMenuOverlay.setOnClickListener(v -> {
            if (isFabMenuOpen) {
                animateFab(); // Close the menu
            }
        });
    }

    /**
     * Checks for authenticated user before proceeding with upload.
     */
    private void handleUploadClick(String type) {
        if (isFabMenuOpen) {
            animateFab(); // Close the menu after click

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            // Check if user is logged in AND is NOT anonymous (i.e., a real user)
            if (user != null && !user.isAnonymous()) {
                startUpload(type);
            } else {
                // User is anonymous or null. Must log in to upload.
                Toast.makeText(getContext(), "You must be logged in to upload a " + type + ". Please check the Profile tab.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startUpload(String type) {
        // TODO: Implement actual upload/go-live navigation logic
        Toast.makeText(getContext(), "Starting " + type.toUpperCase() + " upload flow...", Toast.LENGTH_SHORT).show();
    }


    private void setupRecyclerView() {
        // Initialize the adapter, passing this fragment as the click listener
        playerAdapter = new PlayerPostAdapter(this);

        // Set up the RecyclerView
        binding.playerRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.playerRecyclerView.setAdapter(playerAdapter);
    }

    private void animateFab() {
        if (isFabMenuOpen) {
            // Closing the menu
            binding.fabMainMenu.startAnimation(rotateBackward);

            // 🔥 UPDATED: Hide the overlay immediately on closing
            binding.fabMenuOverlay.setVisibility(View.GONE);

            // Set animation listener to set visibility to GONE AFTER the animation finishes
            fabClose.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) { /* Not used */ }
                @Override
                public void onAnimationEnd(Animation animation) {
                    binding.fabGoLive.setVisibility(View.GONE);
                    binding.fabUploadPodcast.setVisibility(View.GONE);
                    binding.fabUploadVideo.setVisibility(View.GONE);
                }
                @Override
                public void onAnimationRepeat(Animation animation) { /* Not used */ }
            });

            // Start the closing animations
            binding.fabGoLive.startAnimation(fabClose);
            binding.fabUploadPodcast.startAnimation(fabClose);
            binding.fabUploadVideo.startAnimation(fabClose);

            binding.fabGoLive.setClickable(false);
            binding.fabUploadPodcast.setClickable(false);
            binding.fabUploadVideo.setClickable(false);
            isFabMenuOpen = false;

        } else {
            // Opening the menu
            binding.fabMainMenu.startAnimation(rotateForward);

            // 🔥 UPDATED: Show the overlay immediately on opening
            binding.fabMenuOverlay.setVisibility(View.VISIBLE);

            // 1. CRITICAL: Set FAB visibility to VISIBLE *before* starting the open animation
            binding.fabGoLive.setVisibility(View.VISIBLE);
            binding.fabUploadPodcast.setVisibility(View.VISIBLE);
            binding.fabUploadVideo.setVisibility(View.VISIBLE);

            // 2. Start the opening animations
            binding.fabGoLive.startAnimation(fabOpen);
            binding.fabUploadPodcast.startAnimation(fabOpen);
            binding.fabUploadVideo.startAnimation(fabOpen);

            // Ensure no lingering listener from the close sequence affects the open sequence
            fabOpen.setAnimationListener(null);

            binding.fabGoLive.setClickable(true);
            binding.fabUploadPodcast.setClickable(true);
            binding.fabUploadVideo.setClickable(true);

            isFabMenuOpen = true;
        }
    }

    // --- OnMediaClickListener Implementation ---
    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        Log.i(TAG, "Media item clicked: " + mediaItem.getTitle() + " (URL: " + mediaItem.getMediaUrl() + ")");

        // CRITICAL: Log the media view event to the host activity
        if (historyLogger != null) {
            historyLogger.logMediaView(mediaItem);
        }

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