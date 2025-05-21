package com.example.heal; // Replace this!

import static android.app.ProgressDialog.show;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import ui.GalleryFragment;
import ui.HomeFragment;
import ui.RecordFragment;
import ui.ReminderBroadcastReceiver;
import ui.SettingsFragment;
import ui.SlideshowFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private com.google.android.material.floatingactionbutton.FloatingActionButton Fab;
    public Toolbar toolbar;
    public NavigationView navigationView;
    private MenuItem previousMenuItem;
    private View previousItemView;
    private ImageButton MenuTrigger;
    private PopupWindow popupWindow; // Declare PopupWindow as a class member
    // Bottom Sheet related variables
    FrameLayout bottomSheetContent;
    private static final String TAG = "YourActivityTag";
    View bottomSheetView;
    BottomSheetBehavior<View> bottomSheetBehavior;// Use the settings binding!
    // Overlay for graying out
    private View overlayView;
    private View fragmentMain;
    private TextInputEditText RecipientId;
    private TextInputEditText SendMessage;
    private Button sendMessage;
    private Button cancelMessage;
    private HashMap<String, String> sendData = new HashMap<>();
    private EditText recipientEditText; // Replace with your actual ID
    private EditText messageEditText;
    private final String myAppLink = "https://play.google.com/store/apps/details?id=com.example.myapp";
    private final String shareMessage = "Check out this awesome app!";
    private TextView userNameDisplayTextView;
    private static final String CHANNEL_ID = "reminder_channel";
    private static final int REMINDER_NOTIFICATION_ID = 1;
    private static final int REMINDER_REQUEST_CODE = 102; // Different request code for MainActivity
    private static final long REPEAT_INTERVAL = TimeUnit.DAYS.toMillis(1);
    private int currentNavId = R.id.nav_home; // Keep track of the current nav item


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndScheduleReminder();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Load the Home Room when the app starts
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), R.id.nav_home); // Load the loading fragment
            navigationView.setCheckedItem(R.id.nav_home);
            toolbar.setTitle("Home Room");
        } else {
            // Restore the current nav ID
            currentNavId = savedInstanceState.getInt("currentNavId", R.id.nav_home);
            navigationView.setCheckedItem(currentNavId);
            updateToolbarAndNavigation(currentNavId); // Restore title and selected item
        }

        // Initialize Bottom Sheet views and behavior
        bottomSheetContent = findViewById(R.id.bottom_sheet_content);
        bottomSheetView = findViewById(R.id.bottom_sheet_container);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        bottomSheetBehavior.setState(STATE_COLLAPSED); // Initial state

        MenuTrigger = findViewById(R.id.menu_trigger);
        MenuTrigger.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            // 2. Get the window token of the Toolbar (or any view in your activity)
            IBinder windowToken = toolbar.getWindowToken();

            // 3. Check if the keyboard is showing and hide it
            if (imm != null && windowToken != null) {
                imm.hideSoftInputFromWindow(windowToken, 0);
            }
            closeSettings();
            // Inflate the custom menu layout for the popup
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.custom_menu_layout, null);


            // Create the PopupWindow
            popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);

            // Find the Settings TextView in the popup
            TextView settings = popupView.findViewById(R.id.menu_item_1);
            settings.setOnClickListener(v1 -> {
                loadBottomSettingsFragment();
                Log.d(TAG, "Settings clicked. Bottom Sheet State: " + bottomSheetBehavior.getState()); // Add this line
                // When Settings is clicked in the popup:
                // 1. Load the SettingsFragment into the bottom sheet container
                popupWindow.dismiss();
            });

            // Find the Logout TextView in the popup
            TextView logout = popupView.findViewById(R.id.menu_item_2);
            logout.setOnClickListener(view -> {
                // Add your logout functionality here
                Toast.makeText(MainActivity.this, "Logout Clicked", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            });

            // Show the popup window anchored to the MenuTrigger

            popupWindow.showAtLocation(v, Gravity.END, 30, -700);
        });
        //settings collapsed when toolbar clicked
        if (toolbar != null) {
            toolbar.setOnClickListener(v -> {
                closeSettings();
            });
        }
        //settings collapsed when fragment clicked
        fragmentMain = findViewById(R.id.fragment_container);
        // Settings collapsed when drawer opened
        if (drawerLayout != null) {
            drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                    // Perform action when the drawer slides
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                    // 2. Get the window token of the Toolbar (or any view in your activity)
                    IBinder windowToken = toolbar.getWindowToken();

                    // 3. Check if the keyboard is showing and hide it
                    if (imm != null && windowToken != null) {
                        imm.hideSoftInputFromWindow(windowToken, 0);
                    }
                }

                @Override
                public void onDrawerOpened(@NonNull View drawerView) {
                    closeSettings();
                    // Perform action when the drawer opens
                }

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {
                    // Perform action when the drawer closes
                }

                @Override
                public void onDrawerStateChanged(int newState) {
                    // Perform action when the drawer state changes
                }
            });
        }
        // Replace with the actual ID of your TextView
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the current nav ID
        outState.putInt("currentNavId", currentNavId);
    }

    private View findNavigationViewItemView(@NonNull MenuItem item) {
        if (navigationView == null) return null;
        return navigationView.findViewById(item.getItemId());
    }

    @Override
    public void onBackPressed() {
        // 1. Handle DrawerLayout open state
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return; // Consume the back press
        }
        // 2. Handle Bottom Sheet expanded state
        else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            clearBottomFragment();
            toggleBottomSheet();
            // Adjust FAB margin if necessary (this logic seems tied to your FAB animation)
            com.google.android.material.floatingactionbutton.FloatingActionButton Fab = findViewById(R.id.fab);
            if (Fab != null) {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
                if (params.bottomMargin > 16) { // Check if it's not already at default margin
                    params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, (int) getResources().getDimension(R.dimen.fab_null_margin));
                    Fab.setLayoutParams(params);
                }
            }
            Log.d(TAG, "onBackPressed: bottom sheet collapsed");
            return; // Consume the back press
        }
        // 3. Handle PopupWindow showing state
        else if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            return; // Consume the back press
        }

        // 4. Handle exit confirmation ONLY if on HomeFragment AND no other modals are open
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {
            new AlertDialog.Builder(this)
                    .setTitle("Exit Application")
                    .setMessage("Are you sure you want to exit the application?")
                    .setPositiveButton("Yes", (dialog, which) -> finish()) // Finish the activity to exit
                    .setNegativeButton("No", (dialog, which) -> {
                        // Do nothing or dismiss the dialog. The activity remains.
                        dialog.dismiss();
                    })
                    .setCancelable(false) // Prevent dismissing by tapping outside or pressing back again
                    .show();
        } else {
            // If not on HomeFragment, or if it's not the root of the back stack,
            // let the default back behavior (popping fragments) happen.
            super.onBackPressed();
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        currentNavId = id; // Update currentNavId
        View currentItemView = navigationView.findViewById(id);

        // Remove background color from the previously selected item
        if (previousItemView != null && previousItemView != currentItemView) {
            previousItemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        if (id == R.id.nav_home) {
            loadFragment(new HomeFragment(), R.id.nav_home);
            toolbar.setTitle("Heal");
        } else if (id == R.id.nav_records) {
            loadFragment(new RecordFragment(), R.id.nav_records);
            toolbar.setTitle("Data Records");
        } else if (id == R.id.nav_gallery) {
            loadFragment(new GalleryFragment(), R.id.nav_gallery);
            toolbar.setTitle("Art Corner");
        } else if (id == R.id.nav_slideshow) {
            loadFragment(new SlideshowFragment(), R.id.nav_slideshow);
            toolbar.setTitle("Game Room");
        } else if (id == R.id.nav_send) {
            loadFragment(new HomeFragment(), R.id.nav_home);
            showSendPopup();
            toolbar.setTitle("Heal");

        } else if (id == R.id.nav_share) {
            loadFragment(new HomeFragment(), R.id.nav_home);
            Share(myAppLink, shareMessage);
            toolbar.setTitle("Heal");
        }

        // Update the previous selected item and view
        if (previousMenuItem != item) {
            previousMenuItem = item;
            previousItemView = currentItemView;
            if (previousItemView != null && id != R.id.nav_send && id != R.id.nav_share) {
                previousItemView.setBackgroundColor(getResources().getColor(R.color.orange));
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void Share(String appLink, String optionalText) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, appLink + (optionalText != null && !optionalText.isEmpty() ? "\n\n" + optionalText : ""));
        shareIntent.setType("text/plain"); // Set the MIME type to plain text

        Intent chooserIntent = Intent.createChooser(shareIntent, "Share app link via");
        try {
            startActivity(chooserIntent);
        } catch (android.content.ActivityNotFoundException anfe) {
            Toast.makeText(this, "No app can handle this share action.", Toast.LENGTH_SHORT).show();
        }
    }

    // Example usage:
    private void showSendPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.send_window, null);
        popupWindow = new PopupWindow(popupView, 900, ConstraintLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setFocusable(true);
        try {
            popupWindow.showAtLocation(fragmentMain, Gravity.CENTER, 0, 0);

            // Find the button and set the listener *after* the popup view is created
            Button sendMessageButtonPopup = popupView.findViewById(R.id.buttonSend);
            if (sendMessageButtonPopup != null) {
                sendMessageButtonPopup.setOnClickListener(v -> {
                    EditText recipientEditTextPopup = popupView.findViewById(R.id.editTextRecipient);
                    EditText messageEditTextPopup = popupView.findViewById(R.id.editTextMessage);
                    if (recipientEditTextPopup != null && recipientEditTextPopup.length() == 0) {
                        Toast.makeText(this, "User ID Cannot Be empty", Toast.LENGTH_SHORT).show();
                    } else if (messageEditTextPopup != null && messageEditTextPopup.length() == 0) {
                        Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                    } else {
                        dismissSendPopupAndSaveData();
                    }
                });
            } else {
                Log.e(TAG, "buttonSend not found in send_window.xml");
                Toast.makeText(this, "Error: Send button not found", Toast.LENGTH_SHORT).show();
            }

            Button cancelMessageButtonPopup = popupView.findViewById(R.id.buttonCancel);
            if (cancelMessageButtonPopup != null) {
                cancelMessageButtonPopup.setOnClickListener(v -> {
                    if (popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss();
                        popupWindow = null;
                    }
                });
            } else {
                Log.e(TAG, "buttonCancel not found in send_window.xml");
                Toast.makeText(this, "Error: Cancel button not found", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "popup error", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error showing popup", e);
        }
    }

    private void dismissSendPopupAndSaveData() {
        if (popupWindow != null && popupWindow.isShowing()) {
            // Get the content view of the popup window
            View popupContentView = popupWindow.getContentView();

            // Find your input fields within the popupView and save their data
            EditText recipientEditText = popupContentView.findViewById(R.id.editTextRecipient); // Replace with your actual ID
            EditText messageEditText = popupContentView.findViewById(R.id.editTextMessage);   // Replace with your actual ID

            if (recipientEditText != null) {
                sendData.put("recipient", recipientEditText.getText().toString());
            }
            if (messageEditText != null) {
                sendData.put("message", messageEditText.getText().toString());
            }

            // Dismiss the popup window
            popupWindow.dismiss();
            popupWindow = null; // It's good practice to set it to null after dismissing

            // Now you can use the 'sendData' HashMap to access the saved input
            // For example:
            String savedRecipient = sendData.get("recipient");
            String savedMessage = sendData.get("message");
            // Do something with the saved data (e.g., store it, process it)
            Toast.makeText(this, "Message sent to " + savedRecipient, Toast.LENGTH_SHORT).show();
            Log.d("SendData", "Recipient: " + savedRecipient + ", Message: " + savedMessage);
        }
    }

    // Loads a fragment into the main content area
    public void loadFragment(Fragment fragment, int navId) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // Add custom animations BEFORE the replace method
        // Parameters: enter, exit, popEnter, popExit
        // enter: animation for the new fragment entering
        // exit: animation for the current fragment exiting
        // popEnter: animation for the fragment returning when popped from back stack
        // popExit: animation for the fragment that's being popped off the back stack
        ft.setCustomAnimations(
                R.anim.slide_in_right,  // New fragment enters
                R.anim.slide_out_left,  // Current fragment exits
                R.anim.slide_in_left,   // Previous fragment re-enters (when back pressed)
                R.anim.slide_out_right  // Fragment being popped exits
        );

        ft.replace(R.id.fragment_container, fragment);

        if (!(fragment instanceof HomeFragment)) {
            // Only add to back stack if it's not the HomeFragment
            ft.addToBackStack(null);
        }
        ft.commit();
        currentNavId = navId; // Update the currentNavId here
        //updateToolbarAndNavigation(navId); // Consider uncommenting this for dynamic UI updates
    }

    private void updateToolbarAndNavigation(int navId) {
        if (navId == R.id.nav_home) {
            toolbar.setTitle("Heal");
            navigationView.setCheckedItem(R.id.nav_home);
        } else if (navId == R.id.nav_records) {
            toolbar.setTitle("Data Records");
            navigationView.setCheckedItem(R.id.nav_records);
        } else if (navId == R.id.nav_gallery) {
            toolbar.setTitle("Art Corner");
            navigationView.setCheckedItem(R.id.nav_gallery);
        } else if (navId == R.id.nav_slideshow) {
            toolbar.setTitle("Game Room");
            navigationView.setCheckedItem(R.id.nav_slideshow);
        } else if (navId == R.id.nav_send || navId == R.id.nav_share) {
            toolbar.setTitle("Heal");
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    // Method to show/hide the bottom sheet
    public void toggleBottomSheet() {
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            bottomSheetContent.setBackground(getResources().getDrawable(R.drawable.rounded_top_container));
        } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(STATE_COLLAPSED);
            bottomSheetContent.setBackgroundColor(getResources().getColor(R.color.transparent));
        } else {
            bottomSheetBehavior.setState(STATE_COLLAPSED);
            bottomSheetContent.setBackgroundColor(getResources().getColor(R.color.transparent));
        }
    }

    private void loadBottomFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.bottom_sheet_content, fragment);
        ft.commit();
    }

    private void clearBottomFragment() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.bottom_sheet_content);
        if (currentFragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(currentFragment);
            ft.commit();
            fm.executePendingTransactions(); // Ensure the removal happens immediately
        }
    }

    public void closeSettings() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            Fab = findViewById(R.id.fab);
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
            if (params.bottomMargin > 0) { // Check if bottomMargin is 0
                if (params.bottomMargin == (int) getResources().getDimension(R.dimen.fab_default_margin)) {
                    clearBottomFragment();
                    toggleBottomSheet();
                    params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + (int) getResources().getDimension(R.dimen.fab_null_margin));
                    Fab.setLayoutParams(params);
                    Log.d(TAG, "onBackPressed: state collapsed and FAB margin adjusted");

                } else {

                }
            } else {
                Log.d(TAG, "onBackPressed: state expanded, but FAB margin is not 0");
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Reminder Notifications"; // Using normal text
            String description = "Daily reminders for your well-being,you are important and you should take care of yourself"; // Using normal text
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleReminderNotification() {
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, REMINDER_REQUEST_CODE, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Set the alarm to trigger at approximately the same time every day
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 25);
        calendar.set(Calendar.SECOND, 0);

        // If the trigger time is in the past, add one day
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), REPEAT_INTERVAL, pendingIntent);
    }

    private void cancelReminderNotification() {
        Intent notificationIntent = new Intent(this, ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, REMINDER_REQUEST_CODE, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    private boolean getSavedReminderState() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("reminder_enabled", false);
    }

    private void checkAndScheduleReminder() {
        if (getSavedReminderState()) {
            scheduleReminderNotification();
            // Optionally, you can show a toast here if the reminder is already enabled
            // Toast.makeText(this, "Reminder notifications are enabled", Toast.LENGTH_SHORT).show();
        } else {
            cancelReminderNotification();
            // Optionally, you can show a toast here if the reminder is disabled
            // Toast.makeText(this, "Reminder notifications are disabled", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadBottomSettingsFragment() {
        bottomSheetBehavior.setState(STATE_COLLAPSED);
        Fab = findViewById(R.id.fab);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
        if (bottomSheetBehavior.getState() == STATE_COLLAPSED) {
            loadBottomFragment(new SettingsFragment());
            toggleBottomSheet();
            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + (int) getResources().getDimension(R.dimen.fab_bottom_margin));
            Fab.setLayoutParams(params);
        }
    }

    public void saveNameToLocalStorage(String name) {
        SharedPreferences sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_name", name);
        editor.apply();}
}
