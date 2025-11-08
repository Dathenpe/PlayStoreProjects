// main/java/com/f9ld3/Zion/ui/feed/PostDetailActivity.java
package com.f9ld3.Zion.ui.feed;

// --- NEW IMPORTS for Highlight ---
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
// --- END NEW IMPORTS ---

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer; // Import Observer
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.MainActivity;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityPostDetailBinding;
import com.f9ld3.Zion.databinding.PostDetailContentBinding;
import com.f9ld3.Zion.ui.blog.EditPostActivity;
import com.f9ld3.Zion.ui.channel.ChannelActivity;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.dialogs.CustomInputDialogFragment;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.imageview.ShapeableImageView; // <-- Import ShapeableImageView
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // Import Objects
import java.util.concurrent.TimeUnit; // Import TimeUnit
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailActivity extends AppCompatActivity implements CommentAdapter.CommentInteractionListener {

    public static final String EXTRA_POST_ID = "extra_post_id";
    public static final String EXTRA_POST_DATA = "extra_post_data";
    public static final String EXTRA_FOCUS_COMMENT_INPUT = "extra_focus_comment_input";
    // --- NEW: Extra for highlighting a comment ---
    public static final String EXTRA_HIGHLIGHT_COMMENT_ID = "extra_highlight_comment_id";
    // --- END NEW ---
    private static final String TAG = "PostDetailActivity";

    public static final String ACTION_NAVIGATE_TO_CHANNEL = "com.f9ld3.Zion.NAVIGATE_TO_CHANNEL";
    public static final String EXTRA_CHANNEL_ID = "channelId";
    public static final String EXTRA_CHANNEL_NAME = "channelName";

    private ActivityPostDetailBinding binding;
    private PostDetailContentBinding postBinding;
    private CommentsViewModel commentsViewModel;
    private CommentAdapter commentAdapter;
    private PostLikeViewModel postLikeViewModel;
    private PollViewModel pollViewModel;
    private FeedViewModel feedViewModel; // <-- Add FeedViewModel instance

    private String postId;
    private Post currentPostData; // Keep this to store the latest Post object
    private ViewPager2.OnPageChangeCallback pageChangeCallback;

    // --- NEW: Member variable to store the ID ---
    private String highlightCommentId = null;
    // --- END NEW ---

    // Observers for Poll Data
    private Observer<Post> postDataObserver;
    private Observer<Integer> userVoteObserver;

    // *** FIX: Add flag to track if listener has loaded data at least once ***
    private boolean hasLoadedPostFromListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize binding for the included layout
        View postContentView = binding.scrollView.findViewById(R.id.post_content_container);
        postBinding = PostDetailContentBinding.bind(postContentView);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Post");
        }

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        // --- NEW: Get highlight ID from intent ---
        highlightCommentId = getIntent().getStringExtra(EXTRA_HIGHLIGHT_COMMENT_ID);
        // --- END NEW ---

        // Attempt to get initial Post data, but handle if it's null
        Object postDataSerializable = getIntent().getSerializableExtra(EXTRA_POST_DATA);
        if (postDataSerializable instanceof Post) {
            currentPostData = (Post) postDataSerializable;
        } else if (postId != null) {
            Log.w(TAG, "Post data missing or invalid in intent, will rely on observer for postId: " + postId);
            // Initialize currentPostData to null or a placeholder if necessary
            currentPostData = null;
        } else {
            Log.e(TAG, "Post ID is null! Cannot load post. Finishing activity.");
            Toast.makeText(this, "Error loading post.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        boolean shouldFocusComment = getIntent().getBooleanExtra(EXTRA_FOCUS_COMMENT_INPUT, false);

        // Initialize ViewModels
        commentsViewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        postLikeViewModel = new ViewModelProvider(this).get(PostLikeViewModel.class);
        pollViewModel = new ViewModelProvider(this).get(PollViewModel.class);
        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class); // <-- Initialize FeedViewModel

        // Setup RecyclerView only if postId is valid
        if (postId != null) {
            // --- MODIFIED CALL ---
            // Pass the initial post data (which might be null)
            setupRecyclerView(currentPostData);
            // --- END MODIFIED ---
        }

        // Initial UI update (can be partial if currentPostData is null)
        updatePostUi(currentPostData);

        // Load comments, setup input, observe messages (if postId is valid)
        if (postId != null) {
            loadComments();
            setupCommentInput();
            observeViewModelMessages();
            observePostAndPollUpdates(); // Start observing post data
        }

        // --- UPDATED: Only focus if not highlighting ---
        if (shouldFocusComment && highlightCommentId == null) {
            focusCommentInput();
        }
        // --- END UPDATE ---
    }

    // --- Combined observer setup ---
    private void observePostAndPollUpdates() {
        if (postId == null || pollViewModel == null) return;

        clearPollObservers(); // Clear existing before adding new ones

        // *** FIX: Updated observer logic ***
        postDataObserver = updatedPost -> {
            if (updatedPost != null) {
                // --- This is the success case ---
                // Data has been successfully loaded or updated from Firestore.
                hasLoadedPostFromListener = true; // Mark that we've received real data
                currentPostData = updatedPost; // Update the activity's copy
                Integer userVoteIndex = pollViewModel.getUserVoteForPost(postId).getValue();
                updatePostUi(updatedPost); // Update general post UI, which will call renderPollUI if needed
                Log.d(TAG, "PostDataObserver triggered. Post updated: " + updatedPost.getId());
            } else {
                // --- This is the null case ---

                // Check if the listener has *ever* successfully loaded data.
                if (hasLoadedPostFromListener) {
                    // If we HAD data from the listener, and now we get null,
                    // it means the post was genuinely deleted from Firestore.
                    Log.w(TAG, "Post data observer received null after listener had data. Post deleted: " + postId);
                    Toast.makeText(this, "This post is no longer available.", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                } else {
                    // This is the *initial* null value from the LiveData
                    // because the Firestore listener hasn't returned data yet.
                    Log.d(TAG, "Post data observer received initial null. Waiting for data for postId: " + postId);

                    // If we didn't get data from the intent, show the loading state.
                    // If we *did* get data from the intent, updatePostUi(currentPostData)
                    // was already called in onCreate, so we just wait for the listener.
                    if (currentPostData == null) {
                        updatePostUi(null); // Show loading state
                    }
                }
            }
        };
        // *** END FIX ***

        userVoteObserver = userVoteIndex -> {
            Log.d(TAG, "UserVoteObserver triggered for post " + postId + ". New vote index: " + userVoteIndex);
            // Re-render the poll UI if the post data is available
            if (currentPostData != null && (Post.TYPE_POLL.equals(currentPostData.getPostType()) || Post.TYPE_QUIZ.equals(currentPostData.getPostType()))) {
                renderPollUI(currentPostData, userVoteIndex); // Call render directly
            } else {
                Log.d(TAG, "UserVoteObserver: Skipping poll render, currentPostData is null or not a poll type.");
            }
        };

        pollViewModel.getPostData(postId).observe(this, postDataObserver);
        pollViewModel.getUserVoteForPost(postId).observe(this, userVoteObserver);
        Log.d(TAG, "Started observing post data and user vote for postId: " + postId);
    }

    // --- Method to Clear Poll Observers ---
    private void clearPollObservers() {
        if (postId != null && pollViewModel != null) {
            Log.d(TAG, "Clearing poll observers for postId: " + postId);
            LiveData<Post> postLiveData = pollViewModel.getPostData(postId);
            LiveData<Integer> voteLiveData = pollViewModel.getUserVoteForPost(postId);
            if (postDataObserver != null && postLiveData != null) {
                postLiveData.removeObserver(postDataObserver);
                Log.d(TAG, "Removed postDataObserver.");
            }
            if (userVoteObserver != null && voteLiveData != null) {
                voteLiveData.removeObserver(userVoteObserver);
                Log.d(TAG, "Removed userVoteObserver.");
            }
        }
    }

    private void updatePostUi(@Nullable Post post) { // Allow null post
        if (postBinding == null) {
            Log.w(TAG, "updatePostUi aborted: postBinding is null");
            return;
        }

        // --- ADDED ---
        // Update the adapter's post context *every time* the UI is updated
        currentPostData = post; // Update the member variable with the latest data
        if (commentAdapter != null) {
            commentAdapter.setPostData(currentPostData);
        }
        // --- END ADDED ---

        if (post == null) {
            // This is now the "Loading" state
            Log.w(TAG, "updatePostUi called with null post, showing loading state.");
            postBinding.postContent.setText("Loading post details...");
            postBinding.authorName.setVisibility(View.GONE);
            postBinding.authorAvatar.setVisibility(View.GONE);
            postBinding.postTimestamp.setVisibility(View.GONE);
            postBinding.mediaPagerContainer.setVisibility(View.GONE);
            postBinding.pollContainer.setVisibility(View.GONE);
            postBinding.pollDetailsText.setVisibility(View.GONE);
            postBinding.actionsLayout.setVisibility(View.GONE);
            return;
        }

        // --- Post is NOT null, update UI normally ---

        // Ensure elements relying on post data are visible now
        postBinding.authorName.setVisibility(View.VISIBLE);
        postBinding.authorAvatar.setVisibility(View.VISIBLE);
        postBinding.postTimestamp.setVisibility(View.VISIBLE);
        postBinding.actionsLayout.setVisibility(View.VISIBLE);


        //currentPostData = post; // Update the member variable with the latest data - MOVED UP
        Context context = this;
        String currentPostId = post.getId();

        // 1. Header Info
        postBinding.authorName.setText(post.getAuthorName());
        Long postTime = post.getTimestamp();
        if (postTime != null && postTime > 0) {
            postBinding.postTimestamp.setText(DateUtils.getRelativeTimeSpanString(postTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
            postBinding.postTimestamp.setVisibility(View.VISIBLE);
        } else {
            postBinding.postTimestamp.setVisibility(View.GONE);
        }
        Glide.with(context)
                .load(post.getAuthorAvatarUrl())
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(postBinding.authorAvatar);

        // 2. Set Content Text
        setPostText(postBinding.postContent, post.getTextContent());

        // 3. Setup Content Type Specific UI
        final List<MediaItem> items = post.getMediaItems();
        if (items != null && !items.isEmpty() && Post.TYPE_TEXT_MEDIA.equals(post.getPostType())) {
            // Media Pager Setup
            final ViewPager2 mediaPager = postBinding.mediaPagerFeed;
            final TextView mediaIndicator = postBinding.mediaIndicatorFeed;
            postBinding.mediaPagerContainer.setVisibility(View.VISIBLE);

            mediaPager.post(() -> {
                if (postBinding == null || mediaPager == null || items == null || items.isEmpty()) return;
                int pagerWidth = mediaPager.getWidth();
                if (pagerWidth > 0) {
                    // ... (height calculation) ...
                    int pagerHeight = (int) (pagerWidth * (9.0 / 16.0));
                    int maxHeightPx = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 350, getResources().getDisplayMetrics());
                    pagerHeight = Math.min(pagerHeight, maxHeightPx);
                    ViewGroup.LayoutParams params = mediaPager.getLayoutParams();
                    params.height = pagerHeight;
                    mediaPager.setLayoutParams(params);


                    MediaPagerAdapter pagerAdapter;
                    if (mediaPager.getAdapter() instanceof MediaPagerAdapter) {
                        pagerAdapter = (MediaPagerAdapter) mediaPager.getAdapter();
                        // TODO: Implement pagerAdapter.updateItems(items) if needed
                    } else {
                        pagerAdapter = new MediaPagerAdapter(context, items);
                        mediaPager.setAdapter(pagerAdapter);
                    }
                    updateMediaIndicator(mediaPager, mediaIndicator, mediaPager.getCurrentItem(), items.size());

                    // --- Unregister existing callback before registering ---
                    if (pageChangeCallback != null) {
                        mediaPager.unregisterOnPageChangeCallback(pageChangeCallback);
                    }
                    // --- End Unregister ---
                    pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
                        @Override
                        public void onPageSelected(int position) {
                            super.onPageSelected(position);
                            // --- Use local items variable (captured by lambda) ---
                            if (postBinding != null && items != null && !items.isEmpty()) {
                                updateMediaIndicator(mediaPager, mediaIndicator, position, items.size());
                            }
                            // --- End Use local items ---
                        }
                    };
                    mediaPager.registerOnPageChangeCallback(pageChangeCallback);
                    // --- End Callback Setup ---
                }
            });

            postBinding.pollContainer.setVisibility(View.GONE);
            postBinding.pollDetailsText.setVisibility(View.GONE);

        } else if (Post.TYPE_POLL.equals(post.getPostType()) || Post.TYPE_QUIZ.equals(post.getPostType())) {
            // Poll/Quiz Setup
            postBinding.mediaPagerContainer.setVisibility(View.GONE);
            postBinding.pollContainer.setVisibility(View.VISIBLE);
            postBinding.pollDetailsText.setVisibility(View.VISIBLE);
            Integer userVoteIndex = pollViewModel.getUserVoteForPost(post.getId()).getValue();
            renderPollUI(post, userVoteIndex); // Render poll UI based on the latest post data
        }
        else {
            // Text-only post
            postBinding.mediaPagerContainer.setVisibility(View.GONE);
            postBinding.pollContainer.setVisibility(View.GONE);
            postBinding.pollDetailsText.setVisibility(View.GONE);
        }

        // 4. Setup Actions/Re-observe LiveData
        View.OnClickListener authorClickListener = v -> {
            Log.i(TAG, "Author clicked in Detail: " + post.getAuthorName() + " (ID: " + post.getAuthorUid() + ")");
            if (post.getAuthorUid() != null) {
                // --- Navigate back to MainActivity with Intent ---
                Intent intent = new Intent(this, MainActivity.class);
                intent.setAction(ACTION_NAVIGATE_TO_CHANNEL);
                intent.putExtra(EXTRA_CHANNEL_ID, post.getAuthorUid());
                intent.putExtra(EXTRA_CHANNEL_NAME, post.getAuthorName());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // --- End Navigation ---
            }
        };
        postBinding.authorAvatar.setOnClickListener(authorClickListener);
        postBinding.authorName.setOnClickListener(authorClickListener);

        // Like/Dislike Button Setup
        // Check currentPostId before accessing ViewModel maps
        if (currentPostId != null) {
            postLikeViewModel.isLiked(currentPostId).observe(this, isLiked -> {
                if (postBinding == null) return;
                postBinding.likeButton.setImageResource(Boolean.TRUE.equals(isLiked) ? R.drawable.ic_thumb_up_filled_24dp : R.drawable.ic_thumb_up_outline_24dp);
                postBinding.likeButton.setImageTintList(null); // Use selector
            });

            postLikeViewModel.isDisliked(currentPostId).observe(this, isDisliked -> {
                if (postBinding == null) return;
                postBinding.dislikeButton.setImageResource(Boolean.TRUE.equals(isDisliked) ? R.drawable.ic_thumb_down_filled_24dp : R.drawable.ic_thumb_down_outline_24dp);
                postBinding.dislikeButton.setImageTintList(null); // Use selector
            });

            postLikeViewModel.getLikeCount(currentPostId).observe(this, count -> {
                if (postBinding == null) return;
                int currentCount = count != null ? count : 0;
                postBinding.likeCount.setText(formatCount(currentCount));
                postBinding.likeCount.setVisibility(currentCount > 0 ? View.VISIBLE : View.GONE);
            });
            // Observe dislike count if you plan to show it
            // postLikeViewModel.getDislikeCount(currentPostId).observe(this, count -> { ... });

        } else {
            Log.w(TAG, "currentPostId is null, cannot observe like/dislike status or counts.");
            // Set default button states if ID is null
            postBinding.likeButton.setImageResource(R.drawable.ic_thumb_up_outline_24dp);
            postBinding.dislikeButton.setImageResource(R.drawable.ic_thumb_down_outline_24dp);
            postBinding.likeCount.setVisibility(View.GONE);
        }

        postBinding.likeButton.setOnClickListener(v -> {
            if (currentPostData != null && currentPostId != null) postLikeViewModel.toggleLike(currentPostId, currentPostData);
        });
        postBinding.dislikeButton.setOnClickListener(v -> {
            if (currentPostData != null && currentPostId != null) postLikeViewModel.toggleDislike(currentPostId, currentPostData);
        });

        // Comment Button (Hidden)
        postBinding.commentButton.setVisibility(View.GONE);
        postBinding.commentCount.setVisibility(View.GONE);

        // Options Button
        postBinding.postOptionsButton.setOnClickListener(this::showPostOptionsMenu);
    }


    // --- Centralized Poll UI Rendering (Similar to Adapter's) ---
    private void renderPollUI(@Nullable Post post, @Nullable Integer userVoteIndex) {
        if (postBinding == null || post == null) {
            Log.w(TAG, "renderPollUI aborted: binding or post is null.");
            return;
        }
        if (!Post.TYPE_POLL.equals(post.getPostType()) && !Post.TYPE_QUIZ.equals(post.getPostType())) {
            Log.d(TAG, "renderPollUI skipped: Post is not a poll/quiz.");
            return;
        }

        Log.d(TAG, "Rendering poll UI for post: " + post.getId() + ", Vote Index: " + userVoteIndex);

        final Context context = this;
        LayoutInflater inflater = LayoutInflater.from(context);
        postBinding.pollContainer.removeAllViews(); // Clear previous options

        boolean hasVoted = userVoteIndex != null && userVoteIndex != -1;
        boolean isExpired = isPollExpired(post);
        boolean showResults = hasVoted || isExpired;
        long totalVotes = post.getTotalVotes();

        // Update details text
        String details = formatCount((int)totalVotes) + (totalVotes == 1 ? " vote" : " votes");
        if (isExpired) {
            details += " • Final results";
        } else if (post.getPollDurationHours() != null && post.getPollDurationHours() > 0) {
            details += " • " + getPollTimeRemaining(post);
        } else if (Post.TYPE_QUIZ.equals(post.getPostType()) && hasVoted) {
            details += " • Final results";
        }
        postBinding.pollDetailsText.setText(details);

        if (post.getPollOptions() == null) {
            Log.e(TAG, "renderPollUI Error: PollOptions list is null for post: " + post.getId());
            TextView errorText = new TextView(context);
            errorText.setText("Error loading poll options.");
            postBinding.pollContainer.addView(errorText);
            return;
        }

        // Inflate and configure each option view
        for (int i = 0; i < post.getPollOptions().size(); i++) {
            View optionView = inflater.inflate(R.layout.item_poll_option, postBinding.pollContainer, false);
            PollOption option = post.getPollOptions().get(i);
            TextView optionText = optionView.findViewById(R.id.poll_option_text);
            ProgressBar progressBar = optionView.findViewById(R.id.poll_option_progress);
            TextView percentageText = optionView.findViewById(R.id.poll_option_percentage);
            ImageView voteIndicator = optionView.findViewById(R.id.your_vote_indicator);
            ShapeableImageView optionImage = optionView.findViewById(R.id.poll_option_image); // <-- Find image view

            optionText.setText(option.getOptionText());

            // --- Load Option Image ---
            if (option.getImageUrl() != null && !option.getImageUrl().isEmpty()) {
                optionImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(option.getImageUrl())
                        .placeholder(R.drawable.ic_placeholder_24dp)
                        .error(R.drawable.ic_placeholder_24dp)
                        .centerCrop()
                        .into(optionImage);
            } else {
                optionImage.setVisibility(View.GONE);
            }
            // --- End Load Option Image ---

            if (showResults) {
                optionView.setClickable(false);
                progressBar.setVisibility(View.VISIBLE);
                percentageText.setVisibility(View.VISIBLE);
                int percentage = (totalVotes > 0) ? (int) (((float) option.getVoteCount() / totalVotes) * 100) : 0;
                progressBar.setProgress(percentage);
                percentageText.setText(percentage + "%");

                boolean isVotedOption = hasVoted && userVoteIndex != null && userVoteIndex == i;
                voteIndicator.setVisibility(isVotedOption ? View.VISIBLE : View.GONE);

                // --- REVAMPED: Text Color Logic ---
                @ColorInt int progressTextColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.BLACK);
                @ColorInt int defaultTextColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);

                if(isVotedOption || percentage > 5) { // Or some threshold
                    optionText.setTextColor(progressTextColor);
                    percentageText.setTextColor(progressTextColor);
                } else {
                    optionText.setTextColor(defaultTextColor);
                    percentageText.setTextColor(defaultTextColor);
                }
                // --- END REVAMPED ---


                // --- Background Logic ---
                if (Post.TYPE_QUIZ.equals(post.getPostType())) {
                    if (i == post.getQuizCorrectOptionIndex()) {
                        optionView.setBackgroundResource(R.drawable.poll_option_background_correct);
                        voteIndicator.setImageResource(R.drawable.ic_check_circle_24dp);
                        voteIndicator.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.teal)));
                        voteIndicator.setVisibility(View.VISIBLE);
                    } else if (isVotedOption) {
                        optionView.setBackgroundResource(R.drawable.poll_option_background_incorrect);
                        voteIndicator.setImageResource(R.drawable.ic_error_24dp);
                        voteIndicator.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error)));
                        voteIndicator.setVisibility(View.VISIBLE);
                    } else {
                        optionView.setBackgroundResource(R.drawable.poll_option_background_default);
                        voteIndicator.setVisibility(View.GONE); // Ensure it's hidden if not voted/correct
                    }
                } else { // Regular Poll
                    if (isVotedOption) {
                        optionView.setBackgroundResource(R.drawable.poll_option_background_voted);
                        voteIndicator.setImageResource(R.drawable.ic_check_circle_24dp);
                        voteIndicator.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.teal)));
                    } else {
                        optionView.setBackgroundResource(R.drawable.poll_option_background_default);
                    }
                }
                // --- End Background Logic ---

            } else { // Allow voting
                progressBar.setVisibility(View.INVISIBLE);
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
            postBinding.pollContainer.addView(optionView);
        }
        Log.d(TAG, "Finished rendering poll UI for post: " + post.getId());
    }


    // Helper method to check if poll is expired
    private boolean isPollExpired(Post post) {
        if (post == null || post.getPollDurationHours() == null || post.getPollDurationHours() <= 0 || post.getTimestamp() == null) {
            return false;
        }
        long postTimeMillis = post.getTimestamp();
        long durationMillis = TimeUnit.HOURS.toMillis(post.getPollDurationHours());
        long expiryTimeMillis = postTimeMillis + durationMillis;
        return System.currentTimeMillis() > expiryTimeMillis;
    }

    // Helper method to get poll time remaining string
    private String getPollTimeRemaining(Post post) {
        // ... (getPollTimeRemaining implementation remains the same) ...
        if (post == null || post.getPollDurationHours() == null || post.getPollDurationHours() <= 0 || post.getTimestamp() == null) {
            return "";
        }
        long postTimeMillis = post.getTimestamp();
        long durationMillis = TimeUnit.HOURS.toMillis(post.getPollDurationHours());
        long expiryTimeMillis = postTimeMillis + durationMillis;
        long remainingMillis = expiryTimeMillis - System.currentTimeMillis();

        if (remainingMillis <= 0) return "Poll ended";
        long days = TimeUnit.MILLISECONDS.toDays(remainingMillis);
        remainingMillis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(remainingMillis);
        remainingMillis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis);

        if (days > 0) return days + (days == 1 ? " day" : " days") + " left";
        if (hours > 0) return hours + (hours == 1 ? " hour" : " hours") + " left";
        if (minutes > 0) return minutes + (minutes == 1 ? " minute" : " minutes") + " left";
        return "Ending soon";
    }

    // Helper to format count (k, M)
    private String formatCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1_000_000) return String.format("%.1fk", count / 1000.0).replace(".0", "");
        return String.format("%.1fm", count / 1_000_000.0).replace(".0", "");
    }

    // Helper function to update media indicator
    private void updateMediaIndicator(ViewPager2 pager, TextView indicator, int currentPosition, int totalItems) {
        if (indicator == null) return;
        if (totalItems > 1) {
            indicator.setText(String.format("%d / %d", currentPosition + 1, totalItems));
            indicator.setVisibility(View.VISIBLE);
        } else {
            indicator.setVisibility(View.GONE);
        }
    }

    // Helper to set post text with hashtag highlighting
    private void setPostText(TextView textView, String text) {
        if (text == null || text.isEmpty()) {
            textView.setVisibility(View.GONE);
            return;
        }
        textView.setVisibility(View.VISIBLE);
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
        textView.setText(spannableString);
    }

    // Helper to get color from theme attribute
    @ColorInt
    private int getColorFromAttr(@AttrRes int attrRes) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attrRes, typedValue, true);
        return typedValue.data;
    }

    // --- Comments Section Logic ---
    // --- MODIFIED ---
    private void setupRecyclerView(@Nullable Post initialPostData) { // Accept Post
        // Pass the initial post data to the adapter
        commentAdapter = new CommentAdapter(this, initialPostData, this, this);
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentsRecyclerView.setAdapter(commentAdapter);
        binding.commentsRecyclerView.setNestedScrollingEnabled(false);
    }
    // --- END MODIFIED ---

    private void loadComments() {
        commentsViewModel.getComments().observe(this, comments -> {
            if (binding == null) return;
            boolean isEmpty = comments == null || comments.isEmpty();
            binding.commentsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.emptyCommentsText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

            if (!isEmpty) {
                commentAdapter.submitList(comments);
                Log.d(TAG, "Displaying " + comments.size() + " top-level comments.");

                // --- NEW HIGHLIGHT LOGIC ---
                if (highlightCommentId != null) {
                    scrollToAndHighlightComment(comments, highlightCommentId);
                    highlightCommentId = null; // Consume it so it doesn't re-highlight on config change
                }
                // --- END NEW ---

            } else {
                commentAdapter.submitList(null);
                Log.w(TAG, "Received null or empty top-level comments list.");
            }
        });
        commentsViewModel.loadComments(postId);
    }

    // --- NEW: Method to scroll and highlight ---
    private void scrollToAndHighlightComment(List<Comment> comments, String commentId) {
        int position = -1;
        for (int i = 0; i < comments.size(); i++) {
            if (comments.get(i) != null && commentId.equals(comments.get(i).getId())) {
                position = i;
                break;
            }
        }

        if (position == -1) {
            Log.w(TAG, "Could not find comment to highlight: " + commentId);
            return;
        }

        final int finalPosition = position;
        // Use post to ensure layout is complete
        binding.commentsRecyclerView.post(() -> {
            try {
                // 1. Scroll RecyclerView
                LinearLayoutManager layoutManager = (LinearLayoutManager) binding.commentsRecyclerView.getLayoutManager();
                if (layoutManager != null) {
                    // Scrolls to the position and places it at the top of the view
                    layoutManager.scrollToPositionWithOffset(finalPosition, 20);
                } else {
                    binding.commentsRecyclerView.smoothScrollToPosition(finalPosition);
                }

                // 2. We need to wait for the scroll to finish AND the view holder to be bound.
                // A short delay is the most common way to handle this.
                binding.commentsRecyclerView.postDelayed(() -> {
                    if (binding == null) return; // Check if activity was destroyed
                    CommentAdapter.CommentViewHolder vh = (CommentAdapter.CommentViewHolder) binding.commentsRecyclerView.findViewHolderForAdapterPosition(finalPosition);
                    if (vh != null) {
                        // 3. Apply flash animation
                        flashHighlightView(vh.itemView);
                    } else {
                        Log.w(TAG, "ViewHolder not found after scroll, cannot highlight.");
                        // Try one more time, maybe layout was slow
                        binding.commentsRecyclerView.postDelayed(() -> {
                            if (binding == null) return;
                            CommentAdapter.CommentViewHolder vhRetry = (CommentAdapter.CommentViewHolder) binding.commentsRecyclerView.findViewHolderForAdapterPosition(finalPosition);
                            if (vhRetry != null) {
                                flashHighlightView(vhRetry.itemView);
                            } else {
                                Log.e(TAG, "ViewHolder still not found. Highlight failed.");
                            }
                        }, 700); // Longer delay for retry
                    }
                }, 300); // Wait 300ms for scroll to start settling

            } catch (Exception e) {
                Log.e(TAG, "Error during scroll/highlight: ", e);
            }
        });
    }

    // --- NEW: Method to apply flash animation ---
    private void flashHighlightView(View view) {
        if (view == null || this.isFinishing()) return;

        // 1. Get highlight color
        int highlightColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondaryContainer, Color.CYAN);

        // 2. Get original background (if any)
        Drawable originalBackground = view.getBackground();
        if (originalBackground == null) {
            // If no background, get default selectable item background
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            originalBackground = new ColorDrawable(Color.TRANSPARENT); // Fallback
            try {
                // Set the original background resource so we can revert to it
                view.setBackgroundResource(outValue.resourceId);
                originalBackground = view.getBackground();
            } catch (Exception e) {
                Log.w(TAG, "Could not get default selectable background");
            }
        }

        // 3. Set the highlight color immediately
        view.setBackgroundColor(highlightColor);

        // 4. Create fade-out animation
        ObjectAnimator fadeOut = ObjectAnimator.ofArgb(view, "backgroundColor", highlightColor, Color.TRANSPARENT);
        fadeOut.setDuration(1500); // 1.5 seconds to fade
        fadeOut.setStartDelay(500); // Wait 0.5 seconds before fading

        Drawable finalOriginalBackground = originalBackground;
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (view != null) {
                    // Revert to original background
                    view.setBackground(finalOriginalBackground);
                }
            }
            @Override
            public void onAnimationCancel(Animator animation) {
                if (view != null) {
                    view.setBackground(finalOriginalBackground);
                }
            }
        });
        fadeOut.start();
    }
    // --- END NEW ---

    private void observeViewModelMessages() {
        commentsViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                commentsViewModel.clearMessages();
            }
        });
        commentsViewModel.getSuccessMessage().observe(this, success -> {
            if (success != null) {
                Toast.makeText(this, success, Toast.LENGTH_SHORT).show();
                commentsViewModel.clearMessages();
                if ("Comment posted.".equals(success) && binding != null) {
                    binding.commentsRecyclerView.postDelayed(() -> {
                        if (commentAdapter != null && commentAdapter.getItemCount() > 0) {
                            binding.commentsRecyclerView.smoothScrollToPosition(commentAdapter.getItemCount() - 1);
                        }
                    }, 300);
                }
            }
        });
        pollViewModel.getToastMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                pollViewModel.clearToastMessage();
            }
        });
        // --- Observe FeedViewModel for delete status ---
        feedViewModel.getStatusMessage().observe(this, message -> {
            if (message != null && !isFinishing()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                // If deletion was successful, finish the activity
                if ("Post deleted successfully.".equals(message)) {
                    finish();
                }
                feedViewModel.clearStatusMessage(); // Clear message after showing
            }
        });
        // --- End Observe ---
    }


    private void setupCommentInput() {
        binding.buttonPostComment.setOnClickListener(v -> postNewCommentOrReply());
    }

    private void postNewCommentOrReply() {
        String text = binding.editTextComment.getText().toString().trim();
        if (!text.isEmpty()) {
            if (currentPostData != null) {
                // --- MODIFIED ---
                // Get snippet
                String postTextSnippet = (currentPostData.getTextContent() != null && currentPostData.getTextContent().length() > 50)
                        ? currentPostData.getTextContent().substring(0, 50) + "..."
                        : currentPostData.getTextContent();

                commentsViewModel.postCommentOrReply(
                        postId,
                        text,
                        null,
                        currentPostData.getAuthorUid(),
                        postTextSnippet // Pass snippet
                );
                // --- END MODIFIED ---
                binding.editTextComment.setText("");
                hideKeyboard();
                Log.d(TAG, "Posted top-level comment: " + text);
            } else {
                Log.e(TAG, "Cannot post comment: Post data is missing.");
                Toast.makeText(this, "Error: Could not send comment.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Cannot post empty comment", Toast.LENGTH_SHORT).show();
        }
    }

    private void focusCommentInput() {
        if (binding == null) return;
        binding.editTextComment.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            binding.editTextComment.postDelayed(() -> imm.showSoftInput(binding.editTextComment, InputMethodManager.SHOW_IMPLICIT), 200);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view == null && binding != null) view = binding.editTextComment;
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    // --- Post Options Menu Logic ---
    private void showPostOptionsMenu(View anchorView) {
        if (currentPostData == null) {
            Log.w(TAG, "Cannot show options menu, currentPostData is null.");
            return; // Don't show if post data isn't loaded
        }
        PopupMenu popup = new PopupMenu(this, anchorView);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_post_options, popup.getMenu());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isAuthor = currentUser != null && currentUser.getUid().equals(currentPostData.getAuthorUid());

        MenuItem editItem = popup.getMenu().findItem(R.id.action_edit_post);
        MenuItem deleteItem = popup.getMenu().findItem(R.id.action_delete_post);
        MenuItem reportItem = popup.getMenu().findItem(R.id.action_report_post);

        if (editItem != null) editItem.setVisible(isAuthor);
        if (deleteItem != null) deleteItem.setVisible(isAuthor);
        if (reportItem != null) reportItem.setVisible(!isAuthor);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_share_post) {
                sharePost(currentPostData); return true;
            } else if (itemId == R.id.action_edit_post) {
                editPost(currentPostData); return true;
            } else if (itemId == R.id.action_delete_post) {
                deletePost(currentPostData); return true; // <-- Call updated deletePost
            } else if (itemId == R.id.action_report_post) {
                reportPost(currentPostData); return true;
            } else {
                return false;
            }
        });
        popup.show();
    }

    private void editPost(Post post) {
        Intent intent = new Intent(this, EditPostActivity.class);
        intent.putExtra(EditPostActivity.EXTRA_POST_TO_EDIT, (Serializable) post);
        startActivity(intent);
    }

    private void sharePost(Post post) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String shareText = post.getTextContent() != null ? post.getTextContent() : "Check out this post!";
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));
    }

    private void reportPost(Post post) {
        Toast.makeText(this, "Report functionality TBD", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Reporting post ID: " + post.getId());
    }

    // --- UPDATED deletePost method ---
    private void deletePost(Post post) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Delete Post?",
                "Are you sure you want to permanently delete this post and its associated data?", // Updated message
                "Delete", "Cancel");
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override public void onPositiveClick() {
                Log.d(TAG, "Deletion requested for post ID: " + post.getId());
                // --- Call FeedViewModel to delete ---
                feedViewModel.deletePost(post);
                // --- End ViewModel call ---
            }
            @Override public void onNegativeClick() {}
        });
        // Check if activity is finishing before showing dialog
        if (!isFinishing()) {
            dialog.show(getSupportFragmentManager(), "DeletePostDialog");
        }
    }
    // --- End Update ---


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- CommentInteractionListener Implementations ---
    @Override
    public void onReplyClicked(Comment comment) {
        Log.d(TAG, "Reply clicked. Opening replies for: " + comment.getId());
        Intent intent = new Intent(this, RepliesActivity.class);
        intent.putExtra(RepliesActivity.EXTRA_POST, (Serializable) currentPostData);
        intent.putExtra(RepliesActivity.EXTRA_COMMENT_ID, comment.getId());
        intent.putExtra(RepliesActivity.EXTRA_SHOULD_FOCUS_REPLY, true);
        startActivity(intent);
    }

    @Override
    public void onDeleteClicked(Comment comment) {
        if (currentPostData == null) return; // Need post author UID
        Log.d(TAG, "Delete clicked for comment: " + comment.getId());
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Delete Comment?", "Are you sure you want to permanently delete this comment?", "Delete", "Cancel");
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override public void onPositiveClick() { commentsViewModel.deleteComment(comment, currentPostData.getAuthorUid()); }
            @Override public void onNegativeClick() { }
        });
        if (!isFinishing()) dialog.show(getSupportFragmentManager(), "DeleteCommentDialog");
    }

    @Override
    public void onReportClicked(Comment comment) {
        Log.d(TAG, "Report clicked for comment: " + comment.getId());
        CustomInputDialogFragment reportDialog = CustomInputDialogFragment.newInstance(
                "Report Comment", "Please provide a brief reason for reporting this comment (optional).", "Reason for reporting", "Report", "Cancel", false);
        reportDialog.setInputListener(reason -> commentsViewModel.reportComment(comment, reason.isEmpty() ? "No reason provided" : reason));
        if (!isFinishing()) reportDialog.show(getSupportFragmentManager(), "ReportCommentDialog");
    }

    @Override
    public void onViewRepliesClicked(Comment comment, CommentAdapter.CommentViewHolder holder) {
        Log.d(TAG, "View replies clicked. Opening replies for: " + comment.getId());
        Intent intent = new Intent(this, RepliesActivity.class);
        intent.putExtra(RepliesActivity.EXTRA_POST, (Serializable) currentPostData);
        intent.putExtra(RepliesActivity.EXTRA_COMMENT_ID, comment.getId());
        intent.putExtra(RepliesActivity.EXTRA_SHOULD_FOCUS_REPLY, false);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up ViewPager callback
        if (postBinding != null && postBinding.mediaPagerFeed != null && pageChangeCallback != null) {
            postBinding.mediaPagerFeed.unregisterOnPageChangeCallback(pageChangeCallback);
            pageChangeCallback = null;
        }
        // Clear Poll observers
        clearPollObservers();

        if (binding != null && binding.commentsRecyclerView != null) {
            binding.commentsRecyclerView.setAdapter(null);
        }
        binding = null;
        postBinding = null;
    }
}