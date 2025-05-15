package ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.heal.R; // Make sure this is the correct path to your R class

import java.util.ArrayList;
import java.util.List;

import Slider.SliderOne;
import Slider.SliderTwo;

public class HomeFragment extends Fragment {

    private static final long AUTO_SCROLL_DELAY = 3000; // Milliseconds between auto scrolls

    private TextView greeting;
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
    private Button emergencyContactButton;
    private Button copingExercisesButton;
    private Button moreResourcesButton;
    private TextView dailyCheckinTitle;
    private TextView moodQuestion;
    private EditText moodInput;
    private Button submitCheckinButton;
    private TextView groundingExerciseTitle;
    private TextView groundingExerciseDescription;
    private Button startExerciseButton;
    private TextView journalPromptTitle;
    private TextView journalPromptText;
    private EditText journalEntryText;
    private Button saveJournalButton;
    private TextView moodOverTimeTitle;
    private View moodBarChart;
    private TextView moodOverTimeDescription;

    private Handler autoScrollHandler = new Handler();
    private Runnable autoScrollRunnable;
    private int currentPosition = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize TextViews
        greeting = view.findViewById(R.id.greeting);
        trackerTitle = view.findViewById(R.id.tracker_title);
        stepsCounter = view.findViewById(R.id.steps_counter);
        urgeLevelLabel = view.findViewById(R.id.urge_level_label);
        urgeLevelDisplay = view.findViewById(R.id.urge_level_display);
        copingStrategiesLabel = view.findViewById(R.id.coping_strategies_label);
        moodQuestion = view.findViewById(R.id.mood_input);

        // Initialize ViewPager2 for the slider
        sliderViewPager = view.findViewById(R.id.sliderViewPager);
        SliderAdapter sliderAdapter = new SliderAdapter(this);
        sliderViewPager.setAdapter(sliderAdapter);

        //initialize icons
        selfImprovementIcon = view.findViewById(R.id.self_improvement_icon);

        // Initialize Buttons
        logPositiveActionButton = view.findViewById(R.id.add_step_button);
        emergencyContactButton = view.findViewById(R.id.emergency_contact_button);
        copingExercisesButton = view.findViewById(R.id.coping_exercises_button);
        moreResourcesButton = view.findViewById(R.id.more_resources_button);
        submitCheckinButton = view.findViewById(R.id.submit_checkin_button);
        startExerciseButton = view.findViewById(R.id.grounding_exercise_button);
        saveJournalButton = view.findViewById(R.id.save_journal_button);

        // Initialize SeekBar
        urgeLevelSeekbar = view.findViewById(R.id.urge_level_seekbar);

        copingStrategiesInput = view.findViewById(R.id.coping_strategies_input);
        moodInput = view.findViewById(R.id.mood_input);
        journalEntryText = view.findViewById(R.id.journal_entry_text);

        moodBarChart = view.findViewById(R.id.mood_bar_chart);

        return view; // Return the inflated view
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shakeView(selfImprovementIcon);
        startAutoScroll(); // Start automatic sliding when the view is created

        // Optional: Pause auto-scroll on user interaction
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
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sliderViewPager != null && sliderViewPager.getScrollState() == ViewPager2.SCROLL_STATE_IDLE) {
            startAutoScroll(); // Restart auto-scroll if the fragment resumes and wasn't being dragged
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll(); // Stop automatic sliding when the fragment is paused
    }

    private void startAutoScroll() {
        if (sliderViewPager != null && sliderViewPager.getAdapter() != null) {
            stopAutoScroll(); // Ensure any existing callbacks are removed

            autoScrollRunnable = new Runnable() {
                @Override
                public void run() {
                    int itemCount = sliderViewPager.getAdapter().getItemCount();
                    if (itemCount > 0) {
                        currentPosition = (currentPosition + 1) % itemCount;
                        sliderViewPager.setCurrentItem(currentPosition, true); // 'true' for smooth scrolling
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
        ObjectAnimator translateX = ObjectAnimator.ofFloat(view, "translationX", 0f, -20f, 20f, -10f, 10f, 0f);
        translateX.setDuration(700);
        translateX.setInterpolator(new OvershootInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(translateX);
        animatorSet.start();
    }

    // Adapter for ViewPager2 (using Fragments for each slide)
    private static class SliderAdapter extends FragmentStateAdapter {
        private final List<Fragment> fragments;

        public SliderAdapter(@NonNull Fragment fragment) {
            super(fragment);
            this.fragments = new ArrayList<>();
            this.fragments.add(new SliderOne());
            this.fragments.add(new SliderTwo());
            // Add more fragments as needed
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
}