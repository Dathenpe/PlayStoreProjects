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
        AudioRecordingDialogFragment.OnAudioLogSavedListener,
        OnLogActionListener {

    private RecyclerView recyclerView;
    private AudioLogAdapter adapter;
    private List<AudioLog> audioLogs = new ArrayList<>();
    private TextView emptyStateTextView;
    private ExtendedFloatingActionButton recordAudioButton;
    private MainActivity mainActivity;
    private Gson gson;

    private static final String PREFS_AUDIO_LOGS = "audio_logs_prefs";
    private static final String KEY_AUDIO_LOGS = "saved_audio_logs";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gson = new Gson();
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
        adapter = new AudioLogAdapter(audioLogs, this);
        recyclerView.setAdapter(adapter);

        recordAudioButton.setOnClickListener(v -> showRecordDialog());

        updateEmptyState();
    }

    private void showRecordDialog() {
        AudioRecordingDialogFragment dialog = new AudioRecordingDialogFragment();
        dialog.setOnAudioLogSavedListener(this);
        dialog.show(getParentFragmentManager(), AudioRecordingDialogFragment.TAG);
    }

    private void loadAudioLogs() {
        SharedPreferences sharedPreferences = mainActivity.getSharedPreferences(PREFS_AUDIO_LOGS, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_AUDIO_LOGS, null);

        Type type = new TypeToken<ArrayList<AudioLog>>() {}.getType();
        audioLogs = json == null ? new ArrayList<>() : gson.fromJson(json, type);

        // Sort by timestamp (assuming descending order is desired - newest first)
        Collections.sort(audioLogs, (log1, log2) -> log2.getTimestamp().compareTo(log1.getTimestamp()));
    }

    private void saveAudioLogs() {
        SharedPreferences sharedPreferences = mainActivity.getSharedPreferences(PREFS_AUDIO_LOGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(audioLogs);
        editor.putString(KEY_AUDIO_LOGS, json);
        editor.apply();
    }

    private void updateEmptyState() {
        if (audioLogs.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAudioLogSaved(String title, String timestamp, String filePath) {
        AudioLog newLog = new AudioLog(title, timestamp, filePath);
        audioLogs.add(0, newLog); // Add to the start
        saveAudioLogs();
        adapter.updateData(audioLogs);
        updateEmptyState();
        recyclerView.scrollToPosition(0);
    }

    @Override
    public void onDeleteLog(AudioLog log) {
        // Use the custom dialog for confirmation
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Delete Audio Log",
                "Are you sure you want to delete the audio log titled:\n\"" + log.getTitle() + "\"?",
                "DELETE",
                "Cancel"
        );
        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(androidx.fragment.app.DialogFragment dialogFragment) {
                // Delete the file from internal storage
                File fileToDelete = new File(log.getFilePath());
                if (fileToDelete.exists()) {
                    if (fileToDelete.delete()) {
                        audioLogs.remove(log);
                        saveAudioLogs();
                        loadAudioLogs(); // Reload and re-sort
                        adapter.updateData(audioLogs);
                        updateEmptyState();
                        Toast.makeText(getContext(), "Audio log deleted.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Error: Could not delete audio file.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Log metadata exists, but file is gone. Remove metadata anyway.
                    audioLogs.remove(log);
                    saveAudioLogs();
                    loadAudioLogs();
                    adapter.updateData(audioLogs);
                    updateEmptyState();
                    Toast.makeText(getContext(), "Audio log metadata deleted (file already missing).", Toast.LENGTH_SHORT).show();
                }
                dialogFragment.dismiss();
            }

            @Override
            public void onDialogNegativeClick(androidx.fragment.app.DialogFragment dialogFragment) {
                dialogFragment.dismiss();
            }
        });
        dialog.show(getParentFragmentManager(), "DeleteLogConfirmation");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("My Audio Logs");
            mainActivity.navigationView.setCheckedItem(R.id.nav_records);
            mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
            mainActivity.Fab.setVisibility(View.VISIBLE);
        }
        loadAudioLogs();
        if (adapter != null) {
            adapter.updateData(audioLogs);
        }
        updateEmptyState();
    }
}