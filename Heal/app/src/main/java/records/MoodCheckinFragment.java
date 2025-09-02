package records;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ui.CustomMessageDialogFragment;
import ui.HomeFragment.MoodEntry;

public class MoodCheckinFragment extends Fragment implements MoodEntryAdapter.onMoodEntryClickListener {

    private RecyclerView recyclerView;
    private MoodEntryAdapter adapter;
    private List<MoodEntry> moodEntries; // Using the MoodEntry class
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();

    private MainActivity mainActivity;

    private TextView emptyStateTextView;

    private static final String PREFS_MOOD = "mood_prefs";
    private static final String KEY_MOOD_ENTRIES = "mood_entries";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: CopingExercisesFragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mood_checkin, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewMoodEntries);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emptyStateTextView = view.findViewById(R.id.moodEntriesEmptyStateTextView);
        if (getContext() != null) {
            sharedPreferences = getContext().getSharedPreferences(PREFS_MOOD, Context.MODE_PRIVATE);
            loadMoodData(); // Load data when the fragment view is created
        }

        // Initialize adapter with the loaded data and pass 'this' fragment for deletion callback
        adapter = new MoodEntryAdapter(moodEntries, this, this);
        recyclerView.setAdapter(adapter);
        return view;
    }

    // Method to load mood data from SharedPreferences
    private void loadMoodData() {
        String json = sharedPreferences.getString(KEY_MOOD_ENTRIES, null);
        if (json != null) {
            Type type = new TypeToken<List<MoodEntry>>() {}.getType();
            moodEntries = gson.fromJson(json, type);
            // Ensure the list is not null after deserialization
            if (moodEntries == null) {
                moodEntries = new ArrayList<>();
            }
        } else {
            moodEntries = new ArrayList<>();
        }
        Collections.sort(moodEntries, new Comparator<MoodEntry>() {
            @Override
            public int compare(MoodEntry m1, MoodEntry m2) {
                // Assuming MoodEntry now has a getTimestamp() method.
                // If not, use getDay() with a SimpleDateFormat for comparison.
                return Long.compare(m2.getTimestamp(), m1.getTimestamp()); // Newest first
            }
        });
        updateEmptyStateVisibility();
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

            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Delete Mood Entry", // Dialog title
                    "Are you sure you want to delete this mood entry for " + entryToDelete.getDay() + "? This action cannot be undone.", // Confirmation message
                    "Yes", // Positive button text
                    "No" // Negative button text
            );

            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    moodEntries.remove(position);
                    saveMoodDataToPreferences(); // Save the updated list to storage
                    adapter.notifyItemRemoved(position); // Notify RecyclerView that an item has been removed
                    adapter.notifyItemRangeChanged(position, moodEntries.size()); // Notify subsequent items of position changes
                    Toast.makeText(getContext(), "Mood entry deleted!", Toast.LENGTH_SHORT).show();
                    updateEmptyStateVisibility();
                    dialogFragment.dismiss(); // Dismiss the dialog
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    // User clicked "No", just dismiss the dialog
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getParentFragmentManager(), "DeleteMoodEntryDialog"); // Show the dialog
        }
    }

    @Override
    public void onMoodEntryClick(MoodEntry moodEntry) {
        // Now you have the full MoodEntry object.
        // Construct a detailed string from the MoodEntry object's properties.

        StringBuilder detailsBuilder = new StringBuilder();

        // --- Format Day ---
        String formattedDay = moodEntry.getDay(); // Default to raw day string
        if (moodEntry.getDay() != null && !moodEntry.getDay().isEmpty()) {
            try {
                SimpleDateFormat inputDayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date dateDayObj = inputDayFormat.parse(moodEntry.getDay());
                SimpleDateFormat outputDayFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
                formattedDay = outputDayFormat.format(dateDayObj);
            } catch (java.text.ParseException e) {
                // If parsing fails, formattedDay remains the raw day string (already set)
                Log.e("MoodCheckinFragment", "Error parsing day: " + moodEntry.getDay(), e);
            }
        } else {
            formattedDay = "N/A";
        }
        detailsBuilder.append("Date: ").append(formattedDay).append("\n");

        // --- Format Time ---
        if (moodEntry.getTimestamp() > 0) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String formattedTime = timeFormat.format(new Date(moodEntry.getTimestamp()));
            detailsBuilder.append("Time: ").append(formattedTime).append("\n");
        }

        detailsBuilder.append("Mood Level: ").append(moodEntry.getMoodLevel()).append("/10").append("\n");
        detailsBuilder.append("How I'm Feeling: ").append(moodEntry.getMoodText());

        // --- Corrected logic: Create and show the dialog ---
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Mood Entry Details",
                detailsBuilder.toString(),
                "Close",
                null // No negative button needed for a simple close
        );
        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                dialogFragment.dismiss();
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                // This won't be called as the negative button is null
            }
        });
        dialog.show(getParentFragmentManager(), "MoodEntryDetailsDialog");
    }

    private void updateEmptyStateVisibility() {
        if (moodEntries.isEmpty()){
            emptyStateTextView.setVisibility(View.VISIBLE);
            emptyStateTextView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume(){
        mainActivity.toolbar.setTitle("My Mood History");
        mainActivity.navigationView.setCheckedItem(R.id.nav_records);
        mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
        mainActivity.Fab.setVisibility(View.VISIBLE);
        super.onResume();
    }
}
