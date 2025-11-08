package com.f9ld3.Zion.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // Import Toast

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // Import
import androidx.recyclerview.widget.LinearLayoutManager; // Import
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentListNoToolbarBinding;
import com.f9ld3.Zion.ui.feed.PostDetailActivity; // Import
import com.f9ld3.Zion.ui.feed.RepliesActivity; // Import

import java.util.Map; // Import

// Implement the adapter's click listener
public class NotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private static final String TAG = "NotificationsFragment"; // Add TAG
    private FragmentListNoToolbarBinding binding;
    private NotificationViewModel notificationViewModel; // Add ViewModel
    private NotificationAdapter adapter; // Add Adapter

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListNoToolbarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get ViewModel (scoped to Activity, as MainActivity already uses it)
        notificationViewModel = new ViewModelProvider(requireActivity()).get(NotificationViewModel.class);

        setupRecyclerView(); // Call setup

        binding.textPlaceholder.setText(R.string.notifications_empty_text);
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_notifications_24dp, 0, 0);

        // Observe the list of notifications
        notificationViewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            if (binding == null) return;
            boolean isEmpty = notifications == null || notifications.isEmpty();

            binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.textPlaceholder.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

            adapter.submitList(notifications);
            if (!isEmpty) {
                Log.d(TAG, "Displaying " + notifications.size() + " notifications.");
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this); // 'this' is the listener
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    // --- IMPLEMENT CLICK LISTENER ---
    @Override
    public void onNotificationClick(Notification notification) {
        if (getContext() == null || !isAdded()) return;

        // 1. Mark as read
        notificationViewModel.markNotificationAsRead(notification);

        // 2. Navigate
        String type = notification.getType();
        Map<String, Object> data = notification.getData();
        if (data == null) {
            Log.w(TAG, "Notification data is null, cannot navigate.");
            Toast.makeText(getContext(), "Could not open notification.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String postId = (String) data.get("postId");
            String commentId = (String) data.get("commentId");
            String followerId = (String) data.get("followerId");

            if ("post_like".equals(type) || "post_comment".equals(type)) {
                if (postId != null) {
                    Intent intent = new Intent(getContext(), PostDetailActivity.class);
                    intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
                    // --- NEW: Pass comment ID if it's a "post_comment" type ---
                    if ("post_comment".equals(type) && commentId != null) {
                        intent.putExtra(PostDetailActivity.EXTRA_HIGHLIGHT_COMMENT_ID, commentId);
                    }
                    // --- END NEW ---
                    startActivity(intent);
                }
            } else if ("comment_like".equals(type) || "comment_reply".equals(type)) {
                if (postId != null && commentId != null) {
                    // --- UPDATED: Navigate to PostDetail BUT include highlight ID ---
                    Log.w(TAG, "Navigating to PostDetail for reply/comment like notification.");
                    Intent intentPost = new Intent(getContext(), PostDetailActivity.class);
                    intentPost.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
                    intentPost.putExtra(PostDetailActivity.EXTRA_HIGHLIGHT_COMMENT_ID, commentId); // <-- PASS THE ID
                    startActivity(intentPost);
                    // --- END UPDATE ---
                }
            } else if ("follow".equals(type)) {
                if (followerId != null) {
                    // Start ChannelActivity
                    Intent intent = new Intent(getContext(), com.f9ld3.Zion.ui.channel.ChannelActivity.class);
                    intent.putExtra(com.f9ld3.Zion.ui.channel.ChannelActivity.EXTRA_CHANNEL_ID, followerId);
                    intent.putExtra(com.f9ld3.Zion.ui.channel.ChannelActivity.EXTRA_CHANNEL_NAME, (String) data.get("followerName"));
                    startActivity(intent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating from notification", e);
            Toast.makeText(getContext(), "Error opening notification.", Toast.LENGTH_SHORT).show();
        }
    }
    // --- END IMPLEMENTATION ---

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null && binding.recyclerView != null) {
            binding.recyclerView.setAdapter(null); // Detach adapter
        }
        binding = null;
    }
}