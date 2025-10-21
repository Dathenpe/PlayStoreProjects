// main/java/com/f9ld3/Zion/ui/feed/FeedFragment.java
package com.f9ld3.Zion.ui.feed;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem; // Added for Menu
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu; // Added for PopupMenu
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment; // Added for navigation
import androidx.recyclerview.widget.LinearLayoutManager;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFeedBinding;
import com.f9ld3.Zion.ui.blog.CreatePostActivity;
import com.f9ld3.Zion.ui.common.SkeletonAdapter;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.Serializable;

public class FeedFragment extends Fragment {

    private static final String TAG = "FeedFragment";
    private FragmentFeedBinding binding;
    private PostAdapter postAdapter;
    private SkeletonAdapter skeletonAdapter;
    private FeedViewModel feedViewModel;
    private PostLikeViewModel postLikeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        postLikeViewModel = new ViewModelProvider(requireActivity()).get(PostLikeViewModel.class);

        binding = FragmentFeedBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();
        setupSkeletonView();

        feedViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "isLoading changed: " + isLoading);
            boolean showSkeleton = isLoading != null && isLoading;
            binding.skeletonRecyclerView.setVisibility(showSkeleton ? View.VISIBLE : View.GONE);
            binding.feedRecyclerView.setVisibility(showSkeleton ? View.GONE : View.VISIBLE);
        });

        feedViewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                postAdapter.submitList(posts);
                Log.d(TAG, "New list of " + posts.size() + " posts submitted to adapter.");
                // Only hide skeleton if it's currently visible AND loading is finished
                if (binding.skeletonRecyclerView.getVisibility() == View.VISIBLE && (feedViewModel.isLoading().getValue() == null || !feedViewModel.isLoading().getValue())) {
                    binding.skeletonRecyclerView.setVisibility(View.GONE);
                    binding.feedRecyclerView.setVisibility(View.VISIBLE);
                }
            } else {
                Log.w(TAG, "Received null post list from ViewModel.");
                postAdapter.submitList(null); // Clear adapter
                // Ensure feed is visible even if empty (placeholder handled by adapter potentially)
                binding.skeletonRecyclerView.setVisibility(View.GONE);
                binding.feedRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        binding.fabNewBlog.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && !user.isAnonymous()) {
                startActivity(new Intent(requireContext(), CreatePostActivity.class));
            } else {
                Toast.makeText(requireContext(), "Please log in to create a post.", Toast.LENGTH_SHORT).show();
                // Optional: Navigate to login screen
            }
        });

        return root;
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(new PostAdapter.OnPostClickListener() {
            @Override
            public void onPostItemClick(Post post) {
                Log.i(TAG, "Post item clicked: " + post.getId());
                navigateToPostDetail(post);
            }

            @Override
            public void onLikeClick(Post post) {
                Log.i(TAG, "Like clicked for post: " + post.getId());
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && !user.isAnonymous()) {
                    postLikeViewModel.toggleLike(post.getId(), post);
                } else {
                    Toast.makeText(getContext(), R.string.login_for_features, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCommentClick(Post post) {
                Log.i(TAG, "Comment clicked for post: " + post.getId() + " - showing bottom sheet.");
                CommentsBottomSheet commentsSheet = CommentsBottomSheet.newInstance(post.getId(), post);
                commentsSheet.show(getParentFragmentManager(), CommentsBottomSheet.TAG);
            }

            @Override
            public void onAuthorClick(Post post) {
                Log.i(TAG, "Author clicked: " + post.getAuthorName() + " (ID: " + post.getAuthorUid() + ")");
                if (post.getAuthorUid() != null) {
                    Bundle args = new Bundle();
                    args.putString("channelId", post.getAuthorUid());
                    args.putString("channelName", post.getAuthorName()); // Pass name for title
                    try {
                        NavHostFragment.findNavController(FeedFragment.this)
                                .navigate(R.id.navigation_channel, args);
                    } catch (Exception e) {
                        Log.e(TAG, "Navigation to channel failed", e);
                    }
                }
            }

            @Override
            public void onOptionClick(Post post, View anchorView) {
                Log.i(TAG, "Options clicked for post: " + post.getId());
                showPostOptionsMenu(anchorView, post);
            }

        }, getViewLifecycleOwner(), requireActivity());

        binding.feedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.feedRecyclerView.setAdapter(postAdapter);
    }

    private void setupSkeletonView() {
        // Use the improved skeleton layout
        skeletonAdapter = new SkeletonAdapter(R.layout.item_feed_post_skeleton, 5); // Assuming item_feed_post_skeleton exists
        binding.skeletonRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.skeletonRecyclerView.setAdapter(skeletonAdapter);
    }

    private void navigateToPostDetail(Post post) {
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getId());
        intent.putExtra(PostDetailActivity.EXTRA_POST_DATA, (Serializable) post);
        // Optionally focus comment input directly
        // intent.putExtra(PostDetailActivity.EXTRA_FOCUS_COMMENT_INPUT, true);
        startActivity(intent);
    }


    private void showPostOptionsMenu(View anchorView, Post post) {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        // Inflate a menu resource or add items dynamically
        popup.getMenu().add("Share");
        popup.getMenu().add("Report");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(post.getAuthorUid())) {
            popup.getMenu().add("Delete"); // Option to delete own post
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

    // --- Action Handlers for Options Menu ---

    private void sharePost(Post post) {
        if (getContext() == null) return;
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        // Add content to share (e.g., text and a link)
        String shareText = post.getTextContent() != null ? post.getTextContent() : "Check out this post!";
        // You might want a deep link URL for the post here
        // shareText += "\n\n[Link to Post]";
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void reportPost(Post post) {
        // Implement reporting logic (e.g., show dialog, send report to backend)
        Toast.makeText(getContext(), "Report functionality TBD", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Reporting post ID: " + post.getId());
        // You could reuse the CustomInputDialogFragment here for the reason
    }

    private void deletePost(Post post) {
        // Show confirmation dialog before deleting
        if (getContext() == null) return;
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Delete Post?",
                "Are you sure you want to permanently delete this post?",
                "Delete",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                // Call ViewModel method to delete the post from Firestore
                // feedViewModel.deletePost(post.getId()); // Needs implementation in ViewModel
                Log.d(TAG, "Deleting post ID: " + post.getId());
                Toast.makeText(getContext(), "Delete functionality TBD", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNegativeClick() {}
        });
        dialog.show(getParentFragmentManager(), "DeletePostDialog");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.feedRecyclerView.setAdapter(null); // Detach adapter
        binding.skeletonRecyclerView.setAdapter(null); // Detach skeleton adapter
        binding = null;
    }
}