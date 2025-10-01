package com.f9ld3.Zion.ui.feed;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.f9ld3.Zion.databinding.FragmentFeedBinding;



public class FeedFragment extends Fragment implements PostAdapter.OnPostClickListener {

    private static final String TAG = "FeedFragment";
    private FragmentFeedBinding binding;
    private PostAdapter postAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Initialize ViewModel
        FeedViewModel feedViewModel =
                new ViewModelProvider(this).get(FeedViewModel.class);

        binding = FragmentFeedBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // --- 1. Setup RecyclerView ---
        setupRecyclerView();

        // --- 2. Observe LiveData ---
        // Observe the list of posts from the ViewModel
        feedViewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                // Submit the new list to the adapter for display (using DiffUtil)
                postAdapter.submitList(posts);
                Log.d(TAG, "New list of " + posts.size() + " posts submitted to adapter.");
            }
        });

        // --- 3. Handle FAB Click (New Blog) ---
        binding.fabNewBlog.setOnClickListener(v -> {
            // TODO: Implement navigation to a new screen for creating a blog post
            Log.i(TAG, "FAB Clicked: Ready to create a new blog post.");
        });

        return root;
    }

    private void setupRecyclerView() {
        // Initialize the adapter, passing this fragment as the click listener
        postAdapter = new PostAdapter(this);

        // Set up the RecyclerView
        binding.feedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.feedRecyclerView.setAdapter(postAdapter);
    }

    // --- 4. Post Click Listener Implementation ---
    @Override
    public void onPostClick(Post post) {
        // TODO: Implement navigation to a detailed reader view for the blog post.
        Log.i(TAG, "Blog Post clicked: " + post.getTitle() + " (ID: " + post.getId() + ")");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}