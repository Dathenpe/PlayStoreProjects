package com.f9ld3.Zion.ui.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentMediaOptionsBottomSheetBinding;
import com.f9ld3.Zion.ui.likes.LikesViewModel;
import com.f9ld3.Zion.utils.MediaDownloadManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.Serializable;

public class MediaOptionsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "MediaOptionsBottomSheet";
    private static final String ARG_MEDIA_ITEM = "media_item";

    private FragmentMediaOptionsBottomSheetBinding binding;
    private PlayerMedia mediaItem;
    private LikesViewModel likesViewModel;
    private MediaDownloadManager downloadManager;
    private boolean isLiked = false;

    public static MediaOptionsBottomSheet newInstance(PlayerMedia mediaItem) {
        MediaOptionsBottomSheet fragment = new MediaOptionsBottomSheet();
        Bundle args = new Bundle();
        // PlayerMedia needs to be Serializable to be passed this way
        args.putSerializable(ARG_MEDIA_ITEM, (Serializable) mediaItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mediaItem = (PlayerMedia) getArguments().getSerializable(ARG_MEDIA_ITEM);
        }
        likesViewModel = new ViewModelProvider(requireActivity()).get(LikesViewModel.class);
        downloadManager = new MediaDownloadManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMediaOptionsBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mediaItem == null) {
            dismiss();
            return;
        }

        setupUI();
        setupClickListeners();
        observeLikeStatus();
    }

    private void setupUI() {
        binding.mediaTitle.setText(mediaItem.getTitle());
        Glide.with(this)
                .load(mediaItem.getThumbnailUrl())
                .placeholder(R.drawable.ic_placeholder_24dp)
                .into(binding.mediaThumbnail);
    }

    private void observeLikeStatus() {
        likesViewModel.checkLikeStatus(mediaItem.getId());
        likesViewModel.isLiked().observe(getViewLifecycleOwner(), liked -> {
            isLiked = liked;
            updateLikeButtonState();
        });
    }

    private void updateLikeButtonState() {
        if (isLiked) {
            binding.optionLike.setText("Unlike");
            binding.optionLike.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like, 0, 0, 0);
            binding.optionLike.getCompoundDrawables()[0].setTint(getResources().getColor(R.color.error));
        } else {
            binding.optionLike.setText("Like");
            binding.optionLike.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like, 0, 0, 0);
            binding.optionLike.getCompoundDrawables()[0].setTintList(null); // Use default tint
        }
    }

    private void setupClickListeners() {
        binding.optionLike.setOnClickListener(v -> {
            if (isLiked) {
                likesViewModel.unlikeMedia(mediaItem.getId());
            } else {
                likesViewModel.likeMedia(mediaItem);
            }
        });

        binding.optionAddToPlaylist.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Add to Playlist coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Implement add to playlist logic
        });

        binding.optionDownload.setOnClickListener(v -> {
            downloadManager.downloadMedia(mediaItem);
            dismiss();
        });

        binding.optionShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, mediaItem.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this video: " + mediaItem.getTitle() + "\n" + mediaItem.getMediaUrl());
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            dismiss();
        });

        binding.optionReport.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Report functionality coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Implement report logic
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
