package ui;

import static android.content.ContentValues.TAG;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.heal.MainActivity;
import com.example.heal.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.slider.Slider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import Slider.SliderOne;
import Slider.SliderThree;
import Slider.SliderTwo;
import records.CopingExercisesFragment;
import records.SavedStrategiesFragment;
import viewmodels.GeneralViewModel;


public class HomeFragment extends Fragment {

    private MainActivity mainActivity;

    private static final long AUTO_SCROLL_DELAY = 3000;
    private static final long COOLDOWN_DURATION = 24 * 60 * 60 * 1000; // 24 hours (not used anymore, but kept for consistency)
    private static final int MAX_STRATEGIES_DISPLAYED = 3;
    private static final String PREFS_MOOD = "mood_prefs";
    private static final String KEY_MOOD_ENTRIES = "mood_entries";
    private static final String KEY_LAST_CHECKIN = "last_checkin_time"; // Key to store last check-in time
    private static final String PREFS_JOURNAL = "journal_prefs";
    private static final String KEY_JOURNAL_ENTRIES = "journal_entries";


    private ProgressBar loadingProgressBar;
    public TextView greeting;
    private ImageView selfImprovementIcon;
    private ViewPager2 sliderViewPager;
    private TextView trackerTitle;
    private TextView stepsCounter;
    private Button logPositiveActionButton;
    private TextView urgeLevelLabel;
    private TextView urgeLevelDisplay;
    private Slider urgeLevelSeekbar;
    private TextView copingStrategiesLabel;
    private EditText copingStrategiesInput;
    private Button saveCopingStrategyButton;
    private GridLayout savedStrategiesContainer;
    private TextView savedStrategiesTitle;
    private List<String> allSavedStrategies = new ArrayList<>();
    private Button emergencyContactButton;
    private Button copingExercisesButton;
    private Button moreResourcesButton;
    private TextView dailyCheckinTitle;
    private TextView moodQuestion;
    private Slider moodSeekBar;
    private TextView moodValueLabel;
    private EditText moodInputText;
    private Button submitCheckinButton;
    private TextView groundingExerciseTitle;
    private TextView groundingExerciseDescription;
    private Button groundingExerciseButton;
    private TextView journalPromptTitle;
    private TextView journalPromptText;
    private EditText journalEntryText;
    private Button saveJournalButton;
    private TextView moodOverTimeTitle;
    private BarChart moodBarChart;
    private TextView moodOverTimeDescription;
    private Button seeMoreStrategiesButton;

    private Handler autoScrollHandler = new Handler();
    private Runnable autoScrollRunnable;
    private int currentPosition = 0;

    private List<MoodEntry> moodEntries = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();
    private Context context;
    private SliderAdapter sliderAdapter;
    private long lastCheckinTime = 0;
    private CountDownTimer checkinTimer;
    private List<JournalEntry> allJournalEntries = new ArrayList<>();
    private CoordinatorLayout homeCoordinatorLayout;
    private ScrollView homeScrollView;

    private TextView relapseCounterTextView;
    public static TextView relapseCounterTextView2;
    private Button resetRelapseButton;
    public static final String PREFS_RELAPSE = "RelapseCounterPrefs";
    public static final String KEY_LAST_RELAPSE_DATE = "lastRelapseDate";

    // Handler and Runnable for the relapse counter updates
    private Handler relapseCounterHandler;
    private Runnable relapseCounterRunnable;

    private boolean moodSeekBarTouched = false;


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
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity.toolbar.setTitle("Heal");
        mainActivity.navigationView.setCheckedItem(R.id.nav_home);
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        homeCoordinatorLayout = view.findViewById(R.id.home_coordinator_layout);
        greeting = view.findViewById(R.id.greeting);
        trackerTitle = view.findViewById(R.id.tracker_title);
        stepsCounter = view.findViewById(R.id.steps_counter);
        urgeLevelLabel = view.findViewById(R.id.urge_level_label);
        urgeLevelDisplay = view.findViewById(R.id.urge_level_display);
        sliderViewPager = view.findViewById(R.id.sliderViewPager);
        selfImprovementIcon = view.findViewById(R.id.self_improvement_icon);
        logPositiveActionButton = view.findViewById(R.id.add_step_button);
        emergencyContactButton = view.findViewById(R.id.emergency_contact_button);
        copingExercisesButton = view.findViewById(R.id.coping_exercises_button);
        moreResourcesButton = view.findViewById(R.id.more_resources_button);
        submitCheckinButton = view.findViewById(R.id.submit_checkin_button);
        groundingExerciseButton = view.findViewById(R.id.grounding_exercise_button);
        saveJournalButton = view.findViewById(R.id.save_journal_button);
        urgeLevelSeekbar = view.findViewById(R.id.urge_level_seekbar);
        copingStrategiesInput = view.findViewById(R.id.coping_strategies_input);
        saveCopingStrategyButton = view.findViewById(R.id.save_coping_strategy_button);
        moodSeekBar = view.findViewById(R.id.mood_seek_bar);
        moodValueLabel = view.findViewById(R.id.mood_value_label);
        moodInputText = view.findViewById(R.id.mood_input_text);
        journalEntryText = view.findViewById(R.id.journal_entry_text);
        moodBarChart = view.findViewById(R.id.mood_bar_chart);
        seeMoreStrategiesButton = view.findViewById(R.id.see_more_strategies_button);
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        homeScrollView = view.findViewById(R.id.home_scroll_view);
        relapseCounterTextView = view.findViewById(R.id.relapse_counter_text_view);
        relapseCounterTextView2 = view.findViewById(R.id.relapse_counter_text_view);
        resetRelapseButton = view.findViewById(R.id.reset_relapse_button);

        loadJournalEntries();
        GeneralViewModel viewModel = new ViewModelProvider(this).get(GeneralViewModel.class);
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                homeScrollView.setVisibility(View.GONE);
            } else {
                loadingProgressBar.setVisibility(View.GONE);
                homeScrollView.setVisibility(View.VISIBLE);
            }
        });
        saveJournalButton.setOnClickListener(v -> {
            saveJournalEntryToStorage();
        });
        copingStrategiesInput.setOnClickListener(v -> {
            if (mainActivity != null) {
                mainActivity.closeSettings();
            }
        });
        moodInputText.setOnClickListener(v -> {
            if (mainActivity != null) {
                mainActivity.closeSettings();
            }
        });
        journalEntryText.setOnClickListener(v -> {
            if (mainActivity != null) {
                mainActivity.closeSettings();
            }
        });
        //  Initial update of the relapse counter when the fragment is created
        updateRelapseCounter();

        resetRelapseButton.setOnClickListener(v -> {
            showResetRelapseConfirmationDialog();
        });
        loadSavedStrategiesFromPrefs();
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (context != null) {
            sharedPreferences = context.getSharedPreferences("heal_data", Context.MODE_PRIVATE);
        }
        shakeView(selfImprovementIcon);
        sliderAdapter = new SliderAdapter(this);
        sliderViewPager.setAdapter(sliderAdapter);
        startAutoScroll();
        sliderViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    stopAutoScroll();
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    startAutoScroll();
                }
            }
        });

        urgeLevelSeekbar.addOnChangeListener((slider, value, fromUser) -> {
            urgeLevelDisplay.setText("Urgency Level: " + (int) value);
        });

        urgeLevelSeekbar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            Context context = getContext();

            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                // Optional: Handle start of tracking
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                int progress = (int) slider.getValue();
                Handler handler = new Handler(Looper.getMainLooper());
                if (context != null) {
                    if (progress >= 0 && progress <= 3) {
                        Toast.makeText(context, "It'seems that you are doing your grounding exercise, but try to go a bit further.", Toast.LENGTH_SHORT).show();
                        handler.postDelayed(() -> slider.setValue(0), 1000);
                    } else if (progress > 3 && progress <= 6) {
                        Toast.makeText(context, "You seem quite agitated. Grounding exercises are important; please continue.", Toast.LENGTH_SHORT).show();
                        handler.postDelayed(() -> slider.setValue(0), 1500);
                    } else if (progress > 6 && progress <= 9) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Are you okay?");
                        builder.setMessage("This is a high level of distress. Have you completed your grounding exercise? It can really help.");
                        builder.setPositiveButton("Yes, completed", (dialog, which) -> {
                            Toast.makeText(context, "Okay, that's good. Remember to use your grounding techniques whenever you feel overwhelmed.", Toast.LENGTH_SHORT).show();
                            handler.postDelayed(() -> slider.setValue(0), 2000);
                        });
                        builder.setNegativeButton("Not yet", (dialog, which) -> {
                            Toast.makeText(context, "It's crucial that you do it now. Your well-being is important.", Toast.LENGTH_SHORT).show();
                            handler.postDelayed(() -> slider.setValue(0), 2000);
                        });
                        builder.setCancelable(false);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else if (progress == 10) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("This is very high!");
                        builder.setMessage("You're experiencing a very high level of distress. Please complete your grounding exercise immediately. If you feel unsafe, seek help.");
                        builder.setPositiveButton("Completed", (dialog, which) -> {
                            Toast.makeText(context, "Please monitor your feelings, if you feel unsafe seek help.", Toast.LENGTH_SHORT).show();
                            handler.postDelayed(() -> slider.setValue(0), 2000);
                        });
                        builder.setNegativeButton("Need Help", (dialog, which) -> {
                            MainActivity mainActiviy = (MainActivity) getActivity();
                            Toast.makeText(context, "Please seek help", Toast.LENGTH_SHORT).show();
                            handler.postDelayed(() -> slider.setValue(0), 2000);
                            handler.postDelayed(() -> { slider.setValue(0); mainActiviy.loadContacts(); }, 3000);

                        });
                        builder.setCancelable(false);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else {
                        Toast.makeText(context, "Please move the slider", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        logPositiveActionButton.setOnClickListener(v -> {
            int LogSteps = Integer.parseInt(stepsCounter.getText().toString());
            LogSteps++;
            stepsCounter.setText(String.valueOf(LogSteps));
            if (LogSteps == 10) {
                shakeView(logPositiveActionButton);
                Toast.makeText(this.getContext(), "Take a deep breath", Toast.LENGTH_SHORT).show();
            } else if (LogSteps == 20) {
                shakeView(logPositiveActionButton);
                Toast.makeText(this.getContext(), "Good job!", Toast.LENGTH_SHORT).show();
            } else if (LogSteps == 30) {
                shakeView(logPositiveActionButton);
                Toast.makeText(this.getContext(), "You're doing great!", Toast.LENGTH_SHORT).show();
            } else if (LogSteps == 40) {
                shakeView(logPositiveActionButton);
                Toast.makeText(this.getContext(), "Keep it up!", Toast.LENGTH_SHORT).show();
            } else if (LogSteps == 50) {
                shakeView(logPositiveActionButton);
                Toast.makeText(this.getContext(), "Take a break \uD83E\uDD17", Toast.LENGTH_SHORT).show();
            }
        });
        if (saveCopingStrategyButton != null) {
            saveCopingStrategyButton.setOnClickListener(v -> {
                saveCopingStrategy();
            });
        }
        setupBarChart();
        loadMoodData(); // Load mood data here
        updateBarChart(); // Update chart after loading data
        emergencyContactButton.setOnClickListener(v -> {
            mainActivity.loadContacts();
            mainActivity.navigationView.setCheckedItem(R.id.nav_records);
            mainActivity.toolbar.setTitle("My Emergency Contacts");
        });
        copingExercisesButton.setOnClickListener(v -> {
            mainActivity.loadFragment(new CopingExercisesFragment(), 0);
            mainActivity.navigationView.setCheckedItem(R.id.nav_records);
        });
        moreResourcesButton.setOnClickListener(v -> {
            mainActivity.loadFragment(new RecordFragment(), 0);
        });
        groundingExerciseButton.setOnClickListener(v -> {
            CopingExercisesFragment copingFragment = CopingExercisesFragment.newInstance(true);
            mainActivity.loadFragment(copingFragment, 3);
        });
        if (seeMoreStrategiesButton != null) {
            seeMoreStrategiesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mainActivity != null) {
                        mainActivity.loadFragment(new SavedStrategiesFragment(),0); // Ensure this navigation works
                        mainActivity.navigationView.setCheckedItem(R.id.nav_records);
                    }
                }
            });
        }
        loadSavedUsername();
        setupMoodCheckin();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

    }
    @Override
    public void onResume() {
        super.onResume();
        if (sliderViewPager != null && sliderViewPager.getScrollState() == ViewPager2.SCROLL_STATE_IDLE) {
            startAutoScroll();
        }
        checkDailyCheckin();
        loadMoodData();
        updateBarChart();
        startRelapseCounterUpdates();
        loadSavedUsername();
    }
    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
        // Stop the relapse counter updates when the fragment is paused
        stopRelapseCounterUpdates();
    }

    private void startAutoScroll() {
        if (sliderViewPager != null && sliderViewPager.getAdapter() != null) {
            stopAutoScroll(); // Stop any existing auto-scroll
            autoScrollRunnable = new Runnable() {
                @Override
                public void run() {
                    int itemCount = sliderViewPager.getAdapter().getItemCount();
                    if (itemCount > 0) {
                        currentPosition = (currentPosition + 1) % itemCount;
                        sliderViewPager.setCurrentItem(currentPosition, true);
                        autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY);
                    }
                }
            };
            autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
        }
    }

    private void stopAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    public void startRelapseCounterUpdates() {
        // Initialize handler if it's null
        if (relapseCounterHandler == null) {
            relapseCounterHandler = new Handler(Looper.getMainLooper());
        }
        // Stop any existing updates before starting a new one
        stopRelapseCounterUpdates();

        relapseCounterRunnable = new Runnable() {
            @Override
            public void run() {
                updateRelapseCounter();
                relapseCounterHandler.postDelayed(this, 1000);
            }
        };
        relapseCounterHandler.post(relapseCounterRunnable);
    }


    private void stopRelapseCounterUpdates() {
        if (relapseCounterHandler != null && relapseCounterRunnable != null) {
            relapseCounterHandler.removeCallbacks(relapseCounterRunnable);
        }
    }

    private void shakeView(View view) {
        ObjectAnimator translateX = ObjectAnimator.ofFloat(view, "translationX", 0f, -20f, 20f, -20f, 20f, 0f);
        translateX.setDuration(2700);
        translateX.setInterpolator(new OvershootInterpolator());
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(translateX);
        animatorSet.start();
    }

    public void loadSavedUsername() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        try {
            String savedUsername = sharedPreferences.getString("user_name", "Guest");
            greeting.setText("Hello " + savedUsername);
        } catch (Exception e) {
            if (greeting != null) {
                greeting.setText("Hello Guest");
            }
        }
    }

    private static class SliderAdapter extends FragmentStateAdapter {
        private final List<Fragment> fragments;
        public SliderAdapter(@NonNull Fragment fragment) {
            super(fragment);
            this.fragments = new ArrayList<>();
            this.fragments.add(new SliderOne());
            this.fragments.add(new SliderTwo());
            this.fragments.add(new SliderThree());
        }
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }


    private void saveCopingStrategy() {
        String newStrategy = copingStrategiesInput.getText().toString().trim();
        if (!newStrategy.isEmpty()) {
            if (!allSavedStrategies.contains(newStrategy)){
                allSavedStrategies.add(newStrategy);
                SharedPreferences sharedPreferences = getContext().getSharedPreferences("coping_strategies", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("strategies", TextUtils.join(",", allSavedStrategies));
                editor.apply();
                copingStrategiesInput.setText("");
                Toast.makeText(getContext(), "Strategy added!", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(mainActivity, "Strategy already exists", Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(getContext(), "Please enter a coping strategy.", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadSavedStrategiesFromPrefs(){
        if (getContext() == null) return;

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("coping_strategies",context.MODE_PRIVATE);
        String savedStrategiesString = sharedPreferences.getString("strategies",null);
        allSavedStrategies.clear();

        if ( savedStrategiesString != null && !savedStrategiesString.isEmpty()){
            String[] strategiesArray = savedStrategiesString.split(",");
            allSavedStrategies.addAll(Arrays.asList(strategiesArray));
        }
    }


    private void setupBarChart() {
        moodBarChart.getDescription().setEnabled(false);
        moodBarChart.setDrawGridBackground(false);
        moodBarChart.setTouchEnabled(false);
        moodBarChart.setDragEnabled(false);
        moodBarChart.setScaleEnabled(false);
        moodBarChart.setPinchZoom(false);
        moodBarChart.setHighlightPerDragEnabled(false);
        moodBarChart.setHighlightPerTapEnabled(false);
        moodBarChart.setNoDataText("No mood data available");
        moodBarChart.setNoDataTextColor(Color.GRAY);

        XAxis xAxis = moodBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        if (getContext() != null) {
            xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.md_theme_onSurfaceVariant));
        }
        xAxis.setTextSize(9f);
        moodBarChart.getAxisLeft().setAxisMinimum(0f);
        moodBarChart.getAxisLeft().setAxisMaximum(10f);
        moodBarChart.getAxisLeft().setDrawGridLines(true);
        if (getContext() != null) {
            moodBarChart.getAxisLeft().setGridColor(ContextCompat.getColor(getContext(), R.color.md_theme_outline));
        }
        moodBarChart.getAxisLeft().setDrawAxisLine(false);
        if (getContext() != null) {
            moodBarChart.getAxisLeft().setTextColor(ContextCompat.getColor(getContext(), R.color.md_theme_onSurfaceVariant));
        }
        moodBarChart.getAxisLeft().setTextSize(9f);

        moodBarChart.getAxisRight().setEnabled(false);
        moodBarChart.getLegend().setEnabled(false);

        moodBarChart.animateY(1000);
    }

    private void updateBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            String day = sdf.format(calendar.getTime());

            labels.add(new SimpleDateFormat("EEE\nMMM d", Locale.getDefault()).format(calendar.getTime()));
            int moodLevel = 0; // Default to 0 if no entry for the day
            for (MoodEntry moodEntry : moodEntries) {
                if (moodEntry.getDay().equals(day)) {
                    moodLevel = moodEntry.getMoodLevel();
                    break;
                }
            }
            entries.add(new BarEntry(6 - i, moodLevel));
            colors.add(getMoodColor(moodLevel));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Mood");
        dataSet.setColors(colors);
        // Use ContextCompat for dynamic day/night color for value text
        if (getContext() != null) {
            dataSet.setValueTextColor(ContextCompat.getColor(getContext(), R.color.md_theme_onSurface));
        }
        dataSet.setValueTextSize(9f); // Slightly smaller value text size
        dataSet.setDrawValues(true); // Show value on top of bars
        if (getContext() != null) {
            dataSet.setBarBorderColor(ContextCompat.getColor(getContext(), R.color.md_theme_outline));
        }
        dataSet.setBarBorderWidth(0.5f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        moodBarChart.setData(barData);
        XAxis xAxis = moodBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size(), false); // Ensure all labels are shown
        moodBarChart.invalidate();
    }
    private int getMoodColor(int moodLevel) {

        if (moodLevel <= 2) {
            return Color.parseColor("#EF5350"); // Red - Very Bad
        } else if (moodLevel <= 4) {
            return Color.parseColor("#FFCA28"); // Amber - Bad
        } else if (moodLevel <= 6) {
            return Color.parseColor("#FFEE58"); // Light Yellow - Neutral
        } else if (moodLevel <= 8) {
            return Color.parseColor("#9CCC65"); // Light Green - Good
        } else {
            return Color.parseColor("#4CAF50"); // Green - Very Good
        }
    }
    private void saveJournalEntryToStorage() {
        String journalText = journalEntryText.getText().toString().trim();
        if (!journalText.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            JournalEntry newEntry = new JournalEntry(timestamp, journalText);
            allJournalEntries.add(newEntry);
            saveAllJournalEntries();
            journalEntryText.getText().clear();
            Toast.makeText(getContext(), "Journal entry saved!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Please write something in your journal.", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveAllJournalEntries() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_JOURNAL, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(allJournalEntries);
        editor.putString(KEY_JOURNAL_ENTRIES, json);
        editor.apply();
    }
    private void loadJournalEntries() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_JOURNAL, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JOURNAL_ENTRIES, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<JournalEntry>>() {
            }.getType();
            allJournalEntries = gson.fromJson(json, type);
            if (allJournalEntries == null) {
                allJournalEntries = new ArrayList<>();
            }
        } else {
            allJournalEntries = new ArrayList<>();
        }
    }
    public static class JournalEntry {
        private String formattedTimestamp;
        private String text;
        private long creationTimestampMillis;

        public JournalEntry(String formattedTimestamp, String text) {
            this.formattedTimestamp = formattedTimestamp;
            this.text = text;
            this.creationTimestampMillis = System.currentTimeMillis();
        }

        public JournalEntry(String formattedTimestamp, String text, long creationTimestampMillis) {
            this.formattedTimestamp = formattedTimestamp;
            this.text = text;
            this.creationTimestampMillis = creationTimestampMillis;
        }

        public String getTimestamp() {
            return formattedTimestamp;
        }
        public String getText() {
            return text;
        }
        public long getCreationTimestampMillis() {
            return creationTimestampMillis;
        }
    }
    private void setupMoodCheckin() {
        loadLastCheckinTime(); // Load the last check-in time from SharedPreferences
        checkDailyCheckin();    // Check if the user has already checked in today

        moodSeekBar.addOnChangeListener((slider, value, fromUser) -> {
            moodValueLabel.setText("Mood: " + (int) value);
            if(fromUser){
                moodSeekBarTouched = true;
            }
        });
        moodSeekBar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {
                moodSeekBarTouched = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {

            }
        });

        submitCheckinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCheckinAllowed()) {
                    if(moodInputText.length() != 0 ){
                       if (moodSeekBarTouched == true){
                           saveMoodData();
                           saveLastCheckinTime();
                           checkDailyCheckin();
                           Handler handler = new Handler();
                           handler.postDelayed(() -> setupMoodCheckin(),1000);
                       }else {
                           Toast.makeText(mainActivity, "Move the mood bar first", Toast.LENGTH_SHORT).show();
                       }
                    }else{
                        Toast.makeText(mainActivity, "Mood input cannot be empty", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(getContext(), "You can only check in once per day.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Re-set listeners for disabled state to show toasts
        if (!moodSeekBar.isEnabled()) {
            moodSeekBar.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Mood check-in is disabled until tomorrow.", Toast.LENGTH_SHORT).show();
            });
        }
        if (!moodInputText.isEnabled()) {
            moodInputText.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Mood check-in is disabled until tomorrow.", Toast.LENGTH_SHORT).show();
            });
        }
    }
    private void saveMoodData() {
        int moodLevel = (int) moodSeekBar.getValue();
        String moodText = moodInputText.getText().toString().trim();
        if (moodText.isEmpty()) {
            Toast.makeText(getContext(), "Please describe your mood.", Toast.LENGTH_SHORT).show();
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        MoodEntry newMoodEntry = new MoodEntry(today, moodLevel, moodText, System.currentTimeMillis());

        boolean replacedExisting = false;
        for (int i = 0; i < moodEntries.size(); i++) {
            if (moodEntries.get(i).getDay().equals(today)) {
                moodEntries.set(i, newMoodEntry);
                replacedExisting = true;
                break;
            }
        }
        if (!replacedExisting) {
            moodEntries.add(newMoodEntry);
        }

        saveMoodDataToPreferences();
        updateBarChart();
        Toast.makeText(getContext(), "Mood data saved!", Toast.LENGTH_SHORT).show();
        moodInputText.getText().clear();
    }
    private void loadMoodData() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_MOOD, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_MOOD_ENTRIES, null);
        if (json != null) {
            Type type = new TypeToken<List<MoodEntry>>() {
            }.getType();
            moodEntries = gson.fromJson(json, type);
            if (moodEntries == null) {
                moodEntries = new ArrayList<>();
            }
        } else {
            moodEntries = new ArrayList<>();
        }
    }
    private void saveMoodDataToPreferences() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_MOOD, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(moodEntries);
        editor.putString(KEY_MOOD_ENTRIES, json);
        editor.apply();
    }
    public static class MoodEntry {
        private String day;
        private int moodLevel;
        private String moodText;
        private long timestamp;

        public MoodEntry(String day, int moodLevel, String moodText) {
            this.day = day;
            this.moodLevel = moodLevel;
            this.moodText = moodText;
            this.timestamp = System.currentTimeMillis();
        }

        public MoodEntry(String day, int moodLevel, String moodText, long timestamp) {
            this.day = day;
            this.moodLevel = moodLevel;
            this.moodText = moodText;
            this.timestamp = timestamp;
        }

        public String getDay() {
            return day;
        }
        public int getMoodLevel() {
            return moodLevel;
        }
        public String getMoodText() {
            return moodText;
        }
        public long getTimestamp() {
            return timestamp;
        }
    }
    private void saveLastCheckinTime() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_MOOD, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_LAST_CHECKIN, System.currentTimeMillis()); // Save current time in milliseconds
        editor.apply();
    }
    private void loadLastCheckinTime() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_MOOD, Context.MODE_PRIVATE);
        lastCheckinTime = prefs.getLong(KEY_LAST_CHECKIN, 0); // Load the stored time, default to 0 if not found
    }

    private boolean isCheckinAllowed() {
        if (lastCheckinTime == 0) {
            return true;
        }

        // Get the current date (year, month, day)
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(System.currentTimeMillis());
        int currentDay = currentCalendar.get(Calendar.DAY_OF_YEAR);
        int currentYear = currentCalendar.get(Calendar.YEAR);

        // Get the last check-in date (year, month, day)
        Calendar lastCheckinCalendar = Calendar.getInstance();
        lastCheckinCalendar.setTimeInMillis(lastCheckinTime);
        int lastCheckinDay = lastCheckinCalendar.get(Calendar.DAY_OF_YEAR);
        int lastCheckinYear = lastCheckinCalendar.get(Calendar.YEAR);

        // Allow check-in if it's a different day or a different year
        return (currentDay != lastCheckinDay || currentYear != lastCheckinYear);
    }
    private void checkDailyCheckin() {
        if (!isCheckinAllowed()) {
            // User has already checked in today
            moodSeekBar.setEnabled(false);
            moodSeekBar.setValue(0);
            moodInputText.setEnabled(false);
            submitCheckinButton.setEnabled(false);
            submitCheckinButton.setText("You've checked in today!");

            // Add OnClickListeners for disabled elements to show toasts
            if (moodSeekBar != null) {
                moodSeekBar.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "Mood check-in is disabled until tomorrow.", Toast.LENGTH_SHORT).show();
                });
            }
            if (moodInputText != null) {
                moodInputText.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "Mood check-in is disabled until tomorrow.", Toast.LENGTH_SHORT).show();
                });
            }
            if (submitCheckinButton != null) {
                submitCheckinButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "You've already checked in today. Please wait until tomorrow.", Toast.LENGTH_SHORT).show();
                });
            }

        } else {
            // User can check in today
            moodSeekBar.setEnabled(true);
            moodInputText.setEnabled(true);
            submitCheckinButton.setEnabled(true);
            submitCheckinButton.setText("Submit Check-in");
            if (moodSeekBar != null) {
                moodSeekBar.setOnClickListener(null); // Remove listener
            }
            if (moodInputText != null) {
                moodInputText.setOnClickListener(null); // Remove listener
            }
        }
    }

    private void updateRelapseCounter() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_RELAPSE, Context.MODE_PRIVATE);
        long lastRelapseTime = prefs.getLong(KEY_LAST_RELAPSE_DATE, 0L);
        Log.d(TAG, "HomeFragment: updateRelapseCounter - Read lastRelapseTime: " + lastRelapseTime);

        if (lastRelapseTime == 0L) {
            Log.w(TAG, "HomeFragment: updateRelapseCounter - lastRelapseTime is 0. Timer might not have been set yet. Displaying 'Timer not started'.");
            if (relapseCounterTextView != null) {
                relapseCounterTextView.setText("Timer not started"); // Provide feedback
                resetRelapseButton.setText("Start Counter");
                resetRelapseButton.setOnClickListener(v->{
                    resetRelapseCounter();
                    Toast.makeText(mainActivity, "Timer Started", Toast.LENGTH_SHORT).show();
                });
            }
            return; // Exit if timer not started
        }

        long currentTime = System.currentTimeMillis();
        long difference = currentTime - lastRelapseTime;

        long seconds = TimeUnit.MILLISECONDS.toSeconds(difference);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(difference);
        long hours = TimeUnit.MILLISECONDS.toHours(difference);
        long days = TimeUnit.MILLISECONDS.toDays(difference);

        // Calculate remaining hours, minutes, and seconds for display
        long remainingHours = hours % 24;
        long remainingMinutes = minutes % 60;
        long remainingSeconds = seconds % 60;

        String counterText = String.format(Locale.getDefault(),
                "%d Day%s %02d Hour%s %02d Minute%s %02d Second%s",
                days, (days == 1 ? "" : "s"),
                remainingHours, (remainingHours == 1 ? "" : "s"),
                remainingMinutes, (remainingMinutes == 1 ? "" : "s"),
                remainingSeconds, (remainingSeconds == 1 ? "" : "s"));

        relapseCounterTextView.setText(counterText);
    }

    public void resetRelapseCounter() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_RELAPSE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_LAST_RELAPSE_DATE, System.currentTimeMillis());
        editor.apply();
        updateRelapseCounter();
        resetRelapseButton.setText("Reset Counter");
    }

    private void showResetRelapseConfirmationDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Reset Counter")
                .setMessage("Are you sure you want to reset the relapse counter? This action cannot be undone.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    resetRelapseCounter();
                    Handler handler = new Handler();
                    handler.postDelayed(() ->{
                        Toast.makeText(context, "Relapse counter reset!", Toast.LENGTH_SHORT).show();
                    },1000);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .create()
                .show();
    }
}