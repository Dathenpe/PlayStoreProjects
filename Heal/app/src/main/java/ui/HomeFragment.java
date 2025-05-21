package ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.heal.MainActivity;
import com.example.heal.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import Slider.SliderOne;
import Slider.SliderThree;
import Slider.SliderTwo;
import records.CopingExercisesFragment;
import records.EmergencyContactsFragment;
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
    private SeekBar urgeLevelSeekbar;
    private TextView copingStrategiesLabel;
    private EditText copingStrategiesInput;
    private Button saveCopingStrategyButton;
    private GridLayout savedStrategiesContainer;
    private TextView savedStrategiesTitle;
    private List<String> savedStrategiesList = new ArrayList<>();
    private Button emergencyContactButton;
    private Button copingExercisesButton;
    private Button moreResourcesButton;
    private TextView dailyCheckinTitle;
    private TextView moodQuestion;
    private SeekBar moodSeekBar;
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
    private Button seeMoreStrategiesButton; // Added for "See More" button

    private Handler autoScrollHandler = new Handler();
    private Runnable autoScrollRunnable;
    private int currentPosition = 0;

    private List<MoodEntry> moodEntries = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();
    private Context context;
    private SliderAdapter sliderAdapter;
    private long lastCheckinTime = 0; // Store the last check-in time
    private CountDownTimer checkinTimer; // Timer to handle enabling the button at midnight
    private List<JournalEntry> allJournalEntries = new ArrayList<>();
    private CoordinatorLayout homeCoordinatorLayout;
    private ScrollView homeScrollView;


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
        savedStrategiesContainer = view.findViewById(R.id.saved_strategies_container);
        savedStrategiesTitle = view.findViewById(R.id.saved_strategies_title);
        moodBarChart = view.findViewById(R.id.mood_bar_chart);
        seeMoreStrategiesButton = view.findViewById(R.id.see_more_strategies_button);
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        homeScrollView = view.findViewById(R.id.home_scroll_view);

        loadJournalEntries(); // Load saved journal entries

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

        urgeLevelSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            Context context = getContext();

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                urgeLevelDisplay.setText("Urgency Level: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                Handler handler = new Handler(Looper.getMainLooper());
                if (context != null) {
                    if (progress >= 0 && progress <= 3) {
                        Toast.makeText(context, "It'seems that you are doing your grounding exercise, but try to go a bit further.", Toast.LENGTH_SHORT).show();
                        handler.postDelayed(() -> seekBar.setProgress(0), 1000);
                    } else if (progress > 3 && progress <= 6) {
                        Toast.makeText(context, "You seem quite agitated.  Grounding exercises are important; please continue.", Toast.LENGTH_SHORT).show();
                        handler.postDelayed(() -> seekBar.setProgress(0), 1500);
                    } else if (progress > 6 && progress <= 9) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Are you okay?");
                        builder.setMessage("This is a high level of distress.  Have you completed your grounding exercise?  It can really help.");
                        builder.setPositiveButton("Yes, completed", (dialog, which) -> {
                            Toast.makeText(context, "Okay, that's good.  Remember to use your grounding techniques whenever you feel overwhelmed.", Toast.LENGTH_SHORT).show();
                            handler.postDelayed(() -> seekBar.setProgress(0), 2000);
                        });
                        builder.setNegativeButton("Not yet", (dialog, which) -> {
                            Toast.makeText(context, "It's crucial that you do it now.  Your well-being is important.", Toast.LENGTH_SHORT).show();
                            handler.postDelayed(() -> seekBar.setProgress(0), 2000);
                        });
                        builder.setCancelable(false);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else if (progress == 10) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("This is very high!");
                        builder.setMessage("You're experiencing a very high level of distress.  Please complete your grounding exercise immediately.  If you feel unsafe, seek help.");
                        builder.setPositiveButton("Completed", (dialog, which) -> {
                            Toast.makeText(context, "Please monitor your feelings, if you feel unsafe seek help.", Toast.LENGTH_SHORT).show();
                            handler.postDelayed(() -> seekBar.setProgress(0), 2000);
                        });
                        builder.setNegativeButton("Need Help", (dialog, which) -> {
                            Toast.makeText(context, "Please seek help", Toast.LENGTH_SHORT).show();
                            handler.postDelayed(() -> seekBar.setProgress(0), 2000);

                        });
                        builder.setCancelable(false);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else {
                        Toast.makeText(context, "Please move the seekbar", Toast.LENGTH_SHORT).show();
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

        loadSavedStrategies();
        setupBarChart();
        loadMoodData();
        updateBarChart();

        emergencyContactButton.setOnClickListener(v -> {
            mainActivity.loadFragment(new EmergencyContactsFragment(),0);
            mainActivity.navigationView.setCheckedItem(R.id.nav_records);
        });

        copingExercisesButton.setOnClickListener(v -> {
            mainActivity.loadFragment(new CopingExercisesFragment(),0);
            mainActivity.navigationView.setCheckedItem(R.id.nav_records);
        });

        moreResourcesButton.setOnClickListener(v -> {
            mainActivity.loadFragment(new RecordFragment(),0);
        });
        groundingExerciseButton.setOnClickListener(v -> {
            // Handle grounding exercise
        });

        if (seeMoreStrategiesButton != null) {
            seeMoreStrategiesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //  replace this with your actual navigation logic
                    if (mainActivity != null) {
                        // mainActivity.loadAllStrategies();
                    }
                }
            });
        }
        loadSavedUsername();
        setupMoodCheckin(); // Initialize the mood check-in UI and logic

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sliderViewPager != null && sliderViewPager.getScrollState() == ViewPager2.SCROLL_STATE_IDLE) {
            startAutoScroll();
        }
        checkDailyCheckin(); // Check check-in status when the fragment resumes
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
        stopCountdownTimer(); // Stop the timer when the fragment is paused
    }

    private void startAutoScroll() {
        if (sliderViewPager != null && sliderViewPager.getAdapter() != null) {
            stopAutoScroll();
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
        String strategy = copingStrategiesInput.getText().toString().trim();
        if (!strategy.isEmpty()) {
            savedStrategiesList.add(strategy);
            displaySavedStrategies();
            copingStrategiesInput.getText().clear();
            Toast.makeText(getContext(), "Strategy saved!", Toast.LENGTH_SHORT).show();
            saveStrategiesToSharedPreferences();
        } else {
            Toast.makeText(getContext(), "Please enter a coping strategy.", Toast.LENGTH_SHORT).show();
        }
    }

    private void displaySavedStrategies() {
        if (savedStrategiesList.isEmpty()) {
            savedStrategiesTitle.setVisibility(View.GONE);
            savedStrategiesContainer.setVisibility(View.GONE);
            seeMoreStrategiesButton.setVisibility(View.GONE);
        } else {
            savedStrategiesTitle.setVisibility(View.VISIBLE);
            savedStrategiesContainer.setVisibility(View.VISIBLE);
            savedStrategiesContainer.removeAllViews();
            int displayCount = Math.min(savedStrategiesList.size(), MAX_STRATEGIES_DISPLAYED);
            savedStrategiesContainer.setRowCount((int) Math.ceil((displayCount + (savedStrategiesList.size() > MAX_STRATEGIES_DISPLAYED ? 1 : 0)) / 2.0));

            for (int i = 0; i < displayCount; i++) {
                String strategy = savedStrategiesList.get(i);
                View strategyItemView = LayoutInflater.from(getContext()).inflate(R.layout.coping_strategy_item, savedStrategiesContainer, false);
                TextView strategyTextView = strategyItemView.findViewById(R.id.strategy_text_view);
                ImageView deleteImageView = strategyItemView.findViewById(R.id.delete_image_view);

                strategyTextView.setText(strategy);
                strategyTextView.setTextColor(getResources().getColor(R.color.darkgray, getContext().getTheme()));
                strategyTextView.setTextSize(16);

                final int position = i;
                deleteImageView.setOnClickListener(new View.OnClickListener() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                    @Override
                    public void onClick(View v) {
                        builder.setTitle("Delete Strategy")
                                .setMessage("Are you sure you want to delete this strategy?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteStrategy(position);
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(i / 2, 1);
                params.columnSpec = GridLayout.spec(i % 2, 1);
                params.setMargins(8, 8, 8, 8);
                strategyItemView.setLayoutParams(params);
                savedStrategiesContainer.addView(strategyItemView);
            }

            if (savedStrategiesList.size() > MAX_STRATEGIES_DISPLAYED) {
                if (seeMoreStrategiesButton.getParent() != null) {
                    ((ViewGroup) seeMoreStrategiesButton.getParent()).removeView(seeMoreStrategiesButton);
                }
                GridLayout.LayoutParams buttonParams = new GridLayout.LayoutParams();
                buttonParams.rowSpec = GridLayout.spec(MAX_STRATEGIES_DISPLAYED / 2, 1);
                buttonParams.columnSpec = GridLayout.spec(MAX_STRATEGIES_DISPLAYED % 2, 1);
                buttonParams.setMargins(8, 8, 8, 8);
                seeMoreStrategiesButton.setLayoutParams(buttonParams);
                savedStrategiesContainer.addView(seeMoreStrategiesButton);
                seeMoreStrategiesButton.setVisibility(View.VISIBLE);
            } else {
                seeMoreStrategiesButton.setVisibility(View.GONE);
            }
        }
    }

    private void deleteStrategy(int position) {
        savedStrategiesList.remove(position);
        displaySavedStrategies();
        saveStrategiesToSharedPreferences();
        Toast.makeText(getContext(), "Strategy deleted!", Toast.LENGTH_SHORT).show();
    }

    private void saveStrategiesToSharedPreferences() {
        if (getContext() == null) return;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("coping_strategies", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("strategies", TextUtils.join(",", savedStrategiesList));
        editor.apply();
    }

    private void loadSavedStrategies() {
        if (getContext() == null) return;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("coping_strategies", Context.MODE_PRIVATE);
        String savedStrategies = sharedPreferences.getString("strategies", "");
        if (!savedStrategies.isEmpty()) {
            String[] strategiesArray = savedStrategies.split(",");
            for (String strategy : strategiesArray) {
                savedStrategiesList.add(strategy);
            }
            displaySavedStrategies();
        }
    }

    private void setupBarChart() {
        moodBarChart.getDescription().setEnabled(false);
        moodBarChart.setDrawGridBackground(false);
        moodBarChart.setTouchEnabled(true);
        moodBarChart.setDragEnabled(true);
        moodBarChart.setScaleEnabled(true);
        moodBarChart.setPinchZoom(true);
        moodBarChart.setNoDataText("No mood data available");

        XAxis xAxis = moodBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);

        moodBarChart.getAxisLeft().setAxisMinimum(0f);
        moodBarChart.getAxisRight().setEnabled(false);
        moodBarChart.getLegend().setEnabled(false);
    }

    private void updateBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        for (int i = 4; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            String day = sdf.format(calendar.getTime());
            labels.add(day);
            int moodLevel = -1;

            for (MoodEntry moodEntry : moodEntries) {
                if (moodEntry.getDay().equals(day)) {
                    moodLevel = moodEntry.getMoodLevel();
                    break;
                }
            }

            if (moodLevel != -1) {
                entries.add(new BarEntry(4 - i, moodLevel));
                colors.add(getMoodColor(moodLevel));
            } else {
                entries.add(new BarEntry(4 - i, 0));
                colors.add(Color.LTGRAY);
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Mood");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        moodBarChart.setData(barData);
        XAxis xAxis = moodBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setAxisMinimum(0f);
        moodBarChart.getAxisLeft().setAxisMinimum(0f);
        moodBarChart.getAxisLeft().setAxisMaximum(10f);
        moodBarChart.getAxisRight().setEnabled(false);
        moodBarChart.getLegend().setEnabled(false);
        moodBarChart.setTouchEnabled(false);
        moodBarChart.setDragEnabled(false);
        moodBarChart.setScaleEnabled(false);
        moodBarChart.setPinchZoom(false);
        moodBarChart.setHighlightPerDragEnabled(false);
        moodBarChart.setHighlightPerTapEnabled(false);
        moodBarChart.invalidate();
    }

    private int getMoodColor(int moodLevel) {
        final int badMoodColor = Color.RED;
        final int mediumMoodColor = Color.YELLOW;
        final int goodMoodColor = Color.GREEN;

        if (moodLevel <= 3) {
            return badMoodColor;
        } else if (moodLevel <= 6) {
            return mediumMoodColor;
        } else {
            return goodMoodColor;
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
        Gson gson = new Gson();
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

    private static class JournalEntry {
        private String timestamp;
        private String text;

        public JournalEntry(String timestamp, String text) {
            this.timestamp = timestamp;
            this.text = text;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getText() {
            return text;
        }
    }

    private void setupMoodCheckin() {
        loadLastCheckinTime(); // Load the last check-in time from SharedPreferences
        checkDailyCheckin();    // Check if the user has already checked in today
        moodSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                moodValueLabel.setText("Mood: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional: Handle start of tracking
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Optional: Handle end of tracking
            }
        });

        submitCheckinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCheckinAllowed()) {
                    saveMoodData(); // Save mood data if check-in is allowed
                } else {
                    Toast.makeText(getContext(), "You can only check in once per day.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveMoodData() {
        int moodLevel = moodSeekBar.getProgress();
        String moodText = moodInputText.getText().toString().trim();

        if (moodText.isEmpty()) {
            Toast.makeText(getContext(), "Please describe your mood.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        MoodEntry newMoodEntry = new MoodEntry(today, moodLevel, moodText);
        moodEntries.add(newMoodEntry);
        saveMoodDataToPreferences();
        updateBarChart();

        // Disable the input fields and button after submitting data.
        moodSeekBar.setEnabled(false);
        moodInputText.setEnabled(false);
        submitCheckinButton.setEnabled(false);
        saveLastCheckinTime(); // Save the current time as the last check-in time.
        startCheckinTimer(); // Start the timer to re-enable the button at midnight.

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

    private static class MoodEntry {
        private String day;
        private int moodLevel;
        private String moodText;

        public MoodEntry(String day, int moodLevel, String moodText) {
            this.day = day;
            this.moodLevel = moodLevel;
            this.moodText = moodText;
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
            return true; // If lastCheckinTime is 0, it means the user hasn't checked in today.
        }
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastCheckinTime) >= COOLDOWN_DURATION; //checks if 24 hours have passed
    }


    private void checkDailyCheckin() {
        if (!isCheckinAllowed()) {
            // Disable the views if the user has already checked in today.
            moodSeekBar.setEnabled(false);
            moodInputText.setEnabled(false);
            submitCheckinButton.setEnabled(false);

            // Calculate the remaining time until midnight
            long remainingTime = getRemainingTimeUntilMidnight();
            startCheckinTimer(remainingTime); // Start the timer with the remaining time
        } else {
            // Enable the views if the user hasn't checked in today.
            moodSeekBar.setEnabled(true);
            moodInputText.setEnabled(true);
            submitCheckinButton.setEnabled(true);
            submitCheckinButton.setText("Submit Check-in"); // Reset the button text
            stopCountdownTimer(); // Stop any existing timer
        }
    }

    private long getRemainingTimeUntilMidnight() {
        Calendar now = Calendar.getInstance();
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        if (now.after(midnight)) {
            midnight.add(Calendar.DAY_OF_MONTH, 1); // Set to the next midnight
        }
        return midnight.getTimeInMillis() - now.getTimeInMillis();
    }

    private void startCheckinTimer() {
        long remainingTime = getRemainingTimeUntilMidnight();
        startCheckinTimer(remainingTime);
    }

    private void startCheckinTimer(long remainingTime) {
        if (checkinTimer != null) {
            checkinTimer.cancel(); // Cancel any existing timer
        }

        if (remainingTime <= 0) {
            // If the remaining time is 0 or negative, enable the button immediately.
            moodSeekBar.setEnabled(true);
            moodInputText.setEnabled(true);
            submitCheckinButton.setEnabled(true);
            submitCheckinButton.setText("Submit Check-in");
            return;
        }

        checkinTimer = new CountDownTimer(remainingTime, 1000) { // Update the timer every second.
            @Override
            public void onTick(long millisUntilFinished) {
                // Convert milliseconds to hours, minutes, and seconds.
                long hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(hours);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished));

                String timeLeft = String.format(Locale.getDefault(), "Time until next check-in: %02d:%02d:%02d", hours, minutes, seconds);
                submitCheckinButton.setText(timeLeft); // Update the button text with the remaining time.
            }

            @Override
            public void onFinish() {
                // When the timer finishes (reaches 0), enable the button.
                moodSeekBar.setEnabled(true);
                moodInputText.setEnabled(true);
                submitCheckinButton.setEnabled(true);
                submitCheckinButton.setText("Submit Check-in"); // Reset button text
            }
        };
        checkinTimer.start(); // Start the timer.
    }

    private void stopCountdownTimer() {
        if (checkinTimer != null) {
            checkinTimer.cancel(); // Cancel the timer.
            checkinTimer = null; // Set it to null to avoid using it after cancellation.
        }
    }
}

