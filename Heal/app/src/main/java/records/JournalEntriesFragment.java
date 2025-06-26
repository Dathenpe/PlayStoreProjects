package records;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heal.MainActivity;
import com.example.heal.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import records.JournalEntryAdapter.OnJournalEntryClickListener;
import ui.HomeFragment;

public class JournalEntriesFragment extends Fragment implements OnJournalEntryClickListener {

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
            Toast.makeText(context, "Error: CopingExercisesFragment attached to wrong activity", Toast.LENGTH_SHORT).show();
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
        showEditJournalEntryDialog(entry);
    }

    private void showEditJournalEntryDialog(HomeFragment.JournalEntry entryToEdit) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Journal Entry");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        TextView timestampTv = new TextView(getContext());
        timestampTv.setText(entryToEdit.getTimestamp()); // Use getFormattedTimestamp()
        timestampTv.setTextSize(14f);
        timestampTv.setTextColor(getResources().getColor(R.color.text_color_secondary));
        timestampTv.setPadding(0,0,0,16);
        layout.addView(timestampTv);

        final EditText journalEditText = new EditText(getContext());
        journalEditText.setText(entryToEdit.getText());
        journalEditText.setHint("Write your journal entry here...");
        journalEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        journalEditText.setLines(1);
        journalEditText.setMaxLines(12);
        journalEditText.setGravity(Gravity.TOP | Gravity.START); // Corrected line!
        journalEditText.setPadding(20, 20, 20, 20);
        journalEditText.setTextColor(getResources().getColor(R.color.text_color_primary));
        journalEditText.setTextSize(16f);

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.addView(journalEditText);
        layout.addView(scrollView);


        builder.setView(layout);

        // Save Button
        builder.setPositiveButton("Save", (dialog, which) -> {
            String updatedText = journalEditText.getText().toString().trim();
            if (!updatedText.isEmpty()) {
                // Find the existing entry and update it
                for (int i = 0; i < journalEntries.size(); i++) {
                    if (journalEntries.get(i).getTimestamp().equals(entryToEdit.getTimestamp()) &&
                            journalEntries.get(i).getText().equals(entryToEdit.getText())) { // Basic check, ideally use a unique ID
                        journalEntries.set(i, new HomeFragment.JournalEntry(
                                entryToEdit.getTimestamp(),    // Original formatted timestamp string
                                updatedText,                            // New journal text
                                entryToEdit.getCreationTimestampMillis() // Original raw long timestamp
                        ));
                        break;
                    }
                }
                saveJournalEntries();
                loadJournalEntries();
                adapter.updateData(journalEntries);
                Toast.makeText(getContext(), "Journal entry updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Journal entry cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Delete", (dialog, which) -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Entry")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage("Are you sure you want to delete this journal entry?, this action cannot be undone.")
                    .setPositiveButton("Yes", (dialogDelete, whichDelete) -> {
                        journalEntries.remove(entryToEdit);
                        saveJournalEntries();
                        loadJournalEntries();
                        adapter.updateData(journalEntries);
                        updateEmptyState();
                        Toast.makeText(getContext(), "Journal entry deleted.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onResume() {
        mainActivity.toolbar.setTitle("My Journal Entries");
        super.onResume();
        loadJournalEntries();
        if (adapter != null) {
            adapter.updateData(journalEntries);
        }
        updateEmptyState();
    }
}