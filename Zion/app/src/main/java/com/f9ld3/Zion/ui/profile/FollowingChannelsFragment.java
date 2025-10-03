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
 * Fragment to display the list of channels the current user is following.
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

        // Customize the empty state for this page
        binding.textPlaceholder.setText(getString(R.string.channels_empty_text));
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_live_tv_24dp, 0, 0); // Placeholder icon

        // TODO: Setup RecyclerView Adapter and observe profileViewModel.getFollowingChannels()
        // This would require adding LiveData for following channels in ProfileViewModel and fetching logic.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}