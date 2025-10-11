package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding;
import com.f9ld3.Zion.ui.feed.Post;
import com.f9ld3.Zion.ui.feed.PostAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to display the user's uploaded blog posts.
 */
public class MyBlogsFragment extends Fragment implements PostAdapter.OnPostClickListener {

    private static final String TAG = "MyBlogsFragment";
    private FragmentFullPageListBinding binding;
    private ProfileViewModel profileViewModel;
    private PostAdapter blogAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFullPageListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        setupRecyclerView();

        // Customize the empty state for this page (using template)
        binding.textPlaceholder.setText(getString(R.string.blogs_empty_text));
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_feed_24dp, 0, 0);

        profileViewModel.getUserBlogs().observe(getViewLifecycleOwner(), blogs -> {
            if (blogs != null && !blogs.isEmpty()) {
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.textPlaceholder.setVisibility(View.GONE);
                blogAdapter.submitList(blogs);
                Log.d(TAG, "Displaying " + blogs.size() + " user blogs.");
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.textPlaceholder.setVisibility(View.VISIBLE);
                Log.d(TAG, "No user blogs to display.");
            }
        });
    }

    private void setupRecyclerView() {
        blogAdapter = new PostAdapter(this); // 'this' refers to OnPostClickListener
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(blogAdapter);
    }

    @Override
    public void onPostClick(Post post) {
        // TODO: Implement navigation to a detailed reader view for the blog post.
        Log.i(TAG, "Blog Post clicked from MyBlogs: " + post.getTitle() + " (ID: " + post.getId() + ")");
        // Example: Toast.makeText(getContext(), "Opening blog: " + post.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}