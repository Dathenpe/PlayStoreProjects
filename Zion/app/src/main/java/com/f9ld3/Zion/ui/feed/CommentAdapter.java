// main/java/com/f9ld3/Zion/ui/feed/CommentAdapter.java
package com.f9ld3.Zion.ui.feed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.google.android.material.color.MaterialColors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends ListAdapter<Comment, CommentAdapter.CommentViewHolder> {

    private static final String TAG = "CommentAdapter";
    private final CommentInteractionListener listener;
    private final String postAuthorUid; // ID of the user who owns the post
    private final CommentLikeViewModel likeViewModel; // ViewModel for comment likes/dislikes
    private final LifecycleOwner lifecycleOwner;

    public interface CommentInteractionListener {
        void onReplyClicked(Comment comment);
        void onDeleteClicked(Comment comment);
        void onReportClicked(Comment comment);
        void onViewRepliesClicked(Comment comment, CommentViewHolder holder);
        // Add if needed: void onAuthorClicked(Comment comment);
    }

    public CommentAdapter(@NonNull CommentInteractionListener listener, String postAuthorUid,
                          LifecycleOwner lifecycleOwner, FragmentActivity activity) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.postAuthorUid = postAuthorUid;
        this.lifecycleOwner = lifecycleOwner;
        // Get ViewModel scoped to the Activity/Fragment
        this.likeViewModel = new ViewModelProvider(activity).get(CommentLikeViewModel.class);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        // Pass ViewModel and LifecycleOwner to ViewHolder
        return new CommentViewHolder(view, listener, postAuthorUid, likeViewModel, lifecycleOwner);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    // --- ViewHolder ---
    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView authorAvatar;
        private final TextView authorName;
        private final TextView commentText;
        private final TextView timestampText;
        private final ImageButton optionsButton;
        private final TextView viewRepliesText;
        private final View replyThreadLine;
        private final View commentActionsLayout;
        private final View replyButton;
        private final ImageButton likeCommentButton;
        private final TextView likeCommentCount;
        private final ImageButton dislikeCommentButton;
        private final TextView dislikeCommentCount;

        private final CommentInteractionListener listener;
        private final String postAuthorUid;
        private Comment currentComment;
        private final CommentLikeViewModel likeViewModel;
        private final LifecycleOwner lifecycleOwner;

        public CommentViewHolder(@NonNull View itemView, CommentInteractionListener listener, String postAuthorUid,
                                 CommentLikeViewModel likeViewModel, LifecycleOwner lifecycleOwner) {
            super(itemView);
            this.listener = listener;
            this.postAuthorUid = postAuthorUid;
            this.likeViewModel = likeViewModel;
            this.lifecycleOwner = lifecycleOwner;

            // Find Views
            authorAvatar = itemView.findViewById(R.id.author_avatar);
            authorName = itemView.findViewById(R.id.author_name);
            commentText = itemView.findViewById(R.id.comment_text);
            timestampText = itemView.findViewById(R.id.timestamp_text);
            optionsButton = itemView.findViewById(R.id.button_comment_options);
            viewRepliesText = itemView.findViewById(R.id.view_replies_text);
            commentActionsLayout = itemView.findViewById(R.id.comment_actions);
            replyButton = itemView.findViewById(R.id.button_reply);
            replyThreadLine = itemView.findViewById(R.id.reply_thread_line);
            likeCommentButton = itemView.findViewById(R.id.button_like_comment);
            likeCommentCount = itemView.findViewById(R.id.like_comment_count);
            dislikeCommentButton = itemView.findViewById(R.id.button_dislike_comment);
            dislikeCommentCount = itemView.findViewById(R.id.dislike_comment_count);
        }

        void bind(Comment comment) {
            if (comment == null) return;
            currentComment = comment;
            Context context = itemView.getContext();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            // Deleted State Handling
            boolean isDeleted = comment.isDeleted();
            commentText.setText(comment.getTextContent()); // Shows "[Comment deleted]" if needed
            commentText.setAlpha(isDeleted ? 0.6f : 1.0f);
            authorName.setAlpha(isDeleted ? 0.6f : 1.0f);
            authorAvatar.setAlpha(isDeleted ? 0.6f : 1.0f);
            commentActionsLayout.setVisibility(isDeleted ? View.GONE : View.VISIBLE);
            optionsButton.setVisibility(isDeleted ? View.GONE : View.VISIBLE);
            viewRepliesText.setVisibility(isDeleted ? View.GONE : viewRepliesText.getVisibility()); // Hide if deleted

            // Bind Data (if not deleted)
            if (!isDeleted) {
                authorName.setText(comment.getAuthorName());
                if (comment.getTimestamp() != null) {
                    long timeMillis = comment.getTimestamp().toDate().getTime();
                    timestampText.setText(" • " + DateUtils.getRelativeTimeSpanString(timeMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
                    timestampText.setVisibility(View.VISIBLE);
                } else {
                    timestampText.setVisibility(View.GONE);
                }

                Glide.with(context).load(comment.getAuthorAvatarUrl())
                        .placeholder(R.drawable.ic_profile_placeholder).into(authorAvatar);

                // --- Click Listeners ---
                replyButton.setOnClickListener(v -> listener.onReplyClicked(comment));
                optionsButton.setOnClickListener(v -> showOptionsMenu(v, context, currentUser, comment));
                likeCommentButton.setOnClickListener(v -> handleLikeClick(currentUser, comment, context));
                dislikeCommentButton.setOnClickListener(v -> handleDislikeClick(currentUser, comment, context));

                // --- Handle "View Replies" ---
                if (comment.getReplyCount() > 0) {
                    viewRepliesText.setVisibility(View.VISIBLE);
                    int replyCount = comment.getReplyCount();
                    String repliesText = context.getResources().getQuantityString(
                            R.plurals.view_replies_count, // Correct: Use R.plurals
                            replyCount,                  // The count to determine "one" or "other"
                            replyCount                   // The value for the %d placeholder
                    );
                    viewRepliesText.setText(repliesText);

                    viewRepliesText.setOnClickListener(v -> listener.onViewRepliesClicked(comment, this));
                } else {
                    viewRepliesText.setVisibility(View.GONE);
                }

                // --- Observe Like/Dislike State & Counts ---
                observeLikeStatus(comment, context);

            } else {
                // Ensure listeners are nullified if deleted
                replyButton.setOnClickListener(null);
                optionsButton.setOnClickListener(null);
                likeCommentButton.setOnClickListener(null);
                dislikeCommentButton.setOnClickListener(null);
                viewRepliesText.setOnClickListener(null);
                timestampText.setVisibility(View.GONE); // Hide timestamp if deleted
            }

            // --- Show/Hide Thread Line (Based on whether it's a reply) ---
            replyThreadLine.setVisibility(comment.isReply() ? View.VISIBLE : View.GONE);
        }

        private void handleLikeClick(FirebaseUser currentUser, Comment comment, Context context) {
            if (currentUser != null) {
                // Fetch post text snippet (needed for notification)
                // This ideally should be available without fetching again. Pass from activity/fragment if possible.
                String postTextSnippet = ""; // Placeholder - Pass this properly
                String commentTextSnippet = comment.getTextContent();
                likeViewModel.toggleLike(comment.getPostId(), comment.getId(), comment.getAuthorUid(), commentTextSnippet, postTextSnippet);
            } else {

            }
        }

        private void handleDislikeClick(FirebaseUser currentUser, Comment comment, Context context) {
            if (currentUser != null) {
                likeViewModel.toggleDislike(comment.getPostId(), comment.getId());
            } else {

            }
        }

        private void observeLikeStatus(Comment comment, Context context) {
            ColorStateList likedTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.teal)); // Theme color
            ColorStateList dislikedTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error)); // Error color
            ColorStateList defaultTint = ColorStateList.valueOf(MaterialColors.getColor(likeCommentButton, com.google.android.material.R.attr.colorOnSurfaceVariant));

            // Observe Liked Status
            likeViewModel.isLiked(comment.getPostId(), comment.getId()).observe(lifecycleOwner, isLiked -> {
                if (likeCommentButton != null) { // Check view validity
                    likeCommentButton.setImageTintList(Boolean.TRUE.equals(isLiked) ? likedTint : defaultTint);
                }
            });

            // Observe Disliked Status
            likeViewModel.isDisliked(comment.getPostId(), comment.getId()).observe(lifecycleOwner, isDisliked -> {
                if (dislikeCommentButton != null) { // Check view validity
                    dislikeCommentButton.setImageTintList(Boolean.TRUE.equals(isDisliked) ? dislikedTint : defaultTint);
                }
            });

            // Observe Like Count
            likeViewModel.getLikeCount(comment.getPostId(), comment.getId()).observe(lifecycleOwner, count -> {
                if (likeCommentCount != null) { // Check view validity
                    int currentCount = count != null ? count : 0;
                    likeCommentCount.setText(String.valueOf(currentCount));
                    likeCommentCount.setVisibility(currentCount > 0 ? View.VISIBLE : View.GONE);
                }
            });

            // Observe Dislike Count
            likeViewModel.getDislikeCount(comment.getPostId(), comment.getId()).observe(lifecycleOwner, count -> {
                if (dislikeCommentCount != null) { // Check view validity
                    int currentCount = count != null ? count : 0;
                    dislikeCommentCount.setText(String.valueOf(currentCount));
                    dislikeCommentCount.setVisibility(currentCount > 0 ? View.VISIBLE : View.GONE);
                }
            });

            // Set initial state from comment data (for faster initial render)
            likeCommentCount.setText(String.valueOf(comment.getLikeCount()));
            likeCommentCount.setVisibility(comment.getLikeCount() > 0 ? View.VISIBLE : View.GONE);
            dislikeCommentCount.setText(String.valueOf(comment.getDislikeCount()));
            dislikeCommentCount.setVisibility(comment.getDislikeCount() > 0 ? View.VISIBLE : View.GONE);
        }


        // --- Options Menu Logic ---
        private void showOptionsMenu(View anchor, Context context, FirebaseUser currentUser, Comment comment) {
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.inflate(R.menu.menu_comment_options); // Use your menu resource

            // Determine if the current user can delete the comment
            boolean canDelete = currentUser != null &&
                    (currentUser.getUid().equals(comment.getAuthorUid()) ||
                            currentUser.getUid().equals(postAuthorUid));

            popup.getMenu().findItem(R.id.action_delete_comment).setVisible(canDelete);
            // Report option is always visible for logged-in users (except for own comment?)
            popup.getMenu().findItem(R.id.action_report_comment).setVisible(currentUser != null && !currentUser.getUid().equals(comment.getAuthorUid()));

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_delete_comment) {
                    listener.onDeleteClicked(comment);
                    return true;
                } else if (itemId == R.id.action_report_comment) {
                    listener.onReportClicked(comment);
                    return true;
                } else {
                    return false;
                }
            });
            popup.show();
        }

        // Public getter if needed by listener
        public TextView getViewRepliesTextView() {
            return viewRepliesText;
        }
    }


    // --- DiffUtil Callback ---
    private static final DiffUtil.ItemCallback<Comment> DIFF_CALLBACK = new DiffUtil.ItemCallback<Comment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            // Compare all fields relevant to UI display
            return Objects.equals(oldItem.getTextContent(), newItem.getTextContent()) && // getTextContent handles deleted state
                    oldItem.isDeleted() == newItem.isDeleted() &&
                    oldItem.getReplyCount() == newItem.getReplyCount() &&
                    oldItem.getLikeCount() == newItem.getLikeCount() &&
                    oldItem.getDislikeCount() == newItem.getDislikeCount() &&
                    Objects.equals(oldItem.getAuthorName(), newItem.getAuthorName()) &&
                    Objects.equals(oldItem.getAuthorAvatarUrl(), newItem.getAuthorAvatarUrl()) &&
                    Objects.equals(oldItem.getTimestamp(), newItem.getTimestamp()); // Timestamps can be null initially
        }
    };
}