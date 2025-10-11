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
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding;
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;

/**
 * Fragment to display content from followed users/channels.
 */
public class FollowedContentFragment extends Fragment implements PostAdapter.OnPostClickListener {

    private FragmentFullPageListBinding binding;

    private static final String ARG_FOLLOWED_CHANNEL_ID = "followed_channel_id";
    private String followedChannelId;
    private ProfileViewModel profileViewModel;
    private PostAdapter contentAdapter;


    public static FollowedContentFragment newInstance(String followedChannelId) {
        FollowedContentFragment fragment = new FollowedContentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FOLLOWED_CHANNEL_ID, followedChannelId);
        fragment.setArguments(args);
        return fragment;
    }

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

        setupRecyclerView();

        // Customize the empty state for this page (using template)
        binding.textPlaceholder.setText(getString(R.string.followed_content_empty_text));
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_feed_24dp, 0, 0);

        profileViewModel.getFollowedContent().observe(getViewLifecycleOwner(), content -> {
            if (content != null && !content.isEmpty()) {
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.textPlaceholder.setVisibility(View.GONE);
                contentAdapter.submitList(content);
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupRecyclerView() {
        contentAdapter = new PostAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(contentAdapter);
    }

    @Override
    public void onPostClick(Post post) {
        // TODO: Implement navigation to post details.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}