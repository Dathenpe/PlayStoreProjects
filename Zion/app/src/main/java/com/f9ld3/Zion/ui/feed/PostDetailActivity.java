// main/java/com/f9ld3/Zion/ui/feed/PostDetailActivity.java
package com.f9ld3.Zion.ui.feed;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2; // Import ViewPager2
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityPostDetailBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.dialogs.CustomInputDialogFragment;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.tabs.TabLayoutMediator; // Import TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp; // <<< Import Timestamp

import java.io.Serializable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.hdodenhof.circleimageview.CircleImageView; // If you use CircleImageView

public class PostDetailActivity extends AppCompatActivity implements CommentAdapter.CommentInteractionListener {

    public static final String EXTRA_POST_ID = "extra_post_id";
    public static final String EXTRA_POST_DATA = "extra_post_data"; // Key for Post object
    public static final String EXTRA_FOCUS_COMMENT_INPUT = "extra_focus_comment_input"; // New flag
    private static final String TAG = "PostDetailActivity";

    private ActivityPostDetailBinding binding;
    private CommentsViewModel commentsViewModel;
    private CommentAdapter commentAdapter;
    private PostLikeViewModel postLikeViewModel;
    private PollViewModel pollViewModel; // Added for Polls/Quizzes in detail view
    private String postId;
    private Post currentPostData; // To hold the Post object
    private MediaPagerAdapter mediaPagerAdapter; // Add adapter field

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Post"); // Set a default title
        }

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        currentPostData = (Post) getIntent().getSerializableExtra(EXTRA_POST_DATA);
        boolean shouldFocusComment = getIntent().getBooleanExtra(EXTRA_FOCUS_COMMENT_INPUT, false);

        if (postId == null || currentPostData == null) {
            Log.e(TAG, "Post ID or Post Data is null! Finishing activity.");
            Toast.makeText(this, "Error loading post.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize ViewModels
        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        postLikeViewModel = new ViewModelProvider(this).get(PostLikeViewModel.class);
        pollViewModel = new ViewModelProvider(this).get(PollViewModel.class); // Init PollViewModel

        setupRecyclerView(currentPostData.getAuthorUid());
        updatePostUi(currentPostData); // Call updatePostUi AFTER initializing ViewModels
        loadComments(); // loadComments needs postId, which is checked earlier
        setupCommentInput();
        observeViewModelMessages();

        // Focus comment input if requested by the intent
        if (shouldFocusComment) {
            focusCommentInput();
        }

        // Observe the original post for real-time updates (e.g., like count, poll votes)
        observePostUpdates();
    }

    private void observePostUpdates() {
        // Assuming you have a way to observe a single post, e.g., a LiveData in a ViewModel
        // For simplicity, re-fetch or use a dedicated method in FeedViewModel/PostViewModel if available
        // Example: A hypothetical LiveData<Post> getPostById(String postId) in a ViewModel
        /*
        someViewModel.getPostById(postId).observe(this, updatedPost -> {
            if (updatedPost != null) {
                currentPostData = updatedPost; // Keep local data fresh
                updatePostUi(updatedPost); // Re-render UI parts that change
            }
        });
        */
        // As a simpler alternative for now, rely on Firestore listeners within ViewModels to update LiveData used by UI elements (like count)
    }


    private void setupRecyclerView(String postAuthorUid) {
        commentAdapter = new CommentAdapter(this, postAuthorUid, this, this);
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentsRecyclerView.setAdapter(commentAdapter);
        binding.commentsRecyclerView.setNestedScrollingEnabled(false); // Important if inside ScrollView
    }

    // --- REVISED updatePostUi ---
    private void updatePostUi(Post post) {
        if (post == null || binding == null) {
            Log.w(TAG, "updatePostUi called with null post or binding");
            return;
        }
        currentPostData = post; // Update the member variable

        // --- Populate Header ---
        binding.authorName.setText(post.getAuthorName());

        // <<< FIX: Get milliseconds from Long object >>>
        Long postTime = post.getTimestamp(); // <-- CHANGED FROM Timestamp
        if (postTime != null && postTime > 0) { // <-- Check if > 0
            binding.postTimestampDetail.setText(DateUtils.getRelativeTimeSpanString(postTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)); // <-- Removed .toDate().getTime()
            binding.postTimestampDetail.setVisibility(View.VISIBLE);
        } else {
            binding.postTimestampDetail.setVisibility(View.GONE); // Hide if timestamp is null or 0
        }
        // <<< END FIX >>>

        setPostText(binding.postContent, post.getTextContent()); // Use helper for text/hashtags

        Glide.with(this)
                .load(post.getAuthorAvatarUrl())
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.authorAvatar);

        // --- Setup Content Based on Type ---
        switch (post.getPostType()) {
            case Post.TYPE_POLL:
            case Post.TYPE_QUIZ:
                binding.mediaPager.setVisibility(View.GONE);
                binding.tabIndicator.setVisibility(View.GONE);
                // Poll/Quiz specific UI setup (similar to PostAdapter)
                // This part is missing in activity_post_detail.xml, needs adding or handling differently
                // Example: setupPollInDetailView(post);
                Log.d(TAG, "Post type is Poll/Quiz - UI setup TBD in detail view.");
                break;
            case Post.TYPE_TEXT_MEDIA:
            default:
                // Poll/Quiz UI removal/hiding
                // Example: binding.pollContainerDetail.setVisibility(View.GONE);
                setupMediaPager(post);
                break;
        }

        // --- Handle Actions (Like Button) ---
        binding.likeButton.setOnClickListener(v -> {
            if (currentPostData != null) { // Ensure post data is available
                postLikeViewModel.toggleLike(currentPostData.getId(), currentPostData);
            }
        });

        // Observe like state
        postLikeViewModel.isLiked(post.getId()).observe(this, isLiked -> {
            if (binding == null) return;
            ColorStateList tint = ColorStateList.valueOf(
                    isLiked != null && isLiked
                            ? ContextCompat.getColor(this, R.color.teal) // Liked color (adjust R.color if needed)
                            : MaterialColors.getColor(binding.likeButton, com.google.android.material.R.attr.colorOnSurfaceVariant) // Default color
            );
            binding.likeButton.setImageTintList(tint);
        });

        // Display like count
        binding.likeCount.setText(formatCount(post.getLikeCount()));
        binding.likeCount.setVisibility(post.getLikeCount() > 0 ? View.VISIBLE : View.GONE);

        // Comment action button scrolls down or focuses input
        binding.commentActionButton.setOnClickListener(v -> {
            // Scroll to comments or focus input
            focusCommentInput(); // Or scroll: binding.scrollView.smoothScrollTo(...)
        });
        binding.commentActionCount.setText(formatCount(post.getCommentCount()));
        binding.commentActionCount.setVisibility(post.getCommentCount() > 0 ? View.VISIBLE : View.GONE);


        // Post options button
        binding.postOptionsButtonDetail.setOnClickListener(this::showPostOptionsMenu);
    }

    private String formatCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1_000_000) return String.format("%.1fk", count / 1000.0).replace(".0", "");
        return String.format("%.1fm", count / 1_000_000.0).replace(".0", "");
    }


    private void setupMediaPager(Post post) {
        if (post.getMediaItems() != null && !post.getMediaItems().isEmpty()) {
            mediaPagerAdapter = new MediaPagerAdapter(this, post.getMediaItems());
            binding.mediaPager.setAdapter(mediaPagerAdapter);

            // Dynamically set ViewPager height based on aspect ratio (e.g., 16:9 for first item)
            // This is a basic example; adjust as needed
            binding.mediaPager.post(() -> {
                int pagerWidth = binding.mediaPager.getWidth();
                if (pagerWidth > 0) {
                    // Assuming 16:9 aspect ratio for simplicity
                    int pagerHeight = (int) (pagerWidth * (9.0 / 16.0));
                    ViewGroup.LayoutParams params = binding.mediaPager.getLayoutParams();
                    params.height = pagerHeight;
                    binding.mediaPager.setLayoutParams(params);
                    binding.mediaPager.setVisibility(View.VISIBLE);
                } else {
                    binding.mediaPager.setVisibility(View.VISIBLE); // Fallback visibility
                }
            });


            // Setup indicator only if more than one item
            if (post.getMediaItems().size() > 1) {
                binding.tabIndicator.setVisibility(View.VISIBLE);
                new TabLayoutMediator(binding.tabIndicator, binding.mediaPager, (tab, position) -> {
                    // No text needed, just dots (style comes from tabBackground)
                }).attach();
            } else {
                binding.tabIndicator.setVisibility(View.GONE);
            }

        } else {
            binding.mediaPager.setVisibility(View.GONE);
            binding.tabIndicator.setVisibility(View.GONE);
        }
    }


    // --- Helper for setting post text with hashtag highlighting ---
    private void setPostText(TextView textView, String text) {
        if (text == null || text.isEmpty()) {
            textView.setVisibility(View.GONE);
            return;
        }
        textView.setVisibility(View.VISIBLE);
        SpannableString spannableString = new SpannableString(text);
        Pattern hashtagPattern = Pattern.compile("#(\\w+)");
        Matcher matcher = hashtagPattern.matcher(text);

        int hashtagColor = getColorFromAttr(com.google.android.material.R.attr.colorSecondary);

        while (matcher.find()) {
            spannableString.setSpan(new ForegroundColorSpan(hashtagColor),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Add ClickableSpan if needed
        }
        textView.setText(spannableString);
        // Add Linkify if needed
        // Linkify.addLinks(textView, Linkify.WEB_URLS);
        // textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // Helper to get color from theme attribute
    @ColorInt
    private int getColorFromAttr(@AttrRes int attrRes) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attrRes, typedValue, true);
        return typedValue.data;
    }

    private void loadComments() {
        // Observe top-level comments
        commentsViewModel.getComments().observe(this, comments -> {
            if (binding == null) return; // Check binding
            boolean isEmpty = comments == null || comments.isEmpty();
            binding.commentsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.emptyCommentsText.setVisibility(isEmpty ? View.VISIBLE : View.GONE); // Control empty state

            if (!isEmpty) {
                commentAdapter.submitList(comments);
                Log.d(TAG, "Displaying " + comments.size() + " top-level comments.");
            } else {
                commentAdapter.submitList(null);
                Log.w(TAG, "Received null or empty top-level comments list.");
            }
        });

        commentsViewModel.loadComments(postId); // Start loading top-level comments
    }

    private void observeViewModelMessages() {
        // Observe error/success messages from CommentsViewModel
        commentsViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                commentsViewModel.clearMessages();
            }
        });
        commentsViewModel.getSuccessMessage().observe(this, success -> {
            if (success != null) {
                Toast.makeText(this, success, Toast.LENGTH_SHORT).show();
                commentsViewModel.clearMessages();
                // Optional: Scroll to bottom after posting success
                if ("Comment posted.".equals(success) && binding != null) {
                    binding.commentsRecyclerView.postDelayed(() -> {
                        if (commentAdapter.getItemCount() > 0) {
                            binding.commentsRecyclerView.smoothScrollToPosition(commentAdapter.getItemCount() - 1);
                        }
                    }, 300);
                }
            }
        });
    }

    private void setupCommentInput() {
        binding.buttonPostComment.setOnClickListener(v -> postNewCommentOrReply());
    }

    private void postNewCommentOrReply() {
        String text = binding.editTextComment.getText().toString().trim();
        if (!text.isEmpty()) {
            if (currentPostData != null) {
                // Call new method signature, passing null for parentCommentId
                commentsViewModel.postCommentOrReply(
                        postId,
                        text,
                        null, // parentCommentId is null for top-level comments
                        currentPostData.getAuthorUid(),
                        currentPostData.getTextContent() != null && currentPostData.getTextContent().length() > 50
                                ? currentPostData.getTextContent().substring(0, 50) + "..."
                                : currentPostData.getTextContent() // Pass snippet
                );
                binding.editTextComment.setText(""); // Clear input
                hideKeyboard();
                Log.d(TAG, "Posted top-level comment: " + text);
            } else {
                Log.e(TAG, "Cannot post comment: Post data is missing.");
                Toast.makeText(this, "Error: Could not send comment.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Cannot post empty comment", Toast.LENGTH_SHORT).show();
        }
    }

    private void focusCommentInput() {
        if (binding == null) return;
        binding.editTextComment.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // Show keyboard with a slight delay to ensure layout is ready
            binding.editTextComment.postDelayed(() -> imm.showSoftInput(binding.editTextComment, InputMethodManager.SHOW_IMPLICIT), 200);
        }
    }

    // Helper to hide keyboard
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view == null && binding != null) {
            view = binding.editTextComment; // Fallback to EditText
        }
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) { // Add null check
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus(); // Clear focus after hiding
        }
    }

    private void showPostOptionsMenu(View anchorView) {
        if (currentPostData == null) return;
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenu().add("Share");
        popup.getMenu().add("Report");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(currentPostData.getAuthorUid())) {
            popup.getMenu().add("Delete");
        }

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Share".equals(title)) {
                sharePost(currentPostData);
            } else if ("Report".equals(title)) {
                reportPost(currentPostData);
            } else if ("Delete".equals(title)) {
                deletePost(currentPostData);
            } else {
                return false;
            }
            return true;
        });
        popup.show();
    }

    private void sharePost(Post post) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String shareText = post.getTextContent() != null ? post.getTextContent() : "Check out this post!";
        // Add deep link if available
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));
    }

    private void reportPost(Post post) {
        Toast.makeText(this, "Report functionality TBD", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Reporting post ID: " + post.getId());
    }

    private void deletePost(Post post) {
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
                Log.d(TAG, "Deleting post ID: " + post.getId());
                Toast.makeText(PostDetailActivity.this, "Delete functionality TBD", Toast.LENGTH_SHORT).show();
                // Example: someViewModel.deletePost(post.getId());
                // finish(); // Close activity after deletion
            }
            @Override
            public void onNegativeClick() {}
        });
        dialog.show(getSupportFragmentManager(), "DeletePostDialog");
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- Implementation of CommentInteractionListener ---

    @Override
    public void onReplyClicked(Comment comment) {
        Log.d(TAG, "Reply clicked. Opening replies for: " + comment.getId());
        Intent intent = new Intent(this, RepliesActivity.class);
        intent.putExtra(RepliesActivity.EXTRA_POST, (Serializable) currentPostData);
        intent.putExtra(RepliesActivity.EXTRA_COMMENT_ID, comment.getId());
        // Focus the input field in RepliesActivity
        intent.putExtra(RepliesActivity.EXTRA_SHOULD_FOCUS_REPLY, true);
        startActivity(intent);
    }

    @Override
    public void onDeleteClicked(Comment comment) {
        Log.d(TAG, "Delete clicked for comment: " + comment.getId());
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Delete Comment?",
                "Are you sure you want to permanently delete this comment?",
                "Delete",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                commentsViewModel.deleteComment(comment, currentPostData.getAuthorUid());
            }
            @Override
            public void onNegativeClick() { }
        });
        dialog.show(getSupportFragmentManager(), "DeleteCommentDialog");
    }

    @Override
    public void onReportClicked(Comment comment) {
        Log.d(TAG, "Report clicked for comment: " + comment.getId());
        CustomInputDialogFragment reportDialog = CustomInputDialogFragment.newInstance(
                "Report Comment",
                "Please provide a brief reason for reporting this comment (optional).",
                "Reason for reporting",
                "Report",
                "Cancel",
                false // Not password
        );
        reportDialog.setInputListener(reason -> {
            commentsViewModel.reportComment(comment, reason.isEmpty() ? "No reason provided" : reason);
        });
        reportDialog.show(getSupportFragmentManager(), "ReportCommentDialog");
    }

    @Override
    public void onViewRepliesClicked(Comment comment, CommentAdapter.CommentViewHolder holder) {
        Log.d(TAG, "View replies clicked. Opening replies for: " + comment.getId());
        Intent intent = new Intent(this, RepliesActivity.class);
        intent.putExtra(RepliesActivity.EXTRA_POST, (Serializable) currentPostData);
        intent.putExtra(RepliesActivity.EXTRA_COMMENT_ID, comment.getId());
        // Don't focus input field when just viewing
        intent.putExtra(RepliesActivity.EXTRA_SHOULD_FOCUS_REPLY, false);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null && binding.commentsRecyclerView != null) {
            binding.commentsRecyclerView.setAdapter(null); // Detach adapter
        }
        binding = null; // Clean up binding
    }
}