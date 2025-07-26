package com.f9ld3.heal;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import drawing.DrawingCanvasFragment;
import funcorner.MemoryMatchGameFragment;
import funcorner.PaintFragment;
import funcorner.TetrisGameFragment;
import funcorner.WordScrambleGameFragment;
import records.AddEditContactDialogFragment;
import records.CopingExercisesFragment;
import records.EmergencyContact;
import records.EmergencyContactsFragment;
import records.JournalEntriesFragment;
import records.MoodCheckinFragment;
import records.RelapseHistoryFragment;
import records.SavedStrategiesFragment;
import ui.AIFragment;
import ui.CustomMessageDialogFragment;
import ui.FunCornerFragment;
import ui.HomeFragment;
import ui.InterceptTouchRecyclerView;
import ui.NotificationAdapter;
import ui.RecordFragment;
import ui.ReminderBroadcastReceiver;
import ui.SettingsFragment;
import viewmodels.GeneralViewModel;

class FragmentHistoryItem{
    public int navId;
    public String title;

    public FragmentHistoryItem(int navId, String title) {
        this.navId = navId;
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FragmentHistoryItem that = (FragmentHistoryItem) o;
        return navId == that.navId;
    }
    @Override
    public int hashCode() {
        return navId;
    }
}
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        AddEditContactDialogFragment.OnContactSavedListener,DrawingCanvasFragment.OnDrawingSavedListener  {

    private static final String KEY_LAST_RELAPSE_DATE = "lastRelapseDate" ;
    private static final String PREFS_RELAPSE = "RelapseCounterPrefs" ;
    private List<EmergencyContact> emergencyContactList;
    private Gson gson;
    private static final String PREFS_NAME = "EmergencyContactsPrefs";
    private static final String KEY_CONTACTS = "contactsList";

    private DrawerLayout drawerLayout;
    public FloatingActionButton Fab;
    public Toolbar toolbar;
    public NavigationView navigationView;
    private MenuItem previousMenuItem;
    private View previousItemView;
    public ImageButton MenuTrigger;
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
    private TextView userNameDisplayTextView;

    private int currentNavId = R.id.nav_home;
    private static final String FIRST_LAUNCH_KEY = "firstLaunch";
    // New key to track if the app crashed previously
    private static final String KEY_CRASHED_PREVIOUSLY = "crashedPreviously";


    public SharedPreferences settingse;

    private  Boolean isSettingsOpened;

    private int currentWelcomeDialogStep = 0;

    private final String[] WELCOME_TITLES = {
            "Welcome to Heal: Your Path to Wellness",
            "Tracking Your Progress, Celebrating Milestones",
            "You're In Control: Tools & Support",
            "Discover Your Strengths: Coping & More"
    };

    private final String[] WelcomeMessages = {
            "Welcome to Heal! We're here to provide a supportive space and tools to help you navigate your journey towards well-being and recovery.",
            "Let's begin by setting up your progress tracker. It’s a simple way to acknowledge your efforts and celebrate every step forward on your path. Heal also offers tools like Journaling and Mood Check-ins to reflect on your path.",
            "You're in control. The progress tracker is yours to manage. Alongside it, remember to set up your Emergency Contacts for quick support, and explore personalized Coping Strategies.",
            "Discover effective Coping Strategies tailored for you. You can prioritize them for easy access. Remember, Heal also offers a space for your Journal Entries, creative expression in the Art Corner, and helpful Reminders."
    };

    private static final String PREFS_RECENTLY_VISITED =  "RecentlyVisitedPrefs";
    private static final String KEY_RECENTLY_VISITED = "recentlyVisitedFragments";
    private static final int MAX_RECENT_CHIPS = 3;
    private LinearLayout recentlyVisitedChipContainer;

    private LinkedHashMap<Integer,FragmentHistoryItem> fragmentHistoryMap;

    private TextView emptyRecentlyVisitedTextView;
    private InterceptTouchRecyclerView recyclerViewNotifications; // Changed to InterceptTouchRecyclerView
    private NotificationAdapter notificationAdapter;
    private TextView emptyNotificationsTextView;
    public static final String REMINDER_CHANNEL_ID = "reminder_channel";
    public static final int REMINDER_NOTIFICATION_ID_7AM = 100;
    public static final int REMINDER_NOTIFICATION_ID_11AM = 101;
    public static final int REMINDER_NOTIFICATION_ID_6PM = 102;
    public static final int REMINDER_NOTIFICATION_ID_9PM = 103;

    public static final String PREFS_NOTIFICATIONS = "notifications_prefs";
    public static final String KEY_RECENT_NOTIFICATIONS = "recent_notifications";
    public static final String KEY_LAST_MOOD_CHECKIN_DATE = "last_mood_checkin_date";

    private ScrollView recentlySentNotificationsScrollView;

    // New constant for theme preference
    private static final String PREF_SELECTED_THEME_COLOR = "selected_theme_color";

    // Map to store theme color names to their corresponding launcher icon aliases
    private static final Map<String, String> THEME_ICON_ALIASES = new HashMap<>();
    static {
        // IMPORTANT: These alias names must exactly match the 'android:name' in AndroidManifest.xml
        // using the correct application ID as prefix.
        THEME_ICON_ALIASES.put("md_theme_primary", "com.f9ld3.heal.MainActivityAliasHeal"); // Default
        THEME_ICON_ALIASES.put("pink", "com.f9ld3.heal.MainActivityAliasPink");
        THEME_ICON_ALIASES.put("blue", "com.f9ld3.heal.MainActivityAliasBlue");
        THEME_ICON_ALIASES.put("green", "com.f9ld3.heal.MainActivityAliasGreen");
        THEME_ICON_ALIASES.put("purple", "com.f9ld3.heal.MainActivityAliasPurple");
        THEME_ICON_ALIASES.put("orange", "com.f9ld3.heal.MainActivityAliasOrange");
        THEME_ICON_ALIASES.put("teal", "com.f9ld3.heal.MainActivityAliasTeal");
        THEME_ICON_ALIASES.put("brown", "com.f9ld3.heal.MainActivityAliasBrown");
        // Add other themes here if you create more icon drawables for them
    }

    private GeneralViewModel generalViewModel;


    private BroadcastReceiver notificationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ReminderBroadcastReceiver.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
                Log.d(TAG, "Local broadcast received: NOTIFICATION_RECEIVED. Updating notification display.");
                updateRecentlySentNotificationsDisplay();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- START: Crash Restart Logic ---
        // Set a default uncaught exception handler to catch crashes and restart the app
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            private Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                // Log the crash
                Log.e(TAG, "Uncaught exception in thread " + t.getName(), e);

                // Mark that the app crashed previously
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_CRASHED_PREVIOUSLY, true).apply();

                // Restart the app by launching the main activity
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                // Terminate the current process
                System.exit(2);

                // If for some reason the above doesn't work, call the default handler
                defaultUEH.uncaughtException(t, e);
            }
        });
        // --- END: Crash Restart Logic ---

        // Apply theme before calling super.onCreate()
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Set "orange" as the default theme color on first install
        String selectedThemeColorName = sharedPreferences.getString(PREF_SELECTED_THEME_COLOR, "orange");
        int themeResId = getThemeResourceId(selectedThemeColorName);
        setTheme(themeResId); // Apply the theme

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Update the app icon based on the selected theme
        updateAppIcon(selectedThemeColorName); // Call this after setContentView

        NotificationManagerCompat.from(this).cancelAll();
        Log.d(TAG, "All notifications cleared on app launch.");

        LocalBroadcastManager.getInstance(this).registerReceiver(notificationUpdateReceiver,
                new IntentFilter(ReminderBroadcastReceiver.ACTION_NOTIFICATION_RECEIVED));


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        gson = new Gson();

        View headerView = navigationView.getHeaderView(0);
        recentlyVisitedChipContainer = headerView.findViewById(R.id.recently_visited_chip_container);
        emptyRecentlyVisitedTextView = headerView.findViewById(R.id.empty_recently_visited_text_view);

        // Initialize RecyclerView and Adapter
        recyclerViewNotifications = headerView.findViewById(R.id.recyclerViewNotifications);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(new ArrayList<>());
        recyclerViewNotifications.setAdapter(notificationAdapter);
        // Explicitly enable nested scrolling for the RecyclerView
        recyclerViewNotifications.setNestedScrollingEnabled(true);


        emptyNotificationsTextView = headerView.findViewById(R.id.empty_notifications_text_view);


        ImageButton clearNotificationsButton = headerView.findViewById(R.id.clear_notifications_button);

        loadFragmentHistory(); // This method now correctly loads recently visited chips
        updateRecentlyVisitedChips();
        createNotificationChannel();
        updateRecentlySentNotificationsDisplay(); // Initial load of notifications
        boolean reminderEnabled = sharedPreferences.getBoolean("reminder_enabled", false);
        onReminderSettingChanged(reminderEnabled);

        settingse = getSharedPreferences(PREFS_NAME,0);
        boolean isFirstLaunch = settingse.getBoolean(FIRST_LAUNCH_KEY, true);
        Log.d(TAG, "MainActivity: onCreate - isFirstLaunch: " + isFirstLaunch);

        // --- START: Crash Restart and Welcome Message Logic ---
        boolean crashedPreviously = settingse.getBoolean(KEY_CRASHED_PREVIOUSLY, false);
        if (crashedPreviously) {
            Toast.makeText(this, "App is restarting...", Toast.LENGTH_LONG).show();
            settingse.edit().putBoolean(KEY_CRASHED_PREVIOUSLY, false).apply(); // Clear the flag
        } else if (isFirstLaunch) {
            Toast.makeText(this, "App is launching...", Toast.LENGTH_LONG).show();
        }

        generalViewModel = new ViewModelProvider(this).get(GeneralViewModel.class);
        generalViewModel.isLoading.observe(this, isLoading -> {
            if (!isLoading) {
                // Progress bar has finished, now display welcome message or load home fragment
                if (isFirstLaunch) {
                    welcomeMessage();
                } else {
                    if (savedInstanceState == null) {
                        // Handle intent from widgets
                        handleWidgetIntent(getIntent());
                    } else {
                        currentNavId = savedInstanceState.getInt("currentNavId", R.id.nav_home);
                        navigationView.setCheckedItem(currentNavId);
                        updateToolbarAndNavigation(currentNavId);
                        Log.d(TAG, "MainActivity: onCreate - Restoring fragment (not first launch, savedInstanceState exists)");
                    }
                }
            }
        });
        // --- END: Crash Restart and Welcome Message Logic ---


        loadEmergencyContacts();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        bottomSheetContent = findViewById(R.id.bottom_sheet_content);
        bottomSheetView = findViewById(R.id.bottom_sheet_container);
        overlayView = findViewById(R.id.overlay_view);
        ImageButton closeDrawerButton = headerView.findViewById(R.id.nav_close_button);
        if (closeDrawerButton != null) {
            closeDrawerButton.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }
        if (bottomSheetView != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);

            Log.d(TAG, "onCreate: bottomSheetBehavior initialized: " + (bottomSheetBehavior != null));

            bottomSheetBehavior.setPeekHeight(0);
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(STATE_HIDDEN);

            Fab = findViewById(R.id.fab);
            if (Fab != null) {
                bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        if(newState == STATE_EXPANDED){
                            overlayView.setVisibility(View.VISIBLE);
                            setStatusBarColor(R.color.status_bar_overlay_dark);
                        } else if (newState == STATE_COLLAPSED ){
                            Fab.setVisibility(View.VISIBLE);
                            shakeView(Fab);
                            overlayView.setVisibility(View.GONE);
                            setStatusBarColor(R.color.transparent);
                        } else if (newState == STATE_DRAGGING) {
                            overlayView.setVisibility(View.GONE);
                            setStatusBarColor(R.color.transparent);
                        }
                    }
                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                    }
                });
            }
        } else {
            Log.e(TAG, "bottomSheetView (R.id.bottom_sheet_container) is null. BottomSheetBehavior not initialized.");
            Toast.makeText(this, "Error: Bottom sheet container not found in layout!", Toast.LENGTH_LONG).show();
        }

        if (overlayView != null) {
            overlayView.setOnClickListener(v -> {
                if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == STATE_EXPANDED) {
                    closeSettings();
                    overlayView.setVisibility(View.GONE);
                    setStatusBarColor(R.color.transparent);
                    if (Fab.getVisibility() == View.GONE) {
                        Fab.setVisibility(View.VISIBLE);
                        shakeView(Fab);
                    };
                } else if (popupWindow != null && popupWindow.isShowing()) {
                    popupWindow.dismiss();
                    setStatusBarColor(R.color.transparent);
                }
            });
        }

        MenuTrigger = findViewById(R.id.menu_trigger);
        MenuTrigger = findViewById(R.id.menu_trigger);
        MenuTrigger.setOnClickListener(v -> {
            isSettingsOpened = false;
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.overflow_menu, popupMenu.getMenu());

            MenuItem deleteItem = popupMenu.getMenu().findItem(R.id.action_delete_account);
            if (deleteItem != null) {
                SpannableString spannableString = new SpannableString(deleteItem.getTitle());
                int redColor = ContextCompat.getColor(MainActivity.this, R.color.red);
                spannableString.setSpan(new ForegroundColorSpan(redColor), 0, spannableString.length(), 0);
                deleteItem.setTitle(spannableString);
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_settings) {
                    isSettingsOpened = true;
                    loadBottomSettingsFragment();
                    return true;
                } else if (id == R.id.action_delete_account) {
                    Toast.makeText(this, "Account deletion clicked", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });

            popupMenu.setOnDismissListener(a -> {
                if (!isSettingsOpened) {
                    if (overlayView.getVisibility() == View.VISIBLE) {
                        setStatusBarColor(R.color.transparent);
                        overlayView.setVisibility(View.GONE);
                    }
                }
            });

            popupMenu.show();
            if (overlayView.getVisibility() == View.GONE) {
                setStatusBarColor(R.color.status_bar_overlay_dark);
                overlayView.setVisibility(View.VISIBLE);
            }
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
                AddEditContactDialogFragment addEditDialog = AddEditContactDialogFragment.newInstance(null);
                addEditDialog.show(getSupportFragmentManager(), "AddEditContactDialog");
            });
        }
        if (clearNotificationsButton != null) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
            Gson gson = new Gson();
            String json = prefs.getString(KEY_RECENT_NOTIFICATIONS, "[]");
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> notifications = gson.fromJson(json, type);

            clearNotificationsButton.setOnClickListener(v -> {
                if (emptyNotificationsTextView.getVisibility() == View.VISIBLE){
                    Toast.makeText(this, "No Notifications to clear", Toast.LENGTH_SHORT).show();
                }else {
                    // Replaced AlertDialog with CustomMessageDialogFragment
                    CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                            "Clear Notifications",
                            "Are you sure you want to clear all recently sent notifications?",
                            "Clear",
                            "Cancel"
                    );
                    dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                        @Override
                        public void onDialogPositiveClick(DialogFragment dialogFragment) {
                            SharedPreferences pref = getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
                            pref.edit().remove(KEY_RECENT_NOTIFICATIONS).apply();
                            updateRecentlySentNotificationsDisplay();
                            Toast.makeText(MainActivity.this, "Notifications cleared.", Toast.LENGTH_SHORT).show();
                            dialogFragment.dismiss();
                        }

                        @Override
                        public void onDialogNegativeClick(DialogFragment dialogFragment) {
                            dialogFragment.dismiss();
                        }
                    });
                    dialog.show(getSupportFragmentManager(), "ClearNotificationsDialog");
                }
            });

        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleWidgetIntent(intent); // Handle intents when activity is already running
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent != null && "ACTION_LAUNCH_GAME".equals(intent.getAction())) {
            int gameFragmentId = intent.getIntExtra("game_fragment_id", -1);
            if (gameFragmentId != -1) {
                Fragment targetFragment = null;
                String toolbarTitle = "";

                // Close any open bottom sheet or popup
                if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == STATE_EXPANDED) {
                    bottomSheetBehavior.setState(STATE_HIDDEN);
                    clearBottomFragment();
                }
                if (popupWindow != null && popupWindow.isShowing()) {
                    popupWindow.dismiss();
                }

                // Always load HomeFragment first to clear the back stack and ensure a consistent starting point
                loadFragment(new HomeFragment(), R.id.nav_home);

                if (gameFragmentId == R.id.nav_tetris) {
                    targetFragment = new TetrisGameFragment();
                    toolbarTitle = "Tetris";
                } else if (gameFragmentId == R.id.nav_memory_match) {
                    targetFragment = new MemoryMatchGameFragment();
                    toolbarTitle = "Memory Match Game";
                } else if (gameFragmentId == R.id.nav_word_scramble) {
                    targetFragment = new WordScrambleGameFragment();
                    toolbarTitle = "Word Scramble Game";
                } else if (gameFragmentId == R.id.nav_paint) {
                    targetFragment = new PaintFragment();
                    toolbarTitle = "Paint";
                } else {
                    // Fallback to home if unknown ID (already handled by the initial loadFragment call)
                    // targetFragment = new HomeFragment();
                    // toolbarTitle = "Heal";
                }

                if (targetFragment != null) {
                    loadFragment(targetFragment, gameFragmentId);
                    toolbar.setTitle(toolbarTitle);
                    // Ensure navigation view item is checked if it corresponds to a main nav item
                    if (gameFragmentId == R.id.nav_tetris || gameFragmentId == R.id.nav_memory_match || gameFragmentId == R.id.nav_word_scramble || gameFragmentId == R.id.nav_paint) {
                        navigationView.setCheckedItem(R.id.nav_fun_corner);
                    } else if (gameFragmentId == R.id.nav_home) {
                        navigationView.setCheckedItem(R.id.nav_home);
                    }
                    addFragmentToHistory(gameFragmentId, toolbarTitle);
                }
            }
        } else {
            // Default loading for regular app launch if no specific widget intent
            loadFragment(new HomeFragment(), R.id.nav_home);
            navigationView.setCheckedItem(R.id.nav_home);
            toolbar.setTitle("Heal");
            Log.d(TAG, "MainActivity: onCreate - Loading HomeFragment (no specific widget intent).");
        }
    }


    // Helper method to get theme resource ID based on color name
    private int getThemeResourceId(String colorName) {
        switch (colorName) {
            case "md_theme_primary":
                return R.style.Theme_Heal; // Assuming default theme uses md_theme_primary
            case "md_theme_secondary":
                return R.style.Theme_Heal_Secondary;
            case "md_theme_tertiary":
                return R.style.Theme_Heal_Tertiary;
            case "blue":
                return R.style.Theme_Heal_Blue;
            case "green":
                return R.style.Theme_Heal_Green;
            case "purple":
                return R.style.Theme_Heal_Purple;
            case "orange":
                return R.style.Theme_Heal_Orange;
            case "teal": // Added teal theme
                return R.style.Theme_Heal_Teal;
            case "brown": // Added brown theme
                return R.style.Theme_Heal_Brown;
            case "pink": // Added pink theme
                return R.style.Theme_Heal_Pink;
            default:
                return R.style.Theme_Heal; // Default theme
        }
    }

    /**
     * Updates the app's launcher icon based on the selected theme color name.
     * This method enables the alias for the selected theme's icon and disables all others.
     * @param selectedThemeColorName The name of the currently selected theme color.
     */
    public void updateAppIcon(String selectedThemeColorName) {
        PackageManager pm = getPackageManager();
        String currentPackageName = getApplicationContext().getPackageName();

        String selectedAlias = THEME_ICON_ALIASES.get(selectedThemeColorName);
        if (selectedAlias == null) {
            selectedAlias = THEME_ICON_ALIASES.get("md_theme_primary"); // Fallback to default
        }

        // First, disable all aliases
        for (Map.Entry<String, String> entry : THEME_ICON_ALIASES.entrySet()) {
            String aliasName = entry.getValue();
            ComponentName componentName = new ComponentName(currentPackageName, aliasName);
            // Only change if the component is not already disabled
            if (pm.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );
                Log.d(TAG, "Icon alias " + aliasName + " set to DISABLED");
            }
        }

        // Then, enable the selected alias
        if (selectedAlias != null) {
            ComponentName selectedComponentName = new ComponentName(currentPackageName, selectedAlias);
            // Only change if the component is not already enabled
            if (pm.getComponentEnabledSetting(selectedComponentName) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                        selectedComponentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                );
                Log.d(TAG, "Icon alias " + selectedAlias + " set to ENABLED");
            }
        }
    }


    @Override
    protected void onPause(){
        super.onPause();
        saveFragmentHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationManagerCompat.from(this).cancelAll();
        Log.d(TAG, "All notifications cleared on app resume.");
        updateRecentlySentNotificationsDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationUpdateReceiver);
    }

    private void loadFragmentHistory(){
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_RECENTLY_VISITED,Context.MODE_PRIVATE);
        // CORRECTED: Use KEY_RECENTLY_VISITED to load fragment history
        String json = sharedPreferences.getString(KEY_RECENTLY_VISITED,null);
        fragmentHistoryMap = new LinkedHashMap<>();

        if (json != null){
            Type type = new TypeToken<List<FragmentHistoryItem>>(){}.getType();
            List <FragmentHistoryItem> loadedList = gson.fromJson(json,type);
            if (loadedList != null){
                for (FragmentHistoryItem item : loadedList){
                    fragmentHistoryMap.put(item.navId,item);
                }
            }
        }
        Log.d(TAG, "Fragment history loaded. Items: " + fragmentHistoryMap.size());
    }

    private void saveFragmentHistory(){
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_RECENTLY_VISITED,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        List<FragmentHistoryItem> historyToSave = new ArrayList<>(fragmentHistoryMap.values());
        String json = gson.toJson(historyToSave);
        editor.putString(KEY_RECENTLY_VISITED,json);
        editor.apply();
        Log.d(TAG, "Fragment history saved. Items: " + fragmentHistoryMap.size());

    }

    public void addFragmentToHistory(int navId, String title){
        FragmentHistoryItem newItem = new FragmentHistoryItem(navId,title);

        fragmentHistoryMap.remove(newItem.navId);
        fragmentHistoryMap.put(newItem.navId,newItem);

        while (fragmentHistoryMap.size() > MAX_RECENT_CHIPS){
            Map.Entry<Integer,FragmentHistoryItem> oldestEntry = fragmentHistoryMap.entrySet().iterator().next();
            fragmentHistoryMap.remove(oldestEntry.getKey());
        }
        saveFragmentHistory();
        updateRecentlyVisitedChips();
        Log.d(TAG, "Added to history: " + newItem.title + ". Current history size: " + fragmentHistoryMap.size());
    }

    private void updateRecentlyVisitedChips() {
        if (recentlyVisitedChipContainer == null){
            Log.e(TAG, "recentlyVisitedChipContainer is null. Cannot update chips.");
        }else {
            recentlyVisitedChipContainer.removeAllViews();

            List <FragmentHistoryItem> historyForDisplay = new ArrayList<>(fragmentHistoryMap.values());
            Collections.reverse(historyForDisplay);

            if (historyForDisplay.isEmpty()){
                emptyRecentlyVisitedTextView.setVisibility(View.VISIBLE);
                recentlyVisitedChipContainer.setVisibility(View.GONE);
            }else {
                emptyRecentlyVisitedTextView.setVisibility(View.GONE);
                recentlyVisitedChipContainer.setVisibility(View.VISIBLE);

                for (FragmentHistoryItem item : historyForDisplay){
                    Chip chip = (Chip) LayoutInflater.from(this).inflate(R.layout.chip_recently_visited,recentlyVisitedChipContainer,false);
                    chip.setText(item.title);
                    chip.setTag(item.navId);

                    chip.setOnClickListener(v ->{
                        int clickedNavId = (int) v.getTag();
                        if (clickedNavId == R.id.nav_coping_exercises || clickedNavId == R.id.nav_journal_entries || clickedNavId == R.id.nav_mood_checkin ||
                                clickedNavId == R.id.nav_saved_strategies || clickedNavId == R.id.nav_relapse_history ||
                                clickedNavId == R.id.nav_word_scramble || clickedNavId == R.id.nav_tetris || clickedNavId == R.id.nav_memory_match || clickedNavId == R.id.nav_paint){
                            loadFragmentFromChip(clickedNavId);
                        } else if (clickedNavId == R.id.nav_emergency_contacts) {
                            loadContacts();
                            drawerLayout.closeDrawer(GravityCompat.START);
                            navigationView.setCheckedItem(R.id.nav_records);
                        }else {
                            onNavigationItemSelected(navigationView.getMenu().findItem(clickedNavId));
                            drawerLayout.closeDrawer(GravityCompat.START);
                        }
                    });

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

                    layoutParams.setMarginEnd((int) getResources().getDimension(R.dimen.chip_margin_end));
                    recentlyVisitedChipContainer.addView(chip,layoutParams);
                }
            }
            Log.d(TAG, "Chips updated. Displaying " + historyForDisplay.size() + " chips.");
        }
    }

    private void loadFragmentFromChip(int navId) {
        Fragment targetFragment = null;
        String toolbarTitle = null;

        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == STATE_EXPANDED){
            bottomSheetBehavior.setState(STATE_HIDDEN);
            clearBottomFragment();
        }
        if (navId == R.id.nav_coping_exercises){
            targetFragment = new CopingExercisesFragment();
            toolbarTitle = "Coping Exercises";
            navigationView.setCheckedItem(R.id.nav_records);
        } else if (navId == R.id.nav_mood_checkin) {
            targetFragment = new MoodCheckinFragment();
            toolbarTitle = "My Mood History";
            navigationView.setCheckedItem(R.id.nav_records);
        } else if (navId == R.id.nav_saved_strategies) {
            targetFragment = new SavedStrategiesFragment();
            toolbarTitle = "My Coping Strategies";
            navigationView.setCheckedItem(R.id.nav_records);
        } else if (navId == R.id.nav_journal_entries) {
            targetFragment = new JournalEntriesFragment();
            toolbarTitle = "My Journal Entries";
            navigationView.setCheckedItem(R.id.nav_records);
        } else if (navId == R.id.nav_relapse_history) {
            targetFragment = new RelapseHistoryFragment();
            toolbarTitle = "My Relapse History";
            navigationView.setCheckedItem(R.id.nav_records);
        } else if (navId == R.id.nav_word_scramble) {
            targetFragment = new WordScrambleGameFragment();
            toolbarTitle = "Word Scramble Game";
            navigationView.setCheckedItem(R.id.nav_fun_corner);
        }else if (navId == R.id.nav_tetris) {
            targetFragment = new TetrisGameFragment();
            toolbarTitle = "Tetris";
            navigationView.setCheckedItem(R.id.nav_fun_corner);
        }else if (navId == R.id.nav_memory_match) {
            targetFragment = new MemoryMatchGameFragment();
            toolbarTitle = "Memory Match Game";
            navigationView.setCheckedItem(R.id.nav_fun_corner);
        }else if (navId == R.id.nav_paint) {
            targetFragment = new PaintFragment();
            toolbarTitle = "Paint";
            navigationView.setCheckedItem(R.id.nav_fun_corner);
        }
        if (targetFragment != null){
            loadFragment(targetFragment, navId);
            toolbar.setTitle(toolbarTitle);
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }


    private void welcomeMessage() {
        currentWelcomeDialogStep = 0;
        showWelcomeDialogStep(currentWelcomeDialogStep);
    }

    private long startRelapseCounter() {
        SharedPreferences prefs = this.getSharedPreferences(PREFS_RELAPSE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        long startTime = System.currentTimeMillis();
        editor.putLong(KEY_LAST_RELAPSE_DATE, startTime);
        editor.commit();
        Toast.makeText(this, "Relapse counter Started!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "MainActivity: startRelapseCounter - Saved time: " + startTime);
        return startTime;
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentNavId", currentNavId);
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
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        closeSettings();
        if (currentFragment instanceof HomeFragment) {
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit Application",
                    "Are you sure you want to exit the application?",
                    "Yes",
                    "No"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    finish();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "ExitAppDialog");
        } else if (currentFragment instanceof DrawingCanvasFragment){
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit Canvas",
                    "Are you sure you want to exit without saving?",
                    "Yes",
                    "No"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    getSupportFragmentManager().popBackStack();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "ExitCanvasDialog");
        }else if (currentFragment instanceof TetrisGameFragment){
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit Tetris",
                    "Are you sure you want to quit your game?",
                    "Yes",
                    "No"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    getSupportFragmentManager().popBackStack();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "ExitTetrisDialog");
        }else if (currentFragment instanceof MemoryMatchGameFragment){
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit Memory Match",
                    "Are you sure you want to quit your game?",
                    "Yes",
                    "No"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    getSupportFragmentManager().popBackStack();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "ExitMemoryMatchDialog");
        }else if (currentFragment instanceof WordScrambleGameFragment){
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit WordScramble",
                    "Are you sure you want to quit your game?",
                    "Yes",
                    "No"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) { // Removed 'void' from here
                    getSupportFragmentManager().popBackStack();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "ExitWordScrambleDialog");
        }
        else {
            super.onBackPressed();
        }
    }
    public void loadContacts() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (!(currentFragment instanceof EmergencyContactsFragment)) {
            EmergencyContactsFragment targetFragment = new EmergencyContactsFragment();
            Bundle args = new Bundle();
            args.putSerializable("contactList", new ArrayList<>(emergencyContactList));
            targetFragment.setArguments(args);
            loadFragment(targetFragment, R.id.nav_emergency_contacts);
            addFragmentToHistory(R.id.nav_emergency_contacts, "My Emergency Contacts");

        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // Check if the user is on the drawing canvas
        if (currentFragment instanceof DrawingCanvasFragment) {
            drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer immediately

            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit Canvas",
                    "Are you sure you want to leave without saving? Your changes will be lost.",
                    "Leave",
                    "Cancel"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    getSupportFragmentManager().popBackStack();
                    performNavigation(item);
                    dialogFragment.dismiss();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    navigationView.setCheckedItem(R.id.nav_fun_corner);
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "NavExitCanvasDialog");

            return true; // The event is handled by showing the dialog
        } else if (currentFragment instanceof TetrisGameFragment) {
            drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer immediately

            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit Tetris",
                    "Are you sure you want to quit your game?",
                    "Leave",
                    "Cancel"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    getSupportFragmentManager().popBackStack();
                    performNavigation(item);
                    dialogFragment.dismiss();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    navigationView.setCheckedItem(R.id.nav_fun_corner);
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "NavExitTetrisDialog");

            return true; // The event is handled by showing the dialog
        } else if (currentFragment instanceof MemoryMatchGameFragment) {
            drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer immediately
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit Memory Match",
                    "Are you sure you want to quit your game?",
                    "Leave",
                    "Cancel"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    getSupportFragmentManager().popBackStack();
                    performNavigation(item);
                    dialogFragment.dismiss();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    navigationView.setCheckedItem(R.id.nav_fun_corner);
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "NavExitMemoryMatchDialog");

            return true; // The event is handled by showing the dialog
        }else if (currentFragment instanceof WordScrambleGameFragment) {
            drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer immediately
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit WordScramble",
                    "Are you sure you want to quit your game?",
                    "Leave",
                    "Cancel"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    getSupportFragmentManager().popBackStack();
                    performNavigation(item);
                    dialogFragment.dismiss();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    navigationView.setCheckedItem(R.id.nav_fun_corner);
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "NavExitWordScrambleDialog");

            return true; // The event is handled by showing the dialog
        }

        // If not on the canvas, proceed with navigation as normal
        performNavigation(item);
        return true;
    }

    /**
     * Helper method containing the original navigation logic to avoid code duplication.
     */
    private void performNavigation(MenuItem item){
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        int id = item.getItemId();
        View currentItemView = navigationView.findViewById(id);

        if (previousItemView != null && previousItemView != currentItemView) {
            previousItemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        Fragment targetFragment = null;
        String toolbarTitle = "";
        boolean shouldLoadFragment = false;
        FragmentHistoryItem historyItemToAdd = null;

        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == STATE_EXPANDED) {
            bottomSheetBehavior.setState(STATE_HIDDEN);
            clearBottomFragment();
        }

        if (id == R.id.nav_home) {
            toolbarTitle = "Heal";
            if (!(currentFragment instanceof HomeFragment)) {
                targetFragment = new HomeFragment();
                MenuTrigger.setVisibility(View.VISIBLE);
                Fab.setVisibility(View.VISIBLE);
                shakeView(Fab);
                shouldLoadFragment = true;
            }
        } else if (id == R.id.nav_records) {
            toolbarTitle = "Data Records";
            if (!(currentFragment instanceof RecordFragment)) {
                if (currentFragment instanceof EmergencyContactsFragment || currentFragment instanceof CopingExercisesFragment ||
                        currentFragment instanceof JournalEntriesFragment || currentFragment instanceof MoodCheckinFragment ||
                        currentFragment instanceof SavedStrategiesFragment || currentFragment instanceof RelapseHistoryFragment) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                targetFragment = new RecordFragment();
                MenuTrigger.setVisibility(View.VISIBLE);
                Fab.setVisibility(View.VISIBLE);
                shakeView(Fab);
                shouldLoadFragment = true;
            }
            historyItemToAdd = new FragmentHistoryItem(id, toolbarTitle);
        } else if (id == R.id.nav_fun_corner) {
            toolbarTitle = "Fun Corner";

            if (!(currentFragment instanceof FunCornerFragment)) {
                targetFragment = new FunCornerFragment();
                MenuTrigger.setVisibility(View.VISIBLE);
                Fab.setVisibility(View.VISIBLE);
                shakeView(Fab);
                shouldLoadFragment = true;
            }
            historyItemToAdd = new FragmentHistoryItem(id, toolbarTitle);
        } else if (id == R.id.nav_ai) {
            toolbarTitle = "Xavier";
            if (!(currentFragment instanceof AIFragment)) {
                targetFragment = new AIFragment();
                invertShakeView(Fab);
                MenuTrigger.setVisibility(View.GONE);
                bottomSheetBehavior.setState(STATE_HIDDEN);
                overlayView.setVisibility(View.GONE);
                Log.d(TAG, "MainActivity: onNavigationItemSelected - Loading Gemini Fragment");
                shouldLoadFragment = true;
            }
            historyItemToAdd = new FragmentHistoryItem(id, toolbarTitle);
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
            String myAppLink = "https://play.google.com/store/apps/details?id=com.example.myapp";
            String shareMessage = "Check out this awesome app!";
            Share(myAppLink, shareMessage);
        }

        if (shouldLoadFragment && targetFragment != null) {
            loadFragment(targetFragment, id);
        }

        if (historyItemToAdd != null) {
            addFragmentToHistory(historyItemToAdd.navId, historyItemToAdd.title);
        }


        toolbar.setTitle(toolbarTitle);
        drawerLayout.closeDrawer(GravityCompat.START);
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
        if (fragment instanceof HomeFragment || fragment instanceof AIFragment) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fm.executePendingTransactions(); // Ensure previous transactions are processed
        }
        if (!(fragment instanceof HomeFragment) && !(fragment instanceof AIFragment)) {
            ft.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
        }

        ft.replace(R.id.fragment_container, fragment);
        if (!(fragment instanceof HomeFragment)) {
            ft.addToBackStack(null);
        }
        ft.commit();
        fm.executePendingTransactions(); // Crucial for immediate fragment loading
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
        } else if (navId == R.id.nav_fun_corner) {
            toolbar.setTitle("Fun Corner"); // Corrected title for Fun Corner
            navigationView.setCheckedItem(R.id.nav_fun_corner);
        } else if (navId == R.id.nav_ai) {
            toolbar.setTitle("Xavier"); // Ensure AI fragment has a title
            navigationView.setCheckedItem(R.id.nav_ai);
        } else if (navId == R.id.nav_send || navId == R.id.nav_share) {
            toolbar.setTitle("Heal");
            navigationView.setCheckedItem(R.id.nav_home);
        }
        // Add specific titles for game fragments if needed when directly loaded
        else if (navId == R.id.nav_tetris) {
            toolbar.setTitle("Tetris");
            navigationView.setCheckedItem(R.id.nav_fun_corner);
        } else if (navId == R.id.nav_memory_match) {
            toolbar.setTitle("Memory Match Game");
            navigationView.setCheckedItem(R.id.nav_fun_corner);
        } else if (navId == R.id.nav_word_scramble) {
            toolbar.setTitle("Word Scramble Game");
            navigationView.setCheckedItem(R.id.nav_fun_corner);
        } else if (navId == R.id.nav_paint) {
            toolbar.setTitle("Paint");
            navigationView.setCheckedItem(R.id.nav_fun_corner);
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
        if (Fab != null && Fab.getVisibility() != View.VISIBLE){
            Handler handler = new Handler();
            handler.postDelayed(() ->{
                Fab.setVisibility(View.VISIBLE);
                shakeView(Fab);
            },200);
        }
    }
    public void closeSettings() {
        if (bottomSheetBehavior == null) {
            Log.e(TAG, "closeSettings: bottomSheetBehavior is null. Cannot close settings.");
            return;
        }
        if (bottomSheetBehavior.getState() == STATE_EXPANDED) {
            bottomSheetBehavior.setState(STATE_HIDDEN);
            overlayView.setVisibility(View.GONE);
            setStatusBarColor(R.color.transparent);
            if (Fab != null && Fab.getVisibility() == View.GONE){
                Fab.setVisibility(View.VISIBLE);
                shakeView(Fab);
            }
            Log.d(TAG, "closeSettings: Bottom sheet hidden and fragment cleared.");
        } else {
            Log.d(TAG, "closeSettings: Bottom sheet is not expanded. No action needed.");
        }
    }

    // Corrected: Made static to be callable from BroadcastReceiver
    public static void scheduleReminder(Context context){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Add permission check for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms. User needs to grant permission.");
                // In a real app, you'd guide the user to settings here.
                // For this fix, we log a warning. The next method handles the user-facing part.
                return;
            }
        }

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("com.example.heal.REMINDER_ALARM");

        int[][] reminderTimes = {
                {7, 0, REMINDER_NOTIFICATION_ID_7AM},
                {11, 0, REMINDER_NOTIFICATION_ID_11AM},
                {18, 0, REMINDER_NOTIFICATION_ID_6PM},
                {21, 0, REMINDER_NOTIFICATION_ID_9PM}
        };

        for (int[] times : reminderTimes) {
            int hour = times[0];
            int minute = times[1];
            int requestCode = times[2];

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Use setExactAndAllowWhileIdle for all supported versions for consistency
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

            Log.d(TAG, "Scheduled reminder for " + hour + ":" + minute + " with request code " + requestCode);
        }
    }

    // Corrected: Made static
    public static void cancelAllReminders(Context context){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("com.example.heal.REMINDER_ALARM");

        int[] requestCodes = {
                REMINDER_NOTIFICATION_ID_7AM,
                REMINDER_NOTIFICATION_ID_11AM,
                REMINDER_NOTIFICATION_ID_6PM,
                REMINDER_NOTIFICATION_ID_9PM
        };
        for (int requestCode : requestCodes) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled reminder with request code " + requestCode);
        }
    }

    public void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            String name = "Reminder Notifications";
            String description = "Daily reminders for your well-being,you are important and you should take care of yourself";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(REMINDER_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void onReminderSettingChanged(boolean enabled){
        if (enabled){
            // Add permission check before scheduling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (!alarmManager.canScheduleExactAlarms()) {
                    // Guide user to settings
                    // Replaced AlertDialog with CustomMessageDialogFragment
                    CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                            "Permission Needed",
                            "To ensure your reminders are delivered on time, please allow the app to schedule exact alarms.",
                            "Go to Settings",
                            "Cancel"
                    );
                    dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                        @Override
                        public void onDialogPositiveClick(DialogFragment dialogFragment) {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        }

                        @Override
                        public void onDialogNegativeClick(DialogFragment dialogFragment) {
                            dialogFragment.dismiss();
                        }
                    });
                    dialog.show(getSupportFragmentManager(), "PermissionDialog");
                } else {
                    scheduleReminder(this);
                }
            } else {
                scheduleReminder(this);
            }
        } else {
            cancelAllReminders(this);
        }
    }

    public void addSentNotification(String message){
        SharedPreferences prefs = getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_RECENT_NOTIFICATIONS, "[]");
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> notifications = gson.fromJson(json, type);

        if (notifications == null){
            notifications = new ArrayList<>();
        }

        String timeStamp = android.text.format.DateFormat.format("MMM dd, hh:mm a", System.currentTimeMillis()).toString();
        notifications.add(0, timeStamp + " : " + message);

        while (notifications.size() > 5){
            notifications.remove(notifications.size() -1);
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_RECENT_NOTIFICATIONS, gson.toJson(notifications));
        editor.apply();
    }

    public void updateRecentlySentNotificationsDisplay(){
        if (recyclerViewNotifications == null || emptyNotificationsTextView == null) {
            Log.e(TAG, "Notification display RecyclerView or TextView are null.");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_RECENT_NOTIFICATIONS, "[]");
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> notifications = gson.fromJson(json, type);

        if (notifications == null) {
            notifications = new ArrayList<>();
        }

        if (notifications.isEmpty()){
            recyclerViewNotifications.setVisibility(View.GONE);
            emptyNotificationsTextView.setVisibility(View.VISIBLE);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean reminderEnabled = sharedPreferences.getBoolean("reminder_enabled", false);
            if (reminderEnabled) {
                emptyNotificationsTextView.setText("No Notifications Sent Yet");
            } else {
                emptyNotificationsTextView.setText("Enable Notifications In Settings To Recieve Notifications");
            }
        }else {
            recyclerViewNotifications.setVisibility(View.VISIBLE);
            emptyNotificationsTextView.setVisibility(View.GONE);
            notificationAdapter.updateData(notifications);
        }
    }

    public void loadBottomSettingsFragment() {
        if (bottomSheetBehavior == null) {
            Log.e(TAG, "loadBottomSettingsFragment: bottomSheetBehavior is null. Cannot load settings fragment.");
            return;
        }
        loadBottomFragment(new SettingsFragment());
        Handler handler = new Handler();
        handler.postDelayed(()->{
            bottomSheetBehavior.setState(STATE_EXPANDED);
            if (Fab != null && Fab.getVisibility() != View.GONE) {
                Fab.setVisibility(View.GONE);
            }
        },50);
        Log.d(TAG, "loadBottomSettingsFragment: Settings fragment loaded and bottom sheet expanded.");
    }
    public void saveNameToLocalStorage(String name) {
        SharedPreferences sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_name", name);
        editor.apply();
    }
    private  void setStatusBarColor(int colorResId){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this,colorResId));
        }
    }
    public void shakeView(View view) {
        if (Fab != null && Fab.getVisibility() != View.GONE) {
            float startTranslationY = getResources().getDimensionPixelSize(R.dimen.fab_slide_up_distance);
            ObjectAnimator slideUp = ObjectAnimator.ofFloat(view, "translationY", startTranslationY, 0f);
            slideUp.setDuration(400);
            slideUp.setInterpolator(new AccelerateDecelerateInterpolator());

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(slideUp);
            animatorSet.start();
        }
    }
    public void invertShakeView(View view) {
        if (view != null && view.getVisibility() == View.VISIBLE) {
            float endTranslationY = getResources().getDimensionPixelSize(R.dimen.fab_slide_up_distance);
            ObjectAnimator slideDown = ObjectAnimator.ofFloat(view, "translationY", 0f, endTranslationY);
            slideDown.setDuration(300);
            slideDown.setInterpolator(new AccelerateDecelerateInterpolator());

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(slideDown);
            animatorSet.start();

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.GONE);
                }
            });
        }
    }
    private void showWelcomeDialogStep(int step) {
        if (step < 0 || step >= WelcomeMessages.length) {
            Log.w(TAG, "showWelcomeDialogStep: Step " + step + " is out of bounds.");
            return;
        }
        currentWelcomeDialogStep = step;

        String positiveButtonText;
        String negativeButtonText = null;

        if (currentWelcomeDialogStep < WelcomeMessages.length - 1) {
            positiveButtonText = "Next";
        } else {
            positiveButtonText = "Start";
        }

        if (currentWelcomeDialogStep > 0) {
            negativeButtonText = "Previous";
        }

        Log.d(TAG, "showWelcomeDialogStep: Showing CustomMessageDialogFragment for welcome step " + step);
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                WELCOME_TITLES[currentWelcomeDialogStep],
                WelcomeMessages[currentWelcomeDialogStep],
                positiveButtonText,
                negativeButtonText
        );

        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                if (currentWelcomeDialogStep < WelcomeMessages.length - 1) {
                    dialogFragment.dismiss();
                    showWelcomeDialogStep(currentWelcomeDialogStep + 1);
                } else {
                    // This is the "Start" button
                    dialogFragment.dismiss();

                    SharedPreferences.Editor editor = settingse.edit();
                    editor.putBoolean(FIRST_LAUNCH_KEY, false);
                    editor.apply();

                    long initialRelapseTime = startRelapseCounter();

                    HomeFragment homeFragment = new HomeFragment();
                    Bundle args = new Bundle();
                    args.putLong(KEY_LAST_RELAPSE_DATE, initialRelapseTime);
                    homeFragment.setArguments(args);

                    loadFragment(homeFragment, R.id.nav_home);
                    if (navigationView != null) {
                        navigationView.setCheckedItem(R.id.nav_home);
                    }
                    if (toolbar != null) {
                        toolbar.setTitle("Heal");
                    }
                    Log.d(TAG, "MainActivity: welcomeMessage - Loading HomeFragment after timer start (final step).");
                }
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                if (currentWelcomeDialogStep > 0) {
                    dialogFragment.dismiss();
                    showWelcomeDialogStep(currentWelcomeDialogStep - 1);
                } else {
                    // This case should ideally not be reached if negative button is null for step 0
                    dialogFragment.dismiss();
                }
            }
        });
        dialog.setCancelable(false); // Make welcome dialogs non-cancelable by outside touch
        dialog.show(getSupportFragmentManager(), "WelcomeDialogStep" + currentWelcomeDialogStep);
    }

    @Override
    public void onDrawingSaved(String imageUri, String artworkName) {

    }
}
