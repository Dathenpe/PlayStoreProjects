package ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
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

public class HomeFragment extends Fragment {

    private MainActivity mainActivity;

    private static final long AUTO_SCROLL_DELAY = 3000;
    private static final long COOLDOWN_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    private static final int MAX_STRATEGIES_DISPLAYED = 3;

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
    private SliderAdapter sliderAdapter; // Add this line

    private static final String PREFS_JOURNAL = "journal_prefs";
    private static final String KEY_JOURNAL_ENTRIES = "journal_entries";

    private List<JournalEntry> allJournalEntries = new ArrayList<>();
    private CoordinatorLayout homeCoordinatorLayout;


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
        seeMoreStrategiesButton = view.findViewById(R.id.see_more_strategies_button); // Find the new button

        loadJournalEntries(); // Load saved entries when the fragment is created

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
        CoordinatorLayout coordinatorLayout = view.findViewById(R.id.home_coordinator_layout);
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

        });

        copingExercisesButton.setOnClickListener(v -> {

        });

        moreResourcesButton.setOnClickListener(v -> {

        });
        groundingExerciseButton.setOnClickListener(v -> {

        });

        moodSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                moodValueLabel.setText("Current Value: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        if (isMoodCheckinDisabled()) {
            submitCheckinButton.setEnabled(false);
            submitCheckinButton.setText("Check-in Again in 24 hours");
            moodInputText.setEnabled(false); // Disable the EditText
            moodSeekBar.setEnabled(false);
            moodInputText.setOnClickListener(v ->{
                Toast.makeText(getContext(), "Check-in again in 24 hours", Toast.LENGTH_SHORT).show();
            });
            submitCheckinButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Check-in again in 24 hours", Toast.LENGTH_SHORT).show();
            });
        } else {
            submitCheckinButton.setEnabled(true);
            submitCheckinButton.setText("Submit Check-in");
            moodInputText.setEnabled(true); // Enable the EditText
        }


        copingExercisesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCopingStrategy();
            }
        });

        // Set click listener for the "See More" button
        if (seeMoreStrategiesButton != null) {
            seeMoreStrategiesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Navigate to a new fragment or activity to display all strategies
                    //  replace this with your actual navigation logic
                    if (mainActivity != null) {
                        // mainActivity.loadAllStrategies();
                    }
                }
            });
        }
        loadSavedUsername();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        updateCheckinButtonState();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sliderViewPager != null && sliderViewPager.getScrollState() == ViewPager2.SCROLL_STATE_IDLE) {
            startAutoScroll();
        }
        updateCheckinButtonState();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
        stopCountdownTimer();
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
            String savedUsername = sharedPreferences.getString("user_name", "Guest"); // "Guest" is the default value if no username is saved
            greeting.setText("Hello " + savedUsername);
        } catch (Exception e) {
            // Optionally, you can set a default value in the UI in case of an error
            if (greeting != null) {
                greeting.setText("Hello Guest"); // Or some other default
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
            seeMoreStrategiesButton.setVisibility(View.GONE); // Hide the button if no strategies
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

            // Add the "See More" button if there are more strategies than MAX_STRATEGIES_DISPLAYED
            if (savedStrategiesList.size() > MAX_STRATEGIES_DISPLAYED) {
                // Create the button if it doesn't exist
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

    private void loadMoodData() {
        if (context == null) return;
        sharedPreferences = context.getSharedPreferences("heal_data", Context.MODE_PRIVATE);
        String moodDataJson = sharedPreferences.getString("mood_data", null);

        if (moodDataJson != null) {
            Type type = new TypeToken<List<MoodEntry>>() {
            }.getType();
            moodEntries = gson.fromJson(moodDataJson, type);
            if (moodEntries == null) {
                moodEntries = new ArrayList<>();
            }
        } else {
            moodEntries = new ArrayList<>();
        }
    }

    private void saveMoodData() {
        if (context == null) return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String moodDataJson = gson.toJson(moodEntries);
        editor.putString("mood_data", moodDataJson);
        editor.apply();
    }

    private void updateBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        // Ensure we have entries for the last 5 days, even if data is missing
        for (int i = 4; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            String day = sdf.format(calendar.getTime());
            labels.add(day);
            int moodLevel = -1; // Default value if no data for the day

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
                entries.add(new BarEntry(4 - i, 0)); // Add a zero entry for days without data
                colors.add(Color.LTGRAY); // Or any color to indicate no data
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Mood");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawValues(true); // Show values on the bars

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f); // Adjust bar width as needed

        moodBarChart.setData(barData);

        XAxis xAxis = moodBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setAxisMinimum(0f); // Ensure all 5 labels are visible

        moodBarChart.getAxisLeft().setAxisMinimum(0f);
        moodBarChart.getAxisLeft().setAxisMaximum(10f); // Fixed Y-axis range
        moodBarChart.getAxisRight().setEnabled(false);

        moodBarChart.getLegend().setEnabled(false);

        // Disable user interaction
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

    private String[] getDaysArray() {
        String[] days = new String[moodEntries.size()];
        for (int i = 0; i < moodEntries.size(); i++) {
            days[i] = moodEntries.get(i).getDay();
        }
        return days;
    }


    private void disableMoodCheckinFor24Hours() {
        if (context == null) return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("mood_checkin_timestamp", System.currentTimeMillis());
        editor.apply();
        submitCheckinButton.setEnabled(false);
        submitCheckinButton.setText("Check-in Again in 24 hours");
        moodInputText.setEnabled(false); // Disable after submitting
    }

    private boolean isMoodCheckinDisabled() {
        if (context == null) return false;
        long lastCheckinTimestamp = sharedPreferences.getLong("mood_checkin_timestamp", 0);

        Calendar lastCheckinCalendar = Calendar.getInstance();
        if (lastCheckinTimestamp > 0) {
            lastCheckinCalendar.setTimeInMillis(lastCheckinTimestamp);
        }

        Calendar currentCalendar = Calendar.getInstance();
        Calendar resetCalendar = Calendar.getInstance();
        resetCalendar.set(Calendar.HOUR_OF_DAY, 0);
        resetCalendar.set(Calendar.MINUTE, 0);
        resetCalendar.set(Calendar.SECOND, 0);
        resetCalendar.set(Calendar.MILLISECOND, 0);
        resetCalendar.add(Calendar.DAY_OF_YEAR, 1); // Next day at 12 AM

        Calendar lastCheckinStartOfDay = Calendar.getInstance();
        lastCheckinStartOfDay.setTimeInMillis(lastCheckinCalendar.getTimeInMillis());
        lastCheckinStartOfDay.set(Calendar.HOUR_OF_DAY, 0);
        lastCheckinStartOfDay.set(Calendar.MINUTE, 0);
        lastCheckinStartOfDay.set(Calendar.SECOND, 0);
        lastCheckinStartOfDay.set(Calendar.MILLISECOND, 0);

        Calendar currentStartOfDay = Calendar.getInstance();
        currentStartOfDay.setTimeInMillis(currentCalendar.getTimeInMillis());
        currentStartOfDay.set(Calendar.HOUR_OF_DAY, 0);
        currentStartOfDay.set(Calendar.MINUTE, 0);
        currentStartOfDay.set(Calendar.SECOND, 0);
        currentStartOfDay.set(Calendar.MILLISECOND, 0);

        return lastCheckinStartOfDay.getTimeInMillis() >= currentStartOfDay.getTimeInMillis();
    }


    private static class MoodEntry {
        private String day;
        private int moodLevel;
        private String moodText;

        public MoodEntry(String day, int moodLevel) {
            this.day = day;
            this.moodLevel = moodLevel;
        }

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
            Type type = new TypeToken<List<JournalEntry>>() {}.getType();
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

    private void stopCountdownTimer() {
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }
    private Handler countdownHandler = new Handler();
    private Runnable countdownRunnable;

    private void updateCheckinButtonState() {
        if (isMoodCheckinDisabled()) {
            submitCheckinButton.setEnabled(false);
            moodInputText.setEnabled(false);
            moodSeekBar.setEnabled(false);
            moodInputText.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Check-in again in 24 hours", Toast.LENGTH_SHORT).show();
            });
            submitCheckinButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Check-in again in 24 hours", Toast.LENGTH_SHORT).show();
            });

            startCountdownTimer();
        } else {
            submitCheckinButton.setEnabled(true);
            submitCheckinButton.setText("Submit Check-in");
            moodInputText.setEnabled(true);
            moodSeekBar.setEnabled(true);
            moodInputText.setOnClickListener(null); // Remove the disabled message
            submitCheckinButton.setOnClickListener(v -> saveMoodEntry()); // Replace with your actual submit logic
            stopCountdownTimer();
        }
    }
    private void startCountdownTimer() {
        stopCountdownTimer(); // Ensure any existing timer is stopped

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMoodCheckinDisabled()) {
                    Calendar now = Calendar.getInstance();
                    Calendar resetTime = Calendar.getInstance();
                    resetTime.set(Calendar.HOUR_OF_DAY, 0);
                    resetTime.set(Calendar.MINUTE, 0);
                    resetTime.set(Calendar.SECOND, 0);
                    resetTime.set(Calendar.MILLISECOND, 0);
                    resetTime.add(Calendar.DAY_OF_YEAR, 1);

                    long timeLeftInMillis = resetTime.getTimeInMillis() - now.getTimeInMillis();
                    long hoursLeft = TimeUnit.MILLISECONDS.toHours(timeLeftInMillis);
                    long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis) % 60;
                    long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) % 60;

                    String timeLeftString = String.format("Check-in Again in %02d:%02d:%02d", hoursLeft, minutesLeft, secondsLeft);
                    submitCheckinButton.setText(timeLeftString);

                    countdownHandler.postDelayed(this, 1000); // Update every second
                } else {
                    updateCheckinButtonState(); // Re-enable if the time has passed
                }
            }
        };
        countdownHandler.post(countdownRunnable);
    }
    public void saveMoodEntry() {
        if (context == null) return;
        int moodLevel = moodSeekBar.getProgress();
        String moodText = moodInputText.getText().toString().trim();

        if (moodText.isEmpty()) {
            Toast.makeText(getContext(), "Please describe your mood.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        for (int i = 0; i < moodEntries.size(); i++) {
            if (moodEntries.get(i).getDay().equals(today)) {
                moodEntries.set(i, new MoodEntry(today, moodLevel, moodText));
                saveMoodData();
                updateBarChart();
                Toast.makeText(getContext(), "Mood updated for today!", Toast.LENGTH_SHORT).show();
                moodInputText.getText().clear();
                moodSeekBar.setProgress(5);
                disableMoodCheckinFor24Hours();
                return;
            }
        }

        moodEntries.add(new MoodEntry(today, moodLevel, moodText));
        saveMoodData();
        updateBarChart();
        Toast.makeText(getContext(), "Mood saved!", Toast.LENGTH_SHORT).show();
        moodInputText.getText().clear();
        moodSeekBar.setProgress(5);
        disableMoodCheckinFor24Hours();
    }

}



