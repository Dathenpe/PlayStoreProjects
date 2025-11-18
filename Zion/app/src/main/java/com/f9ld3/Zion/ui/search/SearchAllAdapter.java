// main/java/com/f9ld3/Zion/ui/search/SearchAllAdapter.java
package com.f9ld3.Zion.ui.search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast; // <-- IMPORT
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
import com.f9ld3.Zion.ui.social.FollowViewModel; // <-- IMPORT
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // <-- IMPORT
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.Objects; // Import Objects
import java.util.concurrent.TimeUnit; // For formatDuration
import java.util.ArrayList; // Import ArrayList
import java.util.List; // Import List


public class SearchAllAdapter extends ListAdapter<Object, RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 0;
    private static final int TYPE_MEDIA = 1;
    private static final int TYPE_USER = 2;

    private final PlayerPostAdapter.OnMediaClickListener mediaClickListener;
    private final PostAdapter.OnPostClickListener postClickListener;
    private final PostLikeViewModel postLikeViewModel;
    private final PollViewModel pollViewModel;
    private final FollowViewModel followViewModel; // <-- ADD THIS
    private final LifecycleOwner lifecycleOwner;
    private final FragmentActivity activity;


    public SearchAllAdapter(PlayerPostAdapter.OnMediaClickListener mediaListener,
                            PostAdapter.OnPostClickListener postListener,
                            PostLikeViewModel postLikeViewModel,
                            FollowViewModel followViewModel, // <-- ADD THIS
                            LifecycleOwner lifecycleOwner,
                            FragmentActivity activity) {
        super(DIFF_CALLBACK);
        this.mediaClickListener = mediaListener;
        this.postClickListener = postListener;
        this.postLikeViewModel = postLikeViewModel;
        this.pollViewModel = new ViewModelProvider(activity).get(PollViewModel.class);
        this.followViewModel = followViewModel; // <-- ADD THIS
        this.lifecycleOwner = lifecycleOwner;
        this.activity = activity;
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
            View postView = inflater.inflate(R.layout.item_feed_post, parent, false);
            return new PostAdapter.PostViewHolder(postView, postClickListener, postLikeViewModel, pollViewModel, lifecycleOwner);
        }
        if (viewType == TYPE_MEDIA) {
            // Using ItemVideoM3Binding as it's the view holder used in PlayerPostAdapter
            return new PlayerPostAdapter.VideoViewHolder(com.f9ld3.Zion.databinding.ItemVideoM3Binding.inflate(inflater, parent, false));
        }
        // --- PASS VIEWMODELS AND LIFECYCLE OWNER ---
        View userView = inflater.inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(userView, followViewModel, lifecycleOwner, activity);
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = getItem(position);
        if (holder.getItemViewType() == TYPE_POST) {
            ((PostAdapter.PostViewHolder) holder).bind((Post) item);
        } else if (holder.getItemViewType() == TYPE_MEDIA) {
            ((PlayerPostAdapter.VideoViewHolder) holder).bind(
                    (PlayerMedia) item,
                    mediaClickListener,
                    this::formatDuration);
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
        private final TextView email;
        private final Button followButton;

        // --- ADDED MEMBERS ---
        private final FollowViewModel followViewModel;
        private final LifecycleOwner lifecycleOwner;
        private final FragmentActivity activity;

        // --- UPDATED CONSTRUCTOR ---
        public UserViewHolder(View itemView, FollowViewModel followViewModel, LifecycleOwner lifecycleOwner, FragmentActivity activity) {
            super(itemView);
            this.followViewModel = followViewModel;
            this.lifecycleOwner = lifecycleOwner;
            this.activity = activity; // Store activity context

            avatar = itemView.findViewById(R.id.user_avatar);
            username = itemView.findViewById(R.id.user_name);
            email = itemView.findViewById(R.id.user_email);
            followButton = itemView.findViewById(R.id.button_follow);
        }

        void bind(UserProfile user) {
            username.setText(user.getAccountName() != null ? user.getAccountName() : user.getUsername());
            email.setText(user.getEmail());

            Glide.with(itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(avatar);

            String currentUid = FirebaseAuth.getInstance().getUid();
            String targetUserId = user.getUserId(); // Get target ID

            // Hide follow button if viewing own profile
            if (currentUid != null && currentUid.equals(targetUserId)) {
                followButton.setVisibility(View.GONE);
            } else {
                followButton.setVisibility(View.VISIBLE);

                // --- FULLY IMPLEMENTED FOLLOW LOGIC ---

                // 1. Check the *initial* follow status
                // We use the new isFollowing(targetUserId) method
                followViewModel.checkFollowStatus(targetUserId);

                // 2. Observe changes to the follow status (e.g., after a click)
                // Use the new isFollowing(targetUserId) method
                followViewModel.isFollowing(targetUserId).observe(lifecycleOwner, isFollowing -> {
                    if (isFollowing) {
                        followButton.setText("Following");
                        // You can also change style here if you have a "tonal" button style
                        // followButton.setStyle(R.style.Widget_Material3_Button_TonalButton);
                    } else {
                        followButton.setText("Follow");
                        // followButton.setStyle(R.style.Widget_Material3_Button);
                    }
                });

                // 3. Set the click listener to perform the action
                followButton.setOnClickListener(v -> {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null || currentUser.isAnonymous()) {
                        Toast.makeText(itemView.getContext(), R.string.login_for_features, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check the *current* state from the LiveData
                    Boolean isCurrentlyFollowing = followViewModel.isFollowing(targetUserId).getValue();
                    if (isCurrentlyFollowing != null) {
                        if (isCurrentlyFollowing) {
                            followViewModel.unfollowUser(targetUserId);
                        } else {
                            // --- (FIXED: Use fallback for account name) ---
                            String nameToFollow = user.getAccountName() != null && !user.getAccountName().isEmpty()
                                    ? user.getAccountName()
                                    : user.getUsername();
                            if (nameToFollow == null || nameToFollow.isEmpty()) nameToFollow = "user"; // Extra fallback
                            followViewModel.followUser(targetUserId, nameToFollow);
                        }
                    }
                });

                // 4. (Optional but good) Observe toast messages from the shared ViewModel
                // Use Activity as LifecycleOwner for messages to avoid re-showing on bind
                followViewModel.getMessage().observe(activity, message -> {
                    if (message != null && !message.isEmpty()) {
                        Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();
                        followViewModel.clearMessage();
                    }
                });
                // --- END OF IMPLEMENTED LOGIC ---
            }

            // Navigate to the user's profile/channel when the item is clicked
            itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("channelId", user.getUserId());
                args.putString("channelName", user.getAccountName() != null ? user.getAccountName() : user.getUsername());
                try {
                    Navigation.findNavController(v).navigate(R.id.navigation_channel, args);
                } catch (Exception e) {
                    Log.e("SearchAllAdapter", "Navigation failed for user item", e);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Object> DIFF_CALLBACK = new DiffUtil.ItemCallback<Object>() {
        @Override
        public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            if (oldItem instanceof Post && newItem instanceof Post) {
                String oldId = ((Post) oldItem).getId();
                String newId = ((Post) newItem).getId();
                return oldId != null && oldId.equals(newId);
            }
            if (oldItem instanceof PlayerMedia && newItem instanceof PlayerMedia) {
                String oldId = ((PlayerMedia) oldItem).getId();
                String newId = ((PlayerMedia) newItem).getId();
                return oldId != null && oldId.equals(newId);
            }
            if (oldItem instanceof UserProfile && newItem instanceof UserProfile) {
                String oldId = ((UserProfile) oldItem).getUserId();
                String newId = ((UserProfile) newItem).getUserId();
                return oldId != null && oldId.equals(newId);
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            try {
                if (oldItem instanceof Post && newItem instanceof Post) {
                    Post oldP = (Post) oldItem;
                    Post newP = (Post) newItem;
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
                    return Objects.equals(oldM.getTitle(), newM.getTitle()) &&
                            Objects.equals(oldM.getThumbnailUrl(), newM.getThumbnailUrl()) &&
                            Objects.equals(oldM.getAuthorName(), newM.getAuthorName()) &&
                            oldM.getDurationSeconds() == newM.getDurationSeconds();
                }
                if (oldItem instanceof UserProfile && newItem instanceof UserProfile) {
                    UserProfile oldU = (UserProfile) oldItem;
                    UserProfile newU = (UserProfile) newItem;
                    return Objects.equals(oldU.getAccountName(), newU.getAccountName()) &&
                            Objects.equals(oldU.getUsername(), newU.getUsername()) &&
                            Objects.equals(oldU.getEmail(), newU.getEmail()) &&
                            Objects.equals(oldU.getProfileImageUrl(), newU.getProfileImageUrl());
                }
            } catch (NullPointerException e) {
                Log.e("SearchAllAdapter", "NPE during content comparison", e);
                return false;
            }
            return false;
        }
    };
}