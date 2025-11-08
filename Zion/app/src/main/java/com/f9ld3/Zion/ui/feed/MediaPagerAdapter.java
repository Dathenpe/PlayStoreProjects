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
import java.util.ArrayList;
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
        // --- FIX: Inflate the layout that matches the IDs used in the ViewHolder ---
        View view = LayoutInflater.from(context).inflate(R.layout.item_feed_media_page, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);
        holder.bind(item, context);

        // --- UNIFIED CLICK LISTENER ---
        // Both images and videos should open the FullScreenMediaActivity.
        // That activity will handle whether to show an image or play a video.
        View.OnClickListener mediaClickListener = v -> {
            // Get the adapter position at click time, in case it changed
            int clickedPosition = holder.getAbsoluteAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) {
                return; // Invalid position, do nothing
            }

            Intent intent = new Intent(context, FullScreenMediaActivity.class);
            // Pass the entire list and the clicked position
            intent.putExtra(FullScreenMediaActivity.EXTRA_MEDIA_ITEMS, new ArrayList<>(mediaItems));
            intent.putExtra(FullScreenMediaActivity.EXTRA_START_POSITION, clickedPosition);
            context.startActivity(intent);
        };

        holder.imageView.setOnClickListener(mediaClickListener);
        holder.playIcon.setOnClickListener(mediaClickListener); // Also set for the play icon
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
            // --- FIX: Use the IDs from item_feed_media_page.xml ---
            imageView = itemView.findViewById(R.id.media_image_item);
            playIcon = itemView.findViewById(R.id.play_icon_item);
        }

        void bind(MediaItem item, Context context) {
            boolean isVideo = "video".equals(item.getMediaType());

            Glide.with(context)
                    .load(isVideo ? item.getThumbnailUrl() : item.getUrl()) // Use thumbnail for video
                    .placeholder(R.drawable.ic_placeholder_24dp)
                    .error(R.drawable.ic_placeholder_24dp)
                    // --- FIX: Use fitCenter to match the change from the last step ---
                    .fitCenter()
                    .into(imageView);

            // Safely set visibility for the play icon
            if (playIcon != null) {
                playIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);
            }

            // Click listeners are now handled exclusively in onBindViewHolder
        }
    }
}