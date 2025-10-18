package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding;

public class LikesFragment extends Fragment {

    private FragmentFullPageListBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFullPageListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.toolbar.setTitle(R.string.liked_videos);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());


        ProfileViewModel profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Customize the empty state for this page (using template)
        binding.textPlaceholder.setText(getString(R.string.likes_empty_text));
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_like, 0, 0);

        // TODO: Setup RecyclerView Adapter and observe LiveData for liked media
        // profileViewModel.getUserLikes().observe(getViewLifecycleOwner(), likes -> {
        //     if (likes == null || likes.isEmpty()) {
        //         binding.recyclerView.setVisibility(View.GONE);
        //         binding.textPlaceholder.setVisibility(View.VISIBLE);
        //     } else {
        //         binding.recyclerView.setVisibility(View.VISIBLE);
        //         binding.textPlaceholder.setVisibility(View.GONE);
        //         // adapter.submitList(likes);
        //     }
        // });

        // Default: Show empty state
        binding.recyclerView.setVisibility(View.GONE);
        binding.textPlaceholder.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}