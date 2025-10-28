// main/java/com/f9ld3/Zion/ui/blog/MediaPreviewAdapter.java
package com.f9ld3.Zion.ui.blog; // Or move to ui.feed if more appropriate

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.ui.feed.MediaItem; // Import MediaItem
import com.google.android.material.imageview.ShapeableImageView;
import java.util.ArrayList; // Import ArrayList
import java.util.List;

public class MediaPreviewAdapter extends RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder> {

    // Interface for click events
    public interface OnMediaItemClickListener {
        void onMediaItemClick(int position);
    }

    // --- UPDATED: Use List<MediaItem> ---
    private final List<MediaItem> mediaItems;
    private final OnMediaItemClickListener clickListener; // Add listener

    // --- UPDATED: Constructor ---
    public MediaPreviewAdapter(List<MediaItem> mediaItems, OnMediaItemClickListener listener) {
        this.mediaItems = mediaItems != null ? mediaItems : new ArrayList<>(); // Handle null list
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_preview, parent, false);
        // --- UPDATED: Pass listener ---
        return new MediaViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView previewImage;
        private final ImageView playIcon;

        // --- UPDATED: Constructor takes listener ---
        public MediaViewHolder(@NonNull View itemView, OnMediaItemClickListener listener) {
            super(itemView);
            previewImage = itemView.findViewById(R.id.preview_image);
            playIcon = itemView.findViewById(R.id.play_icon);

            // Set click listener on the item view
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onMediaItemClick(position);
                    }
                }
            });
        }

        // --- UPDATED: bind method takes MediaItem ---
        void bind(MediaItem item) {
            boolean isVideo = "video".equals(item.getMediaType());
            // --- UPDATED: Get URL from MediaItem ---
            String url = isVideo ? item.getThumbnailUrl() : item.getUrl();

            Glide.with(itemView.getContext())
                    .load(url) // Load URL from MediaItem
                    .placeholder(R.drawable.ic_placeholder_24dp)
                    .error(R.drawable.ic_placeholder_24dp)
                    .centerCrop()
                    .into(previewImage);

            playIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        }
    }
}