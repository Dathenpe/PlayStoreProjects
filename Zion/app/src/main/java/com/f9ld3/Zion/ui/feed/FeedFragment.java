// main/java/com/f9ld3/Zion/ui/feed/FeedFragment.java
package com.f9ld3.Zion.ui.feed;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFeedBinding;
import com.f9ld3.Zion.ui.blog.CreatePostActivity;
import com.f9ld3.Zion.ui.common.SkeletonAdapter;
import com.f9ld3.Zion.ui.feed.PostLikeViewModel; // Import the correct ViewModel
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.Serializable; // Import Serializable


public class FeedFragment extends Fragment {

    private static final String TAG = "FeedFragment";
    private FragmentFeedBinding binding;
    private PostAdapter postAdapter;
    private SkeletonAdapter skeletonAdapter;
    private FeedViewModel feedViewModel;
    private PostLikeViewModel postLikeViewModel; // Use this instead


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        // Initialize PostLikeViewModel scoped to the Fragment/Activity
        postLikeViewModel = new ViewModelProvider(requireActivity()).get(PostLikeViewModel.class);

        binding = FragmentFeedBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView(); // Pass necessary context
        setupSkeletonView();

        // Observe loading state FIRST
        feedViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "isLoading changed: " + isLoading);
            // Show skeleton only on initial load if desired
            // boolean showSkeleton = isLoading != null && isLoading && postAdapter.getItemCount() == 0;
            boolean showSkeleton = isLoading != null && isLoading; // Simpler: show whenever loading
            binding.skeletonRecyclerView.setVisibility(showSkeleton ? View.VISIBLE : View.GONE);
            binding.feedRecyclerView.setVisibility(showSkeleton ? View.GONE : View.VISIBLE);
        });

        // Observe posts data
        feedViewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                postAdapter.submitList(posts);
                Log.d(TAG, "New list of " + posts.size() + " posts submitted to adapter.");
                // Hide skeleton if it was still visible after loading finished but before list submitted
                if (binding.skeletonRecyclerView.getVisibility() == View.VISIBLE && (feedViewModel.isLoading().getValue() == null || !feedViewModel.isLoading().getValue())) {
                    binding.skeletonRecyclerView.setVisibility(View.GONE);
                    binding.feedRecyclerView.setVisibility(View.VISIBLE);
                }
            } else {
                Log.w(TAG, "Received null post list from ViewModel."); // Log null list
                postAdapter.submitList(null); // Clear adapter if list is null
                binding.skeletonRecyclerView.setVisibility(View.GONE); // Ensure skeleton is hidden
                binding.feedRecyclerView.setVisibility(View.VISIBLE); // Show empty recycler
            }
        });

        binding.fabNewBlog.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && !user.isAnonymous()) {
                startActivity(new Intent(requireContext(), CreatePostActivity.class));
            } else {
                Toast.makeText(requireContext(), "Please log in to create a post.", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    private void setupRecyclerView() {
        // --- Pass LifecycleOwner (getViewLifecycleOwner()) and Activity (requireActivity()) ---
        postAdapter = new PostAdapter(new PostAdapter.OnPostClickListener() {
            @Override
            public void onPostItemClick(Post post) {
                Log.i(TAG, "Post item clicked: " + post.getId());
                Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getId());
                // Pass post data needed for detail view / comment posting notification
                intent.putExtra(PostDetailActivity.EXTRA_POST_DATA, (Serializable) post); // Cast to Serializable
                startActivity(intent);
            }

            @Override
            public void onLikeClick(Post post) {
                Log.i(TAG, "Like clicked for post: " + post.getId());
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && !user.isAnonymous()) {
                    // --- Use PostLikeViewModel ---
                    postLikeViewModel.toggleLike(post.getId(), post);
                } else {
                    Toast.makeText(getContext(), "Login to like posts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCommentClick(Post post) {
                Log.i(TAG, "Comment button clicked for post: " + post.getId());
                Intent intent = new Intent(requireContext(), CommentsActivity.class);
                intent.putExtra(CommentsActivity.EXTRA_POST_ID, post.getId());
                // Pass post data needed for comment posting notification
                intent.putExtra(CommentsActivity.EXTRA_POST_DATA, (Serializable) post); // Cast to Serializable
                startActivity(intent);
            }
        }, getViewLifecycleOwner(), requireActivity()); // Pass owner and activity
        // --- End Update ---

        binding.feedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.feedRecyclerView.setAdapter(postAdapter);
        // Optional: Add ItemAnimator for smoother updates
        // binding.feedRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void setupSkeletonView() {
        skeletonAdapter = new SkeletonAdapter(R.layout.item_feed_post_skeleton, 5); // Show 5 skeleton items
        binding.skeletonRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.skeletonRecyclerView.setAdapter(skeletonAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}