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
import android.app.KeyguardManager;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.format.DateFormat;
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
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import drawing.DrawingCanvasFragment;
import funcorner.MemoryMatchGameFragment;
import funcorner.PaintFragment;
import funcorner.SudokuGameFragment;
import funcorner.TetrisGameFragment;
import funcorner.TicTacToeGameFragment;
import funcorner.WordScrambleGameFragment;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import records.AddEditContactDialogFragment;
import records.AudioLogsFragment;
import records.CopingExercisesFragment;
import records.EmergencyContact;
import records.EmergencyContactsFragment;
import records.JournalEntriesFragment;
import records.MoodCheckinFragment;
import records.RelapseHistoryFragment;
import records.SavedStrategiesFragment;
import retrofit2.Call;
import retrofit2.Response;
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
        AddEditContactDialogFragment.OnContactSavedListener,DrawingCanvasFragment.OnDrawingSavedListener,TypeToConfirmDialogFragment.OnTypeConfirmListener   {

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
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private ActivityResultLauncher<String[]> pickMultipleImagesLauncher;
    private ExecutorService executorService;
    private Handler mainHandler;
    private ArrayList<Uri> attachedImageUris = new ArrayList<>();
    private int currentNavId = R.id.nav_home;
    private static final String FIRST_LAUNCH_KEY = "firstLaunch";
    // New key to track if the app crashed previously
    private static final String KEY_CRASHED_PREVIOUSLY = "crashedPreviously";
    private LinearLayout imagesContainer;


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

    // Constants for custom reminders
    public static final String PREF_CUSTOM_REMINDER_TIMES = "custom_reminder_times";
    public static final String PREF_ACTIVE_REMINDER_REQUEST_CODES = "active_reminder_request_codes";


    private ScrollView recentlySentNotificationsScrollView;

    public SharedPreferences sharedPreferences;

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
    }

    private GeneralViewModel generalViewModel;
    String targetGameName = "";

    public final Map<String, Integer> themeBackgrounds = new HashMap<>();
    private CoordinatorLayout mainLayout;

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
        // Find the main layout after setting the content view
        mainLayout = findViewById(R.id.main_coordinator_layout);
        // Populate the themeBackgrounds map
        themeBackgrounds.put("pink", R.drawable.bg_pink);
        themeBackgrounds.put("blue", R.drawable.bg_blue);
        themeBackgrounds.put("green", R.drawable.bg_green);
        themeBackgrounds.put("purple", R.drawable.bg_purple);
        themeBackgrounds.put("orange", R.drawable.bg_orange);
        themeBackgrounds.put("teal", R.drawable.bg_teal);
        themeBackgrounds.put("brown", R.drawable.bg_brown);

        applySavedTheme();
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
                    initiateAccountDeletionProcess();
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



        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        pickMediaLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                attachedImageUris.clear();
                attachedImageUris.add(uri);
                updateAttachedImagesDisplay();
            }
        });
        pickMultipleImagesLauncher = registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
            if (uris != null && !uris.isEmpty()) {
                // Check for the maximum number of images
                if (uris.size() > 10) {
                    Toast.makeText(this, "You can only attach a maximum of 10 images.", Toast.LENGTH_SHORT).show();
                    return; // Exit without adding images
                }
                attachedImageUris.clear();
                attachedImageUris.addAll(uris);
                updateAttachedImagesDisplay();
            }
        });

    }

    private static final String[] CONFIRMATION_PHRASES = {
            "permanently delete my account",
            "erase all my data now",
            "destroy my account completely",
            "delete this account and all info",
            "confirm permanent account removal",
            "proceed with account termination",
            "I understand all data will be lost",
            "yes, delete everything",
            "I want to close my account",
            "delete my user profile and data",
            "remove my account and history",
            "permanently remove my account",
            "I accept the deletion of my data",
            "confirm account data deletion",
            "delete my personal information",
            "this action cannot be undone",
            "close my account permanently",
            "erase all application data",
            "I wish to delete my account",
            "perform permanent deletion",
            "delete my entire account",
            "delete my account and all data",
            "remove all my information",
            "proceed with deletion",
            "confirm to delete my account",
            "I am sure I want to delete",
            "delete my data forever",
            "I want to erase my account",
            "terminate my account",
            "I approve account deletion",
            "erase my app data",
            "permanently delete my profile",
            "delete everything associated with this account",
            "I am certain I want to delete",
            "account deletion confirmed",
            "erase my account now",
            "yes, I want to delete",
            "confirm the deletion of my account",
            "delete my account permanently",
            "remove my account and data",
            "I am ready to delete my account",
            "clear all my data",
            "I wish to delete my profile",
            "proceed to delete my account",
            "delete my user account",
            "confirm deletion",
            "I want to permanently delete",
            "erase all data",
            "confirm this account deletion",
            "delete my account"
    };

    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 123;
    private boolean waitingForDeviceAuthThenTextConfirm = false;
    private String currentChallengeText; // To store the selected phrase
    // =================================================================================
    // MARK: - ACCOUNT DELETION SYSTEM
    // =================================================================================

    /**
     * Step 1: Starts the account deletion flow.
     * Checks if the device is secure and proceeds to the appropriate confirmation step.
     */
    private void initiateAccountDeletionProcess() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        // Randomly select the confirmation phrase at the start of the flow
        currentChallengeText = CONFIRMATION_PHRASES[new Random().nextInt(CONFIRMATION_PHRASES.length)];

        if (keyguardManager == null || !keyguardManager.isDeviceSecure()) {
            // If device is not secure, skip to the text confirmation step with a warning.
            Log.w(TAG, "Device is not secure. Proceeding directly to text confirmation.");
            Toast.makeText(this, "Device screen lock not set. Please be extra careful.", Toast.LENGTH_LONG).show();
            showTypeToConfirmDeletionDialog(currentChallengeText); // Use the randomized phrase
            return;
        }

        // Device is secure, so create an intent to confirm device credentials.
        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Confirm Account Deletion",
                "Authenticate to permanently erase all app data."
        );

        if (intent != null) {
            waitingForDeviceAuthThenTextConfirm = true; // Set flag before starting activity
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        } else {
            // Fallback in the rare case the intent could not be created.
            Log.e(TAG, "createConfirmDeviceCredentialIntent returned null, despite device being secure.");
            Toast.makeText(this, "Could not initiate device authentication. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Step 2: Handles the result from the device credential confirmation.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (waitingForDeviceAuthThenTextConfirm) {
                waitingForDeviceAuthThenTextConfirm = false; // Reset flag

                if (resultCode == RESULT_OK) {
                    // Authentication successful, proceed to the final text confirmation.
                    Log.i(TAG, "Device authentication successful for account deletion.");
                    // Use the randomized challenge text here
                    showTypeToConfirmDeletionDialog(currentChallengeText);
                } else {
                    // Authentication failed or was cancelled.
                    Log.w(TAG, "Device authentication failed or cancelled.");
                    Toast.makeText(this, "Authentication failed. Account deletion cancelled.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Step 3: Shows the dialog requiring the user to type a confirmation phrase.
     */
    private void showTypeToConfirmDeletionDialog(String textToType) {
        TypeToConfirmDialogFragment typeDialog = TypeToConfirmDialogFragment.newInstance(
                "Final Deletion Confirmation",
                "To permanently delete your account and all data, please type the following phrase exactly:",
                textToType,
                "Delete My Account Now",
                "Cancel"
        );

        if (!isFinishing() && !isDestroyed()) {
            typeDialog.show(getSupportFragmentManager(), "TypeToConfirmDeletionDialog");
        } else {
            Log.w(TAG, "Activity is finishing, cannot show TypeToConfirmDeletionDialog.");
        }
    }

    /**
     * Step 4 (Callback): Called when the user successfully types the confirmation text.
     * This now shows a final confirmation dialog before deleting.
     */
    @Override
    public void onTextConfirmed() {
        Log.i(TAG, "Text confirmation successful. Showing final warning before deletion.");

        // Create and show the final confirmation dialog
        CustomMessageDialogFragment finalDialog = CustomMessageDialogFragment.newInstance(
                "Final Warning",
                "This action is permanent and cannot be undone. Are you absolutely sure you want to delete your account and all data?",
                "Delete Forever",
                "Cancel"
        );

        finalDialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                // User confirmed the final warning, now proceed with deletion
                Toast.makeText(MainActivity.this, "Verification complete. Deleting account...", Toast.LENGTH_SHORT).show();
                performAccountDeletion();
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                // User cancelled at the very last moment
                onTextConfirmationCancelled(); // Reuse the existing cancel logic
            }
        });

        finalDialog.show(getSupportFragmentManager(), "FinalDeleteConfirmationDialog");
    }

    /**
     * Step 4 (Callback): Called when the user cancels the text confirmation dialog.
     */
    @Override
    public void onTextConfirmationCancelled() {
        Log.w(TAG, "Text confirmation step was cancelled.");
        Toast.makeText(MainActivity.this, "Account deletion cancelled.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Step 5: The final, destructive action. Clears all app data and restarts the app.
     * This version uses a hybrid approach to ensure all SharedPreferences are cleared.
     */
    private void performAccountDeletion() {
        Log.i(TAG, "PERFORMING ACCOUNT DELETION: Clearing all SharedPreferences...");

        // --- Merged Logic ---

        // 1. (Your Method) General cleanup attempt via file iteration.
        // This will catch any unknown or dynamically created preference files.
        File prefsDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
        } else {
            prefsDir = new File(getFilesDir().getParentFile(), "shared_prefs");
        }

        if (prefsDir.exists() && prefsDir.isDirectory()) {
            String[] prefFileNames = prefsDir.list();
            if (prefFileNames != null) {
                for (String fileName : prefFileNames) {
                    // We use commit() here for a synchronous, blocking call to be safer during shutdown.
                    getSharedPreferences(fileName.replace(".xml", ""), MODE_PRIVATE).edit().clear().commit();
                }
                Log.i(TAG, "Completed general cleanup of " + prefFileNames.length + " preference files.");
            }
        }

        // 2. (New Method) Explicitly clear all critical SharedPreferences files.
        // This provides a reliable, guaranteed cleanup for known data, overcoming caching issues.
        getSharedPreferences(PREFS_RELAPSE, MODE_PRIVATE).edit().clear().commit();
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().commit(); // For Emergency Contacts
        getSharedPreferences(PREFS_RECENTLY_VISITED, MODE_PRIVATE).edit().clear().commit(); // For Recently Visited
        getSharedPreferences(PREFS_NOTIFICATIONS, MODE_PRIVATE).edit().clear().commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit(); // For Settings
        fragmentHistoryMap.clear();
        updateRecentlyVisitedChips();

        Log.i(TAG, "All critical SharedPreferences have been explicitly cleared.");

        // --- End of Merged Logic ---

        // Cancel all scheduled reminders
        cancelAllReminders(this);

        // Notify user and restart the app in a clean state
        Toast.makeText(this, "Account and all data deleted successfully.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity(); // Close all activities in the current task
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the activity's intent
        handleWidgetIntent(intent); // Handle intents when activity is already running
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent != null && "ACTION_LAUNCH_GAME".equals(intent.getAction())) {
            int gameFragmentId = intent.getIntExtra("game_fragment_id", -1);
            if (gameFragmentId == -1) return;

            FragmentManager fm = getSupportFragmentManager();
            Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);

            // Determine the target game's name for messages
            String targetGameName = "";
            if (gameFragmentId == R.id.nav_tetris) targetGameName = "Tetris";
            else if (gameFragmentId == R.id.nav_memory_match) targetGameName = "Memory Match";
            else if (gameFragmentId == R.id.nav_word_scramble) targetGameName = "Word Scramble";
            else if (gameFragmentId == R.id.nav_paint) targetGameName = "Paint";
            else if (gameFragmentId == R.id.nav_sudoku) targetGameName = "Sudoku";
            else if (gameFragmentId == R.id.nav_tic_tac_toe) targetGameName = "Tic-Tac-Toe";


            // Check if the user is already in the game they're trying to launch
            // Check if the user is already in the game they're trying to launch
            if (currentFragment != null) {
                boolean isAlreadyInTargetGame =
                        (gameFragmentId == R.id.nav_tetris && currentFragment instanceof TetrisGameFragment) ||
                                (gameFragmentId == R.id.nav_memory_match && currentFragment instanceof MemoryMatchGameFragment) ||
                                (gameFragmentId == R.id.nav_word_scramble && currentFragment instanceof WordScrambleGameFragment) ||
                                (gameFragmentId == R.id.nav_paint && (currentFragment instanceof PaintFragment || currentFragment instanceof DrawingCanvasFragment)) ||
                                (gameFragmentId == R.id.nav_tic_tac_toe && currentFragment instanceof TicTacToeGameFragment) ||
                                (gameFragmentId == R.id.nav_sudoku && currentFragment instanceof SudokuGameFragment);

                if (isAlreadyInTargetGame) {
                    Toast.makeText(this, "You are already in " + targetGameName + ".", Toast.LENGTH_SHORT).show();
                    return; // Exit the method to prevent reloading
                }
            }

            // Check if the user is in a different game and might lose progress
            if (currentFragment instanceof TetrisGameFragment ||
                    currentFragment instanceof MemoryMatchGameFragment ||
                    currentFragment instanceof WordScrambleGameFragment ||
                    currentFragment instanceof PaintFragment ||
                    currentFragment instanceof DrawingCanvasFragment ||
                    currentFragment instanceof TicTacToeGameFragment ||
                    currentFragment instanceof SudokuGameFragment) {

                String currentGameName = "the current game"; // Default name
                if (currentFragment instanceof TetrisGameFragment) currentGameName = "Tetris";
                else if (currentFragment instanceof MemoryMatchGameFragment) currentGameName = "Memory Match";
                else if (currentFragment instanceof WordScrambleGameFragment) currentGameName = "Word Scramble";
                else if (currentFragment instanceof PaintFragment || currentFragment instanceof DrawingCanvasFragment) currentGameName = "the Paint canvas";
                else if (currentFragment instanceof TicTacToeGameFragment) currentGameName = "Tic-Tac-Toe";
                else if (currentFragment instanceof SudokuGameFragment) currentGameName = "Sudoku";

                CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                        "Exit " + currentGameName + "?",
                        "Are you sure you want to quit your current game to start " + targetGameName + "?",
                        "Leave",
                        "Cancel"
                );
                dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                    @Override
                    public void onDialogPositiveClick(DialogFragment dialogFragment) {
                        // User confirmed, now launch the new game
                        launchGameFromWidget(gameFragmentId);
                    }

                    @Override
                    public void onDialogNegativeClick(DialogFragment dialogFragment) {
                        // User cancelled, do nothing
                    }
                });
                dialog.show(getSupportFragmentManager(), "WidgetSwitchGameDialog");
                return; // Wait for the user's response from the dialog
            }

            // If not in any game, or if the dialog was confirmed, launch the game directly.
            launchGameFromWidget(gameFragmentId);

        } else if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            // Default loading for regular app launch if no specific widget intent and no fragment is loaded
            loadFragment(new HomeFragment(), R.id.nav_home);
            navigationView.setCheckedItem(R.id.nav_home);
            toolbar.setTitle("Heal");
            Log.d(TAG, "MainActivity: handleWidgetIntent - Loading HomeFragment (regular launch).");
        }
    }

    // Helper method to launch a game from a widget
    private void launchGameFromWidget(int gameFragmentId) {
        Fragment targetFragment = null;
        String toolbarTitle = "";
        loadFragment(new HomeFragment(), R.id.nav_home);
        // Close any open bottom sheet or popup
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == STATE_EXPANDED) {
            bottomSheetBehavior.setState(STATE_HIDDEN);
            clearBottomFragment();
        }
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }

        // Determine which fragment to load
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
        } else if (gameFragmentId == R.id.nav_sudoku) {
            targetFragment = new SudokuGameFragment();
            toolbarTitle = "Sudoku";
        } else if (gameFragmentId == R.id.nav_tic_tac_toe) {
            targetFragment = new TicTacToeGameFragment();
            toolbarTitle = "Tic-Tac-Toe";
        }

        if (targetFragment != null) {
            // Clear the entire back stack to ensure a clean navigation path from the widget
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            loadFragment(targetFragment, gameFragmentId);
            toolbar.setTitle(toolbarTitle);
            // Set the navigation drawer item to "Fun Corner"
            navigationView.setCheckedItem(R.id.nav_fun_corner);
            addFragmentToHistory(gameFragmentId, toolbarTitle);
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
        if (executorService != null) {
            executorService.shutdown();
        }

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
                                clickedNavId == R.id.nav_saved_strategies || clickedNavId == R.id.nav_relapse_history || clickedNavId == R.id.nav_audio_logs ||
                                clickedNavId == R.id.nav_word_scramble || clickedNavId == R.id.nav_tetris || clickedNavId == R.id.nav_memory_match || clickedNavId == R.id.nav_paint || clickedNavId == R.id.nav_sudoku || clickedNavId == R.id.nav_tic_tac_toe){
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

        // Get the current fragment
        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);

        // Check if the navId is for a game fragment
        boolean isGameFragment = (navId == R.id.nav_word_scramble || navId == R.id.nav_tetris ||
                navId == R.id.nav_memory_match || navId == R.id.nav_paint || navId == R.id.nav_sudoku || navId == R.id.nav_tic_tac_toe);

        if (isGameFragment) {
            // Determine the target game's name for messages
            if (navId == R.id.nav_tetris) targetGameName = "Tetris";
            else if (navId == R.id.nav_memory_match) targetGameName = "Memory Match";
            else if (navId == R.id.nav_word_scramble) targetGameName = "Word Scramble";
            else if (navId == R.id.nav_paint) targetGameName = "Paint";
            else if (navId == R.id.nav_sudoku) targetGameName = "Sudoku";
            else if (navId == R.id.nav_tic_tac_toe) targetGameName = "Tic-Tac-Toe";

            // Check if the user is already in the target game
            boolean isAlreadyInTargetGame =
                    (navId == R.id.nav_tetris && currentFragment instanceof TetrisGameFragment) ||
                            (navId == R.id.nav_memory_match && currentFragment instanceof MemoryMatchGameFragment) ||
                            (navId == R.id.nav_word_scramble && currentFragment instanceof WordScrambleGameFragment) ||
                            (navId == R.id.nav_paint && (currentFragment instanceof PaintFragment || currentFragment instanceof DrawingCanvasFragment)) ||
                            ( navId == R.id.nav_tic_tac_toe && currentFragment instanceof TicTacToeGameFragment) ||
                            ( navId == R.id.nav_sudoku && currentFragment instanceof SudokuGameFragment);

            if (isAlreadyInTargetGame) {
                Toast.makeText(this, "You are already in " + targetGameName + ".", Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawer(GravityCompat.START); // Close drawer if open
                return; // Prevent reloading the same game
            }

            // Check if the user is in a different game and might lose progress
            if (currentFragment instanceof TetrisGameFragment ||
                    currentFragment instanceof MemoryMatchGameFragment ||
                    currentFragment instanceof WordScrambleGameFragment ||
                    currentFragment instanceof PaintFragment ||
                    currentFragment instanceof DrawingCanvasFragment ||
                   currentFragment instanceof TicTacToeGameFragment ||
                    currentFragment instanceof SudokuGameFragment) {

                String currentGameName = "the current game";
                if (currentFragment instanceof TetrisGameFragment) currentGameName = "Tetris";
                else if (currentFragment instanceof MemoryMatchGameFragment) currentGameName = "Memory Match";
                else if (currentFragment instanceof WordScrambleGameFragment) currentGameName = "Word Scramble";
                else if (currentFragment instanceof PaintFragment || currentFragment instanceof DrawingCanvasFragment) currentGameName = "the Paint canvas";
                else if (currentFragment instanceof TicTacToeGameFragment) currentGameName = "Tic-Tac-Toe";
                else if (currentFragment instanceof SudokuGameFragment) currentGameName = "Sudoku";

                CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                        "Exit " + currentGameName + "?",
                        "Are you sure you want to quit your current game to start " + targetGameName + "?",
                        "Leave",
                        "Cancel"
                );
                dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                    @Override
                    public void onDialogPositiveClick(DialogFragment dialogFragment) {
                        // User confirmed, now launch the new game
                        dialogFragment.dismiss();
                        performChipFragmentLoad(navId, targetGameName); // Call helper to load
                    }

                    @Override
                    public void onDialogNegativeClick(DialogFragment dialogFragment) {
                        // User cancelled, do nothing, keep the drawer open or close as per original flow
                        dialogFragment.dismiss();
                        // Optionally, reset the navigation view checked item to the current game's category
                        navigationView.setCheckedItem(R.id.nav_fun_corner);
                        drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer if they cancelled
                    }
                });
                dialog.show(getSupportFragmentManager(), "ChipSwitchGameDialog");
                return; // Wait for the user's response from the dialog
            }
        }

        // If not a game fragment, or if the game switch was confirmed, proceed normally
        performChipFragmentLoad(navId, null); // Pass null for game name as it's not a game switch scenario
    }

    // Helper method to perform the actual fragment loading after checks
    private void performChipFragmentLoad(int navId, String gameNameForTitle) {
        Fragment targetFragment = null;
        String toolbarTitle = null;

        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == STATE_EXPANDED){
            bottomSheetBehavior.setState(STATE_HIDDEN);
            clearBottomFragment();
        }

        // Assign target fragment and title based on navId
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
        }else if (navId == R.id.nav_audio_logs) {
            targetFragment = new AudioLogsFragment();
            toolbarTitle = "My Audio Logs";
            navigationView.setCheckedItem(R.id.nav_records);
        }else if (navId == R.id.nav_word_scramble) {
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
        }else if (navId == R.id.nav_sudoku) {
            targetFragment = new SudokuGameFragment();
            toolbarTitle = "Sudoku";
            navigationView.setCheckedItem(R.id.nav_fun_corner);
        }else if (navId == R.id.nav_tic_tac_toe) {
            targetFragment = new TicTacToeGameFragment();
            toolbarTitle = "Tic-Tac-Toe";
            navigationView.setCheckedItem(R.id.nav_fun_corner);
        }
        // Use the provided gameNameForTitle if it's a game switch, otherwise use the default toolbarTitle
        String finalToolbarTitle = (gameNameForTitle != null) ? gameNameForTitle : toolbarTitle;

        if (targetFragment != null){
            // Clear the back stack if navigating to a game from a chip, to ensure clean state
            if (navId == R.id.nav_word_scramble || navId == R.id.nav_tetris ||
                    navId == R.id.nav_memory_match || navId == R.id.nav_paint
            || navId == R.id.nav_sudoku || navId == R.id.nav_tic_tac_toe) {
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            loadFragment(targetFragment, navId);
            toolbar.setTitle(finalToolbarTitle);
            drawerLayout.closeDrawer(GravityCompat.START);
            addFragmentToHistory(navId, finalToolbarTitle); // Add to history after loading
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
                    "Are you sure you want to quit your drawing?", // Unified message
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
        }else if (currentFragment instanceof SudokuGameFragment){
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit Sudoku",
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
            dialog.show(getSupportFragmentManager(), "ExitSudokuDialog");
        }else if (currentFragment instanceof TicTacToeGameFragment){
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit TicTacToe",
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
            dialog.show(getSupportFragmentManager(), "ExitTicTacToeDialog");
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
                    "Are you sure you want to quit your game?", // Unified message
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
        }else if (currentFragment instanceof SudokuGameFragment) {
            drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer immediately

            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit Sudoku",
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
            dialog.show(getSupportFragmentManager(), "NavExitSudokuDialog");

            return true; // The event is handled by showing the dialog
        }else if (currentFragment instanceof TicTacToeGameFragment) {
            drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer immediately

            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit TicTacToe",
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
            dialog.show(getSupportFragmentManager(), "NavExitTicTacToeDialog");

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
        } else if (id == R.id.nav_bug_report) {
            toolbarTitle = "Heal";
            if (!(currentFragment instanceof HomeFragment)) {
                targetFragment = new HomeFragment();
                shouldLoadFragment = true;
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(this::showBugReportPopup, 2500);
            } else {
                showBugReportPopup();
            }
        } else if (id == R.id.nav_suggestion) {
            toolbarTitle = "Heal";
            if (!(currentFragment instanceof HomeFragment)) {
                targetFragment = new HomeFragment();
                shouldLoadFragment = true;
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(this::showSuggestionPopup, 2500);
            } else {
                showSuggestionPopup();
            }
        }else if (id == R.id.nav_share) {
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

    // START: JIRA BUG REPORTING METHODS
    private void showBugReportPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.bug_report_window, null);
        popupWindow = new PopupWindow(
                popupView,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                true);

        popupWindow.showAtLocation(drawerLayout, Gravity.CENTER, 0, 0);
        popupWindow.setFocusable(true);

        if (overlayView.getVisibility() == View.GONE) {
            setStatusBarColor(R.color.status_bar_overlay_dark);
            overlayView.setVisibility(View.VISIBLE);
        }

        final EditText summaryEditText = popupView.findViewById(R.id.bug_report_summary_edit_text);
        final EditText descriptionEditText = popupView.findViewById(R.id.bug_report_description_edit_text);
        Button attachImageButton = popupView.findViewById(R.id.bug_report_attach_image_button);
        Button submitButton = popupView.findViewById(R.id.bug_report_submit_button);
        Button cancelButton = popupView.findViewById(R.id.bug_report_cancel_button);
        imagesContainer = popupView.findViewById(R.id.bug_report_images_container);
        HorizontalScrollView imagesScrollView = popupView.findViewById(R.id.bug_report_images_scroll_view);

        popupWindow.setOnDismissListener(() ->{
            if (overlayView.getVisibility() == View.VISIBLE) {
                setStatusBarColor(R.color.transparent);
                overlayView.setVisibility(View.GONE);
            }
        });

        cancelButton.setOnClickListener(v -> {
            attachedImageUris.clear();
            popupWindow.dismiss();
            if (overlayView.getVisibility() == View.VISIBLE) {
                setStatusBarColor(R.color.transparent);
                overlayView.setVisibility(View.GONE);
            }
        });
        submitButton.setOnClickListener(v -> {
            String summary = summaryEditText.getText().toString();
            String description = descriptionEditText.getText().toString();

            if (summary.isEmpty()) {
                Toast.makeText(this, "Please enter a summary for the bug report.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Submitting bug report...", Toast.LENGTH_LONG).show();

                // 1. Create a copy of the list for the background thread.
                ArrayList<Uri> urisToUpload = new ArrayList<>(attachedImageUris);

                // 2. Clear the original list for the next time the popup is shown.
                attachedImageUris.clear();

                popupWindow.dismiss();
                if (overlayView.getVisibility() == View.VISIBLE) {
                    setStatusBarColor(R.color.transparent);
                    overlayView.setVisibility(View.GONE);
                }

                // 3. Pass the COPY to the background thread.
                submitBugToJiraOnBackgroundThread(summary, description, urisToUpload);
            }
        });
//        popupWindow.setOnDismissListener(() -> attachedImageUris.clear());

        attachImageButton.setOnClickListener(v -> {
            if (attachedImageUris.size() >= 10) {
                Toast.makeText(this, "Maximum of 10 images already attached.", Toast.LENGTH_SHORT).show();
            } else {
                pickMultipleImagesLauncher.launch(new String[]{"image/*"});
            }
        });

        updateAttachedImagesDisplay();
    }

    private void updateAttachedImagesDisplay() {
        if (imagesContainer != null) {
            imagesContainer.removeAllViews(); // Clear previous views
            for (Uri uri : attachedImageUris) {
                addImagePreview(uri); // Add each image preview
            }
        }
    }

    private void addImagePreview(Uri imageUri) {
        // Retrieve dimension values from resources
        int imageSize = getResources().getDimensionPixelSize(R.dimen.attached_image_preview_size);
        int imageMargin = getResources().getDimensionPixelSize(R.dimen.attached_image_preview_margin);

        // Create a new ImageView
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                imageSize,  // Use the defined size for width
                imageSize   // Use the defined size for height
        );
        params.setMargins(0, 0, imageMargin, 0); // Use the defined margin for spacing
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Load the image into the ImageView using Glide or a similar library
        Glide.with(this)
                .load(imageUri)
                .into(imageView);

        // Add the ImageView to the container
        imagesContainer.addView(imageView);
    }

    private void submitBugToJiraOnBackgroundThread(String summary, String description, ArrayList<Uri> attachedUris) {
        executorService.execute(() -> {
            try {
                JiraApiService apiService = RetrofitClient.getJiraApiService();
                String authHeader = RetrofitClient.getAuthHeader();

                // Explicitly set the bug report project key
                String jiraProjectKey = "BUG";

                // 2. Create the Jira issue request
                Project project = new Project(jiraProjectKey);
                IssueType issueType = new IssueType("Bug");
                Fields fields = new Fields(project, summary, description, issueType);
                JiraIssueRequest request = new JiraIssueRequest(fields);

                // 3. Make the API call to create the issue
                Call<JiraIssueResponse> call = apiService.createIssue(authHeader, request);
                Response<JiraIssueResponse> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    JiraIssueResponse issueResponse = response.body();
                    String issueKey = issueResponse.getKey();
                    mainHandler.post(() -> Toast.makeText(this, "Bug report submitted successfully!", Toast.LENGTH_LONG).show());

                    // 4. If there are attachments, upload them
                    if (!attachedUris.isEmpty()) {
                        mainHandler.post(() -> Toast.makeText(this, "Uploading attachments...", Toast.LENGTH_SHORT).show());
                        for (Uri imageUri : attachedUris) {
                            uploadAttachmentToJiraOnBackgroundThread(issueKey, imageUri);
                        }
                    }
                } else {
                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                    Log.e(TAG, "Failed to create Jira issue: " + errorBody);
                    mainHandler.post(() -> Toast.makeText(this, "Failed to submit bug report. " + response.code(), Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while submitting bug report", e);
                mainHandler.post(() -> Toast.makeText(this, "An error occurred while submitting. Check Your Internet Connection.", Toast.LENGTH_LONG).show());
            }
        });
    }
    private void uploadAttachmentToJiraOnBackgroundThread(String issueKey, Uri imageUri) {
        executorService.execute(() -> {
            try {
                JiraApiService apiService = RetrofitClient.getJiraApiService();
                String authHeader = RetrofitClient.getAuthHeader();

                String fileName = "attachment_" + System.currentTimeMillis() + ".jpg";
                File file = getFileFromUri(imageUri, fileName);

                if (file == null) {
                    mainHandler.post(() -> Toast.makeText(this, "Failed to read attachment file.", Toast.LENGTH_SHORT).show());
                    return;
                }

                RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
                MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), requestBody);

                Call<ResponseBody> call = apiService.addAttachment(authHeader, "no-check", issueKey, filePart);
                Response<ResponseBody> response = call.execute();

                file.delete(); // Clean up temp file

                if (response.isSuccessful()) {

                } else {
                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                    Log.e(TAG, "Failed to upload attachment: " + errorBody);
                    mainHandler.post(() -> Toast.makeText(this, "Failed to upload attachment: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while uploading attachment", e);
                mainHandler.post(() -> Toast.makeText(this, "An error occurred during attachment upload.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private File getFileFromUri(Uri uri, String fileName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
            fos.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create temp file from Uri", e);
            return null;
        }
    }
    // END: JIRA BUG REPORTING METHODS
    private void showSuggestionPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.suggestion_window, null);
        popupWindow = new PopupWindow(
                popupView,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.showAtLocation(drawerLayout, Gravity.CENTER, 0, 0);

        if (overlayView.getVisibility() == View.GONE) {
            setStatusBarColor(R.color.status_bar_overlay_dark);
            overlayView.setVisibility(View.VISIBLE);
        }

        // Update the title and icon for the Suggestion popup
        TextView titleTextView = popupView.findViewById(R.id.textViewBugReportTitle);
        titleTextView.setText("Suggestion");
        titleTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lightbulb, 0, 0, 0);

        final EditText summaryEditText = popupView.findViewById(R.id.bug_report_summary_edit_text);
        final EditText descriptionEditText = popupView.findViewById(R.id.bug_report_description_edit_text);
        Button attachImageButton = popupView.findViewById(R.id.bug_report_attach_image_button);
        Button submitButton = popupView.findViewById(R.id.bug_report_submit_button);
        Button cancelButton = popupView.findViewById(R.id.bug_report_cancel_button);
        imagesContainer = popupView.findViewById(R.id.bug_report_images_container);
        HorizontalScrollView imagesScrollView = popupView.findViewById(R.id.bug_report_images_scroll_view);

       popupWindow.setOnDismissListener(() ->{
           if (overlayView.getVisibility() == View.VISIBLE) {
               setStatusBarColor(R.color.transparent);
               overlayView.setVisibility(View.GONE);
           }
       });
        cancelButton.setOnClickListener(v -> {
            attachedImageUris.clear();
            popupWindow.dismiss();
            if (overlayView.getVisibility() == View.VISIBLE) {
                setStatusBarColor(R.color.transparent);
                overlayView.setVisibility(View.GONE);
            }
        });
        submitButton.setOnClickListener(v -> {
            String summary = summaryEditText.getText().toString();
            String description = descriptionEditText.getText().toString();

            if (summary.isEmpty()) {
                Toast.makeText(this, "Please enter a summary for the suggestion.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Submitting suggestion...", Toast.LENGTH_LONG).show();

                // 1. Create a copy of the list for the background thread.
                ArrayList<Uri> urisToUpload = new ArrayList<>(attachedImageUris);

                // 2. Clear the original list for the next time the popup is shown.
                attachedImageUris.clear();

                popupWindow.dismiss();
                if (overlayView.getVisibility() == View.VISIBLE) {
                    setStatusBarColor(R.color.transparent);
                    overlayView.setVisibility(View.GONE);
                }

                // 3. Pass the COPY to the background thread.
                submitSuggestionToJiraOnBackgroundThread(summary, description, urisToUpload);
            }
        });


        attachImageButton.setOnClickListener(v -> {
            if (attachedImageUris.size() >= 10) {
                Toast.makeText(this, "Maximum of 10 images already attached.", Toast.LENGTH_SHORT).show();
            } else {
                pickMultipleImagesLauncher.launch(new String[]{"image/*"});
            }
        });

        updateAttachedImagesDisplay();
    }
    private void submitSuggestionToJiraOnBackgroundThread(String summary, String description, ArrayList<Uri> attachedUris) {
        executorService.execute(() -> {
            try {
                JiraApiService apiService = RetrofitClient.getJiraApiService();
                String authHeader = RetrofitClient.getAuthHeader();

                // Explicitly set the suggestion project key
                String jiraProjectKey = "SUGS";

                // Use "Suggestion" as the issue type
                Project project = new Project(jiraProjectKey);
                IssueType issueType = new IssueType("Suggestions");
                Fields fields = new Fields(project, summary, description, issueType);
                JiraIssueRequest request = new JiraIssueRequest(fields);

                Call<JiraIssueResponse> call = apiService.createIssue(authHeader, request);
                Response<JiraIssueResponse> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    JiraIssueResponse issueResponse = response.body();
                    String issueKey = issueResponse.getKey();
                    mainHandler.post(() -> Toast.makeText(this, "Suggestion submitted successfully!", Toast.LENGTH_LONG).show());

                    if (!attachedUris.isEmpty()) {
                        mainHandler.post(() -> Toast.makeText(this, "Uploading attachments...", Toast.LENGTH_SHORT).show());
                        for (Uri imageUri : attachedUris) {
                            uploadAttachmentToJiraOnBackgroundThread(issueKey, imageUri);
                        }
                    }
                } else {
                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                    Log.e(TAG, "Failed to create Jira suggestion: " + errorBody);
                    mainHandler.post(() -> Toast.makeText(this, "Failed to submit suggestion. " + response.code(), Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while submitting suggestion", e);
                mainHandler.post(() -> Toast.makeText(this, "An error occurred while submitting. Check Your Internet Connection.", Toast.LENGTH_LONG).show());
            }
        });
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
        } else if (navId == R.id.nav_bug_report || navId == R.id.nav_share) {
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

    /**
     * Schedules reminders based on user preferences (custom or default times).
     * This method cancels all previously set reminders before scheduling new ones.
     * @param context The application context.
     */
    public static void scheduleReminders(Context context) {
        // First, cancel any previously scheduled alarms to prevent duplicates
        cancelAllReminders(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarms. Permission not granted.");
            // Cannot show a dialog from here, but the setting is checked in SettingsFragment.
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> customTimes = prefs.getStringSet(PREF_CUSTOM_REMINDER_TIMES, null);
        Set<String> activeRequestCodesStr = new HashSet<>();

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("com.f9ld3.heal.REMINDER_ALARM"); // Use a consistent action

        if (customTimes != null && !customTimes.isEmpty()) {
            // Schedule custom reminders
            for (String time : customTimes) {
                String[] parts = time.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                // Generate a unique request code based on time to avoid collisions
                int requestCode = hour * 100 + minute;

                scheduleAlarmForTime(context, alarmManager, intent, hour, minute, requestCode);
                activeRequestCodesStr.add(String.valueOf(requestCode));
            }
            Log.d(TAG, "Scheduled " + customTimes.size() + " custom reminders.");
        } else {
            // Schedule default reminders if no custom times are set
            int[][] defaultReminderTimes = {
                    {7, 0, REMINDER_NOTIFICATION_ID_7AM},
                    {11, 0, REMINDER_NOTIFICATION_ID_11AM},
                    {18, 0, REMINDER_NOTIFICATION_ID_6PM},
                    {21, 0, REMINDER_NOTIFICATION_ID_9PM}
            };

            for (int[] times : defaultReminderTimes) {
                scheduleAlarmForTime(context, alarmManager, intent, times[0], times[1], times[2]);
                activeRequestCodesStr.add(String.valueOf(times[2]));
            }
            Log.d(TAG, "Scheduled default reminders.");
        }

        // Save the new set of active request codes to SharedPreferences for future cancellation
        prefs.edit().putStringSet(PREF_ACTIVE_REMINDER_REQUEST_CODES, activeRequestCodesStr).apply();
    }

    /**
     * Helper method to set a single alarm at a specific time.
     */
    private static void scheduleAlarmForTime(Context context, AlarmManager alarmManager, Intent intent, int hour, int minute, int requestCode) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If the calculated time is in the past, schedule it for the next day
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.d(TAG, "Scheduled alarm for " + hour + ":" + String.format("%02d", minute) + " with request code " + requestCode);
    }


    /**
     * Cancels all currently scheduled reminders, both custom and default.
     * It reads the list of active request codes from SharedPreferences to know which alarms to target.
     * @param context The application context.
     */
    public static void cancelAllReminders(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("com.f9ld3.heal.REMINDER_ALARM");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> activeRequestCodesStr = prefs.getStringSet(PREF_ACTIVE_REMINDER_REQUEST_CODES, null);

        // Cancel alarms based on the saved request codes
        if (activeRequestCodesStr != null) {
            for (String codeStr : activeRequestCodesStr) {
                int requestCode = Integer.parseInt(codeStr);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.d(TAG, "Cancelled reminder with stored request code " + requestCode);
            }
            // Clear the stored request codes after cancelling them
            prefs.edit().remove(PREF_ACTIVE_REMINDER_REQUEST_CODES).apply();
        }

        // As a fallback, also attempt to cancel the default reminders in case state is inconsistent
        int[] defaultRequestCodes = {
                REMINDER_NOTIFICATION_ID_7AM,
                REMINDER_NOTIFICATION_ID_11AM,
                REMINDER_NOTIFICATION_ID_6PM,
                REMINDER_NOTIFICATION_ID_9PM
        };
        for (int requestCode : defaultRequestCodes) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.d(TAG, "Fallback cancelled default reminder with request code " + requestCode);
            }
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
                    scheduleReminders(this);
                }
            } else {
                scheduleReminders(this);
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

        String timeStamp = DateFormat.format("MMM dd, hh:mm a", System.currentTimeMillis()).toString();
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
    private void applySavedTheme() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedThemeColorName = sharedPreferences.getString("selected_theme_color", "orange");
        Integer drawableResId = themeBackgrounds.get(savedThemeColorName);
        CoordinatorLayout mainLayout = findViewById(R.id.main_coordinator_layout);
        if (drawableResId != null && mainLayout != null) {
            mainLayout.setBackgroundResource(drawableResId);
        }
    }
}
