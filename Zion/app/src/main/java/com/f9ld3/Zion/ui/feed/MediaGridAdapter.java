package com.f9ld3.Zion.ui.feed; // Adjust package if needed

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R; // Ensure R is imported correctly
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class MediaGridAdapter extends RecyclerView.Adapter<MediaGridAdapter.MediaGridViewHolder> {

    private final List<MediaItem> mediaItems;
    private final PostAdapter.OnPostClickListener postClickListener; // Use the same listener
    private final Post parentPost;
    private static final int MAX_VISIBLE_ITEMS = 4; // Show max 4 items

    public MediaGridAdapter(Post post, List<MediaItem> items, PostAdapter.OnPostClickListener listener) {
        this.parentPost = post;
        this.mediaItems = items;
        this.postClickListener = listener;
    }

    @NonNull
    @Override
    public MediaGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_grid_preview, parent, false);
        return new MediaGridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaGridViewHolder holder, int position) {
        boolean isLastVisibleItem = position == MAX_VISIBLE_ITEMS - 1;
        int totalItems = mediaItems.size();
        boolean hasMoreItems = totalItems > MAX_VISIBLE_ITEMS;

        MediaItem item = mediaItems.get(position);
        holder.bind(item, isLastVisibleItem && hasMoreItems, totalItems - MAX_VISIBLE_ITEMS);

        // Make the whole grid item clickable to open the post detail
        holder.itemView.setOnClickListener(v -> {
            if (postClickListener != null) {
                postClickListener.onPostItemClick(parentPost);
            }
        });
    }

    @Override
    public int getItemCount() {
        // Show only up to MAX_VISIBLE_ITEMS
        return Math.min(mediaItems.size(), MAX_VISIBLE_ITEMS);
    }

    static class MediaGridViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView mediaImage;
        ImageView playIcon;
        FrameLayout overlayMore;
        TextView textMoreCount;

        public MediaGridViewHolder(@NonNull View itemView) {
            super(itemView);
            mediaImage = itemView.findViewById(R.id.media_image);
            playIcon = itemView.findViewById(R.id.play_icon);
            overlayMore = itemView.findViewById(R.id.overlay_more);
            textMoreCount = itemView.findViewById(R.id.text_more_count);
        }

        void bind(MediaItem item, boolean showOverlay, int moreCount) {
            Context context = itemView.getContext();
            boolean isVideo = "video".equals(item.getMediaType());

            Glide.with(context)
                    .load(isVideo ? item.getThumbnailUrl() : item.getUrl())
                    .placeholder(R.drawable.ic_placeholder_24dp)
                    .error(R.drawable.ic_placeholder_24dp)
                    .centerCrop()
                    .into(mediaImage);

            playIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);

            if (showOverlay && moreCount > 0) {
                overlayMore.setVisibility(View.VISIBLE);
                textMoreCount.setText(String.format("+%d", moreCount));
            } else {
                overlayMore.setVisibility(View.GONE);
            }
        }
    }
}