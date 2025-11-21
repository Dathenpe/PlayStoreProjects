package com.f9ld3.Zion.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.fragment.NavHostFragment;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentListNoToolbarBinding;
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
import java.util.List;

public class FollowedContentFragment extends Fragment implements PostAdapter.OnPostClickListener, PlayerPostAdapter.OnMediaClickListener {

    private static final String TAG = "FollowedContentFragment";
    private FragmentListNoToolbarBinding binding;

    private ProfileViewModel profileViewModel;
    private SearchAllAdapter contentAdapter;
    private PostLikeViewModel postLikeViewModel;
    private FollowViewModel followViewModel;

    // Track first data arrival to hide progress bar exactly once
    private boolean hasReceivedData = false;

    public FollowedContentFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListNoToolbarBinding.inflate(inflater, container, false);
        // Always show only progress bar on load
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.textPlaceholder.setVisibility(View.GONE);
        hasReceivedData = false; // Always reset for clean entry
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        postLikeViewModel = new ViewModelProvider(requireActivity()).get(PostLikeViewModel.class);
        followViewModel = new ViewModelProvider(requireActivity()).get(FollowViewModel.class);

        setupRecyclerView();

        binding.textPlaceholder.setText(getString(R.string.followed_content_empty_text));
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_feed_24dp, 0, 0);

        profileViewModel.getFollowedContent().observe(getViewLifecycleOwner(), content -> {
            if (binding == null) return;

            // Only hide progress bar after first live data
            if (!hasReceivedData) {
                hasReceivedData = true;
                binding.progressBar.setVisibility(View.GONE);
            }

            boolean isEmpty = content == null || content.isEmpty();

            binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.textPlaceholder.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

            if (!isEmpty) {
                contentAdapter.submitList(content);
            } else {
                contentAdapter.submitList(null);
            }
        });
    }

    private void setupRecyclerView() {
        contentAdapter = new SearchAllAdapter(this, this, postLikeViewModel, followViewModel, getViewLifecycleOwner(), requireActivity());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(contentAdapter);
    }

    // --- Listener Implementations ---
    @Override
    public void onPostItemClick(Post post) {
        if (getContext() == null || !isAdded()) return;
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getId());
        intent.putExtra(PostDetailActivity.EXTRA_POST_DATA, (Serializable) post);
        startActivity(intent);
    }

    @Override
    public void onLikeClick(Post post) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            postLikeViewModel.toggleLike(post.getId(), post);
        } else if (getContext() != null && isAdded()){
            Toast.makeText(getContext(), R.string.login_for_features, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDislikeClick(Post post) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            postLikeViewModel.toggleDislike(post.getId(), post);
        } else if (getContext() != null && isAdded()){
            Toast.makeText(getContext(), R.string.login_for_features, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCommentClick(Post post) {
        if (getContext() == null || !isAdded()) return;
        CommentsBottomSheet commentsSheet = CommentsBottomSheet.newInstance(post.getId(), post);
        commentsSheet.show(getParentFragmentManager(), CommentsBottomSheet.TAG);
    }

    @Override
    public void onOptionClick(Post post, View anchorView) {
        if (getContext() == null || !isAdded()) return;
        showPostOptionsMenu(anchorView, post);
    }

    @Override
    public void onAuthorClick(Post post) {
        if (post.getAuthorUid() != null && isAdded()) {
            Bundle args = new Bundle();
            args.putString("channelId", post.getAuthorUid());
            args.putString("channelName", post.getAuthorName());
            try {
                NavHostFragment.findNavController(this).navigate(R.id.navigation_channel, args);
            } catch (Exception e) {
                Log.e(TAG, "Navigation to channel failed", e);
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Could not navigate to profile.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

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

    private void showPostOptionsMenu(View anchorView, Post post) {
        if (getContext() == null || !isAdded()) return;
        android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), anchorView);
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
        com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment dialog =
                com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment.newInstance(
                        "Delete Post?",
                        "Are you sure you want to permanently delete this post?",
                        "Delete",
                        "Cancel"
                );
        dialog.setDialogListener(new com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment.DialogListener() {
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
        if (binding != null && binding.recyclerView != null) {
            binding.recyclerView.setAdapter(null);
        }
        binding = null;
        hasReceivedData = false; // Reset for next time
    }
}
