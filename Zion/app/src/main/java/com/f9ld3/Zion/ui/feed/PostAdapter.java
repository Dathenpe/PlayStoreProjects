package com.f9ld3.Zion.ui.feed;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ItemFeedPostBinding;
// The Post class is now imported from its own file:
import com.f9ld3.Zion.ui.feed.Post;


public class PostAdapter extends ListAdapter<Post, PostAdapter.PostViewHolder> {

    // Interface to handle click events on the posts
    public interface OnPostClickListener {
        // Updated to use the standalone Post class
        void onPostClick(Post post);
    }

    private final OnPostClickListener listener;

    public PostAdapter(OnPostClickListener listener) {
        // Pass a DiffUtil.ItemCallback implementation to ListAdapter
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        // We assume the ItemFeedPostBinding class has been generated from item_feed_post.xml
        ItemFeedPostBinding binding = ItemFeedPostBinding.inflate(inflater, parent, false);
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = getItem(position);
        holder.bind(post, listener);
    }

    // DiffUtil implementation for efficient list updates
    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            // Check if the unique ID (Firestore document ID) is the same
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            // Check if content fields are the same based on the Post structure
            return oldItem.title.equals(newItem.title) &&
                    oldItem.description.equals(newItem.description) &&
                    oldItem.imageUrl.equals(newItem.imageUrl) &&
                    oldItem.authorName.equals(newItem.authorName) &&
                    oldItem.timestamp == newItem.timestamp;
        }
    };

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        // Removed categoryChip
        private final ImageView thumbnailImageView;

        public PostViewHolder(ItemFeedPostBinding binding) {
            super(binding.getRoot());
            titleTextView = binding.postTitle;
            descriptionTextView = binding.postDescription;
            // Removed: categoryChip = binding.postCategoryChip;
            thumbnailImageView = binding.postThumbnail;
        }

        public void bind(final Post post, final OnPostClickListener listener) {
            titleTextView.setText(post.title);
            descriptionTextView.setText(post.description);
            // Removed: categoryChip.setText(post.category); // Not needed for pure blogs

            // Use Glide to load the thumbnail image from the URL
            Glide.with(itemView.getContext())
                    .load(post.imageUrl) // Using 'imageUrl' as defined in Post
                    // NOTE: R.drawable.ic_placeholder_24dp and R.drawable.ic_error_24dp need to exist
                    // Assuming R.drawable.ic_placeholder_24dp and R.drawable.ic_error_24dp exist in project resources
                    .placeholder(R.drawable.ic_placeholder_24dp)
                    .error(R.drawable.ic_error_24dp)
                    .into(thumbnailImageView);

            // Set the click listener on the entire card view
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostClick(post);
                }
            });
        }
    }
}