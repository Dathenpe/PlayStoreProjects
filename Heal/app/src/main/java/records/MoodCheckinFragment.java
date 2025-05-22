package records;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // This import might not be strictly needed if you only use it for the title
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heal.R; // Ensure this path is correct for your project
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import ui.HomeFragment.MoodEntry;

// Make sure to import HomeFragment.MoodEntry if MoodEntry is an inner class of HomeFragment
// If MoodEntry is a standalone class, you would import it directly.
// Assuming it's an inner class of HomeFragment for now as per your provided code.


import records.MoodEntryAdapter;
 // Static import for simplicity if MoodEntry is public static in HomeFragment

public class MoodCheckinFragment extends Fragment {

    private RecyclerView recyclerView;
    private MoodEntryAdapter adapter;
    private List<MoodEntry> moodEntries; // Using the MoodEntry class
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();

    private static final String PREFS_MOOD = "mood_prefs";
    private static final String KEY_MOOD_ENTRIES = "mood_entries";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mood_checkin, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewMoodEntries);
        // Set up the LinearLayoutManager for vertical scrolling
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (getContext() != null) {
            // Get SharedPreferences instance
            sharedPreferences = getContext().getSharedPreferences(PREFS_MOOD, Context.MODE_PRIVATE);
            loadMoodData(); // Load data when the fragment view is created
        }

        // Initialize adapter with the loaded data and pass 'this' fragment for deletion callback
        adapter = new MoodEntryAdapter(moodEntries, this);
        recyclerView.setAdapter(adapter);

        // Optional: Set title for the fragment if you're not doing it via MainActivity toolbar

        return view;
    }

    // Method to load mood data from SharedPreferences
    private void loadMoodData() {
        String json = sharedPreferences.getString(KEY_MOOD_ENTRIES, null);
        if (json != null) {
            // Use TypeToken to correctly deserialize List<MoodEntry>
            Type type = new TypeToken<List<MoodEntry>>() {}.getType();
            moodEntries = gson.fromJson(json, type);
            // Ensure the list is not null after deserialization
            if (moodEntries == null) {
                moodEntries = new ArrayList<>();
            }
        } else {
            // If no data is found, initialize an empty list
            moodEntries = new ArrayList<>();
        }
    }

    // Method to save mood data back to SharedPreferences, typically called after changes (like deletion)
    private void saveMoodDataToPreferences() {
        if (sharedPreferences == null) return; // Defensive check
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(moodEntries); // Convert the updated list to JSON
        editor.putString(KEY_MOOD_ENTRIES, json);
        editor.apply(); // Apply changes asynchronously
    }
    public void deleteMoodEntry(final int position) { // Make position final if used in inner classes
        if (position >= 0 && position < moodEntries.size()) {
            // Get the mood entry to be deleted for display in the dialog
            MoodEntry entryToDelete = moodEntries.get(position);
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Mood Entry") // Dialog title
                    .setMessage("Are you sure you want to delete this mood entry for " + entryToDelete.getDay() + "?") // Confirmation message
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            moodEntries.remove(position);
                            saveMoodDataToPreferences(); // Save the updated list to storage
                            adapter.notifyItemRemoved(position); // Notify RecyclerView that an item has been removed
                            adapter.notifyItemRangeChanged(position, moodEntries.size()); // Notify subsequent items of position changes
                            Toast.makeText(getContext(), "Mood entry deleted!", Toast.LENGTH_SHORT).show(); // Optional: feedback
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // User clicked "No", just dismiss the dialog
                            dialog.dismiss();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert) // Optional: add an alert icon
                    .show(); // Show the dialog
        }
    }
}