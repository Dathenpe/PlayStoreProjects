package ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.heal.R;

public class RecordFragment extends Fragment {

    private FrameLayout copingExercisesContainer;
    private FrameLayout dailyMoodCheckingsContainer;
    private FrameLayout savedStrategiesContainer;
    private FrameLayout journalEntriesContainer;
    private FrameLayout emergencyContactsContainer;

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

        copingExercisesContainer = view.findViewById(R.id.coping_exercises_container);
        dailyMoodCheckingsContainer = view.findViewById(R.id.daily_mood_checkings_container);
        savedStrategiesContainer = view.findViewById(R.id.saved_strategies_container);
        journalEntriesContainer = view.findViewById(R.id.journal_entries_container);
        emergencyContactsContainer = view.findViewById(R.id.emergency_contacts_container);

        copingExercisesContainer.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Coping Exercises Clicked", Toast.LENGTH_SHORT).show();
            // Navigate to Coping Exercises screen/fragment
            NavHostFragment.findNavController(RecordFragment.this)
                    .navigate(R.id.action_recordFragment_to_copingExercisesFragment);
        });

        dailyMoodCheckingsContainer.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Daily Mood Checkings Clicked", Toast.LENGTH_SHORT).show();
            // Navigate to Daily Mood Checkings screen/fragment
             NavHostFragment.findNavController(RecordFragment.this)
                   .navigate(R.id.action_recordFragment_to_moodCheckinFragment);
        });

        savedStrategiesContainer.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Saved Strategies Clicked", Toast.LENGTH_SHORT).show();
            // Navigate to Saved Strategies screen/fragment
            NavHostFragment.findNavController(RecordFragment.this)
                    .navigate(R.id.action_recordFragment_to_savedStrategiesFragment);
        });

        journalEntriesContainer.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Journal Entries Clicked", Toast.LENGTH_SHORT).show();
            // Navigate to Journal Entries screen/fragment
             NavHostFragment.findNavController(RecordFragment.this)
                    .navigate(R.id.action_recordFragment_to_journalEntriesFragment);
        });

        emergencyContactsContainer.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Emergency Contacts Clicked", Toast.LENGTH_SHORT).show();
            // Navigate to Emergency Contacts screen/fragment
             NavHostFragment.findNavController(RecordFragment.this)
                   .navigate(R.id.action_recordFragment_to_emergencyContactsFragment);
        });
    }
}