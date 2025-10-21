// main/java/com/f9ld3/Zion/ui/feed/PostAdapter.java
package com.f9ld3.Zion.ui.feed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log; // Added for logging
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.google.android.material.color.MaterialColors;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.hdodenhof.circleimageview.CircleImageView;
import com.google.firebase.Timestamp; // <<< Import Timestamp

public class PostAdapter extends ListAdapter<Post, PostAdapter.PostViewHolder> {

    private static final String TAG = "PostAdapter";

    public interface OnPostClickListener {
        void onPostItemClick(Post post);
        void onLikeClick(Post post);
        void onCommentClick(Post post);
        // Add if needed: void onShareClick(Post post);
        void onAuthorClick(Post post); // Navigate to author's profile
        void onOptionClick(Post post, View anchorView); // For more options
    }

    private final OnPostClickListener listener;
    private final LifecycleOwner lifecycleOwner;
    private final PostLikeViewModel postLikeViewModel;
    private final PollViewModel pollViewModel;

    public PostAdapter(OnPostClickListener listener, LifecycleOwner lifecycleOwner, FragmentActivity activity) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.lifecycleOwner = lifecycleOwner;
        // Get ViewModels scoped to the Activity/Fragment
        this.postLikeViewModel = new ViewModelProvider(activity).get(PostLikeViewModel.class);
        this.pollViewModel = new ViewModelProvider(activity).get(PollViewModel.class);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_post, parent, false);
        return new PostViewHolder(view, listener, postLikeViewModel, pollViewModel, lifecycleOwner);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = getItem(position);
        holder.bind(post);
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView authorAvatar;
        private final TextView authorName, postTimestamp, postContent, likeCount, commentCount;
        private final RecyclerView postMediaGrid; // For grid display
        private final ImageButton likeButton, commentButton, optionsButton;
        private final LinearLayout pollContainer;
        private final TextView pollDetailsText;

        private final OnPostClickListener clickListener;
        private final PostLikeViewModel likeViewModel;
        private final PollViewModel pollViewModel;
        private final LifecycleOwner lifecycleOwner;
        private MediaGridAdapter mediaGridAdapter;

        public PostViewHolder(@NonNull View itemView, OnPostClickListener listener, PostLikeViewModel likeViewModel, PollViewModel pollViewModel, LifecycleOwner lifecycleOwner) {
            super(itemView);
            this.clickListener = listener;
            this.likeViewModel = likeViewModel;
            this.pollViewModel = pollViewModel;
            this.lifecycleOwner = lifecycleOwner;

            authorAvatar = itemView.findViewById(R.id.author_avatar);
            authorName = itemView.findViewById(R.id.author_name);
            postTimestamp = itemView.findViewById(R.id.post_timestamp);
            postContent = itemView.findViewById(R.id.post_content);
            postMediaGrid = itemView.findViewById(R.id.post_media_grid);
            likeButton = itemView.findViewById(R.id.like_button);
            likeCount = itemView.findViewById(R.id.like_count);
            commentButton = itemView.findViewById(R.id.comment_button);
            commentCount = itemView.findViewById(R.id.comment_count);
            optionsButton = itemView.findViewById(R.id.post_options_button);
            pollContainer = itemView.findViewById(R.id.poll_container);
            pollDetailsText = itemView.findViewById(R.id.poll_details_text);
        }

        public void bind(final Post post) {
            if (post == null) {
                Log.w(TAG, "Binding null post at position: " + getAdapterPosition());
                // Optionally clear views or show placeholder
                return;
            }
            Context context = itemView.getContext();

            // Author Info
            authorName.setText(post.getAuthorName());

            // <<< FIX: Get milliseconds from Long object >>>
            Long postTime = post.getTimestamp(); // <-- CHANGED FROM Timestamp
            if (postTime != null && postTime > 0) { // <-- Check if > 0
                postTimestamp.setText(DateUtils.getRelativeTimeSpanString(postTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)); // <-- Removed .toDate().getTime()
                postTimestamp.setVisibility(View.VISIBLE);
            } else {
                postTimestamp.setVisibility(View.GONE); // Hide if timestamp is null or 0
            }
            // <<< END FIX >>>

            Glide.with(context)
                    .load(post.getAuthorAvatarUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(authorAvatar);

            // Click listeners for author info
            View.OnClickListener authorClickListener = v -> {
                if (clickListener != null) clickListener.onAuthorClick(post);
            };
            authorAvatar.setOnClickListener(authorClickListener);
            authorName.setOnClickListener(authorClickListener);

            // Post Content
            setPostText(post.getTextContent());

            // Actions Setup
            setupActions(post, context);

            // Content Type Specific Setup
            switch (post.getPostType()) {
                case Post.TYPE_POLL:
                case Post.TYPE_QUIZ:
                    postMediaGrid.setVisibility(View.GONE);
                    pollContainer.setVisibility(View.VISIBLE);
                    pollDetailsText.setVisibility(View.VISIBLE);
                    setupPoll(post);
                    break;
                case Post.TYPE_TEXT_MEDIA:
                default:
                    pollContainer.setVisibility(View.GONE);
                    pollDetailsText.setVisibility(View.GONE);
                    setupMediaGrid(post);
                    break;
            }

            // Overall item click (optional, could navigate to detail)
            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onPostItemClick(post);
            });
        }

        private void setupActions(Post post, Context context) {
            // Like Button
            likeButton.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onLikeClick(post);
            });

            // Observe Like State & Count
            ColorStateList likedTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.teal)); // Use theme color
            ColorStateList defaultTint = ColorStateList.valueOf(MaterialColors.getColor(likeButton, com.google.android.material.R.attr.colorOnSurfaceVariant));

            likeViewModel.isLiked(post.getId()).observe(lifecycleOwner, isLiked -> {
                likeButton.setImageTintList(Boolean.TRUE.equals(isLiked) ? likedTint : defaultTint);
            });

            // Like Count (observe directly from post or use a separate LiveData if needed)
            // Assuming Post object is updated via Firestore listener in Fragment/ViewModel
            likeCount.setText(formatCount(post.getLikeCount()));
            likeCount.setVisibility(post.getLikeCount() > 0 ? View.VISIBLE : View.GONE);


            // Comment Button
            commentButton.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onCommentClick(post);
            });
            commentCount.setText(formatCount(post.getCommentCount()));
            commentCount.setVisibility(post.getCommentCount() > 0 ? View.VISIBLE : View.GONE);

            // Options Button
            optionsButton.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onOptionClick(post, v);
                else showDefaultOptionsMenu(v, context, post); // Fallback
            });
        }

        // Format count (e.g., 1.2K) - simple version
        private String formatCount(int count) {
            if (count < 1000) return String.valueOf(count);
            if (count < 1_000_000) return String.format("%.1fk", count / 1000.0).replace(".0", "");
            return String.format("%.1fm", count / 1_000_000.0).replace(".0", "");
        }


        private void setupMediaGrid(Post post) {
            if (post.getMediaItems() != null && !post.getMediaItems().isEmpty()) {
                postMediaGrid.setVisibility(View.VISIBLE);
                int itemCount = post.getMediaItems().size();
                int spanCount; // Default
                int maxGridHeightDp = 300; // Max height for the grid

                if (itemCount == 2 || itemCount == 4) {
                    spanCount = 2;
                    maxGridHeightDp = 240; // Adjust height for 2xN grid
                } else if (itemCount == 3) {
                    spanCount = 2; // Or use a custom LayoutManager for 3 items
                    maxGridHeightDp = 240;
                } else if (itemCount > 4) {
                    spanCount = 2;
                    maxGridHeightDp = 240;
                } else {
                    spanCount = 1;
                }

                GridLayoutManager layoutManager = new GridLayoutManager(itemView.getContext(), spanCount);
                // --- Special layout for 3 items ---
                if (itemCount == 3) {
                    layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                        @Override
                        public int getSpanSize(int position) {
                            // First item takes full width if span is 2
                            return (position == 0 && spanCount == 2) ? 2 : 1;
                        }
                    });
                }
                // --- End special layout ---

                postMediaGrid.setLayoutManager(layoutManager);
                mediaGridAdapter = new MediaGridAdapter(post, post.getMediaItems(), clickListener);
                postMediaGrid.setAdapter(mediaGridAdapter);

                // Adjust height dynamically (optional, improves appearance)
                ViewGroup.LayoutParams params = postMediaGrid.getLayoutParams();
                params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, maxGridHeightDp, itemView.getContext().getResources().getDisplayMetrics());
                postMediaGrid.setLayoutParams(params);

            } else {
                postMediaGrid.setVisibility(View.GONE);
            }
        }

        private void setupPoll(Post post) {
            pollContainer.removeAllViews(); // Clear previous options
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            // Define context here to ensure it's accessible within the lambda's scope
            final Context context = itemView.getContext();

            pollViewModel.getUserVoteForPost(post.getId()).observe(lifecycleOwner, userVoteIndex -> {
                boolean hasVoted = userVoteIndex != null && userVoteIndex != -1;

                // Update total votes display
                String details = formatCount(post.getTotalVotes()) + (post.getTotalVotes() == 1 ? " vote" : " votes");
                if (post.getPostType().equals(Post.TYPE_QUIZ) && hasVoted) {
                    details += " • Final results"; // Show only after voting for quiz
                } else if (post.getPostType().equals(Post.TYPE_POLL)){
                    // Optionally add "Final Results" if poll is closed (needs field in Post)
                }
                pollDetailsText.setText(details);

                // Re-inflate options based on new state
                pollContainer.removeAllViews(); // Ensure clean slate
                if (post.getPollOptions() == null) return; // Add null check
                for (int i = 0; i < post.getPollOptions().size(); i++) {
                    // *** FIX: Inflate the correct layout R.layout.item_poll_option ***
                    View optionView = inflater.inflate(R.layout.item_poll_option, pollContainer, false);
                    PollOption option = post.getPollOptions().get(i);
                    TextView optionText = optionView.findViewById(R.id.poll_option_text);
                    ProgressBar progressBar = optionView.findViewById(R.id.poll_option_progress);
                    TextView percentageText = optionView.findViewById(R.id.poll_option_percentage);
                    ImageView voteIndicator = optionView.findViewById(R.id.your_vote_indicator); // Checkmark/X

                    optionText.setText(option.getOptionText());

                    if (hasVoted) {
                        // --- Show Results ---
                        optionView.setClickable(false); // Disable clicking after vote
                        progressBar.setVisibility(View.VISIBLE);
                        percentageText.setVisibility(View.VISIBLE);
                        int percentage = (post.getTotalVotes() > 0) ? (int) (((float) option.getVoteCount() / post.getTotalVotes()) * 100) : 0;
                        progressBar.setProgress(percentage);
                        percentageText.setText(percentage + "%");

                        // Highlight user's vote
                        voteIndicator.setVisibility(userVoteIndex == i ? View.VISIBLE : View.GONE);

                        // Quiz Specific Highlighting
                        if (post.getPostType().equals(Post.TYPE_QUIZ)) {
                            if (i == post.getQuizCorrectOptionIndex()) {
                                // Correct answer styling
                                optionView.setBackgroundResource(R.drawable.poll_option_background_correct); // Greenish background
                                if (userVoteIndex == i) {
                                    voteIndicator.setImageResource(R.drawable.ic_check_circle_24dp); // Correct choice
                                    voteIndicator.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.teal))); // Or your success color
                                } else {
                                    // Show checkmark for correct answer even if not chosen
                                    voteIndicator.setImageResource(R.drawable.ic_check_circle_24dp);
                                    voteIndicator.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.teal)));
                                    voteIndicator.setVisibility(View.VISIBLE); // Make sure it's visible
                                }
                            } else if (userVoteIndex == i) {
                                // Incorrect answer styling (if user chose this)
                                optionView.setBackgroundResource(R.drawable.poll_option_background_incorrect); // Reddish background
                                voteIndicator.setImageResource(R.drawable.ic_error_24dp); // Incorrect choice
                                voteIndicator.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error)));
                            } else {
                                // Other incorrect options styling
                                optionView.setBackgroundResource(R.drawable.poll_option_background_default);
                            }
                        } else { // Regular Poll
                            if (userVoteIndex == i) {
                                optionView.setBackgroundResource(R.drawable.poll_option_background_voted); // Highlight voted option
                                voteIndicator.setImageResource(R.drawable.ic_check_circle_24dp); // User's choice
                                voteIndicator.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.teal)));
                            } else {
                                optionView.setBackgroundResource(R.drawable.poll_option_background_default);
                            }
                        }

                    } else {
                        // --- Allow Voting ---
                        progressBar.setVisibility(View.INVISIBLE); // Keep space but hide bar
                        percentageText.setVisibility(View.GONE);
                        voteIndicator.setVisibility(View.GONE);
                        optionView.setBackgroundResource(R.drawable.poll_option_background_default); // Ensure default background
                        final int optionIndex = i;
                        optionView.setOnClickListener(v -> {
                            pollViewModel.castVote(post, optionIndex);
                            // Optionally provide immediate visual feedback before Firestore updates
                            optionView.setBackgroundResource(R.drawable.poll_option_background_voted);
                        });
                    }
                    pollContainer.addView(optionView);
                }
            });
        }


        @ColorInt
        private int getColorFromAttr(@AttrRes int attrRes) {
            TypedValue typedValue = new TypedValue();
            itemView.getContext().getTheme().resolveAttribute(attrRes, typedValue, true);
            return typedValue.data;
        }

        private void setPostText(String text) {
            if (text == null || text.isEmpty()) {
                postContent.setVisibility(View.GONE);
                return;
            }
            postContent.setVisibility(View.VISIBLE);
            // Apply hashtag highlighting (example)
            SpannableString spannableString = new SpannableString(text);
            Pattern hashtagPattern = Pattern.compile("#(\\w+)"); // Basic hashtag pattern
            Matcher matcher = hashtagPattern.matcher(text);
            int hashtagColor = getColorFromAttr(com.google.android.material.R.attr.colorSecondary); // Use secondary color

            while (matcher.find()) {
                spannableString.setSpan(new ForegroundColorSpan(hashtagColor),
                        matcher.start(),
                        matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // Add ClickableSpan here if you want hashtags to be clickable
            }
            postContent.setText(spannableString);
            // Make links clickable (if needed)
            // Linkify.addLinks(postContent, Linkify.WEB_URLS);
            // postContent.setMovementMethod(LinkMovementMethod.getInstance());
        }

        // Default options menu if listener doesn't handle it
        private void showDefaultOptionsMenu(View anchor, Context context, Post post) {
            PopupMenu popup = new PopupMenu(context, anchor);
            // Inflate a default menu or dynamically add items
            popup.getMenu().add("Share");
            popup.getMenu().add("Report");
            // Add more...
            popup.setOnMenuItemClickListener(item -> {
                Toast.makeText(context, item.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        }
    }

    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            // Ensure IDs are not null before comparing
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            // Compare all relevant fields that affect UI
            return Objects.equals(oldItem.getTextContent(), newItem.getTextContent()) &&
                    oldItem.getLikeCount() == newItem.getLikeCount() &&
                    oldItem.getCommentCount() == newItem.getCommentCount() &&
                    Objects.equals(oldItem.getAuthorName(), newItem.getAuthorName()) && // Name changes
                    Objects.equals(oldItem.getAuthorAvatarUrl(), newItem.getAuthorAvatarUrl()) && // Avatar changes
                    Objects.equals(oldItem.getMediaItems(), newItem.getMediaItems()) && // Media changes
                    Objects.equals(oldItem.getPostType(), newItem.getPostType()) && // Type changes
                    Objects.equals(oldItem.getPollOptions(), newItem.getPollOptions()) && // Poll options/votes change
                    oldItem.getTotalVotes() == newItem.getTotalVotes() &&
                    oldItem.getQuizCorrectOptionIndex() == newItem.getQuizCorrectOptionIndex() &&
                    Objects.equals(oldItem.getTimestamp(), newItem.getTimestamp()); // Compare Longs
        }
    };
}