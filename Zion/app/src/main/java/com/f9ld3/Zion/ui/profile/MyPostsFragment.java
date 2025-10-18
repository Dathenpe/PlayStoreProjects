package com.f9ld3.Zion.ui.profile;

import android.content.Intent; // Import Intent
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding;
import com.f9ld3.Zion.ui.feed.CommentsActivity; // Import CommentsActivity
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;
import com.f9ld3.Zion.ui.feed.PostDetailActivity; // Import PostDetailActivity
import com.f9ld3.Zion.ui.feed.PostLikeViewModel; // Import PostLikeViewModel
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.Serializable; // Import Serializable

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
        // *** FIX: Pass LifecycleOwner (getViewLifecycleOwner()) and Activity (requireActivity()) ***
        postAdapter = new PostAdapter(this, getViewLifecycleOwner(), requireActivity()); //
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(postAdapter);
    }

    // *** FIX: Renamed method to match the interface ***
    @Override
    public void onPostItemClick(Post post) { //
        Log.i(TAG, "Post clicked from MyPosts: " + (post.getTextContent() != null ? post.getTextContent() : "No Text") + " (ID: " + post.getId() + ")");
        // Navigate to PostDetailActivity
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
            // Use the PostLikeViewModel to handle the like action
            postLikeViewModel.toggleLike(post.getId(), post); //
            // Toast is now less necessary as the UI should update via observer
            // Toast.makeText(getContext(), "Toggled like!", Toast.LENGTH_SHORT).show();
        } else if (getContext() != null) {
            Toast.makeText(getContext(), "Login to like posts", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCommentClick(Post post) {
        Log.i(TAG, "Comment clicked on post: " + post.getId());
        // Navigate to CommentsActivity
        Intent intent = new Intent(requireContext(), CommentsActivity.class);
        intent.putExtra(CommentsActivity.EXTRA_POST_ID, post.getId());
        intent.putExtra(CommentsActivity.EXTRA_POST_DATA, (Serializable) post);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setAdapter(null); // Detach adapter
        binding = null;
    }
}