package com.f9ld3.Zion.ui.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ItemPlayerPodcastDuoBinding;
import com.f9ld3.Zion.databinding.ItemVideoM3Binding;

import java.util.concurrent.TimeUnit;


public class PlayerPostAdapter extends ListAdapter<PlayerMedia, RecyclerView.ViewHolder> {

    public interface OnMediaClickListener {
        void onMediaClick(PlayerMedia mediaItem);
    }

    private final OnMediaClickListener listener;

    public PlayerPostAdapter(OnMediaClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "";
        }
        long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == PlayerMedia.TYPE_VIDEO) {
            ItemVideoM3Binding binding = ItemVideoM3Binding.inflate(inflater, parent, false);
            return new VideoViewHolder(binding);
        } else if (viewType == PlayerMedia.TYPE_PODCAST_DUO_CONTAINER) {
            ItemPlayerPodcastDuoBinding binding = ItemPlayerPodcastDuoBinding.inflate(inflater, parent, false);
            return new PodcastDuoViewHolder(binding);
        }
        throw new IllegalArgumentException("Invalid view type: " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PlayerMedia mediaItem = getItem(position);
        if (holder.getItemViewType() == PlayerMedia.TYPE_VIDEO) {
            ((VideoViewHolder) holder).bind(mediaItem, listener, this::formatDuration);
        } else if (holder.getItemViewType() == PlayerMedia.TYPE_PODCAST_DUO_CONTAINER) {
            ((PodcastDuoViewHolder) holder).bind(mediaItem, listener, this::formatDuration);
        }
    }

    private static final DiffUtil.ItemCallback<PlayerMedia> DIFF_CALLBACK = new DiffUtil.ItemCallback<PlayerMedia>() {
        @Override
        public boolean areItemsTheSame(@NonNull PlayerMedia oldItem, @NonNull PlayerMedia newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull PlayerMedia oldItem, @NonNull PlayerMedia newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getThumbnailUrl().equals(newItem.getThumbnailUrl()) &&
                    oldItem.getAuthorName().equals(newItem.getAuthorName());
        }
    };

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        private final ItemVideoM3Binding binding;

        public VideoViewHolder(ItemVideoM3Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final PlayerMedia media, final OnMediaClickListener listener, DurationFormatter formatter) {
            binding.videoTitle.setText(media.title);
            String formattedDuration = formatter.format(media.durationSeconds);
            binding.videoDetails.setText(String.format("%s • %s", media.authorName, formattedDuration));

            if (media.durationSeconds > 0) {
                binding.videoDuration.setText(formattedDuration);
                binding.videoDuration.setVisibility(View.VISIBLE);
            } else {
                binding.videoDuration.setVisibility(View.GONE);
            }

            Glide.with(itemView.getContext()).load(media.thumbnailUrl).placeholder(R.drawable.ic_placeholder_24dp).into(binding.videoThumbnail);
            Glide.with(itemView.getContext()).load(media.getUploaderAvatarUrl()).placeholder(R.drawable.ic_profile_placeholder).into(binding.authorAvatar);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onMediaClick(media);
            });

            binding.moreOptionsButton.setOnClickListener(v -> {
                if (itemView.getContext() instanceof FragmentActivity) {
                    FragmentManager fm = ((FragmentActivity) itemView.getContext()).getSupportFragmentManager();
                    MediaOptionsBottomSheet bottomSheet = MediaOptionsBottomSheet.newInstance(media);
                    bottomSheet.show(fm, MediaOptionsBottomSheet.TAG);
                }
            });
        }
    }

    public static class PodcastDuoViewHolder extends RecyclerView.ViewHolder {
        private final ItemPlayerPodcastDuoBinding binding;

        public PodcastDuoViewHolder(ItemPlayerPodcastDuoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final PlayerMedia mediaDuo, final OnMediaClickListener listener, DurationFormatter formatter) {
            final PlayerMedia p1 = mediaDuo.podcastOne;
            final PlayerMedia p2 = mediaDuo.podcastTwo;

            if (p1 != null) {
                binding.podcastItem1.setVisibility(View.VISIBLE);
                binding.podcastTitle1.setText(p1.title);
                binding.podcastAuthor1.setText(p1.authorName);
                if (p1.durationSeconds > 0) {
                    binding.podcastDuration1.setText(formatter.format(p1.durationSeconds));
                    binding.podcastDuration1.setVisibility(View.VISIBLE);
                } else {
                    binding.podcastDuration1.setVisibility(View.GONE);
                }
                Glide.with(itemView.getContext()).load(p1.getUploaderAvatarUrl()).placeholder(R.drawable.ic_profile_placeholder).into(binding.authorAvatar1);


                Glide.with(itemView.getContext()).load(p1.thumbnailUrl).placeholder(R.drawable.ic_placeholder_24dp).into(binding.podcastThumbnail1);
                binding.podcastItem1.setOnClickListener(v -> {
                    if (listener != null) listener.onMediaClick(p1);
                });

                binding.moreOptionsButton1.setOnClickListener(v -> {
                    if (itemView.getContext() instanceof FragmentActivity) {
                        FragmentManager fm = ((FragmentActivity) itemView.getContext()).getSupportFragmentManager();
                        MediaOptionsBottomSheet bottomSheet = MediaOptionsBottomSheet.newInstance(p1);
                        bottomSheet.show(fm, MediaOptionsBottomSheet.TAG);
                    }
                });

            } else {
                binding.podcastItem1.setVisibility(View.INVISIBLE);
            }

            if (p2 != null) {
                binding.podcastItem2.setVisibility(View.VISIBLE);
                binding.podcastTitle2.setText(p2.title);
                binding.podcastAuthor2.setText(p2.authorName);
                if (p2.durationSeconds > 0) {
                    binding.podcastDuration2.setText(formatter.format(p2.durationSeconds));
                    binding.podcastDuration2.setVisibility(View.VISIBLE);
                } else {
                    binding.podcastDuration2.setVisibility(View.GONE);
                }
                Glide.with(itemView.getContext()).load(p2.getUploaderAvatarUrl()).placeholder(R.drawable.ic_profile_placeholder).into(binding.authorAvatar2);
                Glide.with(itemView.getContext()).load(p2.thumbnailUrl).placeholder(R.drawable.ic_placeholder_24dp).into(binding.podcastThumbnail2);
                binding.podcastItem2.setOnClickListener(v -> {
                    if (listener != null) listener.onMediaClick(p2);
                });

                binding.moreOptionsButton2.setOnClickListener(v -> {
                    if (itemView.getContext() instanceof FragmentActivity) {
                        FragmentManager fm = ((FragmentActivity) itemView.getContext()).getSupportFragmentManager();
                        MediaOptionsBottomSheet bottomSheet = MediaOptionsBottomSheet.newInstance(p2);
                        bottomSheet.show(fm, MediaOptionsBottomSheet.TAG);
                    }
                });

            } else {
                binding.podcastItem2.setVisibility(View.INVISIBLE);
            }
        }
    }

    @FunctionalInterface
    interface DurationFormatter {
        String format(long seconds);
    }
}