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
    private CommentsViewModel commentsViewModel;
    private PostLikeViewModel postLikeViewModel;
    private CommentAdapter commentAdapter;

    private String postId;
    private String parentCommentId;
    private String postAuthorUid;
    private Comment parentComment;
    private Post currentPostData;
    private String postTextSnippet;

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
        postTextSnippet = currentPostData.getTextContent();

        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        postLikeViewModel = new ViewModelProvider(this).get(PostLikeViewModel.class);

        setupRecyclerView();
        setupObservers();
        setupClickListeners();
        updatePostUi(currentPostData);
        commentsViewModel.loadCommentThread(postId, parentCommentId);

        if (shouldFocusReply) {
            focusReplyInput(null);
        }
    }

    private void updatePostUi(Post post) {
        if (post == null || binding == null) return;

        View postView = binding.postLayoutContainer.getRoot(); // Get the root view of the included layout

        // FIX 1: Call the constructor with all required arguments.
        // Get ViewModels needed for the constructor. Note: PollViewModel is needed too.
        PollViewModel pollViewModel = new ViewModelProvider(this).get(PollViewModel.class);
        // Pass 'null' for the listener as we handle clicks directly below or disable them.
        // Pass 'this' as the LifecycleOwner.
        PostAdapter.PostViewHolder holder = new PostAdapter.PostViewHolder(
                postView,
                null, // Pass null for OnPostClickListener
                postLikeViewModel,
                pollViewModel, // Pass the PollViewModel
                this // Pass the LifecycleOwner (the Activity)
        );

        // FIX 2: Call the correct bind method signature.
        holder.bind(post); // Pass only the Post object

        // --- Keep the rest of your logic to customize/disable parts ---

        // Re-apply essential listeners or disable elements as needed for this context
        binding.postLayoutContainer.likeButton.setOnClickListener(v -> {
            postLikeViewModel.toggleLike(post.getId(), post);
        });
        // Scroll to replies list instead of opening bottom sheet
        binding.postLayoutContainer.commentButton.setOnClickListener(v -> {
            if(binding.commentsRecyclerView.getAdapter() != null && binding.commentsRecyclerView.getAdapter().getItemCount() > 0) {
                binding.commentsRecyclerView.smoothScrollToPosition(0);
            } else {
                focusReplyInput(null); // Focus input if no replies yet
            }
        });
        binding.postLayoutContainer.authorAvatar.setOnClickListener(null); // Disable author click
        binding.postLayoutContainer.authorName.setOnClickListener(null);   // Disable author click
        binding.postLayoutContainer.postOptionsButton.setVisibility(View.GONE); // Hide options button

        // Remove the general item click listener for the post card in replies view
        binding.postLayoutContainer.getRoot().setOnClickListener(null);

        // Observe like state and update UI (optional if you want to show if liked)
        postLikeViewModel.isLiked(post.getId()).observe(this, isLiked -> {
            if (binding == null) return; // Check binding again inside observer
            ColorStateList likedTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.teal));
            ColorStateList defaultTint = ColorStateList.valueOf(MaterialColors.getColor(binding.postLayoutContainer.likeButton, com.google.android.material.R.attr.colorOnSurfaceVariant));
            binding.postLayoutContainer.likeButton.setImageTintList(Boolean.TRUE.equals(isLiked) ? likedTint : defaultTint);
        });
        // Update counts (optional, could be relevant)
        binding.postLayoutContainer.likeCount.setText(String.valueOf(post.getLikeCount())); // Use simple count here
        binding.postLayoutContainer.likeCount.setVisibility(post.getLikeCount() > 0 ? View.VISIBLE : View.GONE);
        binding.postLayoutContainer.commentCount.setText(String.valueOf(post.getCommentCount()));// Use simple count here
        binding.postLayoutContainer.commentCount.setVisibility(post.getCommentCount() > 0 ? View.VISIBLE : View.GONE);

        // Hide poll container specific details if they exist in the included layout
        if (binding.postLayoutContainer.pollContainer != null) {
            binding.postLayoutContainer.pollContainer.setVisibility(View.GONE);
        }
        if (binding.postLayoutContainer.pollDetailsText != null) {
            binding.postLayoutContainer.pollDetailsText.setVisibility(View.GONE);
        }
        // Hide media grid specific details
        if(binding.postLayoutContainer.postMediaGrid != null){
            binding.postLayoutContainer.postMediaGrid.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter(this, postAuthorUid, this, this);
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentsRecyclerView.setAdapter(commentAdapter);
    }

    private void setupObservers() {
        commentsViewModel.getParentComment().observe(this, comment -> {
            if (binding == null) return;
            if (comment != null) {
                this.parentComment = comment;
                View parentCommentView = getLayoutInflater().inflate(R.layout.item_comment, binding.parentCommentContainer, false);
                CommentAdapter.CommentViewHolder holder = new CommentAdapter.CommentViewHolder(parentCommentView, this, postAuthorUid,
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

        commentsViewModel.postCommentOrReply(
                postId,
                text,
                parentCommentId,
                postAuthorUid,
                postTextSnippet
        );

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
    }
}