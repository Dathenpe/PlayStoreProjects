package ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;

import records.AudioLogsFragment;
import records.CopingExercisesFragment;
import records.JournalEntriesFragment;
import records.MoodCheckinFragment;
import records.RelapseHistoryFragment;
import records.SavedStrategiesFragment;
import viewmodels.GeneralViewModel;

public class RecordFragment extends Fragment {

    private FrameLayout copingExercisesContainer;
    private FrameLayout dailyMoodCheckingsContainer;
    private FrameLayout savedStrategiesContainer;
    private FrameLayout journalEntriesContainer;
    private FrameLayout emergencyContactsContainer;
    private FrameLayout relapseHistoryContainer;
    private FrameLayout audioLogsContainer;

    private ScrollView recordScrollView;

    private Context context;
    private MainActivity mainActivity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: Fragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity.toolbar.setTitle("Data Records");
       mainActivity.navigationView.setCheckedItem(R.id.nav_records);

        copingExercisesContainer = view.findViewById(R.id.coping_exercises_container);
        dailyMoodCheckingsContainer = view.findViewById(R.id.daily_mood_checkings_container);
        savedStrategiesContainer = view.findViewById(R.id.saved_strategies_container);
        journalEntriesContainer = view.findViewById(R.id.journal_entries_container);
        emergencyContactsContainer = view.findViewById(R.id.emergency_contacts_container);
        relapseHistoryContainer = view.findViewById(R.id.relapse_history_container);
        audioLogsContainer = view.findViewById(R.id.audio_logs_container);
        View loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        recordScrollView = view.findViewById(R.id.record_scroll_view);

        GeneralViewModel viewModel = new ViewModelProvider(this).get(GeneralViewModel.class);

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                recordScrollView.setVisibility(View.GONE); // Hide content while loading
            } else {
                loadingProgressBar.setVisibility(View.GONE);
                recordScrollView.setVisibility(View.VISIBLE); // Show content when loading finishes
            }
        });



        copingExercisesContainer.setOnClickListener(v -> {
            mainActivity.loadFragment(new CopingExercisesFragment(), R.id.nav_coping_exercises);
            mainActivity.toolbar.setTitle("Coping Exercises");
            mainActivity.addFragmentToHistory(R.id.nav_coping_exercises, "Coping Exercises");
        });

        dailyMoodCheckingsContainer.setOnClickListener(v -> {
            mainActivity.loadFragment(new MoodCheckinFragment(), R.id.nav_mood_checkin);
            mainActivity.toolbar.setTitle("My Mood History");
            mainActivity.addFragmentToHistory(R.id.nav_mood_checkin, "My Mood History");
        });

        savedStrategiesContainer.setOnClickListener(v -> {
            mainActivity.loadFragment(new SavedStrategiesFragment(), R.id.nav_saved_strategies);
            mainActivity.toolbar.setTitle("My Coping Strategies");
            mainActivity.addFragmentToHistory(R.id.nav_saved_strategies, "My Coping Strategies");
        });

        journalEntriesContainer.setOnClickListener(v -> {
            mainActivity.loadFragment(new JournalEntriesFragment(), R.id.nav_journal_entries);
            mainActivity.toolbar.setTitle("My Journal Entries");
            mainActivity.addFragmentToHistory(R.id.nav_journal_entries, "My Journal Entries");
        });

        emergencyContactsContainer.setOnClickListener(v -> {
            mainActivity.toolbar.setTitle("My Emergency Contacts");
            mainActivity.loadContacts();
        });
        relapseHistoryContainer.setOnClickListener(v -> {
            mainActivity.loadFragment(new RelapseHistoryFragment(), R.id.nav_relapse_history);
            mainActivity.toolbar.setTitle("My Relapse History");
            mainActivity.addFragmentToHistory(R.id.nav_relapse_history, "My Relapse History");
        });
        audioLogsContainer.setOnClickListener(v -> {
            mainActivity.loadFragment(new AudioLogsFragment(), R.id.nav_audio_logs);
            mainActivity.toolbar.setTitle("My Audio Logs");
            mainActivity.addFragmentToHistory(R.id.nav_audio_logs, "My Audio Logs");
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        mainActivity.toolbar.setTitle("Data Records");
        mainActivity.navigationView.setCheckedItem(R.id.nav_records);
        mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
        mainActivity.Fab.setVisibility(View.VISIBLE);
    }

}