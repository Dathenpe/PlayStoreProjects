package com.f9ld3.Zion.ui.notifications;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue; // Import TypedValue
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.Zion.R;
import com.google.android.material.color.MaterialColors;
import com.google.firebase.Timestamp;

import java.util.Objects;

public class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private final OnNotificationClickListener clickListener;

    public NotificationAdapter(OnNotificationClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = getItem(position);
        holder.bind(notification, clickListener);
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView message;
        private final TextView snippet;
        private final TextView timestamp;
        private final View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.notification_icon);
            message = itemView.findViewById(R.id.notification_message);
            snippet = itemView.findViewById(R.id.notification_snippet);
            timestamp = itemView.findViewById(R.id.notification_timestamp);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }

        public void bind(final Notification notification, final OnNotificationClickListener listener) {
            Context context = itemView.getContext();
            message.setText(notification.getMessage());

            // Set icon based on type
            switch (notification.getType()) {
                case "follow":
                    icon.setImageResource(R.drawable.ic_person_24dp);
                    break;
                case "post_like":
                case "comment_like":
                    icon.setImageResource(R.drawable.ic_like);
                    break;
                case "post_comment":
                case "comment_reply":
                    icon.setImageResource(R.drawable.ic_comment);
                    break;
                default:
                    icon.setImageResource(R.drawable.ic_notifications_24dp);
            }

            // Set snippet
            String postSnippet = null;
            String commentSnippet = null;

            if (notification.getData() != null) {
                postSnippet = (String) notification.getData().get("postTextSnippet");
                commentSnippet = (String) notification.getData().get("commentTextSnippet");
            }

            String finalSnippet = null;
            if (commentSnippet != null && !commentSnippet.isEmpty()) {
                finalSnippet = "Your comment: \"" + commentSnippet + "\"";
            } else if (postSnippet != null && !postSnippet.isEmpty()) {
                finalSnippet = "On post: \"" + postSnippet + "\"";
            }

            if (finalSnippet != null) {
                snippet.setText(finalSnippet);
                snippet.setVisibility(View.VISIBLE);
            } else {
                snippet.setVisibility(View.GONE);
            }


            // Set timestamp
            Timestamp ts = notification.getTimestamp();
            if (ts != null) {
                timestamp.setText(DateUtils.getRelativeTimeSpanString(ts.toDate().getTime(),
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
                timestamp.setVisibility(View.VISIBLE);
            } else {
                timestamp.setVisibility(View.GONE);
            }

            // Set unread indicator
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // --- UPDATED BACKGROUND COLOR ---
            if (!notification.isRead()) {
                // Use a semi-transparent version of the secondary color for a stronger highlight
                int baseColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondary, Color.GRAY);
                int unreadColorWithAlpha = Color.argb(30, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)); // ~12% alpha
                itemView.setBackgroundColor(unreadColorWithAlpha);
            } else {
                // Reset to default selectable background
                TypedValue outValue = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                itemView.setBackgroundResource(outValue.resourceId);
            }
            // --- END UPDATE ---


            itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
        }
    }

    private static final DiffUtil.ItemCallback<Notification> DIFF_CALLBACK = new DiffUtil.ItemCallback<Notification>() {
        @Override
        public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.isRead() == newItem.isRead() &&
                    Objects.equals(oldItem.getMessage(), newItem.getMessage()) &&
                    Objects.equals(oldItem.getTimestamp(), newItem.getTimestamp());
        }
    };
}