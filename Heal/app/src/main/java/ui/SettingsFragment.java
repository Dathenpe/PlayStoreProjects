package ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.heal.MainActivity;
import com.example.heal.R;
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
        return settingsRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editNameLayout = view.findViewById(R.id.edit_name_layout);
        switchReminder = view.findViewById(R.id.switch_reminder);
        aboutUsLayout = view.findViewById(R.id.about_us_layout);
        nameText = view.findViewById(R.id.name_text);

        editNameLayout.setOnClickListener(v -> showNameInputDialog());

        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("reminder_enabled", isChecked);
            editor.apply();

            if (isSwitchInitialized) {
                if (isChecked) {
                    Toast.makeText(getContext(), "Reminder notifications enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Reminder notifications disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //  feedbackLayout.setOnClickListener(v -> sendFeedback());
        aboutUsLayout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "About Us clicked", Toast.LENGTH_SHORT).show();
        });
        nameText.setText(getNameFromLocalStorage());
        initializeUi();
        isSwitchInitialized = true;
    }


    private void showNameInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Name");
        final EditText input = new EditText(getContext());
        input.setText(nameText.getText());
        new Handler().postDelayed(input::requestFocus, 300);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName != null && !newName.isEmpty()) {
                mainActivity.saveNameToLocalStorage(newName);
                nameText.setText(newName); // Update the name text directly in SettingsFragment
                Toast.makeText(getContext(), "Name saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Name cannot be empty, changes not saved", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel(); // Dismiss the AlertDialog
        });

        AlertDialog dialog = builder.create();

        // Set the windowSoftInputMode on the created dialog's window
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // Show the dialog instance you just configured
        dialog.show(); // <--- CHANGE THIS LINE
    }

    private String getNameFromLocalStorage() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString("user_name", "Your Name");
    }

    private void initializeUi() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        switchReminder.setChecked(sharedPreferences.getBoolean("reminder_enabled", false));

        String userName = getNameFromLocalStorage();
        nameText.setText(userName);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        editNameLayout = null;
        switchReminder = null;
        feedbackLayout = null;
        aboutUsLayout = null;
        nameText = null;
    }
}
