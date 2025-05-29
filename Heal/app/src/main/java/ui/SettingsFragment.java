package ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.example.heal.MainActivity;
import com.example.heal.R;

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
                    // Removed scheduleReminderNotification();
                } else {
                    Toast.makeText(getContext(), "Reminder notifications disabled", Toast.LENGTH_SHORT).show();
                    // Removed cancelReminderNotification();
                }
            } else {
                // Removed initial scheduling/canceling
            }
        });
        //  feedbackLayout.setOnClickListener(v -> sendFeedback());
        aboutUsLayout.setOnClickListener(v -> {
            // Implement About Us functionality here (e.g., start a new activity or show a dialog)
            Toast.makeText(getContext(), "About Us clicked", Toast.LENGTH_SHORT).show();
        });
        nameText.setText(getNameFromLocalStorage());
        settingsRootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!isViewInside(v, (int) event.getRawX(), (int) event.getRawY())) {
                    return true;
                }
            }
            return false;
        });
        initializeUi();
        isSwitchInitialized = true;
    }

    private boolean isViewInside(View view, int x, int y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        Rect rect = new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
        return rect.contains(x, y);
    }

    private void showNameInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Name");
        final EditText input = new EditText(getContext());
        input.setText(nameText.getText());
        mainActivity.closeSettings();
        new Handler().postDelayed(input::requestFocus, 300);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName != null && !newName.isEmpty()) {
                Toast.makeText(builder.getContext(), "name saved", Toast.LENGTH_SHORT).show();
                mainActivity.saveNameToLocalStorage(newName);
                mainActivity.loadFragment(new HomeFragment(),R.id.nav_home);
              //  dialog.dismiss();
              try {
                  if (mainActivity != null) {
                      mainActivity.loadBottomSettingsFragment();
                  }
              } catch (Exception e) {
                  Toast.makeText(mainActivity,e + " error", Toast.LENGTH_SHORT).show();
              }
            } else {
                if (mainActivity != null) {
                    mainActivity.loadBottomSettingsFragment();
                }
             new Handler().postDelayed(() -> Toast.makeText(mainActivity, "Name cannot be empty, changes not saved", Toast.LENGTH_SHORT).show(), 100);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (mainActivity != null) {
                mainActivity.loadBottomSettingsFragment();
            }
            dialog.cancel();
        });

        builder.show();
    }

    private String getNameFromLocalStorage() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sharedPreferences.getString("user_name", "Your Name");
    }

//    private void sendFeedback() {
//        Intent intent = new Intent(Intent.ACTION_SEND);
//        intent.setType("message/rfc822");
//        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"feedback@example.com"});
//        intent.putExtra(Intent.EXTRA_SUBJECT, "App Feedback");
//        intent.putExtra(Intent.EXTRA_TEXT, "Enter your feedback here:");
//        try {
//            startActivity(Intent.createChooser(intent, "Send Feedback"));
//        } catch (android.content.ActivityNotFoundException ex) {
//            Toast.makeText(getContext(), "No email client found.", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void initializeUi() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        switchReminder.setChecked(sharedPreferences.getBoolean("reminder_enabled", false));

        String userName = getNameFromLocalStorage();
        nameText.setText(userName);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Saving the reminder state is now handled directly in the OnCheckedChangeListener
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