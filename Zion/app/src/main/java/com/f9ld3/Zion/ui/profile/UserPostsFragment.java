// main/java/com/f9ld3/Zion/ui/profile/UserPostsFragment.java
package com.f9ld3.Zion.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentUserPostsBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.feed.CommentsBottomSheet;
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;
import com.f9ld3.Zion.ui.feed.PostDetailActivity;
import com.f9ld3.Zion.ui.feed.PostLikeViewModel;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;
import com.f9ld3.Zion.ui.player.PodcastPlayerActivity;
import com.f9ld3.Zion.ui.player.VideoPlayerActivity;
import com.f9ld3.Zion.ui.search.SearchAllAdapter;
import com.f9ld3.Zion.ui.social.FollowViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserPostsFragment extends Fragment implements PostAdapter.OnPostClickListener, PlayerPostAdapter.OnMediaClickListener {

    private static final String TAG = "UserPostsFragment";
    private static final String ARG_USER_ID = "USER_ID";
    private static final String ARG_INCLUDE_MEDIA = "INCLUDE_MEDIA";

    private FragmentUserPostsBinding binding;
    private ProfileViewModel profileViewModel;
    private SearchAllAdapter adapter;
    private PostLikeViewModel postLikeViewModel;
    private FollowViewModel followViewModel;
    private String userId;
    private boolean includeMedia = false;

    // Track if we've received the first data update
    private boolean hasReceivedData = false;

    public static UserPostsFragment newInstance(String userId) {
        return newInstance(userId, false);
    }

    public static UserPostsFragment newInstance(String userId, boolean includeMedia) {
        UserPostsFragment fragment = new UserPostsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putBoolean(ARG_INCLUDE_MEDIA, includeMedia);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
            includeMedia = getArguments().getBoolean(ARG_INCLUDE_MEDIA);
        }
        postLikeViewModel = new ViewModelProvider(requireActivity()).get(PostLikeViewModel.class);
        followViewModel = new ViewModelProvider(requireActivity()).get(FollowViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserPostsBinding.inflate(inflater, container, false);

        // *** FIX: Set initial state - show progress, hide everything else ***
        if (binding.progressBar != null) binding.progressBar.setVisibility(View.VISIBLE);
        if (binding.recyclerView != null) binding.recyclerView.setVisibility(View.GONE);
        if (binding.emptyState != null) binding.emptyState.setVisibility(View.GONE);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Use 'this' to prevent shared viewmodel state collision across tabs in ViewPager
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupRecyclerView();

        if (userId != null) {
            if (includeMedia) {
                // *** FIX: Mixed mode (for Following tabs) - includes posts, videos, AND podcasts ***
                profileViewModel.fetchUserMixedContent(userId);
                profileViewModel.getUserMixedContent(userId).observe(getViewLifecycleOwner(), this::updateList);
            } else {
                // Post-only mode (for Channel "Posts" tab)
                profileViewModel.fetchUserPosts(userId);
                profileViewModel.getUserPosts().observe(getViewLifecycleOwner(), posts -> {
                    // Convert to Object list for SearchAllAdapter
                    updateList(new ArrayList<>(posts));
                });
            }
        } else {
            updateList(new ArrayList<>());
        }
    }

    private void setupRecyclerView() {
        adapter = new SearchAllAdapter(this, this, postLikeViewModel, followViewModel, getViewLifecycleOwner(), requireActivity());
        if (binding.recyclerView != null) {
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.recyclerView.setAdapter(adapter);
        }
    }

    private void updateList(List<Object> items) {
        if (binding == null) return;

        // *** FIX: Hide progress bar after first data arrives ***
        if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
        hasReceivedData = true;

        boolean isEmpty = items == null || items.isEmpty();
        if (binding.recyclerView != null) binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (binding.emptyState != null) binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (!isEmpty) {
            adapter.submitList(items);
            Log.d(TAG, "Updated list with " + items.size() + " items. IncludeMedia: " + includeMedia);
        } else {
            adapter.submitList(null);
            Log.d(TAG, "Empty list for user: " + userId);
        }
    }

    // --- Implement OnMediaClickListener ---
    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        if (getContext() == null || !isAdded()) return;
        Intent intent = null;
        if (mediaItem.getType() == PlayerMedia.TYPE_VIDEO) {
            intent = new Intent(requireContext(), VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_MEDIA_ITEM, mediaItem);
        } else if (mediaItem.getType() == PlayerMedia.TYPE_PODCAST_SINGLE) {
            intent = new Intent(requireContext(), PodcastPlayerActivity.class);
            intent.putExtra(PodcastPlayerActivity.EXTRA_MEDIA_ITEM, mediaItem);
        }
        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Cannot play this media type yet.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Post Listeners ---
    @Override
    public void onPostItemClick(Post post) {
        if (isAdded() && getActivity() != null) {
            Intent intent = new Intent(requireContext(), PostDetailActivity.class);
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getId());
            intent.putExtra(PostDetailActivity.EXTRA_POST_DATA, (Serializable) post);
            startActivity(intent);
        }
    }

    @Override
    public void onLikeClick(Post post) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            postLikeViewModel.toggleLike(post.getId(), post);
        } else if (getContext() != null && isAdded()) {
            Toast.makeText(getContext(), R.string.login_for_features, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDislikeClick(Post post) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            postLikeViewModel.toggleDislike(post.getId(), post);
        } else if (getContext() != null && isAdded()) {
            Toast.makeText(getContext(), R.string.login_for_features, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCommentClick(Post post) {
        if (isAdded() && getActivity() != null) {
            CommentsBottomSheet commentsSheet = CommentsBottomSheet.newInstance(post.getId(), post);
            commentsSheet.show(getParentFragmentManager(), CommentsBottomSheet.TAG);
        }
    }

    @Override
    public void onOptionClick(Post post, View anchorView) {
        if (isAdded() && getContext() != null) {
            showPostOptionsMenu(anchorView, post);
        }
    }

    @Override
    public void onAuthorClick(Post post) {
        // Optional navigation
    }

    private void showPostOptionsMenu(View anchorView, Post post) {
        if (getContext() == null || !isAdded()) return;
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenu().add("Share");
        popup.getMenu().add("Report");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(post.getAuthorUid())) {
            popup.getMenu().add("Delete");
        }
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Share".equals(title)) {
                sharePost(post);
            } else if ("Report".equals(title)) {
                reportPost(post);
            } else if ("Delete".equals(title)) {
                deletePost(post);
            } else {
                return false;
            }
            return true;
        });
        popup.show();
    }

    private void sharePost(Post post) {
        if (getContext() == null || !isAdded()) return;
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String shareText = post.getTextContent() != null ? post.getTextContent() : "Check out this post!";
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));
    }

    private void reportPost(Post post) {
        if (getContext() == null || !isAdded()) return;
        Toast.makeText(getContext(), "Report functionality TBD", Toast.LENGTH_SHORT).show();
    }

    private void deletePost(Post post) {
        if (getContext() == null || !isAdded()) return;
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Delete Post?",
                "Are you sure you want to permanently delete this post?",
                "Delete",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Delete functionality TBD", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onNegativeClick() {}
        });
        dialog.show(getParentFragmentManager(), "DeletePostDialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null && binding.recyclerView != null) binding.recyclerView.setAdapter(null);
        binding = null;
        hasReceivedData = false;
    }
}