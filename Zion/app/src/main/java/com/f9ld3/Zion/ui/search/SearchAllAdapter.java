// main/java/com/f9ld3/Zion/ui/search/SearchAllAdapter.java
package com.f9ld3.Zion.ui.search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner; // <-- Import LifecycleOwner
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;
import com.f9ld3.Zion.ui.feed.PostLikeViewModel; // <-- Import PostLikeViewModel
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;
import com.google.firebase.auth.FirebaseAuth;
import de.hdodenhof.circleimageview.CircleImageView;

public class SearchAllAdapter extends ListAdapter<Object, RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 0;
    private static final int TYPE_MEDIA = 1;
    private static final int TYPE_USER = 2;

    private final PlayerPostAdapter.OnMediaClickListener mediaClickListener;
    private final PostAdapter.OnPostClickListener postClickListener;
    private final PostLikeViewModel postLikeViewModel; // <-- Add member variable
    private final LifecycleOwner lifecycleOwner;       // <-- Add member variable

    public SearchAllAdapter(PlayerPostAdapter.OnMediaClickListener mediaListener,
                            PostAdapter.OnPostClickListener postListener,
                            PostLikeViewModel postLikeViewModel, // <-- Add constructor parameter
                            LifecycleOwner lifecycleOwner) {   // <-- Add constructor parameter
        super(DIFF_CALLBACK);
        this.mediaClickListener = mediaListener;
        this.postClickListener = postListener;
        this.postLikeViewModel = postLikeViewModel; // <-- Store it
        this.lifecycleOwner = lifecycleOwner;       // <-- Store it
    }

    @Override
    public int getItemViewType(int position) {
        Object item = getItem(position);
        if (item instanceof Post) return TYPE_POST;
        if (item instanceof PlayerMedia) return TYPE_MEDIA;
        if (item instanceof UserProfile) return TYPE_USER;
        return -1; // Should not happen
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_POST) {
            // Ensure you have R.layout.item_feed_post layout defined
            return new PostAdapter.PostViewHolder(inflater.inflate(R.layout.item_feed_post, parent, false));
        }
        if (viewType == TYPE_MEDIA) {
            // Ensure you have R.layout.item_video_m3 layout defined and use ViewBinding
            return new PlayerPostAdapter.VideoViewHolder(com.f9ld3.Zion.databinding.ItemVideoM3Binding.inflate(inflater, parent, false));
        }
        // Ensure you have R.layout.item_user_search layout defined
        return new UserViewHolder(inflater.inflate(R.layout.item_user_search, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = getItem(position);
        if (holder.getItemViewType() == TYPE_POST) {
            // *** FIX: Pass the missing arguments ***
            ((PostAdapter.PostViewHolder) holder).bind(
                    (Post) item,
                    postClickListener,
                    postLikeViewModel,  // <-- Pass the ViewModel
                    lifecycleOwner);    // <-- Pass the LifecycleOwner
        } else if (holder.getItemViewType() == TYPE_MEDIA) {
            // Define how duration should be formatted
            ((PlayerPostAdapter.VideoViewHolder) holder).bind(
                    (PlayerMedia) item,
                    mediaClickListener,
                    seconds -> formatDuration(seconds)); // Using a helper method for formatting
        } else if (holder.getItemViewType() == TYPE_USER) {
            ((UserViewHolder) holder).bind((UserProfile) item);
        }
    }

    // Helper method to format duration (example)
    private String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) return "";
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }


    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView avatar;
        private final TextView username;
        private final TextView email; // Or maybe user handle/bio
        private final Button followButton; // Or similar action button

        public UserViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.user_avatar);
            username = itemView.findViewById(R.id.user_name);
            email = itemView.findViewById(R.id.user_email); // Adjust ID if needed
            followButton = itemView.findViewById(R.id.button_follow); // Adjust ID if needed
        }

        void bind(UserProfile user) {
            // Use Account Name or Username based on availability/preference
            username.setText(user.getAccountName() != null ? user.getAccountName() : user.getUsername());
            email.setText(user.getEmail()); // Or display username/handle like "@"+user.getUsername()

            Glide.with(itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder) // Use a suitable placeholder
                    .error(R.drawable.ic_profile_placeholder) // Use a suitable error placeholder
                    .into(avatar);

            String currentUid = FirebaseAuth.getInstance().getUid();
            // Hide follow button if viewing own profile
            if (currentUid != null && currentUid.equals(user.getUserId())) {
                followButton.setVisibility(View.GONE);
            } else {
                followButton.setVisibility(View.VISIBLE);
                // TODO: Set follow button text/state based on whether current user follows this user
                // followButton.setText("Follow"); // or "Unfollow"
                // followButton.setOnClickListener { /* Handle follow/unfollow action */ }
            }

            // Navigate to the user's profile/channel when the item is clicked
            itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("channelId", user.getUserId());
                // Pass Account Name or Username
                args.putString("channelName", user.getAccountName() != null ? user.getAccountName() : user.getUsername());
                try {
                    Navigation.findNavController(v).navigate(R.id.navigation_channel, args);
                } catch (Exception e) {
                    // Handle potential navigation errors (e.g., NavController not found)
                    Log.e("SearchAllAdapter", "Navigation failed for user item", e);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Object> DIFF_CALLBACK = new DiffUtil.ItemCallback<Object>() {
        @Override
        public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            if (oldItem instanceof Post && newItem instanceof Post) {
                // Ensure IDs are not null
                String oldId = ((Post) oldItem).getId();
                String newId = ((Post) newItem).getId();
                return oldId != null && oldId.equals(newId);
            }
            if (oldItem instanceof PlayerMedia && newItem instanceof PlayerMedia) {
                // Ensure IDs are not null
                String oldId = ((PlayerMedia) oldItem).getId();
                String newId = ((PlayerMedia) newItem).getId();
                return oldId != null && oldId.equals(newId);
            }
            if (oldItem instanceof UserProfile && newItem instanceof UserProfile) {
                // Ensure IDs are not null
                String oldId = ((UserProfile) oldItem).getUserId();
                String newId = ((UserProfile) newItem).getUserId();
                return oldId != null && oldId.equals(newId);
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            // Add null checks for content comparison
            try {
                if (oldItem instanceof Post && newItem instanceof Post) {
                    Post oldP = (Post) oldItem;
                    Post newP = (Post) newItem;
                    return java.util.Objects.equals(oldP.getTextContent(), newP.getTextContent()) &&
                            oldP.getLikeCount() == newP.getLikeCount() && // Compare relevant fields
                            oldP.getCommentCount() == newP.getCommentCount();
                }
                if (oldItem instanceof PlayerMedia && newItem instanceof PlayerMedia) {
                    PlayerMedia oldM = (PlayerMedia) oldItem;
                    PlayerMedia newM = (PlayerMedia) newItem;
                    return java.util.Objects.equals(oldM.getTitle(), newM.getTitle()) &&
                            java.util.Objects.equals(oldM.getAuthorName(), newM.getAuthorName()); // Compare relevant fields
                }
                if (oldItem instanceof UserProfile && newItem instanceof UserProfile) {
                    UserProfile oldU = (UserProfile) oldItem;
                    UserProfile newU = (UserProfile) newItem;
                    // Compare Account Name or Username
                    return java.util.Objects.equals(oldU.getAccountName(), newU.getAccountName()) &&
                            java.util.Objects.equals(oldU.getUsername(), newU.getUsername()) &&
                            java.util.Objects.equals(oldU.getProfileImageUrl(), newU.getProfileImageUrl()); // Compare relevant fields
                }
            } catch (NullPointerException e) {
                Log.e("SearchAllAdapter", "NPE during content comparison", e);
                return false; // Treat as different if NPE occurs
            }
            return false;
        }
    };
}