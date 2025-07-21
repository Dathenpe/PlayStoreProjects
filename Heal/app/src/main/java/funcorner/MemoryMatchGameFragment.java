package funcorner;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import memorymatch.HighScoreDialogFragment;
import ui.CustomMessageDialogFragment; // Import the custom dialog

public class MemoryMatchGameFragment extends Fragment {

    private GridLayout cardGridLayout;
    private TextView scoreTextView;
    private TextView timerTextView;
    private Button resetButton;
    private Spinner themeSpinner;
    private Button highScoresButton;
    private Button pauseButton;
    private FrameLayout overlayContainer;
    private View pausedCard;
    private View gameOverCard;
    private Button restartGameOverButton;


    private List<String> cardValues;
    private List<Button> cards;
    private Button firstCard = null;
    private Button secondCard = null;
    private boolean isBusy = false;
    private int matchesFound = 0;
    private int totalPairs;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime;
    private long timeWhenPaused = 0;
    private boolean timerRunning;
    private boolean isPaused = false;

    private static final String PREFS_NAME = "MemoryMatchGamePrefs";
    private static final String HIGH_SCORES_KEY = "high_scores";

    // --- Emoji Arrays for New Themes ---
    private static final String[] FRUIT_EMOJIS = {
            "🍎", "🍌", "🍒", "🍓", "🍍", "🥝", "🍉", "🍇",
            "🍏", "🍋", "🍑", "🥭", "🍈", "🥥", "🍐", "🍊"
    };

    private static final String[] CAR_EMOJIS = {
            "🚗", "🚕", "🚙", "🚌", "🏎️", "🚓", "🚒", "🚑",
            "🚚", "🚜", "🚲", "🛴", "🛵", "🏍️", "🚂", "🚀"
    };

    private static final String[] ANIMAL_EMOJIS = {
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
            "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐒", "🐔"
    };

    private static final String[] SPORT_EMOJIS = {
            "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉", "🎱",
            "🏓", "🏸", "🏒", "🏑", "🏏", "🥅", "⛳", "🏹"
    };

    private static final String[] FOOD_EMOJIS = {
            "🍔", "🍟", "🍕", "🌭", "🌮", "🌯", "🥙", "🥚",
            "🍳", "🥞", "🥓", "🥩", "🍗", "🍖", "🍞", "🥐"
    };

    private static final String[] FLAG_EMOJIS = {
            "🇺🇸", "🇨🇦", "🇲🇽", "🇧🇷", "🇦🇷", "🇬🇧", "🇫🇷", "🇩🇪",
            "🇮🇹", "🇪🇸", "🇨🇳", "🇯🇵", "🇰🇷", "🇮🇳", "🇦🇺", "🇳🇿"
    };

    private static final String[] SPACE_EMOJIS = {
            "🚀", "🛰️", "🌌", "🌠", "🌟", "💫", "✨", "☄️",
            "💥", "🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓"
    };

    private static final String[] MUSIC_EMOJIS = {
            "🎵", "🎶", "🎼", "🎤", "🎧", "🥁", "🎷", "🎺",
            "🎸", "🎻", "🎹", "🎙️", "📻", "🔊", "🔇", "🔔"
    };
    // --- End Emoji Arrays ---

    private String[] currentEmojis;
    private String currentTheme = "Fruits";
    private int selectedThemePosition = 0;
    private Context context;
    private MainActivity mainActivity;

    // Modified HighScoreEntry to store date instead of userId
    public static class HighScoreEntry implements java.io.Serializable {
        public String date; // Changed from userId to date
        public long timeTaken;
        public String theme;
        long timestamp;

        public HighScoreEntry(String date, long timeTaken, String theme, long timestamp) { // Updated constructor
            this.date = date; // Store date
            this.timeTaken = timeTaken;
            this.theme = theme;
            this.timestamp = timestamp;
        }
    }
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
        return inflater.inflate(R.layout.fragment_memory_match_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardGridLayout = view.findViewById(R.id.card_grid_layout);
        scoreTextView = view.findViewById(R.id.score_text_view);
        timerTextView = view.findViewById(R.id.timer_text_view);
        resetButton = view.findViewById(R.id.reset_button);
        themeSpinner = view.findViewById(R.id.theme_spinner);
        highScoresButton = view.findViewById(R.id.high_scores_button);
        pauseButton = view.findViewById(R.id.pause_button);
        overlayContainer = view.findViewById(R.id.overlay_container);
        pausedCard = view.findViewById(R.id.paused_card);
        gameOverCard = view.findViewById(R.id.game_over_card);
        restartGameOverButton = view.findViewById(R.id.button_restart_game_over);

        // Removed initializeLocalUserId() as userId is no longer stored for high scores

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.memory_match_themes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(adapter);

        String[] themes = getResources().getStringArray(R.array.memory_match_themes);
        selectedThemePosition = Arrays.asList(themes).indexOf(currentTheme);
        if (selectedThemePosition == -1) {
            selectedThemePosition = 0;
            currentTheme = "Fruits";
        }
        themeSpinner.setSelection(selectedThemePosition);


        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newTheme = parent.getItemAtPosition(position).toString();
                if (!currentTheme.equals(newTheme)) {
                    final int newPosition = position;
                    showConfirmationDialog("Change Theme",
                            "Are you sure you want to change the theme to " + newTheme + "? This will reset the current game.",
                            () -> { // Positive click
                                currentTheme = newTheme;
                                selectedThemePosition = newPosition;
                                Toast.makeText(getContext(), "Switching theme to " + currentTheme, Toast.LENGTH_SHORT).show();
                                updateEmojisForTheme();
                                resetGame();
                            },
                            () -> { // Negative click
                                themeSpinner.setSelection(selectedThemePosition);
                            });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        resetButton.setOnClickListener(v -> {
            showConfirmationDialog("Reset Game",
                    "Are you sure you want to reset the game? Your current progress will be lost.",
                    () -> { // Positive click
                        Toast.makeText(getContext(), "Resetting game...", Toast.LENGTH_SHORT).show();
                        resetGame();
                    },
                    null); // No negative action needed other than dismiss
        });
        highScoresButton.setOnClickListener(v -> showHighScoresDialog());
        pauseButton.setOnClickListener(v -> {
            if (isPaused) {
                resumeGameAndHideUI(true); // User clicked to resume, show toast
            } else {
                pauseGameAndShowUI(true); // User clicked to pause, show toast
            }
        });

        overlayContainer.setOnClickListener(v -> {
            if (isPaused) {
                resumeGameAndHideUI(true); // User clicked to resume from overlay, show toast
            }
        });

        restartGameOverButton.setOnClickListener(v -> {
            resetGame();
            overlayContainer.setVisibility(View.GONE);
            gameOverCard.setVisibility(View.GONE);
        });

        updateEmojisForTheme();
        initializeGame();
    }

    /**
     * Helper method to update the currentEmojis array based on the currentTheme.
     */
    private void updateEmojisForTheme() {
        switch (currentTheme) {
            case "Fruits":
                currentEmojis = FRUIT_EMOJIS;
                break;
            case "Cars":
                currentEmojis = CAR_EMOJIS;
                break;
            case "Animals":
                currentEmojis = ANIMAL_EMOJIS;
                break;
            case "Sports":
                currentEmojis = SPORT_EMOJIS;
                break;
            case "Food":
                currentEmojis = FOOD_EMOJIS;
                break;
            case "Flags":
                currentEmojis = FLAG_EMOJIS;
                break;
            case "Space":
                currentEmojis = SPACE_EMOJIS;
                break;
            case "Music":
                currentEmojis = MUSIC_EMOJIS;
                break;
            default:
                currentEmojis = FRUIT_EMOJIS;
                break;
        }
    }

    /**
     * Shows a confirmation dialog using CustomMessageDialogFragment.
     *
     * @param title The title of the dialog.
     * @param message The message to display.
     * @param positiveAction Runnable to execute on positive button click.
     * @param negativeAction Runnable to execute on negative button click (can be null).
     */
    private void showConfirmationDialog(String title, String message,
                                        Runnable positiveAction,
                                        @Nullable Runnable negativeAction) {
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                title,
                message,
                "Yes",
                "No"
        );
        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                if (positiveAction != null) {
                    positiveAction.run();
                }
                dialogFragment.dismiss();
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                if (negativeAction != null) {
                    negativeAction.run();
                }
                dialogFragment.dismiss();
            }
        });
        dialog.show(getParentFragmentManager(), "MemoryMatchConfirmationDialog");
    }


    // Removed initializeLocalUserId() as it's no longer needed

    private void initializeGame() {
        stopTimer();
        timerTextView.setText("Time: 00:00");
        isPaused = false;
        pauseButton.setText("Pause");
        overlayContainer.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        timeWhenPaused = 0;

        int numColumns = cardGridLayout.getColumnCount();
        int numRows = cardGridLayout.getRowCount();
        totalPairs = (numColumns * numRows) / 2;

        String[] emojisToUse = currentEmojis;

        if (emojisToUse.length < totalPairs) {
            Toast.makeText(getContext(), "Not enough emojis for current theme. Falling back to Fruits.", Toast.LENGTH_LONG).show();
            emojisToUse = FRUIT_EMOJIS;
            currentTheme = "Fruits";
            String[] themes = getResources().getStringArray(R.array.memory_match_themes);
            int fruitIndex = Arrays.asList(themes).indexOf("Fruits");
            if (themeSpinner.getSelectedItemPosition() != fruitIndex) {
                themeSpinner.setSelection(fruitIndex);
            }
        }

        if (emojisToUse.length < totalPairs) {
            Toast.makeText(getContext(), "Error: Not enough emojis even with fallback. Game cannot start.", Toast.LENGTH_LONG).show();
            return;
        }

        cardValues = new ArrayList<>();
        for (int i = 0; i < totalPairs; i++) {
            cardValues.add(emojisToUse[i]);
            cardValues.add(emojisToUse[i]);
        }
        Collections.shuffle(cardValues);

        cards = new ArrayList<>();
        cardGridLayout.removeAllViews();

        for (int i = 0; i < cardValues.size(); i++) {
            Button cardButton = new Button(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0; // Reverted to 0 for height, letting GridLayout distribute
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            cardButton.setLayoutParams(params);

            cardButton.setTextSize(24);
            cardButton.setBackgroundResource(R.drawable.card_background);
            cardButton.setText("");
            cardButton.setTag(cardValues.get(i));
            cardButton.setOnClickListener(this::onCardClicked);

            cards.add(cardButton);
            cardGridLayout.addView(cardButton);
        }

        matchesFound = 0;
        updateScore();
        startTimer();

        Log.d("MemoryMatch", "initializeGame: totalPairs = " + totalPairs);
        Log.d("MemoryMatch", "initializeGame: cardValues.size() = " + cardValues.size());
        Log.d("MemoryMatch", "initializeGame: cards.size() (buttons added to list) = " + cards.size());
        Log.d("MemoryMatch", "initializeGame: cardGridLayout.getChildCount() (buttons added to layout) = " + cardGridLayout.getChildCount());
    }

    private void onCardClicked(View view) {
        if (isBusy || isPaused) {
            return;
        }

        Button clickedCard = (Button) view;

        if (clickedCard.getText().toString().length() > 0 || clickedCard == firstCard) {
            return;
        }

        flipCard(clickedCard, (String) clickedCard.getTag());

        if (firstCard == null) {
            firstCard = clickedCard;
        } else {
            secondCard = clickedCard;
            isBusy = true;

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (((String) firstCard.getTag()).equals((String) secondCard.getTag())) {
                    matchesFound++;
                    updateScore();
                    firstCard.setEnabled(false);
                    secondCard.setEnabled(false);

                    animateMatch(firstCard);
                    animateMatch(secondCard);
                    Toast.makeText(getContext(), "That's a match", Toast.LENGTH_SHORT).show();

                    if (matchesFound == totalPairs) {
                        stopTimer();
                        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;
                        Toast.makeText(getContext(), "Congratulations! You matched all pairs in " + formatTime(timeTaken) + "!", Toast.LENGTH_LONG).show();
                        saveHighScore(timeTaken);
                        showGameOverScreen();
                    }
                } else {
                    flipCard(firstCard, "");
                    flipCard(secondCard, "");
                }
                firstCard = null;
                secondCard = null;
                isBusy = false;
            }, 1000);
        }
    }

    private void flipCard(final Button card, final String newText) {
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(card, "rotationY", 0f, 90f);
        flipOut.setDuration(200);
        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                card.setText(newText);
                ObjectAnimator flipIn = ObjectAnimator.ofFloat(card, "rotationY", 90f, 0f);
                flipIn.setDuration(200);
                flipIn.start();
            }
        });
        flipOut.start();
    }

    private void animateMatch(Button card) {
        ObjectAnimator fade = ObjectAnimator.ofFloat(card, "alpha", 1f, 0.7f);
        fade.setDuration(500);
        fade.start();
    }

    private void updateScore() {
        scoreTextView.setText("Matches: " + matchesFound);
    }

    private void startTimer() {
        startTime = System.currentTimeMillis() - timeWhenPaused;
        timerRunning = true;
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTime;
                timerTextView.setText("Time: " + formatTime(millis / 1000));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopTimer() {
        if (timerRunning && timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunning = false;
        }
    }

    private String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void saveHighScore(long timeTaken) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(HIGH_SCORES_KEY, null);
        Type type = new TypeToken<ArrayList<HighScoreEntry>>() {}.getType();
        List<HighScoreEntry> highScores = json == null ? new ArrayList<>() : gson.fromJson(json, type);

        // Get current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Create HighScoreEntry with date instead of userId
        highScores.add(new HighScoreEntry(currentDate, timeTaken, currentTheme, System.currentTimeMillis()));

        Collections.sort(highScores, Comparator.comparingLong(entry -> entry.timeTaken));

        if (highScores.size() > 5) {
            highScores = highScores.subList(0, 5);
        }

        String updatedJson = gson.toJson(highScores);
        prefs.edit().putString(HIGH_SCORES_KEY, updatedJson).apply();

        Toast.makeText(getContext(), "High score saved locally!", Toast.LENGTH_SHORT).show();
    }

    private List<HighScoreEntry> loadHighScores() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(HIGH_SCORES_KEY, null);
        Type type = new TypeToken<ArrayList<HighScoreEntry>>() {}.getType();
        return json == null ? new ArrayList<>() : gson.fromJson(json, type);
    }

    private void showHighScoresDialog() {
        // Pause the game logic and show the UI, but without the toast for this specific action
        if (!isPaused) {
            pauseGameAndShowUI(false); // Pause without toast
        }
        // Ensure paused card is hidden if it was shown by pauseGameAndShowUI(false)
        pausedCard.setVisibility(View.GONE); // Explicitly hide the paused card
        pauseButton.setText("Resume"); // Show resume text
        overlayContainer.setVisibility(View.GONE); // Hide overlay
        Toast.makeText(getContext(), "Game Paused (High Scores)", Toast.LENGTH_SHORT).show(); // Specific toast for high scores
        List<HighScoreEntry> scores = loadHighScores();
        // Pass null for localUserId as it's no longer used for highlighting
        HighScoreDialogFragment dialogFragment = HighScoreDialogFragment.newInstance(scores, null);
        dialogFragment.show(getParentFragmentManager(), "high_scores_dialog");
    }

    /**
     * Pauses the game logic only (stops timer, sets flag). Does NOT affect UI or show toasts.
     */
    private void pauseGame() {
        if ( isPaused) return; // Don't pause if already over or paused

        isPaused = true;
        stopTimer();
        timeWhenPaused = System.currentTimeMillis() - startTime;
        for (Button card : cards) {
            card.setEnabled(false);
        }
    }

    /**
     * Pauses the game logic AND updates UI to show "Game Paused" with a toast.
     * @param showToast If true, a "Game Paused" toast will be shown.
     */
    private void pauseGameAndShowUI(boolean showToast) {
        pauseGame(); // Handle logic first

        pauseButton.setText("Resume");
        overlayContainer.setVisibility(View.VISIBLE);
        pausedCard.setVisibility(View.VISIBLE);
        gameOverCard.setVisibility(View.GONE);

        if (showToast) {
            Toast.makeText(getContext(), "Game Paused", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Resumes the game logic only (starts timer, sets flag). Does NOT affect UI or show toasts.
     */
    private void resumeGame() {
        if (!isPaused) return; // Don't resume if game over or not paused

        isPaused = false;
        startTimer();
        for (Button card : cards) {
            card.setEnabled(true);
        }
    }

    /**
     * Resumes the game logic AND hides "Game Paused" UI with a toast.
     * @param showToast If true, a "Game Resumed" toast will be shown.
     */
    private void resumeGameAndHideUI(boolean showToast) {
        resumeGame(); // Handle logic first

        pauseButton.setText("Pause");
        overlayContainer.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);

        if (showToast) {
            Toast.makeText(getContext(), "Game Resumed", Toast.LENGTH_SHORT).show();
        }
    }


    private void showGameOverScreen() {
        overlayContainer.setVisibility(View.VISIBLE);
        gameOverCard.setVisibility(View.VISIBLE);
        pausedCard.setVisibility(View.GONE);

        for (Button card : cards) {
            card.setEnabled(false);
        }
    }

    private void resetGame() {
        stopTimer();
        timerTextView.setText("Time: 00:00");
        isPaused = false;
        pauseButton.setText("Pause");
        overlayContainer.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        timeWhenPaused = 0;

        firstCard = null;
        secondCard = null;
        isBusy = false;
        matchesFound = 0;
        updateScore();

        initializeGame();
        Toast.makeText(getContext(), "Game Reset!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause the game logic only (no UI, no toast) when fragment is minimized/exited
        if (!isPaused) {
            pauseGame();
        }
    }

    @Override
    public void onResume(){
        mainActivity.toolbar.setTitle("Memory Match Game");
        mainActivity.navigationView.setCheckedItem(R.id.nav_fun_corner);
        mainActivity.MenuTrigger.setVisibility(View.GONE);
        mainActivity.Fab.setVisibility(View.GONE);
        super.onResume();
        // If the game was paused by onPause (e.g., app minimized) and the UI is NOT visible,
        // automatically resume it without a toast.
        if (isPaused  && overlayContainer.getVisibility() != View.VISIBLE) {
            resumeGame(); // Auto-resume logic only, no UI or toast
        }
    }
}
