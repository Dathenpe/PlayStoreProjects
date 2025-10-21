// main/java/com/f9ld3/Zion/ui/profile/FollowedContentFragment.java
package com.f9ld3.Zion.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem; // Added for Menu
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu; // Added for PopupMenu
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // <-- Import ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.fragment.NavHostFragment; // <-- Import NavHostFragment

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment; // Added for Delete confirmation
import com.f9ld3.Zion.ui.feed.CommentsBottomSheet; // <-- Import CommentsBottomSheet
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;
import com.f9ld3.Zion.ui.feed.PostDetailActivity; // <-- Import PostDetailActivity
import com.f9ld3.Zion.ui.feed.PostLikeViewModel; // <-- Import PostLikeViewModel
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;
import com.f9ld3.Zion.ui.player.PodcastPlayerActivity; // <-- Import PodcastPlayerActivity
import com.f9ld3.Zion.ui.player.VideoPlayerActivity; // <-- Import VideoPlayerActivity
import com.f9ld3.Zion.ui.search.SearchAllAdapter;
import com.google.firebase.auth.FirebaseAuth; // <-- Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // <-- Import FirebaseUser

import java.io.Serializable; // <-- Import Serializable
import java.util.ArrayList;
import java.util.List;

public class FollowedContentFragment extends Fragment implements PostAdapter.OnPostClickListener, PlayerPostAdapter.OnMediaClickListener {

    private static final String TAG = "FollowedContentFragment";
    private FragmentFullPageListBinding binding;

    private ProfileViewModel profileViewModel;
    private SearchAllAdapter contentAdapter; // Use the versatile SearchAllAdapter
    private PostLikeViewModel postLikeViewModel; // <-- Add member variable


    public static FollowedContentFragment newInstance(String followedChannelId) {
        return new FollowedContentFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFullPageListBinding.inflate(inflater, container, false);
        // Ensure initial state hides RecyclerView and shows placeholder
        binding.recyclerView.setVisibility(View.GONE);
        binding.textPlaceholder.setVisibility(View.VISIBLE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        // Initialize PostLikeViewModel
        postLikeViewModel = new ViewModelProvider(requireActivity()).get(PostLikeViewModel.class);

        setupRecyclerView(); // Call setupRecyclerView before observing LiveData

        // Configure toolbar from fragment_full_page_list.xml
        binding.toolbar.setTitle("Following"); // Set title
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp); // Set back icon if needed
        binding.toolbar.setNavigationOnClickListener(v -> {
            // Add navigation logic if needed, e.g., pop back stack
            NavHostFragment.findNavController(this).popBackStack();
            // requireActivity().onBackPressed(); // Or simply trigger back press
        });


        binding.textPlaceholder.setText(getString(R.string.followed_content_empty_text));
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_feed_24dp, 0, 0);

        profileViewModel.getFollowedContent().observe(getViewLifecycleOwner(), content -> {
            Log.d(TAG, "Observed followed content update. Size: " + (content != null ? content.size() : "null"));
            if (binding == null) return; // Add null check for binding

            if (content != null && !content.isEmpty()) {
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.textPlaceholder.setVisibility(View.GONE);
                contentAdapter.submitList(content);
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
                // Optionally clear the adapter list when content is empty/null
                contentAdapter.submitList(null);
            }
        });

        // Trigger loading the followed content
        // Make sure this method actually triggers the fetch in your ProfileViewModel
        // You might need a specific method like profileViewModel.fetchFollowedContent()
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Assuming ProfileViewModel fetches followed content when following list is loaded
            profileViewModel.fetchFollowingChannels(currentUser.getUid()); // Or a dedicated fetch method
            profileViewModel.fetchFollowingUsers(currentUser.getUid());
        }
    }

    private void setupRecyclerView() {
        // Pass PostLikeViewModel and LifecycleOwner to constructor
        contentAdapter = new SearchAllAdapter(this, this, postLikeViewModel, getViewLifecycleOwner(), requireActivity());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(contentAdapter);
    }

    // --- Listener Implementations ---

    @Override
    public void onPostItemClick(Post post) {
        Log.i(TAG, "Post item clicked: " + post.getId());
        if (getContext() == null) return;
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getId());
        intent.putExtra(PostDetailActivity.EXTRA_POST_DATA, (Serializable) post);
        startActivity(intent);
    }


    @Override
    public void onLikeClick(Post post) {
        Log.i(TAG, "Like clicked on post: " + post.getId());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            // Use the initialized ViewModel
            postLikeViewModel.toggleLike(post.getId(), post);
        } else if (getContext() != null){
            Toast.makeText(getContext(), R.string.login_for_features, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCommentClick(Post post) {
        Log.i(TAG, "Comment clicked on post: " + post.getId());
        if (getContext() == null) return;
        // Show Comments Bottom Sheet instead of starting Activity
        CommentsBottomSheet commentsSheet = CommentsBottomSheet.newInstance(post.getId(), post);
        commentsSheet.show(getParentFragmentManager(), CommentsBottomSheet.TAG);
    }

    // *** ADDED Missing Method Implementation ***
    @Override
    public void onOptionClick(Post post, View anchorView) {
        Log.i(TAG, "Options clicked for post: " + post.getId());
        showPostOptionsMenu(anchorView, post); // Reuse logic similar to FeedFragment
    }

    // *** ADDED Missing Method Implementation ***
    @Override
    public void onAuthorClick(Post post) {
        Log.i(TAG, "Author clicked: " + post.getAuthorName() + " (ID: " + post.getAuthorUid() + ")");
        if (post.getAuthorUid() != null) {
            Bundle args = new Bundle();
            args.putString("channelId", post.getAuthorUid());
            args.putString("channelName", post.getAuthorName()); // Pass name for title
            try {
                NavHostFragment.findNavController(FollowedContentFragment.this)
                        .navigate(R.id.navigation_channel, args);
            } catch (Exception e) {
                Log.e(TAG, "Navigation to channel failed", e);
            }
        }
    }


    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        Log.i(TAG, "Media item clicked: " + mediaItem.getTitle() + " Type: " + mediaItem.getType());
        if (getContext() == null) return;

        // TODO: Log history if MainActivity implements the HistoryLogger interface

        Intent intent = null;
        if (mediaItem.getType() == PlayerMedia.TYPE_VIDEO) {
            intent = new Intent(requireContext(), VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_MEDIA_ITEM, mediaItem);
        } else if (mediaItem.getType() == PlayerMedia.TYPE_PODCAST_SINGLE) {
            intent = new Intent(requireContext(), PodcastPlayerActivity.class);
            intent.putExtra(PodcastPlayerActivity.EXTRA_MEDIA_ITEM, mediaItem);
        }
        // Add cases for other media types if necessary

        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Cannot play this media type yet.", Toast.LENGTH_SHORT).show();
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
            popup.getMenu().add("Delete"); // Option to delete own post
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
                // Call ViewModel method to delete the post from Firestore
                // Example: feedViewModel.deletePost(post.getId()); // Needs implementation
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
        if (binding != null && binding.recyclerView != null) {
            binding.recyclerView.setAdapter(null); // Detach adapter
        }
        binding = null; // Important for fragments
    }
}