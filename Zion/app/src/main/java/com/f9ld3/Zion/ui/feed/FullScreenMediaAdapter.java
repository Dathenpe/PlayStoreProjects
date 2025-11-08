// main/java/com/f9ld3/Zion/ui/feed/FullScreenMediaAdapter.java
package com.f9ld3.Zion.ui.feed;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.VideoPlayerActivity;
import com.github.chrisbanes.photoview.PhotoView; // Import PhotoView
import java.util.ArrayList;

public class FullScreenMediaAdapter extends RecyclerView.Adapter<FullScreenMediaAdapter.MediaViewHolder> {

    // --- NEW: Interface for Play Click ---
    public interface OnPlayClickListener {
        void onPlayVideo(MediaItem mediaItem);
    }
    // --- End New ---

    private final ArrayList<MediaItem> mediaItems;
    private final Context context;
    private final OnPlayClickListener playClickListener; // <-- Store listener

    // --- UPDATE Constructor ---
    public FullScreenMediaAdapter(Context context, ArrayList<MediaItem> mediaItems, OnPlayClickListener playClickListener) {
        this.context = context;
        this.mediaItems = mediaItems;
        this.playClickListener = playClickListener; // <-- Initialize listener
    }
    // --- End Update ---


    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_full_screen_media, parent, false);
        // --- Pass playClickListener to ViewHolder ---
        return new MediaViewHolder(view, playClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);
        holder.bind(item, context);
    }

    @Override
    public int getItemCount() {
        return mediaItems != null ? mediaItems.size() : 0;
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        PhotoView imageView; // Use PhotoView for zoomable images
        ImageView playIcon;
        private final OnPlayClickListener playClickListener; // <-- Store listener


        // --- UPDATE Constructor ---
        public MediaViewHolder(@NonNull View itemView, OnPlayClickListener playClickListener) {
            super(itemView);
            this.playClickListener = playClickListener; // <-- Initialize listener
            imageView = itemView.findViewById(R.id.full_screen_image_view);
            playIcon = itemView.findViewById(R.id.play_icon);
        }
        // --- End Update ---

        void bind(MediaItem item, Context context) {
            boolean isVideo = "video".equals(item.getMediaType());

            Glide.with(context)
                    .load(isVideo ? item.getThumbnailUrl() : item.getUrl())
                    .placeholder(R.drawable.ic_placeholder_24dp)
                    .error(R.drawable.ic_placeholder_24dp)
                    .into(imageView);

            playIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);
            imageView.setEnabled(!isVideo); // Disable zoom for video thumbnails

            if (isVideo) {
                // --- UPDATE Click Listener ---
                // Clicking the thumbnail/play icon triggers the callback
                View.OnClickListener videoClickListener = v -> {
                    if (playClickListener != null) {
                        playClickListener.onPlayVideo(item); // <-- Trigger callback
                    }
                };
                // --- End Update ---
                imageView.setOnClickListener(videoClickListener);
                playIcon.setOnClickListener(videoClickListener);
            } else {
                // Reset click listeners for images
                imageView.setOnClickListener(null);
                playIcon.setOnClickListener(null);
            }
        }
    }
}