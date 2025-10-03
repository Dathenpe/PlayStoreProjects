package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.data.UserProfile;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class FollowingActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ProfileViewModel profileViewModel;
    private TextView emptyStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following_dynamic_tabbed);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.following);
        }

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        emptyStateTextView = findViewById(R.id.empty_state_text_view);

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // Observe the list of followed channels (users)
        profileViewModel.getFollowingChannels().observe(this, followedChannels -> {
            if (followedChannels != null && !followedChannels.isEmpty()) {
                emptyStateTextView.setVisibility(View.GONE);
                tabLayout.setVisibility(View.VISIBLE);
                viewPager.setVisibility(View.VISIBLE);
                setupViewPager(followedChannels);
            } else {
                emptyStateTextView.setVisibility(View.VISIBLE);
                tabLayout.setVisibility(View.GONE);
                viewPager.setVisibility(View.GONE);
                emptyStateTextView.setText(R.string.following_channels_empty_text);
            }
        });
    }

    private void setupViewPager(List<UserProfile> followedChannels) {
        DynamicViewPagerAdapter adapter = new DynamicViewPagerAdapter(getSupportFragmentManager(), getLifecycle(), followedChannels);
        viewPager.setAdapter(adapter);

        // Attach TabLayoutMediator to synchronize TabLayout and ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(followedChannels.get(position).getUsername()) // Tab title = channel name
        ).attach();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Adapter for dynamically generated fragments (channels only)
    static class DynamicViewPagerAdapter extends FragmentStateAdapter {
        private final List<UserProfile> followedChannels;

        public DynamicViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, List<UserProfile> followedChannels) {
            super(fragmentManager, lifecycle);
            this.followedChannels = followedChannels;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Pass the followed channel's (user's) ID to the fragment
            return FollowedContentFragment.newInstance(followedChannels.get(position).getUserId()); // Fixed: getUserId()
        }

        @Override
        public int getItemCount() {
            return followedChannels.size();
        }
    }
}