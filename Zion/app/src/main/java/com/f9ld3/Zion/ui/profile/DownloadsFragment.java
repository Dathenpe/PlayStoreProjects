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

// 🔥 CORRECTED IMPORT: Using FragmentDownloadsBinding from fragment_downloads.xml
import com.f9ld3.Zion.databinding.FragmentDownloadsBinding;
import com.f9ld3.Zion.R;

/**
 * Fragment to display the list of downloaded media items in a full-page view.
 */
public class DownloadsFragment extends Fragment {

    private FragmentDownloadsBinding binding;
    private ProfileViewModel profileViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDownloadsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Note: Using ViewModelProvider(requireActivity()) if you want the ViewModel scoped to MainActivity,
        // otherwise use ViewModelProvider(this) for a Fragment-scoped ViewModel.
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Setup the RecyclerView
        binding.downloadsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Replace with an actual Adapter (e.g., PlayerPostAdapter if you reuse it)

        // Observe LiveData and handle empty state
        profileViewModel.getUserDownloads().observe(getViewLifecycleOwner(), downloads -> {
            if (downloads == null || downloads.isEmpty()) {
                binding.downloadsRecyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
                // Ensure correct empty state text is set
                binding.textPlaceholder.setText(getString(R.string.downloads_empty_text));
            } else {
                binding.downloadsRecyclerView.setVisibility(View.VISIBLE);
                binding.textPlaceholder.setVisibility(View.GONE);
                // TODO: Update adapter list
                // downloadsAdapter.submitList(downloads);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}