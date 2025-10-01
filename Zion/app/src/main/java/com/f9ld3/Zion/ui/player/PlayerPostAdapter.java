package com.f9ld3.Zion.ui.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ItemPlayerPodcastDuoBinding;
import com.f9ld3.Zion.databinding.ItemPlayerVideoBinding;
// The PlayerMedia class is the data model for this adapter
import com.f9ld3.Zion.ui.player.PlayerMedia;

public class PlayerPostAdapter extends ListAdapter<PlayerMedia, RecyclerView.ViewHolder> {

    // Interface to handle click events on media items
    public interface OnMediaClickListener {
        void onMediaClick(PlayerMedia mediaItem);
    }

    private final OnMediaClickListener listener;

    public PlayerPostAdapter(OnMediaClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // --- View Type Handling ---
    @Override
    public int getItemViewType(int position) {
        // Return the type defined in the data model
        return getItem(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // 🔥 CRITICAL FIX: Use ContextThemeWrapper to apply the application theme,
        // which prevents the InflateException when using MaterialTextView.
        // NOTE: Ensure R.style.Theme_Zion is the correct ID for your app's theme.
        final int themeResId = R.style.Theme_Zion;

        // 1. Create a themed Context and LayoutInflater
        ContextThemeWrapper themedContext = new ContextThemeWrapper(parent.getContext(), themeResId);
        LayoutInflater themedInflater = LayoutInflater.from(themedContext);

        if (viewType == PlayerMedia.TYPE_VIDEO) {
            // Inflate the full-width video card layout using the themed inflater
            ItemPlayerVideoBinding binding = ItemPlayerVideoBinding.inflate(themedInflater, parent, false);
            return new VideoViewHolder(binding);
        } else if (viewType == PlayerMedia.TYPE_PODCAST_DUO_CONTAINER) {
            // Inflate the side-by-side podcast layout using the themed inflater
            ItemPlayerPodcastDuoBinding binding = ItemPlayerPodcastDuoBinding.inflate(themedInflater, parent, false);
            return new PodcastDuoViewHolder(binding);
        }
        // Fallback or error case (should not happen if data is well-structured)
        throw new IllegalArgumentException("Invalid view type: " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PlayerMedia mediaItem = getItem(position);

        if (holder.getItemViewType() == PlayerMedia.TYPE_VIDEO) {
            ((VideoViewHolder) holder).bind(mediaItem, listener);
        } else if (holder.getItemViewType() == PlayerMedia.TYPE_PODCAST_DUO_CONTAINER) {
            ((PodcastDuoViewHolder) holder).bind(mediaItem, listener);
        }
    }

    // --- DiffUtil Callback ---
    private static final DiffUtil.ItemCallback<PlayerMedia> DIFF_CALLBACK = new DiffUtil.ItemCallback<PlayerMedia>() {
        @Override
        public boolean areItemsTheSame(@NonNull PlayerMedia oldItem, @NonNull PlayerMedia newItem) {
            // Use ID for items (if not a duo container), otherwise check container type
            if (oldItem.getType() == PlayerMedia.TYPE_PODCAST_DUO_CONTAINER && newItem.getType() == PlayerMedia.TYPE_PODCAST_DUO_CONTAINER) {
                // For duo containers, assume they are the same if the first item ID is the same
                return oldItem.podcastOne.id.equals(newItem.podcastOne.id);
            }
            return oldItem.id != null && oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull PlayerMedia oldItem, @NonNull PlayerMedia newItem) {
            // A simple check if title/URL are the same
            if (oldItem.getType() == PlayerMedia.TYPE_PODCAST_DUO_CONTAINER && newItem.getType() == PlayerMedia.TYPE_PODCAST_DUO_CONTAINER) {
                return oldItem.podcastOne.title.equals(newItem.podcastOne.title) &&
                        oldItem.podcastTwo.title.equals(newItem.podcastTwo.title);
            }
            return oldItem.title.equals(newItem.title) &&
                    oldItem.mediaUrl.equals(newItem.mediaUrl);
        }
    };

    // --- ViewHolder for Video Item ---
    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView detailsTextView;
        private final ImageView thumbnailImageView;

        public VideoViewHolder(ItemPlayerVideoBinding binding) {
            super(binding.getRoot());
            titleTextView = binding.videoTitle;
            detailsTextView = binding.videoDetails;
            thumbnailImageView = binding.videoThumbnail;
        }

        public void bind(final PlayerMedia media, final OnMediaClickListener listener) {
            titleTextView.setText(media.title);
            // Example of formatting details (assuming durationSeconds is available)
            detailsTextView.setText(String.format("%s • %d min", media.authorName, media.durationSeconds / 60));

            Glide.with(itemView.getContext())
                    .load(media.thumbnailUrl)
                    .placeholder(R.drawable.ic_placeholder_24dp) // Assume placeholder exists
                    .error(R.drawable.ic_error_24dp) // Assume error drawable exists
                    .into(thumbnailImageView);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    // Clicks on the video card pass the video item directly
                    listener.onMediaClick(media);
                }
            });
        }
    }

    // --- ViewHolder for Podcast Duo Item (M3 Expressive) ---
    public static class PodcastDuoViewHolder extends RecyclerView.ViewHolder {
        // Podcast 1 elements
        private final ImageView thumbnail1;
        private final TextView title1;
        private final View card1;

        // Podcast 2 elements
        private final ImageView thumbnail2;
        private final TextView title2;
        private final View card2;

        public PodcastDuoViewHolder(ItemPlayerPodcastDuoBinding binding) {
            super(binding.getRoot());
            // Item 1
            // 🔥 FIXED: Changed from binding.podcastCard1 to binding.podcastItem1
            card1 = binding.podcastItem1;
            thumbnail1 = binding.podcastThumbnail1;
            title1 = binding.podcastTitle1;

            // Item 2
            // 🔥 FIXED: Changed from binding.podcastCard2 to binding.podcastItem2
            card2 = binding.podcastItem2;
            thumbnail2 = binding.podcastThumbnail2;
            title2 = binding.podcastTitle2;
        }

        public void bind(final PlayerMedia mediaDuo, final OnMediaClickListener listener) {
            final PlayerMedia p1 = mediaDuo.podcastOne;
            final PlayerMedia p2 = mediaDuo.podcastTwo;

            // Bind Podcast 1
            if (p1 != null) {
                title1.setText(p1.title);
                Glide.with(itemView.getContext())
                        .load(p1.thumbnailUrl)
                        .placeholder(R.drawable.ic_placeholder_24dp)
                        .error(R.drawable.ic_error_24dp)
                        .into(thumbnail1);

                card1.setOnClickListener(v -> {
                    if (listener != null) {
                        // Clicks on the first card pass the first podcast item
                        listener.onMediaClick(p1);
                    }
                });
            }

            // Bind Podcast 2
            if (p2 != null) {
                title2.setText(p2.title);
                Glide.with(itemView.getContext())
                        .load(p2.thumbnailUrl)
                        .placeholder(R.drawable.ic_placeholder_24dp)
                        .error(R.drawable.ic_error_24dp)
                        .into(thumbnail2);

                card2.setOnClickListener(v -> {
                    if (listener != null) {
                        // Clicks on the second card pass the second podcast item
                        listener.onMediaClick(p2);
                    }
                });
            }
        }
    }
}