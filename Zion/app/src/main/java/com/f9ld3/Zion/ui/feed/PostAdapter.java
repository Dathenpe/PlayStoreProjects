// main/java/com/f9ld3/Zion/ui/feed/PostAdapter.java
package com.f9ld3.Zion.ui.feed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue; // Added
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.AttrRes; // Added
import androidx.annotation.ColorInt; // Added
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider; // Added
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.google.android.material.color.MaterialColors; // Added

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends ListAdapter<Post, PostAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onPostItemClick(Post post);
        void onLikeClick(Post post);
        void onCommentClick(Post post);
    }

    private final OnPostClickListener listener;
    private final LifecycleOwner lifecycleOwner;
    private final PostLikeViewModel postLikeViewModel;

    public PostAdapter(OnPostClickListener listener, LifecycleOwner lifecycleOwner, FragmentActivity activity) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.lifecycleOwner = lifecycleOwner;
        // Get the ViewModel using ViewModelProvider
        this.postLikeViewModel = new ViewModelProvider(activity).get(PostLikeViewModel.class);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = getItem(position);
        holder.bind(post, listener, postLikeViewModel, lifecycleOwner);
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView authorAvatar;
        private final TextView authorName;
        private final TextView postContent;
        private final ImageView postMediaPreview;
        private final ImageButton likeButton;
        private final TextView likeCount;
        private final ImageButton commentButton;
        private final TextView commentCount;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorAvatar = itemView.findViewById(R.id.author_avatar);
            authorName = itemView.findViewById(R.id.author_name);
            postContent = itemView.findViewById(R.id.post_content);
            postMediaPreview = itemView.findViewById(R.id.post_media_preview);
            likeButton = itemView.findViewById(R.id.like_button);
            likeCount = itemView.findViewById(R.id.like_count);
            commentButton = itemView.findViewById(R.id.comment_button);
            commentCount = itemView.findViewById(R.id.comment_count);
        }

        public void bind(final Post post, final OnPostClickListener listener,
                         final PostLikeViewModel likeViewModel, final LifecycleOwner owner) {

            Context context = itemView.getContext();

            authorName.setText(post.getAuthorName());
            setPostText(post.getTextContent());

            Glide.with(context)
                    .load(post.getAuthorAvatarUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(authorAvatar);

            String previewUrl = post.getThumbnailUrl();
            if (previewUrl != null && !previewUrl.isEmpty()) {
                postMediaPreview.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(previewUrl)
                        .placeholder(R.drawable.ic_placeholder_24dp)
                        .error(R.drawable.ic_placeholder_24dp)
                        .centerCrop()
                        .into(postMediaPreview);
            } else {
                postMediaPreview.setVisibility(View.GONE);
            }

            likeCount.setText(String.valueOf(post.getLikeCount()));
            commentCount.setText(String.valueOf(post.getCommentCount()));

            itemView.setOnClickListener(v -> listener.onPostItemClick(post));
            likeButton.setOnClickListener(v -> listener.onLikeClick(post));
            commentButton.setOnClickListener(v -> listener.onCommentClick(post));

            likeViewModel.isLiked(post.getId()).observe(owner, isLiked -> {
                if (isLiked != null && isLiked) {
                    likeButton.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.error)));
                } else {
                    // Use colorOnSurfaceVariant from theme for default/unliked state
                    ColorStateList defaultTint = ColorStateList.valueOf(
                            MaterialColors.getColor(likeButton, com.google.android.material.R.attr.colorOnSurfaceVariant)
                    );
                    likeButton.setImageTintList(defaultTint);
                }
            });
        }

        // Helper to get color from theme attribute
        @ColorInt
        private int getColorFromAttr(@AttrRes int attrRes) {
            TypedValue typedValue = new TypedValue();
            itemView.getContext().getTheme().resolveAttribute(attrRes, typedValue, true);
            return typedValue.data;
        }


        private void setPostText(String text) {
            if (text == null || text.isEmpty()) {
                postContent.setVisibility(View.GONE);
                return;
            }
            postContent.setVisibility(View.VISIBLE);
            SpannableString spannableString = new SpannableString(text);
            Pattern hashtagPattern = Pattern.compile("#(\\w+)");
            Matcher matcher = hashtagPattern.matcher(text);

            // Use theme's secondary color for hashtags
            int hashtagColor = getColorFromAttr(com.google.android.material.R.attr.colorSecondary);

            while (matcher.find()) {
                spannableString.setSpan(new ForegroundColorSpan(hashtagColor),
                        matcher.start(),
                        matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            postContent.setText(spannableString);
        }
    }

    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            // ID must not be null
            return oldItem.id != null && oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            // Check relevant fields for UI changes
            return Objects.equals(oldItem.textContent, newItem.textContent) &&
                    oldItem.likeCount == newItem.likeCount &&
                    oldItem.commentCount == newItem.commentCount &&
                    Objects.equals(oldItem.getThumbnailUrl(), newItem.getThumbnailUrl()) &&
                    Objects.equals(oldItem.getAuthorName(), newItem.getAuthorName()) &&
                    Objects.equals(oldItem.getAuthorAvatarUrl(), newItem.getAuthorAvatarUrl());
            // Note: Add isLiked comparison if you manage like state directly in Post object
        }
    };
}