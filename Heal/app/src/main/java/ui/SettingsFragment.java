package ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    private View settingsRootView;
    private LinearLayout editNameLayout;
    private MaterialSwitch switchReminder;
    private LinearLayout feedbackLayout;
    private LinearLayout aboutUsLayout;
    private TextView nameText;
    private boolean isSwitchInitialized = false;
    private MainActivity mainActivity;
    private Context context;

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


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        settingsRootView = inflater.inflate(R.layout.fragment_settings, container, false);
        editNameLayout = settingsRootView.findViewById(R.id.edit_name_layout);
        nameText = settingsRootView.findViewById(R.id.name_text);
        switchReminder = settingsRootView.findViewById(R.id.switch_reminder);
        aboutUsLayout = settingsRootView.findViewById(R.id.about_us_layout);
        return settingsRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        editNameLayout.setOnClickListener(v -> showEditNameDialog());
        // Initialize UI elements when the view is created
        initializeUi();
    }

    private void showEditNameDialog() {
        // Get the current name from local storage to pre-fill the dialog
        mainActivity.closeSettings();
        String currentName = getNameFromLocalStorage();

        CustomInputDialogFragment dialog = CustomInputDialogFragment.newInstance(
                "Edit Name",
                "Please enter your new name.",
                currentName, // Pass the current name as the hint/pre-filled text
                "Save",
                "Cancel"
        );

        dialog.setListener(new CustomInputDialogFragment.OnInputDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment, String inputText) {
                if (inputText != null && !inputText.isEmpty()) {
                    mainActivity.saveNameToLocalStorage(inputText);
                    nameText.setText(inputText);
                    Toast.makeText(getContext(), "Name saved successfully", Toast.LENGTH_SHORT).show();
                    // Reload the settings fragment to ensure UI consistency if needed
                     new Handler().postDelayed(() -> mainActivity.loadBottomSettingsFragment(), 100);
                } else {
                    Toast.makeText(getContext(), "Name cannot be empty, changes not saved", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                new Handler().postDelayed(() -> mainActivity.loadBottomSettingsFragment(), 100);
            }
        });

        dialog.show(getParentFragmentManager(), "CustomInputDialogFragment");
    }

    private String getNameFromLocalStorage() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString("user_name", "Your Name"); // Default value if not found
    }

    private void initializeUi() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        // Set the reminder switch state based on saved preference
        switchReminder.setChecked(sharedPreferences.getBoolean("reminder_enabled", false));

        // Set the name text from local storage
        String userName = getNameFromLocalStorage();
        nameText.setText(userName);

        // Add listener for the reminder switch
        if (!isSwitchInitialized) {
            switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Save the new state of the reminder switch
                sharedPreferences.edit().putBoolean("reminder_enabled", isChecked).apply();
                if (isChecked) {
                    Toast.makeText(getContext(), "Reminders enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Reminders disabled", Toast.LENGTH_SHORT).show();
                }
            });
            isSwitchInitialized = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ensure UI is updated when returning to the fragment
        initializeUi();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify views to prevent memory leaks
        editNameLayout = null;
        switchReminder = null;
        feedbackLayout = null; // Assuming this exists in your layout
        aboutUsLayout = null;
        nameText = null;
    }
}
