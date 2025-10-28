// main/java/com/f9ld3/Zion/ui/profile/UserPostsFragment.java
package com.f9ld3.Zion.ui.profile;

import android.content.Intent; // Import Intent
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu; // Import PopupMenu
import android.widget.Toast; // Import Toast
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment; // Import NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager;

import com.f9ld3.Zion.R; // Import R
import com.f9ld3.Zion.databinding.FragmentUserPostsBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment; // Import CustomAlertDialogFragment
import com.f9ld3.Zion.ui.feed.CommentsBottomSheet; // Import CommentsBottomSheet
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;
import com.f9ld3.Zion.ui.feed.PostDetailActivity; // Import PostDetailActivity
import com.f9ld3.Zion.ui.feed.PostLikeViewModel; // Import PostLikeViewModel
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser

import java.io.Serializable; // Import Serializable
import java.util.List;

// <<< Implement the full interface >>>
public class UserPostsFragment extends Fragment implements PostAdapter.OnPostClickListener {

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
        postAdapter = new PostAdapter(this, getViewLifecycleOwner(), requireActivity()); // Pass 'this' as the listener
        if (binding.recyclerView != null) { // Add null check
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.recyclerView.setAdapter(postAdapter);
        }
    }


    private void updatePosts(List<Post> posts) {
        if (binding == null) return; // Check if binding is null

        boolean isEmpty = posts == null || posts.isEmpty();

        if (binding.recyclerView != null) binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (binding.emptyState != null) binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);


        if (!isEmpty) {
            postAdapter.submitList(posts);
            Log.d(TAG, "Displaying " + posts.size() + " posts for user: " + userId);
        } else {
            Log.d(TAG, "No posts to display for user: " + userId);
            // Submit null to clear adapter even if it was previously populated
            if (postAdapter != null) {
                postAdapter.submitList(null);
            }
        }
    }

    // --- Implementation of PostAdapter.OnPostClickListener ---

    @Override
    public void onPostItemClick(Post post) {
        Log.i(TAG, "Post clicked: " + post.getId());
        if (isAdded() && getActivity() != null) { // Check attachment
            Intent intent = new Intent(requireContext(), PostDetailActivity.class);
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getId());
            intent.putExtra(PostDetailActivity.EXTRA_POST_DATA, (Serializable) post);
            startActivity(intent);
        }
    }

    @Override
    public void onLikeClick(Post post) {
        Log.i(TAG, "Like clicked on post: " + post.getId());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            postLikeViewModel.toggleLike(post.getId(), post);
        } else if (getContext() != null && isAdded()) { // Check attachment
            Toast.makeText(getContext(), "Login to like posts", Toast.LENGTH_SHORT).show();
        }
    }

    // <<< --- ADDED onDislikeClick --- >>>
    @Override
    public void onDislikeClick(Post post) {
        Log.i(TAG, "Dislike clicked for post: " + post.getId());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            postLikeViewModel.toggleDislike(post.getId(), post); // Call ViewModel method
        } else if (getContext() != null && isAdded()) { // Check attachment
            Toast.makeText(getContext(), R.string.login_for_features, Toast.LENGTH_SHORT).show();
        }
    }
    // <<< --- END of ADDED onDislikeClick --- >>>

    @Override
    public void onCommentClick(Post post) {
        Log.i(TAG, "Comment clicked on post: " + post.getId());
        if (isAdded() && getActivity() != null) { // Check attachment
            // Navigate to CommentsBottomSheet
            CommentsBottomSheet commentsSheet = CommentsBottomSheet.newInstance(post.getId(), post);
            commentsSheet.show(getParentFragmentManager(), CommentsBottomSheet.TAG);
        }
    }


    @Override
    public void onOptionClick(Post post, View anchorView) {
        Log.i(TAG, "Options clicked for post (in UserPosts): " + post.getId());
        if (isAdded() && getContext() != null) { // Check attachment
            showPostOptionsMenu(anchorView, post); // Call helper method
        }
    }


    @Override
    public void onAuthorClick(Post post) {
        // In a list of *another* user's posts, clicking the author might navigate
        // back to their profile, or do nothing.
        Log.i(TAG, "Author clicked (in UserPosts): " + post.getAuthorName());
        if (post.getAuthorUid() != null && !post.getAuthorUid().equals(userId) && isAdded()) {
            // Optional: Navigate if it's not the current user being viewed
            Bundle args = new Bundle();
            args.putString("channelId", post.getAuthorUid());
            args.putString("channelName", post.getAuthorName());
            try {
                NavHostFragment.findNavController(this).navigate(R.id.navigation_channel, args);
            } catch (Exception e) {
                Log.e(TAG, "Navigation failed", e);
            }
        }
    }

    // --- Helper for Options Menu ---
    private void showPostOptionsMenu(View anchorView, Post post) {
        if (getContext() == null || !isAdded()) return; // Check attachment
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenu().add("Share");
        popup.getMenu().add("Report"); // Allow reporting posts even on someone else's profile

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // Allow deletion only if the current user is the author of the post
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
        if (getContext() == null || !isAdded()) return; // Check attachment
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String shareText = post.getTextContent() != null ? post.getTextContent() : "Check out this post!";
        // Add deep link if available
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));
    }

    private void reportPost(Post post) {
        if (getContext() == null || !isAdded()) return; // Check attachment
        // TODO: Implement reporting logic
        Toast.makeText(getContext(), "Report functionality TBD", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Reporting post ID: " + post.getId());
    }

    private void deletePost(Post post) {
        if (getContext() == null || !isAdded()) return; // Check attachment
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Delete Post?",
                "Are you sure you want to permanently delete this post?",
                "Delete",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                // TODO: Call ViewModel method to delete the post from Firestore
                Log.d(TAG, "Deleting post ID: " + post.getId());
                if (isAdded() && getContext() != null) { // Check attachment before toast
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
        if (binding != null && binding.recyclerView != null) { // Add null check for recyclerView
            binding.recyclerView.setAdapter(null); // Detach adapter
        }
        binding = null;
    }
}