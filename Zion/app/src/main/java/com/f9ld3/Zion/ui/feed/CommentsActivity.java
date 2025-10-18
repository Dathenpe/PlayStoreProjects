// main/java/com/f9ld3/Zion/ui/feed/CommentsActivity.java
package com.f9ld3.Zion.ui.feed;

import android.content.Context; // Import Context
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.MenuItem; // Import MenuItem
import android.view.View; // Import View
import android.view.inputmethod.InputMethodManager; // Import InputMethodManager
import android.widget.Toast; // Import Toast
import androidx.annotation.NonNull; // Import NonNull
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.f9ld3.Zion.databinding.ActivityCommentsBinding;
import java.io.Serializable; // Import Serializable


public class CommentsActivity extends AppCompatActivity {
    public static final String EXTRA_POST_ID = "extra_post_id";
    public static final String EXTRA_POST_DATA = "extra_post_data"; // Key for Post object
    private static final String TAG = "CommentsActivity"; // Added TAG

    private ActivityCommentsBinding binding;
    private CommentsViewModel viewModel;
    private CommentAdapter adapter;
    private String postId;
    private Post currentPostData; // To hold the Post object

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) { // Check if support action bar is not null
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        // Get the Post data passed via Intent
        currentPostData = (Post) getIntent().getSerializableExtra(EXTRA_POST_DATA); // Cast Serializable

        if (postId == null) {
            Log.e(TAG, "Post ID is null! Finishing activity."); // Log error
            Toast.makeText(this, "Error: Post ID missing.", Toast.LENGTH_SHORT).show(); // Show error to user
            finish();
            return;
        }
        // Optional: Check if currentPostData is null and try to fetch if needed
        if (currentPostData == null) {
            Log.w(TAG, "Post data not passed via intent. Notifications for new comments might be incomplete.");
            // Consider fetching post data here if essential for notifications
        }


        viewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        setupRecyclerView();

        viewModel.getComments().observe(this, comments -> {
            if (comments != null) {
                adapter.submitList(comments);
                if (!comments.isEmpty()) {
                    // Scroll to bottom only if needed (e.g., after posting)
                    // binding.commentsRecyclerView.scrollToPosition(comments.size() - 1);
                    Log.d(TAG, "Displaying " + comments.size() + " comments."); // Log count
                } else {
                    Log.d(TAG, "No comments to display.");
                }
            } else {
                Log.w(TAG, "Received null comments list.");
                adapter.submitList(null); // Clear adapter
            }
        });
        viewModel.loadComments(postId); // Start loading comments

        binding.buttonPostComment.setOnClickListener(v -> postNewComment());
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter();
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentsRecyclerView.setAdapter(adapter);
    }

    private void postNewComment() {
        String text = binding.editTextComment.getText().toString().trim();
        if (!text.isEmpty()) {
            // Pass postData for notifications
            if (currentPostData != null) {
                viewModel.postComment(postId, text, currentPostData);
                binding.editTextComment.setText(""); // Clear input
                hideKeyboard(); // Hide keyboard after posting
                Log.d(TAG, "Posted comment: " + text); // Log post action
                // Optional: Scroll to bottom after a short delay to allow list update
                binding.commentsRecyclerView.postDelayed(() -> {
                    if (adapter.getItemCount() > 0) {
                        binding.commentsRecyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                }, 300);
            } else {
                Log.e(TAG, "Cannot post comment: Post data is missing.");
                Toast.makeText(this, "Error: Could not send comment.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Cannot post empty comment", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper to hide keyboard
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { // Use onOptionsItemSelected for toolbar back button
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Ensure Post class implements Serializable
    // public static class Post implements Serializable { ... }
}