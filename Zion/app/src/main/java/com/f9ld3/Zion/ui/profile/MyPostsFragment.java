package com.f9ld3.Zion.ui.profile;

import android.content.Intent; // Import Intent
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu; // <-- Import PopupMenu
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment; // <-- Import CustomAlertDialogFragment
import com.f9ld3.Zion.ui.feed.CommentsBottomSheet; // Import CommentsBottomSheet
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;
import com.f9ld3.Zion.ui.feed.PostDetailActivity; // Import PostDetailActivity
import com.f9ld3.Zion.ui.feed.PostLikeViewModel; // Import PostLikeViewModel
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser

import java.io.Serializable; // Import Serializable
import java.util.List;

public class MyPostsFragment extends Fragment implements PostAdapter.OnPostClickListener {

    private static final String TAG = "MyPostsFragment";
    private FragmentFullPageListBinding binding;
    private ProfileViewModel profileViewModel;
    private PostAdapter postAdapter;
    private PostLikeViewModel postLikeViewModel; // Add PostLikeViewModel instance

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFullPageListBinding.inflate(inflater, container, false);
        // Initialize PostLikeViewModel scoped to the Activity/Fragment lifecycle
        postLikeViewModel = new ViewModelProvider(requireActivity()).get(PostLikeViewModel.class); // Initialize here
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup Toolbar
        binding.toolbar.setTitle(R.string.my_posts);
        binding.toolbar.setNavigationOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).popBackStack();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error popping back stack: ", e);
                if (getActivity() != null) getActivity().finish(); // Fallback
            }
        });

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Explicitly fetch posts for the current user to prevent showing stale data
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            profileViewModel.fetchUserPosts(currentUser.getUid());
        } else {
            Log.w(TAG, "Current user is null, cannot fetch posts.");
            // Optionally show a message or hide the list
        }

        setupRecyclerView(); // Call setupRecyclerView after initializing postLikeViewModel

        binding.textPlaceholder.setText(getString(R.string.blogs_empty_text));
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_feed_24dp, 0, 0);

        profileViewModel.getUserPosts().observe(getViewLifecycleOwner(), posts -> {
            if (binding == null) return; // Check if binding is null (fragment destroyed)

            if (posts != null && !posts.isEmpty()) {
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.textPlaceholder.setVisibility(View.GONE);
                postAdapter.submitList(posts);
                Log.d(TAG, "Displaying " + posts.size() + " user posts.");
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
                Log.d(TAG, "No user posts to display.");
                postAdapter.submitList(null); // Clear adapter if list becomes empty/null
            }
        });
    }

    private void setupRecyclerView() {
        // Pass LifecycleOwner and Activity
        postAdapter = new PostAdapter(this, getViewLifecycleOwner(), requireActivity());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(postAdapter);
    }

    // --- Interface Method Implementations ---

    @Override
    public void onPostItemClick(Post post) {
        Log.i(TAG, "Post clicked from MyPosts: " + (post.getTextContent() != null ? post.getTextContent() : "No Text") + " (ID: " + post.getId() + ")");
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
        CommentsBottomSheet commentsSheet = CommentsBottomSheet.newInstance(post.getId(), post);
        commentsSheet.show(getParentFragmentManager(), CommentsBottomSheet.TAG);
    }

    // *** ADDED: Implementation for onOptionClick ***
    @Override
    public void onOptionClick(Post post, View anchorView) {
        Log.i(TAG, "Options clicked for post: " + post.getId());
        showPostOptionsMenu(anchorView, post);
    }

    // *** ADDED: Implementation for onAuthorClick (Can be empty if not needed here) ***
    @Override
    public void onAuthorClick(Post post) {
        // In "My Posts", clicking the author might not be needed,
        // but the method must be implemented.
        Log.i(TAG, "Author clicked (in MyPosts): " + post.getAuthorName());
        // Optional: Navigate to own profile? Or do nothing.
    }


    // --- Helper for Post Options Menu (Similar to FeedFragment) ---
    private void showPostOptionsMenu(View anchorView, Post post) {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenu().add("Share");
        // No "Report" option for own posts

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // Should always be true in "My Posts" fragment, but check anyway
        if (currentUser != null && currentUser.getUid().equals(post.getAuthorUid())) {
            popup.getMenu().add("Delete"); // Option to delete own post
        }

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Share".equals(title)) {
                sharePost(post);
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
        if (getContext() == null) return;
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String shareText = post.getTextContent() != null ? post.getTextContent() : "Check out my post!";
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));
    }

    private void deletePost(Post post) {
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
                // Example: profileViewModel.deleteUserPost(post.getId()); // Needs implementation
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
        if (binding != null && binding.recyclerView != null) { // Add null check for recyclerView
            binding.recyclerView.setAdapter(null); // Detach adapter
        }
        binding = null;
    }
}