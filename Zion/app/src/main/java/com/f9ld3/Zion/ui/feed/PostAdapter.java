// main/java/com/f9ld3/Zion/ui/feed/PostAdapter.java
package com.f9ld3.Zion.ui.feed;

import android.annotation.SuppressLint; // Import SuppressLint
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color; // Import Color
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import androidx.annotation.Nullable; // Import Nullable
import androidx.core.content.ContextCompat; // Ensure ContextCompat is imported
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData; // Import LiveData
import androidx.lifecycle.Observer; // Import Observer
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2; // Import ViewPager2
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R; // Ensure R is imported
import com.f9ld3.Zion.databinding.ItemFeedMediaPageBinding; // Import the pager item binding
import com.google.android.material.color.MaterialColors;
import com.google.android.material.imageview.ShapeableImageView; // <-- Import ShapeableImageView

import java.util.concurrent.TimeUnit; // Import TimeUnit
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.hdodenhof.circleimageview.CircleImageView;
import com.google.firebase.Timestamp;

public class PostAdapter extends ListAdapter<Post, PostAdapter.PostViewHolder> {

    private static final String TAG = "PostAdapter";

    public interface OnPostClickListener {
        void onPostItemClick(Post post);
        void onLikeClick(Post post);
        void onDislikeClick(Post post);
        void onCommentClick(Post post);
        void onAuthorClick(Post post);
        void onOptionClick(Post post, View anchorView);
    }

    private final OnPostClickListener listener;
    private final LifecycleOwner lifecycleOwner;
    private final PostLikeViewModel postLikeViewModel;
    private final PollViewModel pollViewModel;

    public PostAdapter(OnPostClickListener listener, LifecycleOwner lifecycleOwner, FragmentActivity activity) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.lifecycleOwner = lifecycleOwner;
        this.postLikeViewModel = new ViewModelProvider(activity).get(PostLikeViewModel.class);
        this.pollViewModel = new ViewModelProvider(activity).get(PollViewModel.class);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_post, parent, false);
        // Pass PollViewModel to ViewHolder
        return new PostViewHolder(view, listener, postLikeViewModel, pollViewModel, lifecycleOwner);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = getItem(position);
        holder.bind(post);
    }

    // --- ViewHolder ---
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        // --- Standard Views ---
        private final CircleImageView authorAvatar;
        private final TextView authorName, postTimestamp, postContent, likeCount, commentCount;
        private final ImageButton likeButton, dislikeButton, commentButton, optionsButton;
        private final LinearLayout pollContainer;
        private final TextView pollDetailsText;

        // --- Media Pager Views ---
        private final FrameLayout mediaPagerContainer;
        private final ViewPager2 mediaPagerFeed; // Use ViewPager2
        private final TextView mediaIndicatorFeed; // Keep indicator
        private FeedMediaPagerAdapter feedMediaPagerAdapter; // Adapter for the internal pager - made non-final

        // --- References ---
        private final OnPostClickListener clickListener;
        private final PostLikeViewModel likeViewModel;
        private final PollViewModel pollViewModel;
        private final LifecycleOwner lifecycleOwner;
        private Post currentBoundPost; // Store the initially bound post

        // Observer references to manage removal
        private Observer<Post> postDataObserver;
        private Observer<Integer> userVoteObserver;
        private String currentPostId = null;

        public PostViewHolder(@NonNull View itemView, OnPostClickListener listener, PostLikeViewModel likeViewModel, PollViewModel pollViewModel, LifecycleOwner lifecycleOwner) {
            super(itemView);
            this.clickListener = listener;
            this.likeViewModel = likeViewModel;
            this.pollViewModel = pollViewModel;
            this.lifecycleOwner = lifecycleOwner;

            // --- Find Standard Views ---
            authorAvatar = itemView.findViewById(R.id.author_avatar);
            authorName = itemView.findViewById(R.id.author_name);
            postTimestamp = itemView.findViewById(R.id.post_timestamp);
            postContent = itemView.findViewById(R.id.post_content);
            likeButton = itemView.findViewById(R.id.like_button);
            dislikeButton = itemView.findViewById(R.id.dislike_button);
            likeCount = itemView.findViewById(R.id.like_count);
            commentButton = itemView.findViewById(R.id.comment_button);
            commentCount = itemView.findViewById(R.id.comment_count);
            optionsButton = itemView.findViewById(R.id.post_options_button);
            pollContainer = itemView.findViewById(R.id.poll_container);
            pollDetailsText = itemView.findViewById(R.id.poll_details_text);

            // --- Find Media Pager Views (Using updated IDs) ---
            mediaPagerContainer = itemView.findViewById(R.id.media_pager_container);
            mediaPagerFeed = itemView.findViewById(R.id.media_pager_feed); // Ensure this ID exists
            mediaIndicatorFeed = itemView.findViewById(R.id.media_indicator_feed); // Ensure this ID exists
        }

        public void bind(final Post post) {
            if (post == null) {
                Log.w(TAG, "Binding null post at position: " + getAdapterPosition());
                // Clear any existing observers if binding a null post
                clearPollObservers();
                return;
            }
            currentBoundPost = post; // Store initially bound post
            currentPostId = post.getId(); // Store current ID
            Context context = itemView.getContext();

            // Author Info
            authorName.setText(post.getAuthorName());

            // --- THIS IS THE FIX ---
            Timestamp postTime = post.getTimestamp(); // Get the Timestamp object
            if (postTime != null) {
                try {
                    long postTimeMillis = postTime.toDate().getTime(); // Convert to long milliseconds
                    postTimestamp.setText(DateUtils.getRelativeTimeSpanString(postTimeMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
                    postTimestamp.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting timestamp", e);
                    postTimestamp.setVisibility(View.GONE);
                }
            } else {
                postTimestamp.setVisibility(View.GONE);
            }
            // --- END FIX ---

            Glide.with(context)
                    .load(post.getAuthorAvatarUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(authorAvatar);
            View.OnClickListener authorClickListener = v -> {
                if (clickListener != null && currentBoundPost != null) clickListener.onAuthorClick(currentBoundPost); // Use currentBoundPost
            };
            authorAvatar.setOnClickListener(authorClickListener);
            authorName.setOnClickListener(authorClickListener);

            // Post Content Text
            setPostText(post.getTextContent());

            // Actions Setup (Pass currentBoundPost or use currentPostId)
            setupActions(post, context);

            // Content Type Specific Setup
            switch (post.getPostType()) {
                case Post.TYPE_POLL:
                case Post.TYPE_QUIZ:
                    mediaPagerContainer.setVisibility(View.GONE);
                    if (feedMediaPagerAdapter != null) feedMediaPagerAdapter.setMediaItems(new ArrayList<>());
                    pollContainer.setVisibility(View.VISIBLE);
                    pollDetailsText.setVisibility(View.VISIBLE);
                    // *** CALL NEW setupPollObservers ***
                    setupPollObservers(post.getId());
                    break;
                case Post.TYPE_TEXT_MEDIA:
                default:
                    // *** Clear observers if switching away from poll/quiz ***
                    clearPollObservers();
                    pollContainer.setVisibility(View.GONE);
                    pollDetailsText.setVisibility(View.GONE);
                    setupMediaPager(post.getMediaItems()); // *** Call updated setupMediaPager ***
                    break;
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null && currentBoundPost != null) clickListener.onPostItemClick(currentBoundPost); // Use currentBoundPost
            });
        }

        // --- Setup Media Pager (Using ViewPager2) ---
        private void setupMediaPager(List<MediaItem> items) {
            if (items == null || items.isEmpty()) {
                mediaPagerContainer.setVisibility(View.GONE);
                if (feedMediaPagerAdapter != null) feedMediaPagerAdapter.setMediaItems(new ArrayList<>()); // Clear pager adapter
                return;
            }

            mediaPagerContainer.setVisibility(View.VISIBLE);

            // Initialize adapter if it doesn't exist
            if (feedMediaPagerAdapter == null) {
                feedMediaPagerAdapter = new FeedMediaPagerAdapter(itemView.getContext(),
                        (pagerItems, position) -> {
                            if (clickListener != null && currentBoundPost != null) { // Use currentBoundPost
                                // Navigate to fullscreen view when clicking media pager item
                                Intent intent = new Intent(itemView.getContext(), FullScreenMediaActivity.class);
                                intent.putExtra(FullScreenMediaActivity.EXTRA_MEDIA_ITEMS, new ArrayList<>(pagerItems)); // Pass the list
                                intent.putExtra(FullScreenMediaActivity.EXTRA_START_POSITION, position); // Pass the clicked position
                                itemView.getContext().startActivity(intent);
                            }
                        }
                );
                mediaPagerFeed.setAdapter(feedMediaPagerAdapter);
                // Register callback only once during initialization
                mediaPagerFeed.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        // Check if adapter is not null before calling getItemCount
                        if (feedMediaPagerAdapter != null) {
                            updateMediaIndicator(position, feedMediaPagerAdapter.getItemCount());
                        }
                    }
                });
            }

            feedMediaPagerAdapter.setMediaItems(items);
            mediaPagerFeed.setCurrentItem(0, false); // Start at the first item
            updateMediaIndicator(0, items.size());

            // Dynamically set ViewPager height based on aspect ratio (e.g., 16:9)
            mediaPagerFeed.post(() -> {
                // Check if view is still attached
                if (mediaPagerFeed == null || itemView == null || itemView.getContext() == null) return;
                int pagerWidth = mediaPagerFeed.getWidth();
                if (pagerWidth > 0) {
                    int pagerHeight = (int) (pagerWidth * (9.0 / 16.0));
                    int maxHeightPx = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 350,
                            itemView.getContext().getResources().getDisplayMetrics()
                    );
                    pagerHeight = Math.min(pagerHeight, maxHeightPx);

                    ViewGroup.LayoutParams params = mediaPagerFeed.getLayoutParams();
                    params.height = pagerHeight;
                    mediaPagerFeed.setLayoutParams(params);
                }
            });
        }
        // --- End Setup Media Pager ---

        // --- Update Pager Indicator ---
        private void updateMediaIndicator(int currentPosition, int totalItems) {
            if (mediaIndicatorFeed == null) return; // Add null check
            if (totalItems > 1) {
                mediaIndicatorFeed.setText(String.format("%d / %d", currentPosition + 1, totalItems));
                mediaIndicatorFeed.setVisibility(View.VISIBLE);
            } else {
                mediaIndicatorFeed.setVisibility(View.GONE);
            }
        }
        // --- End Update Pager Indicator ---


        private void setupActions(Post post, Context context) {
            // Like Button
            likeButton.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onLikeClick(post);
            });
            // Dislike Button
            dislikeButton.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onDislikeClick(post);
            });
            // Observe Like State
            likeViewModel.isLiked(post.getId()).observe(lifecycleOwner, isLiked -> {
                if (likeButton == null) return;
                likeButton.setImageResource(Boolean.TRUE.equals(isLiked) ? R.drawable.ic_thumb_up_filled_24dp : R.drawable.ic_thumb_up_outline_24dp);
                likeButton.setImageTintList(null); // Use selector in layout
            });
            // Observe Dislike State
            likeViewModel.isDisliked(post.getId()).observe(lifecycleOwner, isDisliked -> {
                if (dislikeButton == null) return;
                dislikeButton.setImageResource(Boolean.TRUE.equals(isDisliked) ? R.drawable.ic_thumb_down_filled_24dp : R.drawable.ic_thumb_down_outline_24dp);
                dislikeButton.setImageTintList(null); // Use selector in layout
            });

            // Observe Live Like Count
            likeViewModel.getLikeCount(post.getId()).observe(lifecycleOwner, count -> {
                if (likeCount != null) {
                    int currentCount = count != null ? count : 0;
                    likeCount.setText(formatCount(currentCount));
                    likeCount.setVisibility(currentCount > 0 ? View.VISIBLE : View.GONE);
                }
            });
            // Observe Live Dislike Count (Optional display - currently hidden by default)
            likeViewModel.getDislikeCount(post.getId()).observe(lifecycleOwner, count -> {
                // Example: if you had a dislikeCount TextView
                // if (dislikeCount != null) {
                //     int currentCount = count != null ? count : 0;
                //     dislikeCount.setText(formatCount(currentCount));
                //     // Decide whether to show dislike count based on your app's design
                //     dislikeCount.setVisibility(View.GONE); // Or View.VISIBLE if count > 0
                // }
            });

            // Comment Button
            commentButton.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onCommentClick(post);
            });
            // Comment Count
            commentCount.setText(formatCount(post.getCommentCount()));
            commentCount.setVisibility(post.getCommentCount() > 0 ? View.VISIBLE : View.GONE);

            // Options Button
            optionsButton.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onOptionClick(post, v);
                else showDefaultOptionsMenu(v, context, post); // Fallback if listener is null
            });
        }


        private String formatCount(int count) {
            if (count < 1000) return String.valueOf(count);
            if (count < 1_000_000) return String.format("%.1fk", count / 1000.0).replace(".0", "");
            return String.format("%.1fm", count / 1_000_000.0).replace(".0", "");
        }

        // --- NEW: Setup Observers for Poll Data ---
        private void setupPollObservers(String postId) {
            if (postId == null || pollViewModel == null) return;

            // Clear previous observers before setting new ones
            clearPollObservers();

            // Store observers to remove them later
            postDataObserver = updatedPost -> {
                if (updatedPost != null && updatedPost.getId().equals(currentPostId)) { // Ensure update is for the correct post
                    Integer userVoteIndex = pollViewModel.getUserVoteForPost(postId).getValue(); // Get current vote status
                    renderPollUI(updatedPost, userVoteIndex); // Render with updated post and current vote
                }
            };

            userVoteObserver = userVoteIndex -> {
                // Check if the update is for the currently bound post
                if (currentPostId != null && currentPostId.equals(postId)) {
                    Post currentPost = pollViewModel.getPostData(postId).getValue(); // Get current post data
                    // If post data hasn't loaded yet, use the initially bound post as fallback
                    if (currentPost == null) {
                        currentPost = currentBoundPost;
                    }
                    renderPollUI(currentPost, userVoteIndex); // Render with current post and updated vote
                }
            };

            // Observe both LiveData streams
            pollViewModel.getPostData(postId).observe(lifecycleOwner, postDataObserver);
            pollViewModel.getUserVoteForPost(postId).observe(lifecycleOwner, userVoteObserver);
            Log.d(TAG, "Started poll observers for post: " + postId);

            // Initial render call in case data is already available
            Post initialPostData = pollViewModel.getPostData(postId).getValue();
            Integer initialVoteIndex = pollViewModel.getUserVoteForPost(postId).getValue();
            if (initialPostData != null) {
                renderPollUI(initialPostData, initialVoteIndex);
            } else if (currentBoundPost != null && (currentBoundPost.getPostType().equals(Post.TYPE_POLL) || currentBoundPost.getPostType().equals(Post.TYPE_QUIZ))) {
                renderPollUI(currentBoundPost, initialVoteIndex); // Render initial state if post data not yet loaded
            }
        }

        // --- NEW: Method to Clear Observers ---
        private void clearPollObservers() {
            if (currentPostId != null && pollViewModel != null) {
                LiveData<Post> postLiveData = pollViewModel.getPostData(currentPostId);
                LiveData<Integer> voteLiveData = pollViewModel.getUserVoteForPost(currentPostId);
                if (postDataObserver != null && postLiveData != null) {
                    postLiveData.removeObserver(postDataObserver);
                    Log.d(TAG, "Removed postDataObserver for post: " + currentPostId);
                }
                if (userVoteObserver != null && voteLiveData != null) {
                    voteLiveData.removeObserver(userVoteObserver);
                    Log.d(TAG, "Removed userVoteObserver for post: " + currentPostId);
                }
            }
            postDataObserver = null;
            userVoteObserver = null;
            // Don't reset currentPostId here, it's needed if bind is called again for the same item
        }


        // --- UPDATED: Centralized UI Rendering Logic for Polls/Quizzes ---
        private void renderPollUI(@Nullable Post post, @Nullable Integer userVoteIndex) {
            // Check if views are still valid (ViewHolder might be recycled)
            if (pollContainer == null || pollDetailsText == null || itemView == null || itemView.getContext() == null) {
                Log.w(TAG, "renderPollUI aborted: Views are null");
                return;
            }
            // Handle cases where post data might be null initially or due to error
            if (post == null) {
                Log.w(TAG, "renderPollUI aborted: Post data is null for ID: " + currentPostId);
                // Optionally show an error/loading state in the poll container
                pollContainer.removeAllViews();
                pollDetailsText.setText("Loading poll...");
                return;
            }
            // Ensure this rendering is for the correct post ID
            if (!post.getId().equals(currentPostId)) {
                Log.w(TAG, "renderPollUI aborted: Post ID mismatch (" + post.getId() + " vs " + currentPostId + ")");
                return;
            }


            final Context context = itemView.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            pollContainer.removeAllViews(); // Clear previous options

            boolean hasVoted = userVoteIndex != null && userVoteIndex != -1;
            boolean isExpired = isPollExpired(post); // Use potentially updated post
            boolean showResults = hasVoted || isExpired;
            long totalVotes = post.getTotalVotes(); // Use totalVotes from the updated post

            String details = formatCount((int)totalVotes) + (totalVotes == 1 ? " vote" : " votes");
            if (isExpired) {
                details += " • Final results";
            } else if (post.getPollDurationHours() != null && post.getPollDurationHours() > 0) {
                details += " • " + getPollTimeRemaining(post);
            } else if (Post.TYPE_QUIZ.equals(post.getPostType()) && hasVoted) {
                details += " • Final results"; // Show "Final results" for quiz immediately after vote
            }
            pollDetailsText.setText(details);

            // Check if poll options are available
            if (post.getPollOptions() == null) {
                Log.e(TAG, "renderPollUI Error: PollOptions list is null for post: " + post.getId());
                // Optionally display an error message in the poll container
                TextView errorText = new TextView(context);
                errorText.setText("Error loading poll options.");
                pollContainer.addView(errorText);
                return; // Stop processing if options are null
            }

            for (int i = 0; i < post.getPollOptions().size(); i++) {
                View optionView = inflater.inflate(R.layout.item_poll_option, pollContainer, false);
                PollOption option = post.getPollOptions().get(i); // Use option from updated post
                TextView optionText = optionView.findViewById(R.id.poll_option_text);
                ProgressBar progressBar = optionView.findViewById(R.id.poll_option_progress);
                TextView percentageText = optionView.findViewById(R.id.poll_option_percentage);
                ImageView voteIndicator = optionView.findViewById(R.id.your_vote_indicator);
                // --- NEW: Find ImageView for option image ---
                ShapeableImageView optionImage = optionView.findViewById(R.id.poll_option_image);

                optionText.setText(option.getOptionText());

                // --- NEW: Load Option Image ---
                if (option.getImageUrl() != null && !option.getImageUrl().isEmpty()) {
                    optionImage.setVisibility(View.VISIBLE);
                    Glide.with(context)
                            .load(option.getImageUrl())
                            .placeholder(R.drawable.ic_placeholder_24dp) // Optional placeholder
                            .error(R.drawable.ic_placeholder_24dp) // Optional error placeholder
                            .centerCrop()
                            .into(optionImage);
                } else {
                    optionImage.setVisibility(View.GONE);
                }
                // --- End Load Option Image ---


                if (showResults) {
                    optionView.setClickable(false); // Disable clicks when showing results
                    progressBar.setVisibility(View.VISIBLE);
                    percentageText.setVisibility(View.VISIBLE);
                    // Use updated counts
                    int percentage = (totalVotes > 0) ? (int) (((float) option.getVoteCount() / totalVotes) * 100) : 0;
                    progressBar.setProgress(percentage);
                    percentageText.setText(percentage + "%");

                    // --- REVAMPED: Text Color Logic ---
                    boolean isVotedOption = hasVoted && userVoteIndex != null && userVoteIndex == i;
                    // Define colors based on theme attributes
                    @ColorInt int progressTextColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.BLACK);
                    @ColorInt int defaultTextColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);

                    // --- YOUTUBE STYLE TEXT COLOR ---
                    // If progress is high OR it's the voted option, text is light/on-progress-color
                    // Otherwise, text is the default/on-surface-color
                    if (percentage > 50 || (isVotedOption && percentage > 5)) { // 50% is a common threshold
                        optionText.setTextColor(Color.WHITE); // Or a theme attr like ?colorOnSecondary
                        percentageText.setTextColor(Color.WHITE);
                    } else {
                        optionText.setTextColor(defaultTextColor);
                        percentageText.setTextColor(defaultTextColor);
                    }
                    // --- END YOUTUBE STYLE ---


                    voteIndicator.setVisibility(isVotedOption ? View.VISIBLE : View.GONE);

                    // --- NEW Background & Progress Bar Logic (YouTube Style) ---
                    int progressDrawableRes;

                    // 1. Determine the correct progress bar drawable
                    if (Post.TYPE_QUIZ.equals(post.getPostType())) {
                        if (i == post.getQuizCorrectOptionIndex()) {
                            // This is the correct answer
                            progressDrawableRes = R.drawable.poll_progress_drawable_correct;
                            voteIndicator.setImageResource(R.drawable.ic_check_circle_24dp);
                            voteIndicator.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.teal))); // Or your green color
                            voteIndicator.setVisibility(View.VISIBLE); // Always show check for correct answer
                        } else if (isVotedOption) {
                            // This is the option the user voted for, and it's incorrect
                            progressDrawableRes = R.drawable.poll_progress_drawable_incorrect;
                            voteIndicator.setImageResource(R.drawable.ic_error_24dp); // Use an 'X' or error icon
                            voteIndicator.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error)));
                            voteIndicator.setVisibility(View.VISIBLE); // Show indicator for the incorrect vote
                        } else {
                            // This is an incorrect answer the user did not vote for
                            progressDrawableRes = R.drawable.poll_progress_drawable; // <-- CORRECTED
                            voteIndicator.setVisibility(View.GONE);
                        }
                    } else { // Regular Poll
                        if (isVotedOption) {
                            // This is the option the user voted for
                            progressDrawableRes = R.drawable.poll_progress_drawable_voted;
                            voteIndicator.setImageResource(R.drawable.ic_check_circle_24dp);
                            voteIndicator.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.teal)));
                            voteIndicator.setVisibility(View.VISIBLE);
                        } else {
                            // This is an option the user did not vote for
                            progressDrawableRes = R.drawable.poll_progress_drawable; // <-- CORRECTED
                            voteIndicator.setVisibility(View.GONE);
                        }
                    }

                    // 2. Set the progress bar drawable
                    // Make sure to import androidx.core.content.ContextCompat
                    if (progressBar != null && context != null) { // Check context
                        progressBar.setProgressDrawable(ContextCompat.getDrawable(context, progressDrawableRes));
                    }

                    // 3. Set the outer background to the default, non-changing border
                    optionView.setBackgroundResource(R.drawable.poll_option_background_default);
                    // --- END NEW Logic ---

                } else { // Allow voting
                    progressBar.setVisibility(View.INVISIBLE); // Keep invisible but occupy space
                    percentageText.setVisibility(View.GONE);
                    voteIndicator.setVisibility(View.GONE);
                    // --- REVAMPED: Reset text color ---
                    @ColorInt int defaultTextColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
                    optionText.setTextColor(defaultTextColor);
                    // --- END REVAMPED ---
                    optionView.setBackgroundResource(R.drawable.poll_option_background_default);
                    optionView.setClickable(true);
                    final int optionIndex = i;

                    optionView.setOnClickListener(v -> {
                        // --- REVAMPED: Disable all options on click ---
                        ViewGroup container = (ViewGroup) optionView.getParent();
                        if (container != null) {
                            for (int j = 0; j < container.getChildCount(); j++) {
                                View child = container.getChildAt(j);
                                if (child != null) child.setClickable(false);
                            }
                        }
                        // --- END REVAMPED ---
                        pollViewModel.castVote(post, optionIndex);
                    });
                }
                pollContainer.addView(optionView);
            }
        }
        // --- End Poll Rendering Update ---


        // Helper method to check if poll is expired
        private boolean isPollExpired(Post post) {
            if (post == null || post.getPollDurationHours() == null || post.getPollDurationHours() <= 0 || post.getTimestamp() == null) {
                return false;
            }
            // --- THIS IS THE FIX ---
            long postTimeMillis = post.getTimestamp().toDate().getTime(); // Convert Timestamp to long
            // --- END FIX ---
            long durationMillis = TimeUnit.HOURS.toMillis(post.getPollDurationHours());
            long expiryTimeMillis = postTimeMillis + durationMillis;
            return System.currentTimeMillis() > expiryTimeMillis;
        }

        // Helper method to get poll time remaining string
        private String getPollTimeRemaining(Post post) {
            if (post == null || post.getPollDurationHours() == null || post.getPollDurationHours() <= 0 || post.getTimestamp() == null) {
                return "";
            }
            // --- THIS IS THE FIX ---
            long postTimeMillis = post.getTimestamp().toDate().getTime(); // Convert Timestamp to long
            // --- END FIX ---
            long durationMillis = TimeUnit.HOURS.toMillis(post.getPollDurationHours());
            long expiryTimeMillis = postTimeMillis + durationMillis;
            long remainingMillis = expiryTimeMillis - System.currentTimeMillis();

            if (remainingMillis <= 0) {
                return "Poll ended";
            }

            long days = TimeUnit.MILLISECONDS.toDays(remainingMillis);
            remainingMillis -= TimeUnit.DAYS.toMillis(days);
            long hours = TimeUnit.MILLISECONDS.toHours(remainingMillis);
            remainingMillis -= TimeUnit.HOURS.toMillis(hours);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis);

            if (days > 0) {
                return days + (days == 1 ? " day" : " days") + " left";
            } else if (hours > 0) {
                return hours + (hours == 1 ? " hour" : " hours") + " left";
            } else if (minutes > 0) {
                return minutes + (minutes == 1 ? " minute" : " minutes") + " left";
            } else {
                return "Ending soon";
            }
        }

        @ColorInt
        private int getColorFromAttr(@AttrRes int attrRes) {
            TypedValue typedValue = new TypedValue();
            itemView.getContext().getTheme().resolveAttribute(attrRes, typedValue, true);
            return typedValue.data;
        }

        private void setPostText(String text) {
            if (postContent == null) return; // Add null check
            if (text == null || text.isEmpty()) {
                postContent.setVisibility(View.GONE);
                return;
            }
            postContent.setVisibility(View.VISIBLE);
            SpannableString spannableString = new SpannableString(text);
            Pattern hashtagPattern = Pattern.compile("#(\\w+)");
            Matcher matcher = hashtagPattern.matcher(text);
            int hashtagColor = getColorFromAttr(com.google.android.material.R.attr.colorSecondary);

            while (matcher.find()) {
                spannableString.setSpan(new ForegroundColorSpan(hashtagColor),
                        matcher.start(),
                        matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            postContent.setText(spannableString);
        }


        private void showDefaultOptionsMenu(View anchor, Context context, Post post) {
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.getMenu().add("Share");
            popup.getMenu().add("Report");
            popup.setOnMenuItemClickListener(item -> {
                Toast.makeText(context, item.getTitle() + " clicked (Default)", Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        }
    } // End PostViewHolder

    // --- Internal Adapter for Media Pager ---
    private static class FeedMediaPagerAdapter extends RecyclerView.Adapter<FeedMediaPagerAdapter.FeedMediaViewHolder> {
        private List<MediaItem> mediaItems = new ArrayList<>();
        private final Context context;
        private final OnMediaItemClickListener internalListener;

        interface OnMediaItemClickListener {
            void onMediaItemClicked(List<MediaItem> items, int position);
        }

        FeedMediaPagerAdapter(Context context, OnMediaItemClickListener listener) {
            this.context = context;
            this.internalListener = listener;
        }

        @SuppressLint("NotifyDataSetChanged")
        void setMediaItems(List<MediaItem> items) {
            this.mediaItems = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
            notifyDataSetChanged(); // Use specific notifications if performance is an issue
        }

        @NonNull
        @Override
        public FeedMediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // *** Use correct binding name ***
            ItemFeedMediaPageBinding binding = ItemFeedMediaPageBinding.inflate(LayoutInflater.from(context), parent, false);
            return new FeedMediaViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull FeedMediaViewHolder holder, int position) {
            MediaItem item = mediaItems.get(position);
            holder.bind(item, context);
            holder.itemView.setOnClickListener(v -> {
                if (internalListener != null) {
                    // Use getBindingAdapterPosition() for safety
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        internalListener.onMediaItemClicked(mediaItems, currentPosition);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mediaItems.size();
        }

        static class FeedMediaViewHolder extends RecyclerView.ViewHolder {
            // *** Use correct binding name ***
            private final ItemFeedMediaPageBinding binding;

            FeedMediaViewHolder(ItemFeedMediaPageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(MediaItem item, Context context) {
                boolean isVideo = "video".equals(item.getMediaType());
                String url = isVideo ? item.getThumbnailUrl() : item.getUrl();

                Glide.with(context)
                        .load(url)
                        .placeholder(R.drawable.ic_placeholder_24dp)
                        .error(R.drawable.ic_placeholder_24dp)
                        .centerCrop()
                        .into(binding.mediaImageItem); // *** Use binding id ***

                binding.playIconItem.setVisibility(isVideo ? View.VISIBLE : View.GONE); // *** Use binding id ***
            }
        }
    }


    // --- DIFF_CALLBACK (Includes pollDurationHours & totalVotes & imageUrl in PollOption) ---
    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            // Add null checks for safety
            return oldItem != null && newItem != null && oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            // Add null checks for safety
            if (oldItem == null || newItem == null) {
                return Objects.equals(oldItem, newItem); // Use Objects.equals for null safety
            }
            // Compare all relevant fields, including the new poll option structure
            return Objects.equals(oldItem.getTextContent(), newItem.getTextContent()) &&
                    oldItem.getLikeCount() == newItem.getLikeCount() &&
                    oldItem.getDislikeCount() == newItem.getDislikeCount() &&
                    oldItem.getCommentCount() == newItem.getCommentCount() &&
                    Objects.equals(oldItem.getAuthorName(), newItem.getAuthorName()) &&
                    Objects.equals(oldItem.getAuthorAvatarUrl(), newItem.getAuthorAvatarUrl()) &&
                    Objects.equals(oldItem.getMediaItems(), newItem.getMediaItems()) &&
                    Objects.equals(oldItem.getPostType(), newItem.getPostType()) &&
                    Objects.equals(oldItem.getPollOptions(), newItem.getPollOptions()) && // PollOption.equals checks imageURL now
                    oldItem.getTotalVotes() == newItem.getTotalVotes() &&
                    oldItem.getQuizCorrectOptionIndex() == newItem.getQuizCorrectOptionIndex() &&
                    Objects.equals(oldItem.getTimestamp(), newItem.getTimestamp()) &&
                    Objects.equals(oldItem.getPollDurationHours(), newItem.getPollDurationHours());
        }
    };
}