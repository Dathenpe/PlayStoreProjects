// main/java/com/f9ld3/Zion/ui/feed/MediaPagerAdapter.java
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
import com.f9ld3.Zion.ui.player.PlayerMedia; // Import PlayerMedia
import com.f9ld3.Zion.ui.player.VideoPlayerActivity; // To play videos on click
import java.util.List;

public class MediaPagerAdapter extends RecyclerView.Adapter<MediaPagerAdapter.MediaViewHolder> {

    private final List<MediaItem> mediaItems;
    private final Context context;

    public MediaPagerAdapter(Context context, List<MediaItem> mediaItems) {
        this.context = context;
        this.mediaItems = mediaItems;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_media_page, parent, false);
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
        ImageView imageView;
        ImageView playIcon; // Icon to show over video thumbnails

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.media_image_view);
            playIcon = itemView.findViewById(R.id.play_icon);
        }

        void bind(MediaItem item, Context context) {
            boolean isVideo = "video".equals(item.getMediaType());

            Glide.with(context)
                    .load(isVideo ? item.getThumbnailUrl() : item.getUrl()) // Use thumbnail for video
                    .placeholder(R.drawable.ic_placeholder_24dp)
                    .error(R.drawable.ic_placeholder_24dp)
                    .into(imageView);

            playIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);

            // Add click listener to play video
            if (isVideo) {
                imageView.setOnClickListener(v -> {
                    // Create PlayerMedia object to pass to VideoPlayerActivity
                    PlayerMedia playerMedia = new PlayerMedia(); // You might need a constructor or setters
                    playerMedia.id = String.valueOf(System.currentTimeMillis()); // Temporary ID if needed
                    playerMedia.type = PlayerMedia.TYPE_VIDEO;
                    playerMedia.title = "Post Video"; // Use post title or generate one
                    playerMedia.mediaUrl = item.getUrl();
                    playerMedia.thumbnailUrl = item.getThumbnailUrl();
                    // Add author details if available from the Post object
                    // playerMedia.authorName = ...
                    // playerMedia.uploaderUid = ...
                    // playerMedia.uploaderAvatarUrl = ...

                    Intent intent = new Intent(context, VideoPlayerActivity.class);
                    intent.putExtra(VideoPlayerActivity.EXTRA_MEDIA_ITEM, playerMedia);
                    context.startActivity(intent);
                });
            } else {
                imageView.setOnClickListener(null); // Remove listener for images
            }
        }
    }
}