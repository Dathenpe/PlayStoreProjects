package records;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import records.JournalEntryAdapter.OnJournalEntryClickListener;
import ui.HomeFragment;

public class JournalEntriesFragment extends Fragment implements
        OnJournalEntryClickListener, // For clicking on entries in the list
        EditJournalEntryDialogFragment.OnJournalEntryModifiedListener { // For receiving updates from the edit dialog

    private RecyclerView recyclerView;
    private JournalEntryAdapter adapter;
    private List<HomeFragment.JournalEntry> journalEntries;
    private TextView emptyStateTextView;
    private Gson gson;

    private MainActivity mainActivity;

    private static final String PREFS_JOURNAL = "journal_prefs";
    private static final String KEY_JOURNAL_ENTRIES = "journal_entries";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gson = new Gson();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: JournalEntriesFragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal_entries, container, false);

        recyclerView = view.findViewById(R.id.journal_entries_recycler_view);
        emptyStateTextView = view.findViewById(R.id.empty_journal_state_text_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadJournalEntries();

        adapter = new JournalEntryAdapter(journalEntries, this);
        recyclerView.setAdapter(adapter);

        updateEmptyState();

        return view;
    }

    private void loadJournalEntries() {
        if (getContext() == null) {
            journalEntries = new ArrayList<>();
            return;
        }
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_JOURNAL, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JOURNAL_ENTRIES, null);
        if (json != null) {
            Type type = new TypeToken<List<HomeFragment.JournalEntry>>() {}.getType();
            journalEntries = gson.fromJson(json, type);
            if (journalEntries == null) {
                journalEntries = new ArrayList<>();
            }
        } else {
            journalEntries = new ArrayList<>();
        }
        // Sort entries by timestamp in descending order (newest first)
        Collections.sort(journalEntries, (e1, e2) -> Long.compare(e2.getCreationTimestampMillis(), e1.getCreationTimestampMillis()));
    }

    private void saveJournalEntries() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_JOURNAL, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(journalEntries);
        editor.putString(KEY_JOURNAL_ENTRIES, json);
        editor.apply();
    }

    private void updateEmptyState() {
        if (journalEntries.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            emptyStateTextView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onJournalEntryClick(HomeFragment.JournalEntry entry) {
        // Launch the new EditJournalEntryDialogFragment
        EditJournalEntryDialogFragment dialogFragment = EditJournalEntryDialogFragment.newInstance(entry);
        dialogFragment.setTargetFragment(this, 0); // Set target fragment to receive results
        dialogFragment.show(getParentFragmentManager(), "EditJournalEntryDialog");
    }

    // --- Implementation of OnJournalEntryModifiedListener ---
    @Override
    public void onJournalEntrySaved(HomeFragment.JournalEntry updatedEntry) {
        // Find the entry and update it
        boolean found = false;
        for (int i = 0; i < journalEntries.size(); i++) {
            // Assuming timestamp + creationTimestampMillis is a unique identifier
            if (journalEntries.get(i).getTimestamp().equals(updatedEntry.getTimestamp()) &&
                    journalEntries.get(i).getCreationTimestampMillis() == updatedEntry.getCreationTimestampMillis()) {
                journalEntries.set(i, updatedEntry);
                found = true;
                break;
            }
        }
        if (found) {
            saveJournalEntries();
            loadJournalEntries(); // Reload and re-sort to ensure correct order
            adapter.updateData(journalEntries);
            Toast.makeText(getContext(), "Journal entry updated!", Toast.LENGTH_SHORT).show();
        } else {
            // This case should ideally not happen if the entry was clicked from the list
            // but could be handled (e.g., add as new, or show error)
            Toast.makeText(getContext(), "Error: Could not find entry to update.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onJournalEntryDeleted(String entryTimestamp) {
        // Find and remove the entry
        HomeFragment.JournalEntry entryToRemove = null;
        for (HomeFragment.JournalEntry entry : journalEntries) {
            if (entry.getTimestamp().equals(entryTimestamp)) { // Using timestamp as ID
                entryToRemove = entry;
                break;
            }
        }
        if (entryToRemove != null) {
            journalEntries.remove(entryToRemove);
            saveJournalEntries();
            loadJournalEntries(); // Reload and re-sort
            adapter.updateData(journalEntries);
            updateEmptyState();
            Toast.makeText(getContext(), "Journal entry deleted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error: Could not find entry to delete.", Toast.LENGTH_SHORT).show();
        }
    }
    // --- End of OnJournalEntryModifiedListener implementation ---

    @Override
    public void onResume() {
        mainActivity.toolbar.setTitle("My Journal Entries");
        super.onResume();
        loadJournalEntries(); // Ensure data is fresh when returning to the fragment
        if (adapter != null) {
            adapter.updateData(journalEntries);
        }
        updateEmptyState();
    }
}
