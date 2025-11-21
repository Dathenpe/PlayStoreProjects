// main/java/com/f9ld3/Zion/ui/profile/FollowingFragment.java
package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.databinding.ActivityFollowingDynamicTabbedBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class FollowingFragment extends Fragment {

    private static final String TAG = "FollowingFragment";
    private ActivityFollowingDynamicTabbedBinding binding;
    private ProfileViewModel profileViewModel;
    private ViewPagerAdapter viewPagerAdapter;

    // Track if we've received the first data update
    private boolean hasReceivedData = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityFollowingDynamicTabbedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // *** FIX: Set initial loading state - show only progress bar ***
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tabLayout.setVisibility(View.GONE);
        binding.viewPager.setVisibility(View.GONE);
        binding.emptyStateTextView.setVisibility(View.GONE);
        hasReceivedData = false; // Always reset for clean entry

        binding.toolbar.setTitle(R.string.following);
        binding.toolbar.setNavigationOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).popBackStack();
            } catch (IllegalStateException e) {
                if (getActivity() != null) getActivity().onBackPressed();
            }
        });

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        setupViewPager();

        profileViewModel.getFollowing().observe(getViewLifecycleOwner(), followedUsers -> {
            if (binding == null) return;

            // *** FIX: Only proceed if data is not null (ignore initial null emission) ***
            if (followedUsers == null) return;

            // *** FIX: Hide progress bar only after first non-null data arrives ***
            if (!hasReceivedData) {
                hasReceivedData = true;
                binding.progressBar.setVisibility(View.GONE);
            }

            // *** FIX: Now update UI based on data ***
            if (followedUsers.isEmpty()) {
                binding.tabLayout.setVisibility(View.GONE);
                binding.viewPager.setVisibility(View.GONE);
                binding.emptyStateTextView.setVisibility(View.VISIBLE);
                binding.emptyStateTextView.setText(R.string.following_empty_text);
            } else {
                binding.tabLayout.setVisibility(View.VISIBLE);
                binding.viewPager.setVisibility(View.VISIBLE);
                binding.emptyStateTextView.setVisibility(View.GONE);
                viewPagerAdapter.setFollowingList(followedUsers);
            }
        });

        // Fetch data
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            profileViewModel.fetchFollowingChannels(currentUser.getUid());
            profileViewModel.fetchFollowingUsers(currentUser.getUid());
        } else {
            // No user logged in - hide progress and show empty state
            binding.progressBar.setVisibility(View.GONE);
            binding.emptyStateTextView.setVisibility(View.VISIBLE);
            binding.emptyStateTextView.setText(R.string.login_for_features);
            hasReceivedData = true;
        }
    }

    private void setupViewPager() {
        viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager(), getLifecycle());
        binding.viewPager.setAdapter(viewPagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setText(viewPagerAdapter.getPageTitle(position));
        }).attach();

        binding.tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        binding.tabLayout.setTabGravity(TabLayout.GRAVITY_START);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        hasReceivedData = false; // Reset for next time
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {
        private final List<UserProfile> followedUsers = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        public void setFollowingList(@NonNull List<UserProfile> users) {
            followedUsers.clear();
            followedUsers.addAll(users);
            notifyDataSetChanged();
        }

        @Nullable
        public UserProfile getUserAt(int position) {
            int userIndex = position - 1;
            if (userIndex >= 0 && userIndex < followedUsers.size()) {
                return followedUsers.get(userIndex);
            }
            return null;
        }

        public CharSequence getPageTitle(int position) {
            if (position == 0) return "All";
            UserProfile user = getUserAt(position);
            if (user != null) {
                String tabName = user.getAccountName() != null && !user.getAccountName().isEmpty()
                        ? user.getAccountName() : user.getUsername();
                return tabName != null ? tabName : "User";
            }
            return "Error";
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new FollowedContentFragment(); // "All" tab
            } else {
                UserProfile user = getUserAt(position);
                if (user != null) {
                    return UserPostsFragment.newInstance(user.getUserId(), true);
                } else {
                    return new Fragment();
                }
            }
        }

        @Override
        public int getItemCount() {
            return followedUsers.size() + 1;
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) return "all".hashCode();
            UserProfile user = getUserAt(position);
            return user != null ? user.getUserId().hashCode() : RecyclerView.NO_ID;
        }

        @Override
        public boolean containsItem(long itemId) {
            if (itemId == "all".hashCode()) return true;
            for (UserProfile user : followedUsers) {
                if (user.getUserId().hashCode() == itemId) return true;
            }
            return false;
        }
    }
}