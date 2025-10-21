// main/java/com/f9ld3/Zion/ui/search/SearchFragment.java
package com.f9ld3.Zion.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.MenuItem; // Import MenuItem for options menu handling
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu; // Import PopupMenu
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment; // Import for delete confirmation
import com.f9ld3.Zion.ui.feed.CommentsBottomSheet; // Import CommentsBottomSheet
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;
import com.f9ld3.Zion.ui.feed.PostDetailActivity; // Import PostDetailActivity
import com.f9ld3.Zion.ui.feed.PostLikeViewModel; // <-- Import PostLikeViewModel
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;
import com.f9ld3.Zion.ui.player.PodcastPlayerActivity; // Import PodcastPlayerActivity
import com.f9ld3.Zion.ui.player.VideoPlayerActivity; // Import VideoPlayerActivity
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser

import java.io.Serializable; // Import Serializable
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment implements PlayerPostAdapter.OnMediaClickListener, PostAdapter.OnPostClickListener {

    private static final String TAG = "SearchFragment"; // Add TAG for logging

    private TabLayout tabLayout;
    private EditText searchEditText;
    private ImageButton buttonClose;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textPlaceholder;

    private SearchViewModel searchViewModel;
    private PlayerPostAdapter videoAdapter;
    private PlayerPostAdapter podcastAdapter;
    private PostAdapter postAdapter;
    private SearchAllAdapter allAdapter;
    private PostLikeViewModel postLikeViewModel; // <-- Add member variable

    private static final int TAB_ALL = 0;
    private static final int TAB_POSTS = 1;
    private static final int TAB_VIDEOS = 2;
    private static final int TAB_PODCASTS = 3;
    private static final int TAB_USERS = 4;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        tabLayout = view.findViewById(R.id.tab_layout);
        searchEditText = view.findViewById(R.id.search_input_edit_text);
        buttonClose = view.findViewById(R.id.button_close);
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        textPlaceholder = view.findViewById(R.id.text_placeholder);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        // *** Get PostLikeViewModel scoped to the Activity/Fragment lifecycle ***
        postLikeViewModel = new ViewModelProvider(requireActivity()).get(PostLikeViewModel.class);

        setupSearchBar();
        setupTabs();
        setupRecyclerView(); // Now happens after postLikeViewModel is initialized
        setupObservers();

        // Set initial empty state
        updateEmptyState(true);
    }

    private void setupSearchBar() {
        buttonClose.setOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).popBackStack();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error navigating back: ", e);
                if (getActivity() != null) getActivity().finish(); // Fallback to finish activity
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() > 1) { // Start search after 2 chars
                    searchViewModel.searchAll(query);
                } else {
                    searchViewModel.clearResults();
                    updateEmptyState(true); // Show initial prompt when query is cleared
                }
            }
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Posts"));
        tabLayout.addTab(tabLayout.newTab().setText("Videos"));
        tabLayout.addTab(tabLayout.newTab().setText("Podcasts"));
        tabLayout.addTab(tabLayout.newTab().setText("Users"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { switchTab(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Pass 'this' as the listener for both media and post clicks
        videoAdapter = new PlayerPostAdapter(this);
        podcastAdapter = new PlayerPostAdapter(this);
        // *** Pass the PostLikeViewModel, LifecycleOwner, and Activity ***
        postAdapter = new PostAdapter(this, getViewLifecycleOwner(), requireActivity());
        // *** Pass the PostLikeViewModel, LifecycleOwner, and Activity ***
        allAdapter = new SearchAllAdapter(this, this, postLikeViewModel, getViewLifecycleOwner(), requireActivity());
        recyclerView.setAdapter(allAdapter); // Start with the 'All' adapter
    }

    private void setupObservers() {
        searchViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading != null && isLoading ? View.VISIBLE : View.GONE);
            // Hide empty state while loading, unless it's the initial prompt
            if (isLoading != null && isLoading && textPlaceholder != null && !textPlaceholder.getText().equals("Start typing to search")) {
                textPlaceholder.setVisibility(View.GONE);
                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            }
        });

        searchViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && getContext() != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                updateEmptyState(true); // Show error state
            }
        });

        // Observe results for the currently selected tab
        searchViewModel.getAllResults().observe(getViewLifecycleOwner(), results -> {
            if (tabLayout != null && tabLayout.getSelectedTabPosition() == TAB_ALL && allAdapter != null) {
                Log.d(TAG, "Updating ALL results. Count: " + (results != null ? results.size() : "null"));
                allAdapter.submitList(results);
                updateEmptyState(results == null || results.isEmpty());
            }
        });

        searchViewModel.getPostResults().observe(getViewLifecycleOwner(), posts -> {
            if (tabLayout != null && tabLayout.getSelectedTabPosition() == TAB_POSTS && postAdapter != null) {
                Log.d(TAG, "Updating POSTS results. Count: " + (posts != null ? posts.size() : "null"));
                postAdapter.submitList(posts);
                updateEmptyState(posts == null || posts.isEmpty());
            }
        });


        searchViewModel.getVideoResults().observe(getViewLifecycleOwner(), videos -> {
            if (tabLayout != null && tabLayout.getSelectedTabPosition() == TAB_VIDEOS && videoAdapter != null) {
                videoAdapter.submitList(videos);
                updateEmptyState(videos == null || videos.isEmpty());
            }
        });

        searchViewModel.getPodcastResults().observe(getViewLifecycleOwner(), podcasts -> {
            if (tabLayout != null && tabLayout.getSelectedTabPosition() == TAB_PODCASTS && podcastAdapter != null) {
                podcastAdapter.submitList(podcasts);
                updateEmptyState(podcasts == null || podcasts.isEmpty());
            }
        });

        searchViewModel.getUserResults().observe(getViewLifecycleOwner(), users -> {
            if (tabLayout != null && tabLayout.getSelectedTabPosition() == TAB_USERS && allAdapter != null) {
                // The 'allAdapter' can handle UserProfile type, convert List<UserProfile> to List<Object>
                allAdapter.submitList(users != null ? new ArrayList<>(users) : null);
                updateEmptyState(users == null || users.isEmpty());
            }
        });
    }

    private void switchTab(int position) {
        if (recyclerView == null) return; // Add null check for safety
        Log.d(TAG, "Switching to tab: " + position);
        RecyclerView.Adapter<?> currentAdapter = null;
        List<?> currentData = null;

        switch (position) {
            case TAB_ALL:
                currentAdapter = allAdapter;
                currentData = searchViewModel.getAllResults().getValue();
                recyclerView.setAdapter(allAdapter);
                allAdapter.submitList(currentData != null ? (List<Object>) currentData : null);
                break;
            case TAB_POSTS:
                currentAdapter = postAdapter;
                currentData = searchViewModel.getPostResults().getValue();
                recyclerView.setAdapter(postAdapter);
                postAdapter.submitList(currentData != null ? (List<Post>) currentData : null);
                break;
            case TAB_VIDEOS:
                currentAdapter = videoAdapter;
                currentData = searchViewModel.getVideoResults().getValue();
                recyclerView.setAdapter(videoAdapter);
                videoAdapter.submitList(currentData != null ? (List<PlayerMedia>) currentData : null);
                break;
            case TAB_PODCASTS:
                currentAdapter = podcastAdapter;
                currentData = searchViewModel.getPodcastResults().getValue();
                recyclerView.setAdapter(podcastAdapter);
                podcastAdapter.submitList(currentData != null ? (List<PlayerMedia>) currentData : null);
                break;
            case TAB_USERS:
                currentAdapter = allAdapter;
                List<UserProfile> users = searchViewModel.getUserResults().getValue();
                currentData = users != null ? new ArrayList<>(users) : new ArrayList<>();
                recyclerView.setAdapter(allAdapter);
                allAdapter.submitList((List<Object>) currentData);
                break;
        }
        updateEmptyState(currentData == null || currentData.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        if (textPlaceholder == null || recyclerView == null || progressBar == null || searchEditText == null) return; // Add null checks

        String currentQuery = searchEditText.getText().toString().trim();
        if (progressBar.getVisibility() == View.VISIBLE) {
            textPlaceholder.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        } else if (currentQuery.isEmpty()) {
            textPlaceholder.setText("Start typing to search");
            textPlaceholder.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else if (isEmpty) {
            textPlaceholder.setText(R.string.no_results_found);
            textPlaceholder.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textPlaceholder.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }


    // --- Click Listener Implementations ---

    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        Log.i(TAG, "Media item clicked in Search: " + mediaItem.getTitle() + " Type: " + mediaItem.getType());
        if (getContext() == null) return;

        Intent intent = null;
        if (mediaItem.getType() == PlayerMedia.TYPE_VIDEO) {
            intent = new Intent(requireContext(), VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_MEDIA_ITEM, mediaItem);
        } else if (mediaItem.getType() == PlayerMedia.TYPE_PODCAST_SINGLE) {
            intent = new Intent(requireContext(), PodcastPlayerActivity.class);
            intent.putExtra(PodcastPlayerActivity.EXTRA_MEDIA_ITEM, mediaItem);
        }

        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Cannot play this media type.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Post Click Listeners ---
    @Override
    public void onPostItemClick(Post post) {
        Log.i(TAG, "Post item clicked in Search: " + post.getId());
        if (getContext() == null) return;
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getId());
        intent.putExtra(PostDetailActivity.EXTRA_POST_DATA, (Serializable) post);
        startActivity(intent);
    }


    @Override
    public void onLikeClick(Post post) {
        Log.i(TAG, "Like clicked for post in Search: " + post.getId());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            postLikeViewModel.toggleLike(post.getId(), post);
        } else if (getContext() != null) {
            Toast.makeText(getContext(), R.string.login_for_features, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCommentClick(Post post) {
        Log.i(TAG, "Comment clicked for post in Search: " + post.getId());
        if (getContext() == null) return;
        CommentsBottomSheet commentsSheet = CommentsBottomSheet.newInstance(post.getId(), post);
        commentsSheet.show(getParentFragmentManager(), CommentsBottomSheet.TAG);
    }

    // *** ADDED: Implementation for onOptionClick ***
    @Override
    public void onOptionClick(Post post, View anchorView) {
        Log.i(TAG, "Options clicked for post in Search: " + post.getId());
        showPostOptionsMenu(anchorView, post); // Reuse logic similar to FeedFragment
    }

    // *** ADDED: Implementation for onAuthorClick ***
    @Override
    public void onAuthorClick(Post post) {
        Log.i(TAG, "Author clicked in Search: " + post.getAuthorName() + " (ID: " + post.getAuthorUid() + ")");
        if (post.getAuthorUid() != null) {
            Bundle args = new Bundle();
            args.putString("channelId", post.getAuthorUid());
            args.putString("channelName", post.getAuthorName());
            try {
                NavHostFragment.findNavController(SearchFragment.this)
                        .navigate(R.id.navigation_channel, args);
            } catch (Exception e) {
                Log.e(TAG, "Navigation to channel failed", e);
            }
        }
    }


    // --- Helper for Post Options Menu (Similar to FeedFragment) ---
    private void showPostOptionsMenu(View anchorView, Post post) {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenu().add("Share");
        popup.getMenu().add("Report");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(post.getAuthorUid())) {
            popup.getMenu().add("Delete");
        }

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Share".equals(title)) {
                sharePost(post);
            } else if ("Report".equals(title)) {
                reportPost(post);
            } else if ("Delete".equals(title)) {
                deletePost(post);
            } else {
                return false;
            }
            return true;
        });
        popup.show();
    }

    private void sharePost(Post post) {
        if (getContext() == null) return;
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String shareText = post.getTextContent() != null ? post.getTextContent() : "Check out this post!";
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));
    }

    private void reportPost(Post post) {
        Toast.makeText(getContext(), "Report functionality TBD", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Reporting post ID: " + post.getId());
    }

    private void deletePost(Post post) {
        if (getContext() == null) return;
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Delete Post?",
                "Are you sure you want to permanently delete this post?",
                "Delete",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                // Call ViewModel method to delete
                Log.d(TAG, "Deleting post ID: " + post.getId());
                Toast.makeText(getContext(), "Delete functionality TBD", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNegativeClick() {}
        });
        dialog.show(getParentFragmentManager(), "DeletePostDialog");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify views to prevent memory leaks
        tabLayout = null;
        searchEditText = null;
        buttonClose = null;
        if (recyclerView != null) {
            recyclerView.setAdapter(null); // Detach adapter
        }
        recyclerView = null;
        progressBar = null;
        textPlaceholder = null;
        Log.d(TAG, "SearchFragment onDestroyView");
    }
}