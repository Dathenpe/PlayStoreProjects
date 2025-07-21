package ui;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private View settingsRootView;
    private LinearLayout editNameLayout;
    private MaterialSwitch switchReminder;
    private LinearLayout feedbackLayout; // This was declared but not used in the original layout, keeping it for consistency if it's meant to be there.
    private LinearLayout aboutUsLayout;
    private TextView nameText;
    private boolean isSwitchInitialized = false;
    private MainActivity mainActivity;
    private Context context;

    // New UI elements for theme selection
    private LinearLayout themeColorContainer;
    private ImageView selectedThemeCircle = null; // To keep track of the currently selected circle

    // Map to store theme color names and their corresponding resource IDs
    private final Map<String, Integer> themeColors = new HashMap<>();
    private static final String PREF_SELECTED_THEME_COLOR = "selected_theme_color";

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
        themeColorContainer = settingsRootView.findViewById(R.id.theme_color_container); // Initialize new container

        // Populate theme colors map
        // Note: md_theme_primary is included here for internal logic/fallback, but will be excluded from UI selection
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
                    if (mainActivity != null) {
                        mainActivity.onReminderSettingChanged(true);
                    }
                } else {
                    Toast.makeText(getContext(), "Reminders disabled", Toast.LENGTH_SHORT).show();
                    if (mainActivity != null) {
                        mainActivity.onReminderSettingChanged(false);
                    }
                }
            });
            isSwitchInitialized = true;
        }

        // Initialize theme color selection UI
        setupThemeColorSelection();
    }

    private void setupThemeColorSelection() {
        if (themeColorContainer == null) {
            return; // Should not happen if onCreateView is successful
        }
        themeColorContainer.removeAllViews(); // Clear any existing views

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        // Set "orange" as the new default theme color for first install
        String savedThemeColorName = sharedPreferences.getString(PREF_SELECTED_THEME_COLOR, "orange");

        for (Map.Entry<String, Integer> entry : themeColors.entrySet()) {
            String colorName = entry.getKey();

            // Skip creating a circle for the default primary theme if it's not meant to be selectable
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

            // Create a circular drawable
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(colorValue);
            drawable.setStroke(
                    (int) getResources().getDimension(R.dimen.theme_circle_border_width_normal),
                    Color.BLACK
            );
            colorCircle.setBackground(drawable);
            colorCircle.setTag(colorName); // Store the color name as a tag

            // Set initial selection
            if (colorName.equals(savedThemeColorName)) {
                highlightThemeCircle(colorCircle);
                selectedThemeCircle = colorCircle;
            }

            colorCircle.setOnClickListener(v -> {
                String selectedName = (String) v.getTag();

                // Show confirmation dialog
                CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                        "Change Theme",
                        "Are you sure you want to change the theme to " + selectedName.replace("md_theme_", "").replace("_", " ") + "? The app will require a restart",
                        "Change",
                        "Cancel"
                );
                dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                    @Override
                    public void onDialogPositiveClick(DialogFragment dialogFragment) {
                        applyThemeColor(selectedName);
                        Toast.makeText(getContext(), "Theme set to " + selectedName.replace("md_theme_", "").replace("_", " ") + ". please restart the app...", Toast.LENGTH_LONG).show();
                        if (mainActivity != null) {
                            mainActivity.recreate(); // Restart the activity to apply the theme
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

        // Update UI to highlight the newly selected circle
        if (selectedThemeCircle != null) {
            unhighlightThemeCircle(selectedThemeCircle);
        }

        // Find the new selected circle by its tag
        for (int i = 0; i < themeColorContainer.getChildCount(); i++) {
            View child = themeColorContainer.getChildAt(i);
            if (child instanceof ImageView && child.getTag() != null && child.getTag().equals(colorName)) {
                selectedThemeCircle = (ImageView) child;
                highlightThemeCircle(selectedThemeCircle);
                break;
            }
        }

        // The app restart is now handled by the confirmation dialog's positive click listener
    }

    private void highlightThemeCircle(ImageView circle) {
        GradientDrawable drawable = (GradientDrawable) circle.getBackground();
        if (drawable != null) {
            drawable.setStroke(
                    (int) getResources().getDimension(R.dimen.theme_circle_border_width_selected),
                    ContextCompat.getColor(getContext(), R.color.black) // Use black for border
            );
        }
    }

    private void unhighlightThemeCircle(ImageView circle) {
        GradientDrawable drawable = (GradientDrawable) circle.getBackground();
        if (drawable != null) {
            drawable.setStroke(
                    (int) getResources().getDimension(R.dimen.theme_circle_border_width_normal),
                    Color.BLACK // Normal border color
            );
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
        feedbackLayout = null;
        aboutUsLayout = null;
        nameText = null;
        themeColorContainer = null;
        selectedThemeCircle = null;
    }
}
