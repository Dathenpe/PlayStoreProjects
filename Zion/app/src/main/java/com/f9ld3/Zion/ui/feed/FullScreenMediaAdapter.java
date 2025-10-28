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

    private final ArrayList<MediaItem> mediaItems;
    private final Context context;

    public FullScreenMediaAdapter(Context context, ArrayList<MediaItem> mediaItems) {
        this.context = context;
        this.mediaItems = mediaItems;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_full_screen_media, parent, false);
        return new MediaViewHolder(view);
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

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.full_screen_image_view);
            playIcon = itemView.findViewById(R.id.play_icon);
        }

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
                // Clicking the thumbnail/play icon starts the video player
                View.OnClickListener videoClickListener = v -> {
                    PlayerMedia playerMedia = new PlayerMedia();
                    playerMedia.id = String.valueOf(System.currentTimeMillis()); // Temporary ID
                    playerMedia.type = PlayerMedia.TYPE_VIDEO;
                    playerMedia.title = "Post Video"; // Consider passing post title
                    playerMedia.mediaUrl = item.getUrl();
                    playerMedia.thumbnailUrl = item.getThumbnailUrl();
                    // Add author details if available

                    Intent intent = new Intent(context, VideoPlayerActivity.class);
                    intent.putExtra(VideoPlayerActivity.EXTRA_MEDIA_ITEM, playerMedia);
                    context.startActivity(intent);
                };
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