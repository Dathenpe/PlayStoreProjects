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
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding;

/**
 * Fragment to display channels the user is following.
 */
public class FollowingChannelsFragment extends Fragment {

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
        binding.textPlaceholder.setText("You are not following any channels yet.");
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_tv_24dp, 0, 0);

        // TODO: Setup RecyclerView Adapter and observe profileViewModel.getFollowingChannels()
        profileViewModel.getFollowingChannels().observe(getViewLifecycleOwner(), channels -> {
            if (channels == null || channels.isEmpty()) {
                binding.recyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
            } else {
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.textPlaceholder.setVisibility(View.GONE);
                // adapter.submitList(channels);
            }
        });

        // Default: Show empty state if no observation yet
        binding.recyclerView.setVisibility(View.GONE);
        binding.textPlaceholder.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}