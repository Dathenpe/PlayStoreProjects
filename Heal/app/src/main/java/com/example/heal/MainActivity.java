package com.example.heal;
import static android.app.ProgressDialog.show;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;

import static ui.HomeFragment.KEY_LAST_RELAPSE_DATE;
import static ui.HomeFragment.PREFS_RELAPSE;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import records.AddEditContactDialogFragment;
import records.CopingExercisesFragment;
import records.EmergencyContact;
import records.EmergencyContactsFragment;
import records.JournalEntriesFragment;
import records.MoodCheckinFragment;
import records.SavedStrategiesFragment;
import ui.GalleryFragment;
import ui.HomeFragment;
import ui.RecordFragment;
import ui.ReminderBroadcastReceiver;
import ui.SettingsFragment;
import ui.AIFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        AddEditContactDialogFragment.OnContactSavedListener {

    private static final String KEY_LAST_RELAPSE_DATE = "RelapseCounterPrefs" ;
    private static final String PREFS_RELAPSE = "lastRelapseDate" ;
    private List<EmergencyContact> emergencyContactList;
    private Gson gson;
    private static final String PREFS_NAME = "EmergencyContactsPrefs";
    private static final String KEY_CONTACTS = "contactsList";

    private DrawerLayout drawerLayout;
    private FloatingActionButton Fab;
    public Toolbar toolbar;
    public NavigationView navigationView;
    private MenuItem previousMenuItem;
    private View previousItemView;
    private ImageButton MenuTrigger;
    private PopupWindow popupWindow;
    FrameLayout bottomSheetContent;
    private static final String TAG = "MainActivity";
    View bottomSheetView;
    BottomSheetBehavior<View> bottomSheetBehavior;
    private View overlayView;
    private View fragmentMain;
    private TextInputEditText RecipientId;
    private TextInputEditText SendMessage;
    private Button sendMessage;
    private Button cancelMessage;
    private HashMap<String, String> sendData = new HashMap<>();
    private EditText recipientEditText;
    private EditText messageEditText;
    private final String myAppLink = "https://play.google.com/store/apps/details?id=com.example.myapp";
    private final String shareMessage = "Check out this awesome app!";
    private TextView userNameDisplayTextView;
    private static final String CHANNEL_ID = "reminder_channel";
    private static final int REMINDER_NOTIFICATION_ID = 1;
    private static final int REMINDER_REQUEST_CODE = 102;
    private static final long REPEAT_INTERVAL = TimeUnit.DAYS.toMillis(1);
    private int currentNavId = R.id.nav_home;
    private static final String FIRST_LAUNCH_KEY = "firstLaunch";

    public SharedPreferences settingse;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Set content view first

        // Initialize UI components that depend on the layout
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        settingse = getSharedPreferences(PREFS_NAME,0);
        boolean isFirstLaunch = settingse.getBoolean(FIRST_LAUNCH_KEY, true);
        Log.d(TAG, "MainActivity: onCreate - isFirstLaunch: " + isFirstLaunch);


        if (isFirstLaunch){
            welcomeMessage(); // This will handle setting the flag, starting timer, and then loading HomeFragment
        } else {
            // If not first launch, load HomeFragment immediately
            if (savedInstanceState == null) {
                loadFragment(new HomeFragment(), R.id.nav_home);
                navigationView.setCheckedItem(R.id.nav_home);
                toolbar.setTitle("Home Room");
                Log.d(TAG, "MainActivity: onCreate - Loading HomeFragment (not first launch, savedInstanceState is null)");
            } else {
                currentNavId = savedInstanceState.getInt("currentNavId", R.id.nav_home);
                navigationView.setCheckedItem(currentNavId);
                updateToolbarAndNavigation(currentNavId);
                Log.d(TAG, "MainActivity: onCreate - Restoring fragment (not first launch, savedInstanceState exists)");
            }
        }

        // Rest of the onCreate logic that runs regardless of first launch
        gson = new Gson();
        loadEmergencyContacts();
        checkAndScheduleReminder();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

      if (bottomSheetBehavior != null){
          bottomSheetContent = findViewById(R.id.bottom_sheet_content);
          bottomSheetView = findViewById(R.id.bottom_sheet_container);
          bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
          bottomSheetBehavior.setState(STATE_COLLAPSED);
      }

        MenuTrigger = findViewById(R.id.menu_trigger);
        MenuTrigger.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            IBinder windowToken = toolbar.getWindowToken();
            if (imm != null && windowToken != null) {
                imm.hideSoftInputFromWindow(windowToken, 0);
            }
            closeSettings();
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.custom_menu_layout, null);
            popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);

            TextView settings = popupView.findViewById(R.id.menu_item_1);
            settings.setOnClickListener(v1 -> {
                loadBottomSettingsFragment();
                Log.d(TAG, "Settings clicked. Bottom Sheet State: " + bottomSheetBehavior.getState());
                popupWindow.dismiss();
            });

            TextView deleteData = popupView.findViewById(R.id.menu_item_2);
            deleteData.setOnClickListener(view -> {
                Toast.makeText(MainActivity.this, "Account Deletion Clicked", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            });
            popupWindow.showAtLocation(v, Gravity.END, 30, -700);
        });

        if (toolbar != null) {
            toolbar.setOnClickListener(v -> {
                closeSettings();
            });
        }
        fragmentMain = findViewById(R.id.fragment_container);
        if (drawerLayout != null) {
            drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    IBinder windowToken = toolbar.getWindowToken();
                    if (imm != null && windowToken != null) {
                        imm.hideSoftInputFromWindow(windowToken, 0);
                    }
                }

                @Override
                public void onDrawerOpened(@NonNull View drawerView) {
                    closeSettings();
                }

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {
                }

                @Override
                public void onDrawerStateChanged(int newState) {
                }
            });
        }

        Fab = findViewById(R.id.fab);
        if (Fab != null) {
            Fab.setOnClickListener(v -> {
                closeSettings();
                AddEditContactDialogFragment addEditDialog = AddEditContactDialogFragment.newInstance(null);
                addEditDialog.show(getSupportFragmentManager(), "AddEditContactDialog");
            });
        }
    }

    private void welcomeMessage() {
        new AlertDialog.Builder(this)
                .setTitle("Welcome to Heal")
                .setMessage("Welcome to Heal! This app is designed to support you on your journey. We'll start a relapse counter for you now. You can reset it anytime.")
                .setPositiveButton("OK", (dialog, which) -> {
                    SharedPreferences.Editor editor = settingse.edit();
                    editor.putBoolean(FIRST_LAUNCH_KEY, false);
                    editor.apply();
                    Log.d(TAG, "MainActivity: welcomeMessage - FIRST_LAUNCH_KEY set to false.");
                    startRelapseCounter(); // Start the counter and save the time

                    // Removed explicit call to HomeFragment's startRelapseCounterUpdates()
                    // HomeFragment's onResume will now handle starting the timer.

                    // Now load the HomeFragment after the counter has been started
                    loadFragment(new HomeFragment(), R.id.nav_home);
                    navigationView.setCheckedItem(R.id.nav_home);
                    toolbar.setTitle("Home Room");
                    Log.d(TAG, "MainActivity: welcomeMessage - Loading HomeFragment after timer start (final step).");
                })
                .setCancelable(false)
                .show();
    }

    private void startRelapseCounter() {
        SharedPreferences prefs = this.getSharedPreferences(PREFS_RELAPSE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        long startTime = System.currentTimeMillis();
        editor.putLong(KEY_LAST_RELAPSE_DATE, startTime);
        editor.apply();
        Toast.makeText(this, "Relapse counter Started!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "MainActivity: startRelapseCounter - Saved time: " + startTime);
    }



    private void saveEmergencyContacts() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(emergencyContactList);
        editor.putString(KEY_CONTACTS, json);
        editor.apply();
        Log.d(TAG, "Emergency contacts saved. Count: " + emergencyContactList.size());
    }

    private void loadEmergencyContacts() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_CONTACTS, null);

        if (json == null) {
            emergencyContactList = new ArrayList<>();
            Log.d(TAG, "No emergency contacts found. Initialized empty list.");
        } else {
            Type type = new TypeToken<ArrayList<EmergencyContact>>() {}.getType();
            emergencyContactList = gson.fromJson(json, type);
            Log.d(TAG, "Emergency contacts loaded. Count: " + emergencyContactList.size());
        }
    }

    @Override
    public void onContactSaved(EmergencyContact contact) {
        boolean found = false;
        for (int i = 0; i < emergencyContactList.size(); i++) {
            if (emergencyContactList.get(i).getId().equals(contact.getId())) {
                emergencyContactList.set(i, contact);
                found = true;
                break;
            }
        }
        if (!found) {
            emergencyContactList.add(contact);
        }

        saveEmergencyContacts();

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof EmergencyContactsFragment) {
            ((EmergencyContactsFragment) currentFragment).refreshContactsFromActivity(emergencyContactList);
        } else {
            Toast.makeText(this, "Contact saved! You can see it in Emergency Contacts.", Toast.LENGTH_SHORT).show();
        }
    }

    public void removeEmergencyContact(String contactId) {
        if (emergencyContactList != null) {
            EmergencyContact contactToRemove = null;
            for (EmergencyContact contact : emergencyContactList) {
                if (contact.getId().equals(contactId)) {
                    contactToRemove = contact;
                    break;
                }
            }
            if (contactToRemove != null) {
                emergencyContactList.remove(contactToRemove);
                saveEmergencyContacts();

                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof EmergencyContactsFragment) {
                    ((EmergencyContactsFragment) currentFragment).refreshContactsFromActivity(emergencyContactList);
                }
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentNavId", currentNavId);
    }

    private View findNavigationViewItemView(@NonNull MenuItem item) {
        if (navigationView == null) return null;
        return navigationView.findViewById(item.getItemId());
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        } else if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            return;
        }
        closeSettings();
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment && bottomSheetBehavior.getState() == STATE_COLLAPSED) {
            new AlertDialog.Builder(this)
                    .setTitle("Exit Application")
                    .setMessage("Are you sure you want to exit the application?")
                    .setPositiveButton("Yes", (dialog, which) -> finish())
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
    public void loadContacts() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (!(currentFragment instanceof EmergencyContactsFragment)) {
            EmergencyContactsFragment targetFragment = new EmergencyContactsFragment();
            Bundle args = new Bundle();
            // THIS IS CRUCIAL: Ensure emergencyContactList in MainActivity has data
            args.putSerializable("contactList", new ArrayList<>(emergencyContactList));
            targetFragment.setArguments(args);
            loadFragment(targetFragment, R.id.nav_emergency_contacts); // Or whatever ID you use
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        View currentItemView = navigationView.findViewById(id);

        if (previousItemView != null && previousItemView != currentItemView) {
            previousItemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        Fragment targetFragment = null;
        String toolbarTitle = "";
        boolean shouldLoadFragment = false;

        if (id == R.id.nav_home) {
            toolbarTitle = "Heal";
            if (!(currentFragment instanceof HomeFragment)) {
                targetFragment = new HomeFragment();
                shouldLoadFragment = true;
            }
        } else if (id == R.id.nav_records) {
            toolbarTitle = "Data Records";
            if (!(currentFragment instanceof RecordFragment)) {
                if (currentFragment instanceof EmergencyContactsFragment || currentFragment instanceof CopingExercisesFragment ||  currentFragment instanceof JournalEntriesFragment ||  currentFragment instanceof MoodCheckinFragment ||  currentFragment instanceof SavedStrategiesFragment) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                targetFragment = new RecordFragment();
                shouldLoadFragment = true;
            }
        } else if (id == R.id.nav_gallery) {
            toolbarTitle = "Art Corner";
            if (!(currentFragment instanceof GalleryFragment)) {
                targetFragment = new GalleryFragment();
                shouldLoadFragment = true;
            }
        } else if (id == R.id.nav_ai) {
            toolbarTitle = "Gemini";
            if (!(currentFragment instanceof AIFragment)) {
                targetFragment = new AIFragment();
                shouldLoadFragment = true;
            }
        } else if (id == R.id.nav_send) {
            toolbarTitle = "Heal";
            if (!(currentFragment instanceof HomeFragment)) {
                targetFragment = new HomeFragment();
                shouldLoadFragment = true;
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(this::showSendPopup, 1000);
            } else {
                showSendPopup();
            }
        } else if (id == R.id.nav_share) {
            toolbarTitle = "Heal";
            if (!(currentFragment instanceof HomeFragment)) {
                targetFragment = new HomeFragment();
                shouldLoadFragment = true;
            }
            Share(myAppLink, shareMessage);
        }
        // The `nav_emergency_contacts` block was here, removed.


        if (shouldLoadFragment && targetFragment != null) {
            loadFragment(targetFragment, id);
        }

        toolbar.setTitle(toolbarTitle);
        if (previousMenuItem != item) {
            if (previousItemView != null) {
                previousItemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }
            previousMenuItem = item;
            previousItemView = currentItemView;

            if (previousItemView != null && id != R.id.nav_send && id != R.id.nav_share) {
                previousItemView.setBackgroundColor(getResources().getColor(R.color.orange));
            }
        } else {
            if (currentItemView != null && id != R.id.nav_send && id != R.id.nav_share) {
                currentItemView.setBackgroundColor(getResources().getColor(R.color.orange));
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void Share(String appLink, String optionalText) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, appLink + (optionalText != null && !optionalText.isEmpty() ? "\n\n" + optionalText : ""));
        shareIntent.setType("text/plain");
        navigationView.setCheckedItem(R.id.nav_home);
        Intent chooserIntent = Intent.createChooser(shareIntent, "Share app link via");
        try {
            startActivity(chooserIntent);
        } catch (ActivityNotFoundException anfe) {
            Toast.makeText(this, "No app can handle this share action.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSendPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.send_window, null);
        popupWindow = new PopupWindow(popupView, 900, ConstraintLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setFocusable(true);
        navigationView.setCheckedItem(R.id.nav_home);
        try {
            popupWindow.showAtLocation(fragmentMain, Gravity.CENTER, 0, 0);

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
            View popupContentView = popupWindow.getContentView();
            EditText recipientEditText = popupContentView.findViewById(R.id.editTextRecipient);
            EditText messageEditText = popupContentView.findViewById(R.id.editTextMessage);

            if (recipientEditText != null) {
                sendData.put("recipient", recipientEditText.getText().toString());
            }
            if (messageEditText != null) {
                sendData.put("message", messageEditText.getText().toString());
            }

            popupWindow.dismiss();
            popupWindow = null;

            String savedRecipient = sendData.get("recipient");
            String savedMessage = sendData.get("message");
            Toast.makeText(this, "Message sent to " + savedRecipient, Toast.LENGTH_SHORT).show();
            Log.d("SendData", "Recipient: " + savedRecipient + ", Message: " + savedMessage);
        }
    }

    public void loadFragment(Fragment fragment, int navId) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );
        if (fragment instanceof HomeFragment) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fm.executePendingTransactions();
        }

        ft.replace(R.id.fragment_container, fragment);
        if (!(fragment instanceof HomeFragment)) {
            ft.addToBackStack(null);
        }
        ft.commit();
        currentNavId = navId;
        Log.d(TAG, "MainActivity: loadFragment - Fragment loaded: " + fragment.getClass().getSimpleName());
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
        } else if (navId == R.id.nav_ai) {
            toolbar.setTitle("Game Room");
            navigationView.setCheckedItem(R.id.nav_ai);
        } else if (navId == R.id.nav_send || navId == R.id.nav_share) {
            toolbar.setTitle("Heal");
            navigationView.setCheckedItem(R.id.nav_home);
        }
        // The `nav_emergency_contacts` block was here, removed.
    }

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
        ft.addToBackStack(null);
        ft.commit();
    }

    private void clearBottomFragment() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.bottom_sheet_content);
        if (currentFragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(currentFragment);
            ft.commit();
            fm.executePendingTransactions();
        }
    }

    public void closeSettings() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            Fab = findViewById(R.id.fab);
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
            if (params.bottomMargin > 0) {
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
            String name = "Reminder Notifications";
            String description = "Daily reminders for your well-being,you are important and you should take care of yourself";
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

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 25);
        calendar.set(Calendar.SECOND, 0);

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
        } else {
            cancelReminderNotification();
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
        editor.apply();
    }
}
