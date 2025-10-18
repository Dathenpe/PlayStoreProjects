// main/java/com/f9ld3/Zion/ui/feed/PostDetailActivity.java
package com.f9ld3.Zion.ui.feed;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager; // Import InputMethodManager
import android.content.Context; // Import Context
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityPostDetailBinding; // Use generated binding
import java.io.Serializable; // Import Serializable


public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "extra_post_id";
    public static final String EXTRA_POST_DATA = "extra_post_data"; // Key for Post object
    private static final String TAG = "PostDetailActivity";

    private ActivityPostDetailBinding binding;
    private CommentsViewModel commentsViewModel;
    private CommentAdapter commentAdapter;
    private FeedViewModel feedViewModel; // Use FeedViewModel if it handles single post loading
    private PostLikeViewModel postLikeViewModel; // For like button in included layout
    private String postId;
    private Post currentPostData; // To hold the Post object

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Post"); // Or set dynamically
        }

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        // Get the Post data passed via Intent
        currentPostData = (Post) getIntent().getSerializableExtra(EXTRA_POST_DATA); // Cast Serializable

        if (postId == null) {
            Log.e(TAG, "Post ID is null!");
            Toast.makeText(this, "Error loading post.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize ViewModels
        // Use requireActivity() if ViewModel should be shared with FeedFragment, 'this' otherwise
        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class); // Assuming FeedViewModel can load single post
        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        postLikeViewModel = new ViewModelProvider(this).get(PostLikeViewModel.class); // For like button


        setupRecyclerView();

        // Load details either from passed data or fetch if needed
        if (currentPostData != null && postId.equals(currentPostData.getId())) {
            Log.d(TAG,"Using Post data passed via Intent.");
            updatePostUi(currentPostData);
        } else {
            Log.d(TAG,"Post data not passed or mismatched, fetching post: " + postId);
            loadPostDetails(postId); // Fetch if not passed or mismatched
        }

        loadComments();
        setupCommentInput();
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter();
        // Access RecyclerView inside the included layout
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentsRecyclerView.setAdapter(commentAdapter);
        binding.commentsRecyclerView.setNestedScrollingEnabled(false); // If inside ScrollView
    }

    private void loadPostDetails(String id) {
        // --- Use FeedViewModel to fetch the specific post by postId ---
        // (You might need to add a getPostById method to FeedViewModel or use a dedicated PostDetailViewModel)
        /*
        feedViewModel.getPostById(id).observe(this, post -> { // Hypothetical method
            if (post != null) {
                currentPostData = post; // Store fetched data
                updatePostUi(post);
            } else {
                Log.e(TAG, "Failed to load post details for ID: " + id);
                Toast.makeText(this, "Failed to load post details", Toast.LENGTH_SHORT).show();
                // Optionally finish();
            }
        });
        */

        // Placeholder if direct fetching isn't implemented yet:
        Log.w(TAG, "Post fetching logic not fully implemented. Using placeholder UI.");
        Toast.makeText(this, "Loading post details...", Toast.LENGTH_SHORT).show();
        // You MUST update the included layout's views:
        binding.postLayoutContainer.authorName.setText("Loading...");
        binding.postLayoutContainer.postContent.setText("Loading post content...");
        binding.postLayoutContainer.postMediaPreview.setVisibility(View.GONE);
        binding.postLayoutContainer.likeCount.setText("0");
        binding.postLayoutContainer.commentCount.setText("0");

        // If using placeholder, set currentPostData to a dummy or null to prevent notification errors
        // currentPostData = null; // Or create a dummy Post object if needed later
    }

    // Method to update the included post layout
    private void updatePostUi(Post post) {
        if (post == null) return;

        // --- Access views within the included layout using the ID ---
        View postView = binding.postLayoutContainer.getRoot(); // Get the root of the included layout
        PostAdapter.PostViewHolder holder = new PostAdapter.PostViewHolder(postView); // Use ViewHolder temporarily for binding logic

        // Bind data using ViewHolder's logic (or replicate it here)
        // Note: This assumes PostViewHolder is accessible or you replicate its binding
        // holder.bind(post, /* listener (can be null/dummy here) */, postLikeViewModel, this);
        // OR, update manually:
        binding.postLayoutContainer.authorName.setText(post.getAuthorName());
        // You might need to call holder.setPostText() or replicate its logic
        binding.postLayoutContainer.postContent.setText(post.getTextContent()); // Simple text for now
        Glide.with(this)
                .load(post.getAuthorAvatarUrl())
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.postLayoutContainer.authorAvatar);

        String previewUrl = post.getThumbnailUrl();
        if (previewUrl != null && !previewUrl.isEmpty()) {
            binding.postLayoutContainer.postMediaPreview.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(previewUrl)
                    .placeholder(R.drawable.ic_placeholder_24dp)
                    .error(R.drawable.ic_placeholder_24dp)
                    .centerCrop()
                    .into(binding.postLayoutContainer.postMediaPreview);
        } else {
            binding.postLayoutContainer.postMediaPreview.setVisibility(View.GONE);
        }

        binding.postLayoutContainer.likeCount.setText(String.valueOf(post.getLikeCount()));
        binding.postLayoutContainer.commentCount.setText(String.valueOf(post.getCommentCount()));

        // --- Setup Like Button Interaction ---
        binding.postLayoutContainer.likeButton.setOnClickListener(v -> {
            // Use the postLikeViewModel to toggle the like state
            postLikeViewModel.toggleLike(post.getId(), post);
        });
        // Observe like state for the included layout's button
        postLikeViewModel.isLiked(post.getId()).observe(this, isLiked -> {
            if (isLiked != null && isLiked) {
                binding.postLayoutContainer.likeButton.setImageTintList(getColorStateList(R.color.error));
            } else {
                binding.postLayoutContainer.likeButton.setImageTintList(getColorStateList(R.color.gray_secondary));
            }
        });
        // Hide comment button in detail view? Or make it scroll to comments
        binding.postLayoutContainer.commentButton.setOnClickListener(v -> binding.commentsRecyclerView.smoothScrollToPosition(0)); // Example: scroll to top

    }


    private void loadComments() {
        commentsViewModel.getComments().observe(this, comments -> {
            if (comments != null) {
                commentAdapter.submitList(comments);
                Log.d(TAG, "Displaying " + comments.size() + " comments.");
                // Update comment count on the post view itself
                binding.postLayoutContainer.commentCount.setText(String.valueOf(comments.size()));
            } else {
                Log.w(TAG, "Received null comments list.");
                commentAdapter.submitList(null);
                binding.postLayoutContainer.commentCount.setText("0");
            }
        });
        commentsViewModel.loadComments(postId);
    }

    private void setupCommentInput() {
        binding.buttonPostComment.setOnClickListener(v -> {
            String text = binding.editTextComment.getText().toString().trim();
            if (!text.isEmpty()) {
                // --- Pass postData to postComment ---
                if (currentPostData != null) {
                    commentsViewModel.postComment(postId, text, currentPostData);
                    binding.editTextComment.setText(""); // Clear input
                    // Hide keyboard
                    hideKeyboard();
                    Log.d(TAG, "Posted comment: " + text);
                } else {
                    Log.e(TAG, "Cannot post comment: Post data is missing.");
                    Toast.makeText(this, "Error: Post data missing.", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "Cannot post empty comment", Toast.LENGTH_SHORT).show();
            }
        });
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Ensure Post class implements Serializable
    // public static class Post implements Serializable { ... }
}