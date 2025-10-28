// main/java/com/f9ld3/Zion/ui/feed/CommentsBottomSheet.java
package com.f9ld3.Zion.ui.feed;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.f9ld3.Zion.R;
// *** CHANGE BINDING TYPE ***
import com.f9ld3.Zion.databinding.BottomSheetCommentsBinding; // Import the new binding
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.dialogs.CustomInputDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.io.Serializable;

public class CommentsBottomSheet extends BottomSheetDialogFragment implements CommentAdapter.CommentInteractionListener {
    public static final String EXTRA_POST_ID = "extra_post_id";
    public static final String EXTRA_POST_DATA = "extra_post_data";
    public static final String TAG = "CommentsBottomSheet"; // Renamed TAG

    // *** CHANGE BINDING TYPE ***
    private BottomSheetCommentsBinding binding; // Use the new binding type
    private CommentsViewModel viewModel;
    private CommentAdapter adapter;
    private String postId;
    private Post currentPostData;

    public static CommentsBottomSheet newInstance(String postId, Post postData) {
        CommentsBottomSheet fragment = new CommentsBottomSheet();
        Bundle args = new Bundle();
        args.putString(EXTRA_POST_ID, postId);
        args.putSerializable(EXTRA_POST_DATA, postData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString(EXTRA_POST_ID);
            currentPostData = (Post) getArguments().getSerializable(EXTRA_POST_DATA);
        }

        if (postId == null || currentPostData == null) {
            Log.e(TAG, "Post ID or Post Data is null! Dismissing.");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: Post data missing.", Toast.LENGTH_SHORT).show();
            }
            // Cannot call dismiss() directly in onCreate, handle in onViewCreated
            return;
        }

        viewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // *** INFLATE NEW LAYOUT ***
        binding = BottomSheetCommentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if data is missing (from onCreate check)
        if (postId == null || currentPostData == null) {
            dismissAllowingStateLoss(); // Use dismissAllowingStateLoss if view might not be fully ready
            return;
        }

        setupRecyclerView(); // IDs should match the new layout

        viewModel.getComments().observe(getViewLifecycleOwner(), comments -> {
            if (binding == null) return;
            boolean isEmpty = comments == null || comments.isEmpty();
            binding.commentsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE); // Use new ID

            if (!isEmpty) {
                adapter.submitList(comments);
                Log.d(TAG, "Displaying " + comments.size() + " top-level comments.");
            } else {
                Log.w(TAG, "Received null or empty top-level comments list.");
                adapter.submitList(null);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && getContext() != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), success -> {
            if (success != null && getContext() != null) {
                Toast.makeText(getContext(), success, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });

        viewModel.loadComments(postId);
        binding.buttonPostComment.setOnClickListener(v -> postNewCommentOrReply()); // IDs should match
    }

    @NonNull @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                BottomSheetBehavior.from(bottomSheet).setSkipCollapsed(true); // Prevent collapsing halfway
            }
        });
        return dialog;
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter(this, currentPostData.getAuthorUid(), getViewLifecycleOwner(), requireActivity());
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // Use new ID
        binding.commentsRecyclerView.setAdapter(adapter);
    }

    private void postNewCommentOrReply() {
        String text = binding.editTextComment.getText().toString().trim(); // Use new ID
        if (!text.isEmpty()) {
            viewModel.postCommentOrReply(
                    postId,
                    text,
                    null,
                    currentPostData.getAuthorUid(),
                    currentPostData.getTextContent()
            );
            binding.editTextComment.setText(""); // Use new ID
            hideKeyboard();
        } else if (getContext() != null) {
            Toast.makeText(getContext(), "Cannot post empty comment", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        View view = getDialog() != null ? getDialog().getCurrentFocus() : null;
        if (view == null && binding != null) {
            view = binding.editTextComment; // Use new ID
        }
        if (view != null && getContext() != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus();
        }
    }

    @Override
    public void onReplyClicked(Comment comment) {
        Log.d(TAG, "Reply clicked. Opening replies activity for: " + comment.getId());
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), RepliesActivity.class);
        intent.putExtra(RepliesActivity.EXTRA_POST, currentPostData);
        intent.putExtra(RepliesActivity.EXTRA_COMMENT_ID, comment.getId());
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
                viewModel.deleteComment(comment, currentPostData.getAuthorUid());
            }
            @Override
            public void onNegativeClick() { }
        });
        dialog.show(getParentFragmentManager(), "DeleteCommentDialog");
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
            viewModel.reportComment(comment, reason.isEmpty() ? "No reason provided" : reason);
        });
        reportDialog.show(getParentFragmentManager(), "ReportCommentDialog");
    }

    @Override
    public void onViewRepliesClicked(Comment comment, CommentAdapter.CommentViewHolder holder) {
        Log.d(TAG, "View replies clicked. Opening replies activity for: " + comment.getId());
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), RepliesActivity.class);
        intent.putExtra(RepliesActivity.EXTRA_POST, currentPostData);
        intent.putExtra(RepliesActivity.EXTRA_COMMENT_ID, comment.getId());
        intent.putExtra(RepliesActivity.EXTRA_SHOULD_FOCUS_REPLY, false);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}