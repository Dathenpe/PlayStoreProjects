package records;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heal.R; // Ensure this path is correct for your project

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Make sure to import HomeFragment.MoodEntry if MoodEntry is an inner class of HomeFragment
import static ui.HomeFragment.MoodEntry; // Correct static import to reference MoodEntry from HomeFragment

public class MoodEntryAdapter extends RecyclerView.Adapter<MoodEntryAdapter.MoodEntryViewHolder> {

    private List<MoodEntry> moodEntryList;
    private MoodCheckinFragment fragment; // Reference to the fragment for deletion callback

    // Constructor that takes the list of mood entries and a reference to the fragment
    public MoodEntryAdapter(List<MoodEntry> moodEntryList, MoodCheckinFragment fragment) {
        this.moodEntryList = moodEntryList;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public MoodEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single mood entry item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mood_entry, parent, false);
        return new MoodEntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodEntryViewHolder holder, int position) {
        MoodEntry currentEntry = moodEntryList.get(position);

        // Format the day string (e.g., "yyyy-MM-dd") into a more readable format
        try {
            SimpleDateFormat inputDayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date dateDay = inputDayFormat.parse(currentEntry.getDay());
            SimpleDateFormat outputDayFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()); // e.g., "Mon, Jan 22, 2024"
            holder.dayTextView.setText(outputDayFormat.format(dateDay));
        } catch (java.text.ParseException e) {
            // Fallback to raw day string if parsing fails
            holder.dayTextView.setText(currentEntry.getDay());
        }

        // Display mood level and text
        holder.moodLevelTextView.setText("Mood: " + currentEntry.getMoodLevel());
        holder.moodTextTextView.setText(currentEntry.getMoodText());

        // Display timestamp if available
        if (currentEntry.getTimestamp() > 0) { // Check if timestamp exists and is valid
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault()); // e.g., "03:02 PM"
            holder.moodTimeTextView.setText("Time: " + timeFormat.format(new Date(currentEntry.getTimestamp())));
            holder.moodTimeTextView.setVisibility(View.VISIBLE); // Make sure the TextView is visible
        } else {
            holder.moodTimeTextView.setVisibility(View.GONE); // Hide the TextView if no valid timestamp
        }

        // Set up the click listener for the delete button
        holder.deleteButton.setOnClickListener(v -> {
            // Call the delete method in the MoodRecordsFragment using the adapter position
            fragment.deleteMoodEntry(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return moodEntryList.size(); // Return the total number of items in the list
    }

    // ViewHolder class to hold references to the views for each item
    public static class MoodEntryViewHolder extends RecyclerView.ViewHolder {
        TextView dayTextView;
        TextView moodTimeTextView; // For displaying the time
        TextView moodLevelTextView;
        TextView moodTextTextView;
        ImageView deleteButton;

        public MoodEntryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link the TextViews and ImageView to their respective IDs in item_mood_entry.xml
            dayTextView = itemView.findViewById(R.id.moodEntryDateTextView);
            moodTimeTextView = itemView.findViewById(R.id.moodEntryTimeTextView);
            moodLevelTextView = itemView.findViewById(R.id.moodEntryLevelTextView);
            moodTextTextView = itemView.findViewById(R.id.moodEntryDetailsTextView);
            deleteButton = itemView.findViewById(R.id.deleteMoodEntryIcon);
        }
    }
}