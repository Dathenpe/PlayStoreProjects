// main/java/com/f9ld3/Zion/ui/profile/FollowedContentFragment.java
package com.f9ld3.Zion.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // <-- Import ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.fragment.NavHostFragment; // <-- Import NavHostFragment

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding;
import com.f9ld3.Zion.ui.feed.CommentsActivity; // <-- Import CommentsActivity
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
        // *** FIX: Initialize PostLikeViewModel ***
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
        profileViewModel.refreshProfile();
    }

    private void setupRecyclerView() {
        // *** FIX: Pass PostLikeViewModel and LifecycleOwner to constructor ***
        contentAdapter = new SearchAllAdapter(this, this, postLikeViewModel, getViewLifecycleOwner());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(contentAdapter);
    }

    // --- Listener Implementations ---

    @Override
    public void onPostItemClick(Post post) { // Renamed from onPostClick
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
            Toast.makeText(getContext(), "Login to like posts", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCommentClick(Post post) {
        Log.i(TAG, "Comment clicked on post: " + post.getId());
        if (getContext() == null) return;
        Intent intent = new Intent(requireContext(), CommentsActivity.class);
        intent.putExtra(CommentsActivity.EXTRA_POST_ID, post.getId());
        intent.putExtra(CommentsActivity.EXTRA_POST_DATA, (Serializable) post);
        startActivity(intent);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for fragments
    }
}