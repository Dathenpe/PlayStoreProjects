// main/java/com/f9ld3/Zion/ui/feed/RepliesActivity.java
package com.f9ld3.Zion.ui.feed;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityRepliesBinding;
import com.f9ld3.Zion.databinding.ItemFeedPostBinding; // Import the included post binding
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.dialogs.CustomInputDialogFragment;
import com.google.android.material.color.MaterialColors;

import java.io.Serializable;

public class RepliesActivity extends AppCompatActivity implements CommentAdapter.CommentInteractionListener {

    public static final String EXTRA_POST = "extra_post_data";
    public static final String EXTRA_COMMENT_ID = "extra_comment_id";
    public static final String EXTRA_SHOULD_FOCUS_REPLY = "extra_focus_reply";

    private static final String TAG = "RepliesActivity";

    private ActivityRepliesBinding binding;
    private ItemFeedPostBinding postBinding; // Binding for the included post layout
    private CommentsViewModel commentsViewModel;
    private PostLikeViewModel postLikeViewModel;
    private CommentAdapter commentAdapter;

    private String postId;
    private String parentCommentId;
    private String postAuthorUid;
    private Comment parentComment;
    private Post currentPostData;
    private String postTextSnippet; // <-- Store snippet

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRepliesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Replies");
        }

        currentPostData = (Post) getIntent().getSerializableExtra(EXTRA_POST);
        parentCommentId = getIntent().getStringExtra(EXTRA_COMMENT_ID);
        boolean shouldFocusReply = getIntent().getBooleanExtra(EXTRA_SHOULD_FOCUS_REPLY, false);

        if (currentPostData == null || parentCommentId == null) {
            Log.e(TAG, "Missing data! Post or CommentID is null.");
            Toast.makeText(this, "Error loading replies.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        postId = currentPostData.getId();
        postAuthorUid = currentPostData.getAuthorUid();
        // --- MODIFIED ---
        // Create and store the snippet
        postTextSnippet = (currentPostData.getTextContent() != null && currentPostData.getTextContent().length() > 50)
                ? currentPostData.getTextContent().substring(0, 50) + "..."
                : currentPostData.getTextContent();
        // --- END MODIFIED ---

        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        postLikeViewModel = new ViewModelProvider(this).get(PostLikeViewModel.class);

        setupRecyclerView();
        setupObservers();
        setupClickListeners();
        // Initial load of the parent comment and its replies
        commentsViewModel.loadCommentThread(postId, parentCommentId);

        if (shouldFocusReply) {
            focusReplyInput(null);
        }
    }

    private void updatePostUi(Post post) {
        if (post == null || postBinding == null) return;

        // Use the PostViewHolder to bind the post inside the included layout
        PollViewModel pollViewModel = new ViewModelProvider(this).get(PollViewModel.class);
        PostAdapter.PostViewHolder holder = new PostAdapter.PostViewHolder(
                postBinding.getRoot(),
                null, // Pass null for OnPostClickListener
                postLikeViewModel,
                pollViewModel,
                this
        );

        holder.bind(post);

        // --- Customizations for RepliesActivity ---

        // Disable navigation and options on the contextual post view
        postBinding.getRoot().setOnClickListener(null);
        postBinding.authorAvatar.setOnClickListener(null);
        postBinding.authorName.setOnClickListener(null);
        postBinding.postOptionsButton.setVisibility(View.GONE);
        postBinding.commentButton.setOnClickListener(null); // Disable comment button on post itself
        postBinding.commentCount.setVisibility(View.GONE); // Hide post comment count

        // Manually re-observe like state and update UI (as the ViewHolder's observer might not survive Activity lifecycle)
        postLikeViewModel.isLiked(post.getId()).observe(this, isLiked -> {
            if (postBinding == null) return;
            ColorStateList likedTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.teal));
            ColorStateList defaultTint = ColorStateList.valueOf(MaterialColors.getColor(postBinding.likeButton, com.google.android.material.R.attr.colorOnSurfaceVariant));
            postBinding.likeButton.setImageTintList(Boolean.TRUE.equals(isLiked) ? likedTint : defaultTint);
        });
    }

    private void setupRecyclerView() {
        // --- MODIFIED CALL ---
        // Pass the full currentPostData object to the adapter
        commentAdapter = new CommentAdapter(this, currentPostData, this, this);
        // --- END MODIFIED ---
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentsRecyclerView.setAdapter(commentAdapter);
    }

    private void setupObservers() {
        commentsViewModel.getParentComment().observe(this, comment -> {
            if (binding == null) return;
            this.parentComment = comment;

            // --- FIX: Check if parent comment is a reply and hide original post if true ---
            // The logic: if the parent comment itself has a parent ID, it's a deep thread, so hide the original post.
            boolean isReplyToReply = comment != null && comment.isReply();


            if (comment != null) {
                View parentCommentView = getLayoutInflater().inflate(R.layout.item_comment, binding.parentCommentContainer, false);
                CommentAdapter.CommentViewHolder holder = new CommentAdapter.CommentViewHolder(parentCommentView, this, postAuthorUid,
                        // --- MODIFIED ---
                        postTextSnippet, // Pass snippet
                        // --- END MODIFIED ---
                        new ViewModelProvider(this).get(CommentLikeViewModel.class), this);
                holder.bind(comment);

                parentCommentView.findViewById(R.id.view_replies_text).setVisibility(View.GONE);
                parentCommentView.findViewById(R.id.button_reply).setVisibility(View.GONE);

                binding.parentCommentContainer.removeAllViews();
                binding.parentCommentContainer.addView(parentCommentView);

                binding.editTextComment.setHint("Replying to " + comment.getAuthorName() + "...");
            }
        });

        commentsViewModel.getComments().observe(this, replies -> {
            if (binding == null) return;
            boolean isEmpty = replies == null || replies.isEmpty();
            binding.commentsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.emptyStateRepliesText.setVisibility(isEmpty ? View.VISIBLE : View.GONE); // Control visibility

            if (!isEmpty) {
                commentAdapter.submitList(replies);
                Log.d(TAG, "Displaying " + replies.size() + " replies.");
            } else {
                Log.d(TAG, "No replies to display.");
                commentAdapter.submitList(null); // Clear adapter
            }
        });


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
            }
        });
    }

    private void setupClickListeners() {
        binding.buttonPostComment.setOnClickListener(v -> postNewReply());
    }

    private void postNewReply() {
        String text = binding.editTextComment.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Cannot post empty reply", Toast.LENGTH_SHORT).show();
            return;
        }

        if (parentComment == null) {
            Toast.makeText(this, "Error: Parent comment not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- MODIFIED CALL ---
        // Pass the stored postTextSnippet
        commentsViewModel.postCommentOrReply(
                postId,
                text,
                parentCommentId,
                postAuthorUid,
                postTextSnippet // Use stored snippet
        );
        // --- END MODIFIED ---

        binding.editTextComment.setText("");
        hideKeyboard();
    }

    private void focusReplyInput(@Nullable String authorName) {
        if (binding == null) return;
        if (authorName != null) {
            binding.editTextComment.setHint("Replying to " + authorName + "...");
        }
        binding.editTextComment.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.editTextComment, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view == null && binding != null) view = binding.editTextComment;
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus();
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

    @Override
    public void onReplyClicked(Comment comment) {
        Log.d(TAG, "Reply clicked for nested comment: " + comment.getId());
        focusReplyInput(comment.getAuthorName());
        if (binding != null) {
            binding.editTextComment.setText("@" + comment.getAuthorName() + " ");
            binding.editTextComment.setSelection(binding.editTextComment.length());
        }
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
                commentsViewModel.deleteComment(comment, postAuthorUid);
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
                false
        );
        reportDialog.setInputListener(reason -> {
            commentsViewModel.reportComment(comment, reason.isEmpty() ? "No reason provided" : reason);
        });
        reportDialog.show(getSupportFragmentManager(), "ReportCommentDialog");
    }

    @Override
    public void onViewRepliesClicked(Comment comment, CommentAdapter.CommentViewHolder holder) {
        Log.d(TAG, "View replies clicked for nested reply: " + comment.getId());
        Intent intent = new Intent(this, RepliesActivity.class);
        intent.putExtra(RepliesActivity.EXTRA_POST, currentPostData);
        intent.putExtra(RepliesActivity.EXTRA_COMMENT_ID, comment.getId());
        intent.putExtra(RepliesActivity.EXTRA_SHOULD_FOCUS_REPLY, false);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        postBinding = null;
    }
}