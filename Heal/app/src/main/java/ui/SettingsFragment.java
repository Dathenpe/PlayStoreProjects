package ui;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    // UI elements for theme selection
    private LinearLayout themeColorContainer;
    private ImageView selectedThemeCircle = null;

    // UI elements for custom reminders
    private Button addReminderTimeButton;
    private ChipGroup customTimesChipGroup;
    private TextView customTimesInfoText;

    private final Map<String, Integer> themeColors = new HashMap<>();
    private static final String PREF_SELECTED_THEME_COLOR = "selected_theme_color";

    private TextView emptyStateText;
    private LinearLayout chipAndButtonContainer;

    // New constant for the maximum number of custom reminders
    private static final int MAX_CUSTOM_REMINDERS = 4;

    private CoordinatorLayout mainLayout;

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
        themeColorContainer = settingsRootView.findViewById(R.id.theme_color_container);
        //feedbackLayout = settingsRootView.findViewById(R.id.feedback_layout);

        // Initialize custom reminder views
        addReminderTimeButton = settingsRootView.findViewById(R.id.add_reminder_time_button);
        customTimesChipGroup = settingsRootView.findViewById(R.id.custom_times_chip_group);
        customTimesInfoText = settingsRootView.findViewById(R.id.custom_times_info_text);
        emptyStateText = settingsRootView.findViewById(R.id.empty_state_text);
        chipAndButtonContainer = settingsRootView.findViewById(R.id.chip_and_button_container);

        themeColors.put("md_theme_primary", R.color.md_theme_primary);
        themeColors.put("pink", R.color.pink);
        themeColors.put("blue", R.color.blue);
        themeColors.put("green", R.color.green);
        themeColors.put("purple", R.color.purple);
        themeColors.put("orange", R.color.orange);
        themeColors.put("teal", R.color.teal);
        themeColors.put("brown", R.color.brown);

        return settingsRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        editNameLayout.setOnClickListener(v -> showEditNameDialog());
        addReminderTimeButton.setOnClickListener(v -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            Set<String> customTimes = new HashSet<>(prefs.getStringSet(MainActivity.PREF_CUSTOM_REMINDER_TIMES, new HashSet<>()));

            if (customTimes.size() >= MAX_CUSTOM_REMINDERS) {
                Toast.makeText(getContext(), "You have reached the maximum of " + MAX_CUSTOM_REMINDERS + " custom reminders.", Toast.LENGTH_LONG).show();
            } else {
                showTimePickerDialog();
            }
        });
        initializeUi();
    }

    private void showEditNameDialog() {
        mainActivity.closeSettings();
        String currentName = getNameFromLocalStorage();

        CustomInputDialogFragment dialog = CustomInputDialogFragment.newInstance(
                "Edit Name",
                "Please enter your new name.",
                currentName,
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
                } else {
                    Toast.makeText(getContext(), "Name cannot be empty, changes not saved", Toast.LENGTH_SHORT).show();
                }
                new Handler().postDelayed(() -> mainActivity.loadBottomSettingsFragment(), 100);
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                new Handler().postDelayed(() -> mainActivity.loadBottomSettingsFragment(), 100);
            }
        });
        dialog.setCancelable(false);
        dialog.show(getParentFragmentManager(), "CustomInputDialogFragment");
    }

    private String getNameFromLocalStorage() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString("user_name", "Your Name");
    }

    private void initializeUi() {
        setupNameText();
        setupReminderSwitch();
        setupCustomReminders();
        setupThemeColorSelection();
        setupInfoLayouts();
    }

    private void setupNameText() {
        String userName = getNameFromLocalStorage();
        nameText.setText(userName);
    }

    private void setupReminderSwitch() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        switchReminder.setChecked(sharedPreferences.getBoolean("reminder_enabled", false));

        if (!isSwitchInitialized) {
            switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedPreferences.edit().putBoolean("reminder_enabled", isChecked).apply();
                if (mainActivity != null) {
                    if (isChecked) {
                        Toast.makeText(getContext(), "Reminders enabled", Toast.LENGTH_SHORT).show();
                        mainActivity.onReminderSettingChanged(true);
                    } else {
                        Toast.makeText(getContext(), "Reminders disabled", Toast.LENGTH_SHORT).show();
                        mainActivity.onReminderSettingChanged(false);
                    }
                }
            });
            isSwitchInitialized = true;
        }
    }

    private void setupInfoLayouts() {
        if (feedbackLayout != null) {
            feedbackLayout.setOnClickListener(v -> Toast.makeText(getContext(), "Feedback feature is not yet implemented.", Toast.LENGTH_SHORT).show());
        }
        if (aboutUsLayout != null) {
            aboutUsLayout.setOnClickListener(v -> Toast.makeText(getContext(), "About Us feature is not yet implemented.", Toast.LENGTH_SHORT).show());
        }
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minuteOfHour) -> {
            String time = String.format(Locale.US, "%02d:%02d", hourOfDay, minuteOfHour);
            addCustomTime(time);
        }, hour, minute, true);
        timePickerDialog.show();
    }

    private void addCustomTime(String time) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Set<String> customTimes = new HashSet<>(prefs.getStringSet(MainActivity.PREF_CUSTOM_REMINDER_TIMES, new HashSet<>()));

        if (customTimes.add(time)) {
            prefs.edit().putStringSet(MainActivity.PREF_CUSTOM_REMINDER_TIMES, customTimes).apply();
            Toast.makeText(getContext(), "Reminder time added: " + time, Toast.LENGTH_SHORT).show();
            if (mainActivity != null && switchReminder.isChecked()) {
                MainActivity.scheduleReminders(mainActivity);
            }
            updateCustomTimesChips();
        } else {
            Toast.makeText(getContext(), "This time is already added.", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeCustomTime(String time) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Set<String> customTimes = new HashSet<>(prefs.getStringSet(MainActivity.PREF_CUSTOM_REMINDER_TIMES, new HashSet<>()));
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Delete Custom Reminder Time",
                "Are you sure you want to delete this custom reminder time?",
                "Delete",
                "Cancel"
        );
        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                if (customTimes.remove(time)) {
                    prefs.edit().putStringSet(MainActivity.PREF_CUSTOM_REMINDER_TIMES, customTimes).apply();
                    Toast.makeText(getContext(), "Reminder time removed: " + time, Toast.LENGTH_SHORT).show();
                    if (mainActivity != null && switchReminder.isChecked()) {
                        MainActivity.scheduleReminders(mainActivity);
                    }
                    updateCustomTimesChips();
                }
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                // Do nothing, dialog will be dismissed
            }
        });
        dialog.show(getParentFragmentManager(), "CustomReminderDeletionConfirmationDialog");
    }

    private void replaceCustomTime(String oldTime, int newHour, int newMinute) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Set<String> customTimes = new HashSet<>(prefs.getStringSet(MainActivity.PREF_CUSTOM_REMINDER_TIMES, new HashSet<>()));

        // Remove the old time string
        customTimes.remove(oldTime);

        // Create the new time string
        String newTime = String.format(Locale.US, "%02d:%02d", newHour, newMinute);

        if (customTimes.add(newTime)) {
            prefs.edit().putStringSet(MainActivity.PREF_CUSTOM_REMINDER_TIMES, customTimes).apply();
            Toast.makeText(getContext(), "Reminder time updated to: " + newTime, Toast.LENGTH_SHORT).show();
            if (mainActivity != null && switchReminder.isChecked()) {
                MainActivity.scheduleReminders(mainActivity);
            }
            updateCustomTimesChips();
        } else {
            // This case handles if the user tries to edit to an already existing time
            prefs.edit().putStringSet(MainActivity.PREF_CUSTOM_REMINDER_TIMES, customTimes).apply(); // Re-add the old time
            Toast.makeText(getContext(), "This time is already added.", Toast.LENGTH_SHORT).show();
            updateCustomTimesChips();
        }
    }

    private void setupCustomReminders() {
        updateCustomTimesChips();
    }

    private void updateCustomTimesChips() {
        if (customTimesChipGroup == null) return;
        customTimesChipGroup.removeAllViews();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Set<String> customTimes = prefs.getStringSet(MainActivity.PREF_CUSTOM_REMINDER_TIMES, null);

        if (customTimes != null && !customTimes.isEmpty()) {
            customTimesInfoText.setVisibility(View.VISIBLE);
            List<String> sortedTimes = new ArrayList<>(customTimes);
            Collections.sort(sortedTimes);

            for (String time : sortedTimes) {
                Chip chip = new Chip(getContext());
                chip.setText(time);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> removeCustomTime(time));

                // Add the new OnClickListener to allow editing
                chip.setOnClickListener(v -> {
                    String[] timeParts = time.split(":");
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, newHour, newMinute) -> {
                        replaceCustomTime(time, newHour, newMinute);
                    }, hour, minute, true);
                    timePickerDialog.show();
                });

                customTimesChipGroup.addView(chip);
            }
        } else {
            customTimesInfoText.setVisibility(View.GONE);
        }
        updateCustomRemindersUi();
    }

    private void setupThemeColorSelection() {
        if (themeColorContainer == null) {
            return;
        }
        themeColorContainer.removeAllViews();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String savedThemeColorName = sharedPreferences.getString(PREF_SELECTED_THEME_COLOR, "orange");

        for (Map.Entry<String, Integer> entry : themeColors.entrySet()) {
            String colorName = entry.getKey();
            if (colorName.equals("md_theme_primary")) {
                continue;
            }

            int colorResId = entry.getValue();
            int colorValue = ContextCompat.getColor(getContext(), colorResId);

            ImageView colorCircle = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.theme_circle_size),
                    (int) getResources().getDimension(R.dimen.theme_circle_size)
            );
            params.setMargins(0, 0, (int) getResources().getDimension(R.dimen.theme_circle_margin), 0);
            colorCircle.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(colorValue);
            drawable.setStroke(
                    (int) getResources().getDimension(R.dimen.theme_circle_border_width_normal),
                    Color.BLACK
            );
            colorCircle.setBackground(drawable);
            colorCircle.setTag(colorName);

            if (colorName.equals(savedThemeColorName)) {
                highlightThemeCircle(colorCircle);
                selectedThemeCircle = colorCircle;
            }

            colorCircle.setOnClickListener(v -> {
                String selectedName = (String) v.getTag();

                CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                        "Change Theme",
                        "Are you sure you want to change the theme? The app will restart to apply changes.",
                        "Change",
                        "Cancel"
                );
                dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                    @Override
                    public void onDialogPositiveClick(DialogFragment dialogFragment) {
                        applyThemeColor(selectedName);
                        Toast.makeText(getContext(), "Theme changed. Restarting app...", Toast.LENGTH_LONG).show();
                        if (mainActivity != null) {
                            mainActivity.recreate();
                        }
                        dialogFragment.dismiss();
                    }

                    @Override
                    public void onDialogNegativeClick(DialogFragment dialogFragment) {
                        dialogFragment.dismiss();
                    }
                });
                dialog.show(getParentFragmentManager(), "ThemeChangeConfirmDialog");
            });
            themeColorContainer.addView(colorCircle);
        }
    }

    private void applyThemeColor(String colorName) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPreferences.edit().putString(PREF_SELECTED_THEME_COLOR, colorName).apply();

        if (selectedThemeCircle != null) {
            unhighlightThemeCircle(selectedThemeCircle);
        }

        for (int i = 0; i < themeColorContainer.getChildCount(); i++) {
            View child = themeColorContainer.getChildAt(i);
            if (child instanceof ImageView && child.getTag() != null && child.getTag().equals(colorName)) {
                selectedThemeCircle = (ImageView) child;
                highlightThemeCircle(selectedThemeCircle);
                break;
            }
        }

        Integer drawableResId = mainActivity.themeBackgrounds.get(colorName);
        if (drawableResId != null && mainLayout != null) {
            mainLayout.setBackgroundResource(drawableResId);
        }
    }

    private void highlightThemeCircle(ImageView circle) {
        GradientDrawable drawable = (GradientDrawable) circle.getBackground();
        if (drawable != null) {
            drawable.setStroke(
                    (int) getResources().getDimension(R.dimen.theme_circle_border_width_selected),
                    ContextCompat.getColor(getContext(), R.color.black)
            );
        }
    }

    private void unhighlightThemeCircle(ImageView circle) {
        GradientDrawable drawable = (GradientDrawable) circle.getBackground();
        if (drawable != null) {
            drawable.setStroke(
                    (int) getResources().getDimension(R.dimen.theme_circle_border_width_normal),
                    Color.BLACK
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null && mainActivity.Fab != null) {
            mainActivity.Fab.setVisibility(View.GONE);
        }
        initializeUi();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        settingsRootView = null;
        editNameLayout = null;
        switchReminder = null;
        feedbackLayout = null;
        aboutUsLayout = null;
        nameText = null;
        themeColorContainer = null;
        selectedThemeCircle = null;
        addReminderTimeButton = null;
        customTimesChipGroup = null;
        customTimesInfoText = null;
    }
    private void updateCustomRemindersUi() {
        if (customTimesChipGroup.getChildCount() > 0) {
            emptyStateText.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                emptyStateText.setVisibility(View.GONE);
                addReminderTimeButton.animate()
                        .translationX(0)
                        .setDuration(500)
                        .start();
            }).start();
            customTimesChipGroup.setVisibility(View.VISIBLE);
        } else {
            customTimesChipGroup.setVisibility(View.GONE);
            emptyStateText.animate().alpha(1f).setDuration(300).withStartAction(() -> {
                emptyStateText.setVisibility(View.VISIBLE);
                addReminderTimeButton.animate()
                        .translationX(75)
                        .setDuration(500)
                        .start();
            }).start();
        }
    }
}