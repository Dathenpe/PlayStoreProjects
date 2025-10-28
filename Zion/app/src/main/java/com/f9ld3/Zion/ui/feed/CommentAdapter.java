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
        private final TextView dislikeCommentCount; // Although likely hidden

        private final CommentInteractionListener listener;
        private final String postAuthorUid;
        // Removed: private Comment currentComment; // No longer strictly needed if listeners use the 'comment' param directly
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
            dislikeCommentCount = itemView.findViewById(R.id.dislike_comment_count); // Find the dislike count view
        }

        void bind(final Comment comment) { // Make comment final
            if (comment == null) {
                Log.w(TAG, "Attempting to bind a null comment at position: " + getAbsoluteAdapterPosition());
                // Optionally clear views or show a placeholder state
                return;
            }
            // currentComment = comment; // Can still keep if needed elsewhere
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
            // Hide "View Replies" if the parent comment is deleted
            viewRepliesText.setVisibility(isDeleted ? View.GONE : (comment.getReplyCount() > 0 ? View.VISIBLE : View.GONE));


            // Bind Data (if not deleted)
            if (!isDeleted) {
                authorName.setText(comment.getAuthorName());
                if (comment.getTimestamp() != null) {
                    try {
                        long timeMillis = comment.getTimestamp().toDate().getTime();
                        timestampText.setText(" • " + DateUtils.getRelativeTimeSpanString(timeMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
                        timestampText.setVisibility(View.VISIBLE);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Timestamp was null when trying to format for comment: " + comment.getId());
                        timestampText.setVisibility(View.GONE);
                    }
                } else {
                    timestampText.setVisibility(View.GONE);
                }


                Glide.with(context).load(comment.getAuthorAvatarUrl())
                        .placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(authorAvatar);

                // --- Click Listeners (Using 'comment' passed to bind) ---
                replyButton.setOnClickListener(v -> listener.onReplyClicked(comment));
                optionsButton.setOnClickListener(v -> showOptionsMenu(v, context, currentUser, comment));
                likeCommentButton.setOnClickListener(v -> handleLikeClick(currentUser, comment, context));
                dislikeCommentButton.setOnClickListener(v -> handleDislikeClick(currentUser, comment, context));
                // --- End Click Listener Update ---


                // --- Handle "View Replies" ---
                if (comment.getReplyCount() > 0) {
                    viewRepliesText.setVisibility(View.VISIBLE);
                    int replyCount = comment.getReplyCount();
                    String repliesText = context.getResources().getQuantityString(
                            R.plurals.view_replies_count, // Use R.plurals
                            replyCount,                  // Count for quantity selection
                            replyCount                   // Value for %d placeholder
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
                authorName.setText("User"); // Generic name if deleted
                authorAvatar.setImageResource(R.drawable.ic_profile_placeholder); // Placeholder avatar
            }

            // --- Show/Hide Thread Line (Based on whether it's a reply) ---
            replyThreadLine.setVisibility(comment.isReply() ? View.VISIBLE : View.GONE);
        }

        // --- UPDATED: Pass the specific comment object ---
        private void handleLikeClick(FirebaseUser currentUser, final Comment commentToLike, Context context) {
            if (currentUser != null && !currentUser.isAnonymous()) {
                // Fetch post text snippet (needed for notification)
                // This ideally should be available without fetching again. Pass from activity/fragment if possible.
                String postTextSnippet = ""; // Placeholder - Pass this properly
                String commentTextSnippet = commentToLike.getTextContent(); // Use current comment text
                likeViewModel.toggleLike(commentToLike.getPostId(), commentToLike.getId(), commentToLike.getAuthorUid(), commentTextSnippet, postTextSnippet);
            } else {
                Toast.makeText(context, R.string.login_for_features, Toast.LENGTH_SHORT).show();
            }
        }

        // --- UPDATED: Pass the specific comment object ---
        private void handleDislikeClick(FirebaseUser currentUser, final Comment commentToDislike, Context context) {
            if (currentUser != null && !currentUser.isAnonymous()) {
                likeViewModel.toggleDislike(commentToDislike.getPostId(), commentToDislike.getId());
            } else {
                Toast.makeText(context, R.string.login_for_features, Toast.LENGTH_SHORT).show();
            }
        }
        // --- End Update ---


        private void observeLikeStatus(Comment comment, Context context) {
            // Ensure comment and its IDs are valid before observing
            if (comment == null || comment.getPostId() == null || comment.getId() == null) {
                Log.w(TAG, "Cannot observe like status for invalid comment/IDs.");
                // Reset UI to default state
                if (likeCommentButton != null) {
                    likeCommentButton.setImageResource(R.drawable.ic_thumb_up_outline_24dp);
                    likeCommentButton.setImageTintList(null);
                }
                if (dislikeCommentButton != null) {
                    dislikeCommentButton.setImageResource(R.drawable.ic_thumb_down_outline_24dp);
                    dislikeCommentButton.setImageTintList(null);
                }
                if (likeCommentCount != null) likeCommentCount.setVisibility(View.GONE);
                if (dislikeCommentCount != null) dislikeCommentCount.setVisibility(View.GONE);
                return;
            }

            // Observe Liked Status
            likeViewModel.isLiked(comment.getPostId(), comment.getId()).observe(lifecycleOwner, isLiked -> {
                if (likeCommentButton != null) { // Check view validity
                    likeCommentButton.setImageResource(Boolean.TRUE.equals(isLiked)
                            ? R.drawable.ic_thumb_up_filled_24dp // Use filled icon
                            : R.drawable.ic_thumb_up_outline_24dp); // Use outline icon
                    likeCommentButton.setImageTintList(null); // Remove programmatic tint
                }
            });

            // Observe Disliked Status
            likeViewModel.isDisliked(comment.getPostId(), comment.getId()).observe(lifecycleOwner, isDisliked -> {
                if (dislikeCommentButton != null) { // Check view validity
                    dislikeCommentButton.setImageResource(Boolean.TRUE.equals(isDisliked)
                            ? R.drawable.ic_thumb_down_filled_24dp // Use filled icon
                            : R.drawable.ic_thumb_down_outline_24dp); // Use outline icon
                    dislikeCommentButton.setImageTintList(null); // Remove programmatic tint
                }
            });

            // --- Observe Like Count (Update UI solely based on observer) ---
            likeViewModel.getLikeCount(comment.getPostId(), comment.getId()).observe(lifecycleOwner, count -> {
                if (likeCommentCount != null) { // Check view validity
                    int currentCount = count != null ? count : 0;
                    likeCommentCount.setText(String.valueOf(currentCount)); // Update text based on LiveData
                    likeCommentCount.setVisibility(currentCount > 0 ? View.VISIBLE : View.GONE); // Update visibility based on LiveData
                }
            });

            // --- Observe Dislike Count (Update UI solely based on observer) ---
            likeViewModel.getDislikeCount(comment.getPostId(), comment.getId()).observe(lifecycleOwner, count -> {
                if (dislikeCommentCount != null) { // Check view validity
                    int currentCount = count != null ? count : 0;
                    dislikeCommentCount.setText(String.valueOf(currentCount)); // Update text based on LiveData
                    // YouTube generally hides dislike count, keep visibility GONE
                    dislikeCommentCount.setVisibility(View.GONE); // Update visibility based on LiveData
                    // Or show if > 0:
                    // dislikeCommentCount.setVisibility(currentCount > 0 ? View.VISIBLE : View.GONE);
                }
            });

            // --- REMOVE Initial Count Setting from Comment Object ---
            // (Lines removed)
            // --- END REMOVE ---
        }


        // --- Options Menu Logic ---
        private void showOptionsMenu(View anchor, Context context, FirebaseUser currentUser, Comment comment) {
            PopupMenu popup = new PopupMenu(context, anchor);

            // Inflate menu resource only if it exists
            try {
                popup.inflate(R.menu.menu_comment_options);

                // Determine if the current user can delete the comment
                boolean canDelete = currentUser != null &&
                        (currentUser.getUid().equals(comment.getAuthorUid()) ||
                                (postAuthorUid != null && currentUser.getUid().equals(postAuthorUid))); // Check postAuthorUid

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

            } catch (Exception e) {
                Log.e(TAG, "Error inflating or showing comment options menu", e);
                // Fallback or show simple toast if menu inflation fails
                Toast.makeText(context, "Options unavailable", Toast.LENGTH_SHORT).show();
            }
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
            // Check if IDs are non-null before comparing
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