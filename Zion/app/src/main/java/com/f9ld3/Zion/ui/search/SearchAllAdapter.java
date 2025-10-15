package com.f9ld3.Zion.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;

import de.hdodenhof.circleimageview.CircleImageView;

public class SearchAllAdapter extends ListAdapter<Object, RecyclerView.ViewHolder> {

    private static final int TYPE_VIDEO = 1;
    private static final int TYPE_PODCAST = 2;
    private static final int TYPE_USER = 3;

    private final PlayerPostAdapter.OnMediaClickListener mediaClickListener;

    public SearchAllAdapter(PlayerPostAdapter.OnMediaClickListener listener) {
        super(DIFF_CALLBACK);
        this.mediaClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = getItem(position);
        if (item instanceof PlayerMedia) {
            PlayerMedia media = (PlayerMedia) item;
            if (media.getType() == PlayerMedia.TYPE_VIDEO) {
                return TYPE_VIDEO;
            } else {
                return TYPE_PODCAST;
            }
        } else if (item instanceof UserProfile) {
            return TYPE_USER;
        }
        return TYPE_VIDEO; // Default
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_VIDEO:
            case TYPE_PODCAST:
                View mediaView = inflater.inflate(R.layout.item_video_m3, parent, false);
                return new MediaViewHolder(mediaView);
            case TYPE_USER:
                View userView = inflater.inflate(R.layout.item_user_search, parent, false);
                return new UserViewHolder(userView);
            default:
                View defaultView = inflater.inflate(R.layout.item_video_m3, parent, false);
                return new MediaViewHolder(defaultView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = getItem(position);

        if (holder instanceof MediaViewHolder && item instanceof PlayerMedia) {
            ((MediaViewHolder) holder).bind((PlayerMedia) item, mediaClickListener);
        } else if (holder instanceof UserViewHolder && item instanceof UserProfile) {
            ((UserViewHolder) holder).bind((UserProfile) item);
        }
    }

    // ViewHolder for Media (Video/Podcast)
    static class MediaViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final TextView title;
        private final TextView details;
        private final CircleImageView avatar;

        public MediaViewHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.video_thumbnail);
            title = itemView.findViewById(R.id.video_title);
            details = itemView.findViewById(R.id.video_details);
            avatar = itemView.findViewById(R.id.author_avatar);
        }

        public void bind(PlayerMedia media, PlayerPostAdapter.OnMediaClickListener listener) {
            title.setText(media.getTitle());
            details.setText(media.getAuthorName());

            Glide.with(itemView.getContext())
                    .load(media.getThumbnailUrl())
                    .placeholder(R.drawable.ic_placeholder_24dp)
                    .into(thumbnail);

            Glide.with(itemView.getContext())
                    .load(media.getUploaderAvatarUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(avatar);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMediaClick(media);
                }
            });
        }
    }

    // ViewHolder for Users
    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView avatar;
        private final TextView username;
        private final TextView email;

        public UserViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.user_avatar);
            username = itemView.findViewById(R.id.user_name);
            email = itemView.findViewById(R.id.user_email);
        }

        public void bind(UserProfile user) {
            username.setText(user.getUsername());
            email.setText(user.getEmail());

            Glide.with(itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(avatar);

            itemView.setOnClickListener(v -> {
                // TODO: Navigate to user profile
            });
        }
    }

    private static final DiffUtil.ItemCallback<Object> DIFF_CALLBACK = new DiffUtil.ItemCallback<Object>() {
        @Override
        public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            if (oldItem.getClass() != newItem.getClass()) {
                return false;
            }

            if (oldItem instanceof PlayerMedia && newItem instanceof PlayerMedia) {
                return ((PlayerMedia) oldItem).getId().equals(((PlayerMedia) newItem).getId());
            } else if (oldItem instanceof UserProfile && newItem instanceof UserProfile) {
                return ((UserProfile) oldItem).getUserId().equals(((UserProfile) newItem).getUserId());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            return oldItem.equals(newItem);
        }

    };
}