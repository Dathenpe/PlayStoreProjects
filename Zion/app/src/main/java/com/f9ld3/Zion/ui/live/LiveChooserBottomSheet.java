package com.f9ld3.Zion.ui.live;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.f9ld3.Zion.databinding.FragmentLiveChooserBottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LiveChooserBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "LiveChooserBottomSheet";
    private FragmentLiveChooserBottomSheetBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLiveChooserBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.optionLiveVideo.setOnClickListener(v -> handleSelection("live_video"));
        binding.optionLivePodcast.setOnClickListener(v -> handleSelection("live_podcast"));
    }

    private void handleSelection(String type) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            Intent intent = null;
            if ("live_video".equals(type)) {
                intent = new Intent(requireContext(), GoLiveActivity.class);
            } else if ("live_podcast".equals(type)) {
                intent = new Intent(requireContext(), GoLivePodcastActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
            }
        } else {
            Toast.makeText(getContext(), "You must be logged in to go live.", Toast.LENGTH_LONG).show();
        }
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}