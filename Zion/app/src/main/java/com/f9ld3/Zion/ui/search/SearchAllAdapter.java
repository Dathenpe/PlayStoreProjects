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
import androidx.lifecycle.ViewModelProvider; // Import ViewModelProvider
import androidx.fragment.app.FragmentActivity; // Import FragmentActivity
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.ui.feed.PollViewModel; // <-- Import PollViewModel
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;
import com.f9ld3.Zion.ui.feed.PostLikeViewModel; // <-- Import PostLikeViewModel
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;
import com.google.firebase.auth.FirebaseAuth;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.Objects; // Import Objects
import java.util.concurrent.TimeUnit; // For formatDuration


public class SearchAllAdapter extends ListAdapter<Object, RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 0;
    private static final int TYPE_MEDIA = 1;
    private static final int TYPE_USER = 2;

    private final PlayerPostAdapter.OnMediaClickListener mediaClickListener;
    private final PostAdapter.OnPostClickListener postClickListener;
    private final PostLikeViewModel postLikeViewModel;
    private final PollViewModel pollViewModel; // <-- Add PollViewModel member variable
    private final LifecycleOwner lifecycleOwner;
    private final FragmentActivity activity; // <-- Add FragmentActivity member variable


    public SearchAllAdapter(PlayerPostAdapter.OnMediaClickListener mediaListener,
                            PostAdapter.OnPostClickListener postListener,
                            PostLikeViewModel postLikeViewModel,
                            LifecycleOwner lifecycleOwner,
                            FragmentActivity activity) { // <-- Add activity parameter
        super(DIFF_CALLBACK);
        this.mediaClickListener = mediaListener;
        this.postClickListener = postListener;
        this.postLikeViewModel = postLikeViewModel;
        // Get PollViewModel scoped to the Activity/Fragment
        this.pollViewModel = new ViewModelProvider(activity).get(PollViewModel.class); // <-- Initialize PollViewModel
        this.lifecycleOwner = lifecycleOwner;
        this.activity = activity; // <-- Store activity
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
            // *** FIX: Pass all required arguments to PostViewHolder constructor ***
            View postView = inflater.inflate(R.layout.item_feed_post, parent, false);
            return new PostAdapter.PostViewHolder(postView, postClickListener, postLikeViewModel, pollViewModel, lifecycleOwner);
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
            // Bind method only needs the post object
            ((PostAdapter.PostViewHolder) holder).bind((Post) item);
        } else if (holder.getItemViewType() == TYPE_MEDIA) {
            ((PlayerPostAdapter.VideoViewHolder) holder).bind(
                    (PlayerMedia) item,
                    mediaClickListener,
                    this::formatDuration); // Using the helper method
        } else if (holder.getItemViewType() == TYPE_USER) {
            ((UserViewHolder) holder).bind((UserProfile) item);
        }
    }

    // Helper method to format duration
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
                    // Compare based on PostAdapter's DIFF_CALLBACK logic
                    return Objects.equals(oldP.getTextContent(), newP.getTextContent()) &&
                            oldP.getLikeCount() == newP.getLikeCount() &&
                            oldP.getCommentCount() == newP.getCommentCount() &&
                            Objects.equals(oldP.getAuthorName(), newP.getAuthorName()) &&
                            Objects.equals(oldP.getAuthorAvatarUrl(), newP.getAuthorAvatarUrl()) &&
                            Objects.equals(oldP.getMediaItems(), newP.getMediaItems()) &&
                            Objects.equals(oldP.getPostType(), newP.getPostType()) &&
                            Objects.equals(oldP.getPollOptions(), newP.getPollOptions()) &&
                            oldP.getTotalVotes() == newP.getTotalVotes() &&
                            oldP.getQuizCorrectOptionIndex() == newP.getQuizCorrectOptionIndex();
                }
                if (oldItem instanceof PlayerMedia && newItem instanceof PlayerMedia) {
                    PlayerMedia oldM = (PlayerMedia) oldItem;
                    PlayerMedia newM = (PlayerMedia) newItem;
                    // Compare relevant fields for PlayerMedia
                    return Objects.equals(oldM.getTitle(), newM.getTitle()) &&
                            Objects.equals(oldM.getThumbnailUrl(), newM.getThumbnailUrl()) &&
                            Objects.equals(oldM.getAuthorName(), newM.getAuthorName()) &&
                            oldM.getDurationSeconds() == newM.getDurationSeconds();
                }
                if (oldItem instanceof UserProfile && newItem instanceof UserProfile) {
                    UserProfile oldU = (UserProfile) oldItem;
                    UserProfile newU = (UserProfile) newItem;
                    // Compare relevant fields for UserProfile
                    return Objects.equals(oldU.getAccountName(), newU.getAccountName()) &&
                            Objects.equals(oldU.getUsername(), newU.getUsername()) &&
                            Objects.equals(oldU.getEmail(), newU.getEmail()) &&
                            Objects.equals(oldU.getProfileImageUrl(), newU.getProfileImageUrl());
                }
            } catch (NullPointerException e) {
                Log.e("SearchAllAdapter", "NPE during content comparison", e);
                return false; // Treat as different if NPE occurs
            }
            return false;
        }
    };
}