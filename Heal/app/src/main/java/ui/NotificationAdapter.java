package ui; // Or 'adapters' if you create a new package

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.R; // Ensure this points to your R file

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<String> notificationList;

    public NotificationAdapter(List<String> notificationList) {
        this.notificationList = notificationList;
    }

    // Method to update the data in the adapter
    public void updateData(List<String> newNotificationList) {
        this.notificationList = newNotificationList;
        notifyDataSetChanged(); // Notify RecyclerView that the data has changed
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single notification item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        String notificationMessage = notificationList.get(position);
        holder.notificationTextView.setText("• " + notificationMessage);
        // You can add more styling or click listeners here if needed
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    // ViewHolder class to hold the views for each item
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView notificationTextView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationTextView = itemView.findViewById(R.id.notificationTextView); // Assuming you'll create this ID
            // Apply text color and size as per your MainActivity's updateRecentlySentNotificationsDisplay()
            notificationTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));
            notificationTextView.setTextSize(14); // Corresponds to 14sp
        }
    }
}
