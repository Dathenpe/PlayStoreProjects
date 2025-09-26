package records;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Added for logging
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

import com.f9ld3.heal.MainActivity; // Assuming this is your main activity
import com.f9ld3.heal.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import records.AudioLogAdapter.OnLogActionListener;
import ui.AudioRecordingDialogFragment;
import ui.CustomMessageDialogFragment;

public class AudioLogsFragment extends Fragment implements
        AudioRecordingDialogFragment.OnAudioLogSavedListener, // Ensure this matches the updated interface
        OnLogActionListener {

    private RecyclerView recyclerView;
    private AudioLogAdapter adapter;
    private List<AudioLog> audioLogs = new ArrayList<>();
    private TextView emptyStateTextView;
    private ExtendedFloatingActionButton recordAudioButton;
    private MainActivity mainActivity; // Assuming MainActivity from your context
    private Gson gson;

    private static final String PREFS_AUDIO_LOGS = "audio_logs_prefs";
    private static final String KEY_AUDIO_LOGS = "saved_audio_logs";
    private static final String TAG = "AudioLogsFragment"; // For logging

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Log.w(TAG, "onAttach: Context is not MainActivity instance");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gson = new Gson(); // Initialize Gson
        loadAudioLogs();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_logs, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewAudioLogs);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
        recordAudioButton = view.findViewById(R.id.recordAudioButton);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Pass 'this' as the activity context for the adapter if needed, or getContext()
        adapter = new AudioLogAdapter(audioLogs, this, requireActivity());
        recyclerView.setAdapter(adapter);

        recordAudioButton.setOnClickListener(v -> showRecordDialog());

        updateEmptyState();
    }

    private void showRecordDialog() {
        Log.d(TAG, "showRecordDialog called.");
        // Stop any currently playing audio log BEFORE showing the dialog
        AudioLogAdapter.releaseGlobalPlayer();
        Log.d(TAG, "Global player released.");

        AudioRecordingDialogFragment dialog = new AudioRecordingDialogFragment();
        dialog.setOnAudioLogSavedListener(this);

        if (isAdded() && getParentFragmentManager() != null) {
            dialog.show(getParentFragmentManager(), AudioRecordingDialogFragment.TAG);
            Log.d(TAG, "AudioRecordingDialogFragment shown.");
        } else {
            Log.e(TAG, "showRecordDialog: Fragment not attached or FragmentManager is null.");
            if(getContext() != null) {
                Toast.makeText(getContext(), "Cannot open recorder now. Try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadAudioLogs() {
        if (getContext() == null) {
            Log.e(TAG, "loadAudioLogs: Context is null, cannot load.");
            return;
        }
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_AUDIO_LOGS, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_AUDIO_LOGS, null);
        Log.d(TAG, "loadAudioLogs: Loaded JSON: " + (json != null ? json.substring(0, Math.min(json.length(), 100)) + "..." : "null"));


        Type type = new TypeToken<ArrayList<AudioLog>>() {}.getType();
        if (json != null) {
            try {
                audioLogs = gson.fromJson(json, type);
                if (audioLogs == null) audioLogs = new ArrayList<>(); // handle null fromJson result
            } catch (Exception e) { // Catch broader exceptions during parsing
                Log.e(TAG, "loadAudioLogs: Error parsing JSON from SharedPreferences", e);
                audioLogs = new ArrayList<>(); // Reset to empty list on error
            }
        } else {
            audioLogs = new ArrayList<>();
        }
        Log.d(TAG, "loadAudioLogs: Parsed " + audioLogs.size() + " logs.");


        // Sort by timestamp (assuming descending order is desired - newest first)
        // Ensure timestamp is not null for comparison
        Collections.sort(audioLogs, (log1, log2) -> {
            if (log1.getTimestamp() == null && log2.getTimestamp() == null) return 0;
            if (log1.getTimestamp() == null) return 1; // nulls last
            if (log2.getTimestamp() == null) return -1; // nulls last
            return log2.getTimestamp().compareTo(log1.getTimestamp());
        });
    }

    private void saveAudioLogs() {
        if (getContext() == null) {
            Log.e(TAG, "saveAudioLogs: Context is null, cannot save.");
            return;
        }
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_AUDIO_LOGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(audioLogs);
        Log.d(TAG, "saveAudioLogs: Saving JSON: " + json.substring(0, Math.min(json.length(),100)) + "...");
        editor.putString(KEY_AUDIO_LOGS, json);
        editor.apply();
        Log.d(TAG, "saveAudioLogs: Saved " + audioLogs.size() + " logs.");
    }

    private void updateEmptyState() {
        if (emptyStateTextView == null || recyclerView == null) return;
        if (audioLogs.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "updateEmptyState: isEmpty = " + audioLogs.isEmpty());
    }

    @Override
    public void onAudioLogSaved(String title, String timestamp, String filePath, long durationMillis) {
        Log.d(TAG, "onAudioLogSaved: Received title=" + title + ", path=" + filePath + ", duration=" + durationMillis);
        AudioLog newLog = new AudioLog(title, timestamp, filePath, durationMillis);
        audioLogs.add(0, newLog);
        saveAudioLogs();
        if (adapter != null) {
            adapter.updateData(audioLogs); // Let adapter handle data updates internally
        }
        updateEmptyState();
        if (recyclerView != null) {
            recyclerView.scrollToPosition(0);
        }
    }

    @Override
    public void onDeleteLog(final AudioLog logToDelete) {
        Log.d(TAG, "onDeleteLog: Attempting to delete log: " + logToDelete.getTitle());
        if (getContext() == null || getParentFragmentManager() == null) {
            Log.e(TAG, "onDeleteLog: Context or FragmentManager is null.");
            return;
        }

        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Delete Audio Log",
                "Are you sure you want to delete the audio log titled:\n\"" + logToDelete.getTitle() + "\"?",
                "DELETE",
                "Cancel"
        );
        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(androidx.fragment.app.DialogFragment dialogFragment) {
                Log.d(TAG, "onDeleteLog: Positive click. Deleting file and log for: " + logToDelete.getTitle());
                File fileToDelete = new File(logToDelete.getFilePath());
                boolean fileDeleted = false;
                if (fileToDelete.exists()) {
                    if (fileToDelete.delete()) {
                        Log.d(TAG, "onDeleteLog: File deleted successfully: " + logToDelete.getFilePath());
                        fileDeleted = true;
                    } else {
                        Log.w(TAG, "onDeleteLog: Error deleting audio file: " + logToDelete.getFilePath());
                        Toast.makeText(getContext(), "Error: Could not delete audio file.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "onDeleteLog: File was already missing: " + logToDelete.getFilePath());
                    // File is missing, but we should still remove the log entry
                    fileDeleted = true; // Consider it "handled" for metadata removal
                }

                // Remove from list and save, even if file deletion had issues or file was missing,
                // to remove the stale metadata entry.
                boolean removedFromList = audioLogs.remove(logToDelete);
                if (removedFromList) {
                    Log.d(TAG, "onDeleteLog: Removed log from list.");
                } else {
                    Log.w(TAG, "onDeleteLog: Log was not found in the list for removal: " + logToDelete.getTitle());
                }
                saveAudioLogs(); // Save the updated list

                // It's often better to reload and re-sort to ensure consistency,
                // especially if multiple operations could happen.
                // However, for a simple remove, directly updating adapter is also fine if list is managed carefully.
                loadAudioLogs(); // Reloads and re-sorts
                if (adapter != null) {
                    adapter.updateData(new ArrayList<>(audioLogs)); // Update with a new list copy
                }
                updateEmptyState();

                if (fileDeleted && removedFromList) {
                    Toast.makeText(getContext(), "Audio log deleted.", Toast.LENGTH_SHORT).show();
                } else if (removedFromList && !fileToDelete.exists()){
                    Toast.makeText(getContext(), "Audio log metadata deleted (file was missing).", Toast.LENGTH_SHORT).show();
                }
                dialogFragment.dismiss();
            }

            @Override
            public void onDialogNegativeClick(androidx.fragment.app.DialogFragment dialogFragment) {
                Log.d(TAG, "onDeleteLog: Negative click. Delete cancelled for: " + logToDelete.getTitle());
                dialogFragment.dismiss();
            }
        });
        dialog.show(getParentFragmentManager(), "DeleteLogConfirmation");
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (mainActivity != null) { // Check if mainActivity is not null
            mainActivity.toolbar.setTitle("My Audio Logs");
            mainActivity.navigationView.setCheckedItem(R.id.nav_records);
            mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
            mainActivity.Fab.setVisibility(View.VISIBLE);
        }
        loadAudioLogs(); // Load fresh data
        if (adapter != null) {
            adapter.updateData(new ArrayList<>(audioLogs)); // Update adapter with a new list copy
        }
        updateEmptyState();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause. Releasing global player.");
        // It's also good practice to release player onPause if the fragment is not visible
        AudioLogAdapter.releaseGlobalPlayer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView. Releasing global player.");
        AudioLogAdapter.releaseGlobalPlayer(); // Ensure player is released
        recyclerView = null; // Help GC
        adapter = null;      // Help GC
        emptyStateTextView = null;
        recordAudioButton = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null; // Clear reference to activity
    }
}
