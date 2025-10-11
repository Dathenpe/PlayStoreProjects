package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding; // Reusing generic list layout

/**
 * Fragment to display the list of users the current user is following.
 */
public class FollowingFragment extends Fragment {

    private FragmentFullPageListBinding binding;
    private ProfileViewModel profileViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFullPageListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Customize the empty state for this page (using template)
        binding.textPlaceholder.setText("You are not following anyone yet.");
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_person_24dp, 0, 0);

        // TODO: Setup RecyclerView Adapter and observe profileViewModel.getFollowing()
        // Example toggle (replace with real data observation):
        // profileViewModel.getFollowing().observe(getViewLifecycleOwner(), following -> {
        //     if (following == null || following.isEmpty()) {
        //         binding.recyclerView.setVisibility(View.GONE);
        //         binding.textPlaceholder.setVisibility(View.VISIBLE);
        //     } else {
        //         binding.recyclerView.setVisibility(View.VISIBLE);
        //         binding.textPlaceholder.setVisibility(View.GONE);
        //         // adapter.submitList(following);
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