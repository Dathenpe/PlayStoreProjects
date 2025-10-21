package com.f9ld3.Zion.ui.profile;

import android.content.Intent; // Import Intent
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // Import Toast
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.f9ld3.Zion.databinding.FragmentUserPostsBinding;
import com.f9ld3.Zion.ui.feed.CommentsBottomSheet; // Import CommentsBottomSheet
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;
import com.f9ld3.Zion.ui.feed.PostDetailActivity; // Import PostDetailActivity
import com.f9ld3.Zion.ui.feed.PostLikeViewModel; // Import PostLikeViewModel
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser

import java.io.Serializable; // Import Serializable
import java.util.List;

public class UserPostsFragment extends Fragment {

    private static final String TAG = "UserPostsFragment"; // Add TAG
    private FragmentUserPostsBinding binding;
    private ProfileViewModel profileViewModel;
    private PostAdapter postAdapter;
    private PostLikeViewModel postLikeViewModel; // Add PostLikeViewModel instance
    private String userId;

    public static UserPostsFragment newInstance(String userId) {
        UserPostsFragment fragment = new UserPostsFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
        } else {
            Log.e(TAG, "User ID not passed in arguments!");
            // Handle error, maybe navigate back or show a message
        }
        // Initialize PostLikeViewModel scoped to the Activity/Fragment lifecycle
        postLikeViewModel = new ViewModelProvider(requireActivity()).get(PostLikeViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserPostsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Use requireActivity() for ProfileViewModel if it's shared across ChannelFragment tabs
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        setupRecyclerView(); // Call setupRecyclerView after initializing postLikeViewModel

        if (userId != null) {
            profileViewModel.fetchUserPosts(userId);
            profileViewModel.getUserPosts().observe(getViewLifecycleOwner(), this::updatePosts);
        } else {
            // Handle case where userId is null after fragment creation
            updatePosts(null); // Show empty state
            Log.e(TAG, "User ID is null in onViewCreated, cannot fetch posts.");
        }
    }

    private void setupRecyclerView() {
        // Pass LifecycleOwner and Activity, Implement ALL required methods
        postAdapter = new PostAdapter(new PostAdapter.OnPostClickListener() {
            @Override
            public void onPostItemClick(Post post) {
                Log.i(TAG, "Post clicked: " + post.getId());
                Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getId());
                intent.putExtra(PostDetailActivity.EXTRA_POST_DATA, (Serializable) post);
                startActivity(intent);
            }

            @Override
            public void onLikeClick(Post post) {
                Log.i(TAG, "Like clicked on post: " + post.getId());
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && !user.isAnonymous()) {
                    postLikeViewModel.toggleLike(post.getId(), post);
                } else if (getContext() != null) {
                    Toast.makeText(getContext(), "Login to like posts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCommentClick(Post post) {
                Log.i(TAG, "Comment clicked on post: " + post.getId());
                // Navigate to CommentsBottomSheet
                // FIX: Use BottomSheetDialogFragment's show method
                CommentsBottomSheet commentsSheet = CommentsBottomSheet.newInstance(post.getId(), post);
                commentsSheet.show(getParentFragmentManager(), CommentsBottomSheet.TAG);
                // Intent intent = new Intent(requireContext(), CommentsBottomSheet.class); // This is incorrect for BottomSheetDialogFragment
                // intent.putExtra(CommentsBottomSheet.EXTRA_POST_ID, post.getId());
                // intent.putExtra(CommentsBottomSheet.EXTRA_POST_DATA, (Serializable) post);
                // startActivity(intent); // Do not start as activity
            }

            // *** ADDED missing onOptionClick method ***
            @Override
            public void onOptionClick(Post post, View anchorView) {
                Log.i(TAG, "Options clicked for post (in UserPosts): " + post.getId());
                // You can implement the options menu logic here if needed,
                // similar to FeedFragment or MyPostsFragment,
                // potentially reusing a helper method. For now, just logging.
                Toast.makeText(getContext(), "Options clicked", Toast.LENGTH_SHORT).show();
            }

            // *** ADDED missing onAuthorClick method ***
            @Override
            public void onAuthorClick(Post post) {
                // In a list of *another* user's posts, you might want to navigate
                // back to their profile, or do nothing.
                Log.i(TAG, "Author clicked (in UserPosts): " + post.getAuthorName());
                // Optional: Navigation logic if needed
            }

        }, getViewLifecycleOwner(), requireActivity()); // Pass LifecycleOwner and Activity

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(postAdapter);
    }


    private void updatePosts(List<Post> posts) {
        if (binding == null) return; // Check if binding is null

        if (posts == null || posts.isEmpty()) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
            Log.d(TAG, "No posts to display for user: " + userId);
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
            postAdapter.submitList(posts);
            Log.d(TAG, "Displaying " + posts.size() + " posts for user: " + userId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.recyclerView.setAdapter(null); // Detach adapter
        }
        binding = null;
    }
}