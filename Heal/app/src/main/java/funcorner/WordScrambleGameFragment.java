package funcorner;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import wordscramble.GameMode;
import wordscramble.HighScoreDialogFragment;
import wordscramble.HighScoreEntry;
import wordscramble.InfoDialogFragment;

public class WordScrambleGameFragment extends Fragment {

    private static final String TAG = "WordScrambleGameFragment";
    private static final String PREFS_NAME = "WordScrambleHighScores";
    private static final String HIGH_SCORES_KEY = "high_scores";
    private static final long FEEDBACK_ANIMATION_DURATION = 300; // Milliseconds for color animation
    private static final long NEXT_WORD_DELAY = 700; // Milliseconds delay after correct guess
    private static final long LAYOUT_RETRY_DELAY = 50; // Milliseconds to retry layout if width is 0
    private static final long SHAKE_ANIMATION_DURATION = 500; // Duration for shake animation

    // Clue related constants
    private static final int MAX_CLUES_PER_GAME_SESSION = 5; // Overall clues per game session
    private static final int MAX_CLUES_PER_WORD = 2; // Maximum clues allowed per single word
    private static final int ENDLESS_MODE_CLUE_INTERVAL_ROUNDS = 5; // Get 1 clue every 5 rounds in endless mode

    // Word list for the game - now populated from file
    private List<String> wordListFromFile;

    // UI Elements
    private GridLayout scrambledLettersContainer;
    private TextView guessDisplayText;
    private MaterialButton submitButton;
    private TextView scoreTextView;
    private TextView timerTextView;
    private TextView roundTextView;
    private Spinner modeSpinner;
    private MaterialButton nextWordButton;
    private MaterialButton resetButton;
    private MaterialButton highScoresButton;
    private MaterialButton pauseButton;
    private MaterialButton backspaceButton;
    private MaterialButton clueButton; // New clue button
    private FrameLayout overlayContainer;
    private View pausedCard;
    private MaterialCardView gameOverCard; // Changed to MaterialCardView
    private MaterialButton restartGameOverButton;
    // New buttons for pause dialog
    private MaterialButton resumeGameButton;
    private MaterialButton restartGamePausedButton;
    private MaterialButton saveAndQuitButton; // New Save and Quit button
    private MaterialButton infoGameButton; // New Info button
    // New TextViews for game summary in the game over card
    private TextView summaryScoreTextView;
    private TextView correctWordsSummaryTextView;
    private TextView missedWordsSummaryTextView;


    // Game State Variables
    private String currentWord;
    private char[] scrambledWordChars;
    private int score;
    private Random random;

    private GameMode currentGameMode = GameMode.TEN_ROUNDS;
    private int currentRound;
    private int totalRounds;
    private List<String> correctWordsList;
    private List<String> missedWordsList;

    // For interactive letter selection
    private List<TextView> scrambledLetterViews; // Stores references to all letter TextViews
    private List<Integer> selectedLetterIndices; // Stores indices of selected selected letters from scrambledLetterViews

    // Clue state variables
    private int cluesRemaining; // Session-wide clues
    private int cluesUsedForCurrentWord; // Clues used on the current word

    // Timer Variables
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime;
    private long timeWhenPaused = 0;
    private boolean timerRunning;
    private boolean isPaused = false;
    private boolean isGameOver = false;
    private boolean isBusyWithFeedback = false; // New flag to prevent input during feedback animation

    private MainActivity mainActivity;
    private Context context;

    private final Gson gson = new Gson();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        random = new Random();
        timerHandler = new Handler(Looper.getMainLooper());
        correctWordsList = new ArrayList<>();
        missedWordsList = new ArrayList<>();
        wordListFromFile = new ArrayList<>();
        scrambledLetterViews = new ArrayList<>();
        selectedLetterIndices = new ArrayList<>();
        loadWordsFromFile();
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
        return inflater.inflate(R.layout.fragment_word_scramble_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI elements
        scrambledLettersContainer = view.findViewById(R.id.scrambled_letters_container);
        guessDisplayText = view.findViewById(R.id.guess_display_text_view);
        submitButton = view.findViewById(R.id.submit_button);
        scoreTextView = view.findViewById(R.id.score_text_view);
        timerTextView = view.findViewById(R.id.timer_text_view);
        roundTextView = view.findViewById(R.id.round_text_view);
        modeSpinner = view.findViewById(R.id.mode_spinner);
        nextWordButton = view.findViewById(R.id.next_word_button);
        resetButton = view.findViewById(R.id.reset_button);
        highScoresButton = view.findViewById(R.id.high_scores_button);
        pauseButton = view.findViewById(R.id.pause_button);
        backspaceButton = view.findViewById(R.id.backspace_button);
        clueButton = view.findViewById(R.id.clue_button); // Initialize clue button
        overlayContainer = view.findViewById(R.id.overlay_container);
        pausedCard = view.findViewById(R.id.paused_card);
        gameOverCard = view.findViewById(R.id.game_over_card); // Cast to MaterialCardView
        restartGameOverButton = view.findViewById(R.id.button_restart_game_over);
        // New buttons for pause dialog
        resumeGameButton = view.findViewById(R.id.button_resume_game);
        restartGamePausedButton = view.findViewById(R.id.button_restart_game_paused);
        saveAndQuitButton = view.findViewById(R.id.button_save_and_quit); // Initialize save and quit button
        infoGameButton = view.findViewById(R.id.button_info_game); // Initialize info button

        // Initialize new summary TextViews
        summaryScoreTextView = view.findViewById(R.id.summary_score_text_view);
        correctWordsSummaryTextView = view.findViewById(R.id.correct_words_summary_text_view);
        missedWordsSummaryTextView = view.findViewById(R.id.missed_words_summary_text_view);

        // Set up mode spinner
        ArrayAdapter<GameMode> modeAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, GameMode.values());
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setSelection(currentGameMode.ordinal()); // Set initial selection based on currentGameMode

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GameMode selectedMode = (GameMode) parent.getItemAtPosition(position);
                if (selectedMode != currentGameMode) {
                    // Store the current game mode before showing the dialog
                    GameMode previousGameMode = currentGameMode;
                    showConfirmationDialog("Change Game Mode",
                            "Are you sure you want to change the game mode to " + selectedMode.displayName + "? This will reset the current game.",
                            (dialog, which) -> {
                                currentGameMode = selectedMode;
                                initializeGame();
                                Toast.makeText(getContext(), "Game mode set to " + currentGameMode.displayName, Toast.LENGTH_SHORT).show();
                            },
                            (dialog, which) -> {
                                // If "No" is clicked, revert the spinner selection
                                modeSpinner.setSelection(previousGameMode.ordinal());
                            },
                            dialog -> { // Corrected lambda for OnDismissListener
                                // On dialog dismissal (e.g., by clicking outside), revert the spinner selection
                                modeSpinner.setSelection(previousGameMode.ordinal());
                            });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Set up other listeners
        submitButton.setOnClickListener(v -> checkGuess());
        nextWordButton.setOnClickListener(v -> {
            showConfirmationDialog("Skip Word",
                    "Are you sure you want to skip this word? You won't get points for it.",
                    (dialog, which) -> {
                        Toast.makeText(getContext(), "Skipped word was: " + currentWord, Toast.LENGTH_LONG).show();
                        missedWordsList.add(currentWord);
                        currentRound++;
                        generateNewWord();
                    },
                    null, null); // No onDismiss for skip word
        });
        resetButton.setOnClickListener(v -> {
            showConfirmationDialog("Reset Game",
                    "Are you sure you want to reset the game? Your current progress will be lost.",
                    (dialog, which) -> resetGame(),
                    null, null); // No onDismiss for reset
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
            // If the user taps the overlay, it's a manual resume, so show toast.
            if (isPaused && !isGameOver) {
                resumeGameAndHideUI(true); // User clicked to resume from overlay, show toast
            }
        });

        restartGameOverButton.setOnClickListener(v -> {
            // No confirmation dialog for restart from game over screen
            resetGame();
            overlayContainer.setVisibility(View.GONE);
            gameOverCard.setVisibility(View.GONE);
        });

        // Set up new pause dialog button listeners
        resumeGameButton.setOnClickListener(v -> resumeGameAndHideUI(true)); // Resume button in dialog
        restartGamePausedButton.setOnClickListener(v -> {
            showConfirmationDialog("Restart Game",
                    "Are you sure you want to restart the game? Your current progress will be lost.",
                    (dialog, which) -> {
                        resetGame();
                        overlayContainer.setVisibility(View.GONE);
                        pausedCard.setVisibility(View.GONE); // Ensure paused card is hidden
                    },
                    null, null); // No onDismiss for restart from paused
        });

        // Set up save and quit button listener
        saveAndQuitButton.setOnClickListener(v -> {
            if (currentGameMode == GameMode.ENDLESS) {
                showConfirmationDialog("Save and Quit",
                        "Are you sure you want to save your score and quit the game?",
                        (dialog, which) -> {
                            handleGameOver(); // Show game over summary first
                            // Delay exiting the fragment slightly to allow the user to see the summary
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (getParentFragmentManager() != null) {
                                    getParentFragmentManager().popBackStack(); // Exit the fragment
                                } else if (getActivity() != null) {
                                    getActivity().finish(); // Fallback for activity
                                }
                                Toast.makeText(getContext(), "Game saved and quit!", Toast.LENGTH_SHORT).show();
                            }, 1500); // 1.5 second delay
                        },
                        null, null); // No onDismiss for save and quit
            } else {
                // This toast should ideally not be shown if the button is hidden correctly
                Toast.makeText(getContext(), "Save and Quit is only available in Endless Mode.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up info button listener
        infoGameButton.setOnClickListener(v -> showInfoDialog());

        // Set up backspace button listener
        backspaceButton.setOnClickListener(v -> removeLastLetterFromGuess());
        // Set up long press listener for backspace to clear all
        backspaceButton.setOnLongClickListener(v -> {
            clearAllLettersFromGuess();
            Toast.makeText(getContext(), "Guess cleared!", Toast.LENGTH_SHORT).show();
            return true; // Consume the long click event
        });

        // Set up clue button listener with confirmation
        clueButton.setOnClickListener(v -> showClueConfirmationDialog());

        initializeGame();
    }

    /**
     * Loads words from the wordlist.txt file in the raw resources.
     */
    private void loadWordsFromFile() {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot load words from file.");
            return;
        }
        wordListFromFile.clear();
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = getResources().openRawResource(R.raw.wordlist); // Assuming wordlist.txt is in res/raw
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim().toUpperCase(Locale.getDefault());
                if (!word.isEmpty()) {
                    wordListFromFile.add(word);
                }
            }
            Log.d(TAG, "Loaded " + wordListFromFile.size() + " words from file.");
        } catch (IOException e) {
            Log.e(TAG, "Error reading wordlist.txt: " + e.getMessage());
            Toast.makeText(getContext(), "Error loading words. Please check wordlist.txt", Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (reader != null) reader.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams: " + e.getMessage());
            }
        }

        if (wordListFromFile.isEmpty()) {
            Log.w(TAG, "Word list from file is empty. Falling back to hardcoded words.");
            // Fallback to a default list if the file is empty or missing
            Collections.addAll(wordListFromFile,
                    "APPLE", "BANANA", "ORANGE", "GRAPE", "STRAWBERRY", "BLUEBERRY",
                    "COMPUTER", "PROGRAMMING", "DEVELOPER", "JAVASCRIPT", "PYTHON", "ANDROID",
                    "FRAGMENT", "ACTIVITY", "RECYCLERVIEW", "LAYOUT", "WIDGET", "BUTTON",
                    "SUNSHINE", "RAINBOW", "MOUNTAIN", "OCEAN", "FOREST", "RIVER",
                    "KEYBOARD", "MOUSE", "MONITOR", "PRINTER", "SPEAKER", "HEADPHONE",
                    "ELEPHANT", "GIRAFFE", "TIGER", "LION", "ZEBRA", "MONKEY",
                    "GUITAR", "PIANO", "DRUMS", "VIOLIN", "TRUMPET", "FLUTE",
                    "FOOTBALL", "BASKETBALL", "TENNIS", "SOCCER", "VOLLEYBALL", "SWIMMING"
            );
        }
    }


    /**
     * Shows a confirmation dialog with a warning icon.
     * @param title The title of the dialog.
     * @param message The message to display.
     * @param positiveClickListener Listener for the positive button.
     * @param negativeClickListener Listener for the negative button (can be null).
     * @param dismissListener Optional listener for when the dialog is dismissed (e.g., by outside touch).
     */
    private void showConfirmationDialog(String title, String message,
                                        android.content.DialogInterface.OnClickListener positiveClickListener,
                                        @Nullable android.content.DialogInterface.OnClickListener negativeClickListener,
                                        @Nullable android.content.DialogInterface.OnDismissListener dismissListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", positiveClickListener)
                .setNegativeButton("No", negativeClickListener != null ? negativeClickListener : (dialog, which) -> dialog.dismiss());

        // Make the dialog cancelable by outside touch
        builder.setCancelable(true); // Changed to true

        // Set the optional dismiss listener
        if (dismissListener != null) {
            builder.setOnDismissListener(dismissListener);
        }

        builder.show();
    }

    /**
     * Shows a confirmation dialog specifically for using a clue.
     */
    private void showClueConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Use a Clue?")
                .setMessage("Are you sure you want to use a clue? You have " + cluesRemaining + " clues remaining for this game session, and " + (MAX_CLUES_PER_WORD - cluesUsedForCurrentWord) + " clues left for this word.")
                .setIcon(R.drawable.ic_lightbulb_outline) // Use the new lightbulb icon
                .setPositiveButton("Yes", (dialog, which) -> giveClue())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Shows the game information dialog.
     */
    private void showInfoDialog() {
        InfoDialogFragment dialogFragment = InfoDialogFragment.newInstance();
        dialogFragment.show(getParentFragmentManager(), "game_info_dialog");
    }

    /**
     * Initializes a new game, resetting scores, timer, and generating the first word.
     */
    private void initializeGame() {
        Log.d(TAG, "Initializing game...");
        stopTimer();
        timerTextView.setText("Time: 00:00");
        score = 0;
        currentRound = 1;
        isGameOver = false;
        isPaused = false;
        timeWhenPaused = 0;
        correctWordsList.clear();
        missedWordsList.clear();
        isBusyWithFeedback = false; // Reset busy flag

        // Set total rounds based on game mode
        switch (currentGameMode) {
            case ENDLESS:
                totalRounds = -1; // -1 indicates endless mode
                cluesRemaining = MAX_CLUES_PER_GAME_SESSION; // Start with max clues for endless
                break;
            case TEN_ROUNDS:
                totalRounds = 10;
                cluesRemaining = MAX_CLUES_PER_GAME_SESSION;
                break;
            case TWENTY_ROUNDS:
                totalRounds = 20;
                cluesRemaining = MAX_CLUES_PER_GAME_SESSION;
                break;
        }
        cluesUsedForCurrentWord = 0; // Reset clues used for the current word

        updateScoreAndRoundDisplay();
        updateClueButtonText(); // Update clue button text

        overlayContainer.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        setGameControlsEnabled(true); // Call this after setting currentGameMode

        // Set initial pause button icon
        pauseButton.setIconResource(R.drawable.ic_pause_white_24dp);

        generateNewWord();
        startTimer();
    }

    /**
     * Generates a new random word and scrambles it for the game.
     */
    private void generateNewWord() {
        if (totalRounds != -1 && currentRound > totalRounds) {
            handleGameOver();
            return;
        }

        if (wordListFromFile.isEmpty()) {
            Toast.makeText(getContext(), "No words available to play!", Toast.LENGTH_LONG).show();
            handleGameOver();
            return;
        }

        String newWord = wordListFromFile.get(random.nextInt(wordListFromFile.size()));
        // Ensure the scrambled word is not the same as the original word
        char[] tempScrambledChars = scrambleWord(newWord);
        while (String.valueOf(tempScrambledChars).equals(newWord)) {
            tempScrambledChars = scrambleWord(newWord);
        }
        currentWord = newWord;
        scrambledWordChars = tempScrambledChars;

        displayScrambledWord(); // Update UI with individual letters
        guessDisplayText.setText(""); // Clear previous guess
        selectedLetterIndices.clear(); // Clear selected indices for new word
        setGameControlsEnabled(true); // Re-enable controls for new word
        updateScoreAndRoundDisplay(); // Update round display for new word
        cluesUsedForCurrentWord = 0; // Reset clues used for the new word

        // Grant a clue in Endless mode every few rounds
        if (currentGameMode == GameMode.ENDLESS && currentRound > 1 && (currentRound - 1) % ENDLESS_MODE_CLUE_INTERVAL_ROUNDS == 0) {
            if (cluesRemaining < MAX_CLUES_PER_GAME_SESSION) {
                cluesRemaining++;
                Toast.makeText(getContext(), "You earned a clue!", Toast.LENGTH_SHORT).show();
            }
        }
        updateClueButtonText(); // Update clue button text after potential clue gain

        Log.d(TAG, "New word generated: " + currentWord + ", Scrambled: " + String.valueOf(scrambledWordChars));
    }

    /**
     * Scrambles a given word.
     * @param word The word to scramble.
     * @return The scrambled word as a char array.
     */
    private char[] scrambleWord(String word) {
        List<Character> characters = new ArrayList<>();
        for (char c : word.toCharArray()) {
            characters.add(c);
        }
        Collections.shuffle(characters);
        char[] scrambled = new char[characters.size()];
        for (int i = 0; i < characters.size(); i++) {
            scrambled[i] = characters.get(i);
        }
        return scrambled;
    }

    /**
     * Dynamically creates and displays individual letter views for the scrambled word in a grid.
     */
    private void displayScrambledWord() {
        scrambledLettersContainer.removeAllViews(); // Clear previous letters
        scrambledLetterViews.clear(); // Clear references to old TextViews

        Context context = getContext();
        if (context == null) return;

        // Ensure the container is laid out to get its width
        scrambledLettersContainer.post(() -> {
            int containerWidth = scrambledLettersContainer.getWidth();
            if (containerWidth == 0) {
                Log.w(TAG, "Scrambled letters container width is 0 during post, re-posting in " + LAYOUT_RETRY_DELAY + "ms...");
                // Re-post after a short delay to give layout system more time
                scrambledLettersContainer.postDelayed(this::displayScrambledWord, LAYOUT_RETRY_DELAY);
                return;
            }

            // Dynamically set column count based on word length for better aesthetics
            int wordLength = scrambledWordChars.length;
            int dynamicColumnCount;
            if (wordLength <= 5) { // For words up to 5 letters, use word length as column count
                dynamicColumnCount = wordLength;
            } else { // For words longer than 5 letters, cap at 5 columns
                dynamicColumnCount = 5;
            }
            scrambledLettersContainer.setColumnCount(dynamicColumnCount);


            // Convert 8dp padding of the container to pixels
            int containerPaddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            int usableWidth = containerWidth - (2 * containerPaddingPx);

            int marginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
            int totalHorizontalMarginPerLetter = 2 * marginPx; // Left and right margin for each letter

            // Calculate ideal letter size based on the number of columns and usable width
            // Use dynamicColumnCount here
            int calculatedLetterSizePx = (usableWidth - (dynamicColumnCount * totalHorizontalMarginPerLetter)) / dynamicColumnCount;

            // Set a minimum letter size for readability, increased to 40dp
            int minLetterSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());

            int actualLetterSizePx = Math.max(minLetterSizePx, calculatedLetterSizePx);

            Log.d(TAG, "Container Width: " + containerWidth + ", Calculated Letter Size: " + calculatedLetterSizePx + ", Actual Letter Size: " + actualLetterSizePx + ", Dynamic Columns: " + dynamicColumnCount);

            // Re-clear views just in case, though it should be clear from the initial call
            scrambledLettersContainer.removeAllViews();
            scrambledLetterViews.clear(); // Clear again before adding new ones

            for (int i = 0; i < scrambledWordChars.length; i++) {
                char c = scrambledWordChars[i];
                TextView letterView = new TextView(getContext());
                // Use GridLayout.LayoutParams
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = actualLetterSizePx;
                params.height = actualLetterSizePx;
                params.setMargins(marginPx, marginPx, marginPx, marginPx);
                params.setGravity(Gravity.CENTER); // Center each item in its grid cell
                letterView.setLayoutParams(params);

                letterView.setText(String.valueOf(c));
                letterView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24); // Keep original text size
                letterView.setGravity(Gravity.CENTER);

                // Set initial appearance
                resetSingleLetterView(letterView);

                final int letterIndex = i; // Store final index for use in listener
                letterView.setOnClickListener(v -> {
                    // This is the gatekeeper. No action if paused, game over, or animating.
                    if (isBusyWithFeedback || isPaused || isGameOver) {
                        return;
                    }
                    handleLetterClick((TextView) v, letterIndex);
                });

                scrambledLettersContainer.addView(letterView);
                scrambledLetterViews.add(letterView); // Add to our list of views
            }
        });
    }

    /**
     * Handles a click on a scrambled letter, either appending it to the guess or deselecting it.
     * This version uses the selectedLetterIndices list as the single source of truth and leaves letters always clickable.
     * @param letterView The TextView of the clicked letter.
     * @param index The index of the letterView in the scrambledLetterViews list.
     */
    private void handleLetterClick(TextView letterView, int index) {
        // Determine if we are selecting or deselecting based on the presence of the index in our list.
        boolean isCurrentlySelected = selectedLetterIndices.contains(index);

        if (isCurrentlySelected) {
            // --- DESELECTION LOGIC ---
            Log.d(TAG, "Deselecting letter at index: " + index);

            // 1. Find the position of the letter in the guess string.
            int positionInGuess = selectedLetterIndices.indexOf(index);
            if (positionInGuess == -1) {
                Log.e(TAG, "State inconsistency: letter is selected but not found in index list.");
                return; // Should not happen
            }

            // 2. Remove the character from the guess display string.
            StringBuilder newGuess = new StringBuilder(guessDisplayText.getText().toString());
            newGuess.deleteCharAt(positionInGuess);
            guessDisplayText.setText(newGuess.toString());

            // 3. Remove the index from our tracking list using its value.
            selectedLetterIndices.remove(Integer.valueOf(index));

            // 4. Reset the letter's appearance.
            resetSingleLetterView(letterView);

        } else {
            // --- SELECTION LOGIC ---
            Log.d(TAG, "Selecting letter at index: " + index);

            // 1. Append the new letter to the guess display.
            guessDisplayText.append(letterView.getText());

            // 2. Add the index to our tracking list.
            selectedLetterIndices.add(index);

            // 3. Visually mark the letter as used (selected).
            markLetterAsSelected(letterView);
        }
    }

    /**
     * Changes the appearance of a letter view to show it has been selected.
     * @param letterView The TextView to mark as selected.
     */
    private void markLetterAsSelected(TextView letterView) {
        Context context = getContext();
        if (context == null) return;

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        drawable.setColor(ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant)); // A darker color for selected
        letterView.setBackground(drawable);
        letterView.setTextColor(ContextCompat.getColor(context, R.color.md_theme_surface)); // Lighter text
    }

    /**
     * Resets the visual state of a single letter TextView to its default, unselected state.
     * @param letterView The TextView to reset.
     */
    private void resetSingleLetterView(TextView letterView) {
        Context context = getContext();
        if (context == null) return;

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        drawable.setColor(ContextCompat.getColor(context, R.color.lightgray)); // Default color
        letterView.setBackground(drawable);
        letterView.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onBackground)); // Default text color
    }

    /**
     * Removes the last letter from the guess TextView and re-enables the corresponding letter view.
     */
    private void removeLastLetterFromGuess() {
        String currentGuess = guessDisplayText.getText().toString();
        if (currentGuess.length() > 0 && !selectedLetterIndices.isEmpty()) {
            // Get the index of the last selected letter
            int lastScrambledIndex = selectedLetterIndices.remove(selectedLetterIndices.size() - 1);

            // Re-enable the corresponding TextView in the scrambled letters container
            if (lastScrambledIndex >= 0 && lastScrambledIndex < scrambledLetterViews.size()) {
                TextView letterView = scrambledLetterViews.get(lastScrambledIndex);
                if (letterView != null) {
                    resetSingleLetterView(letterView);
                }
            }
            // Update guess EditText after removing the letter and resetting its view
            guessDisplayText.setText(currentGuess.substring(0, currentGuess.length() - 1));
        }
    }

    /**
     * Clears all letters from the guess display and resets all scrambled letter views.
     */
    private void clearAllLettersFromGuess() {
        guessDisplayText.setText("");
        selectedLetterIndices.clear(); // Clear all selected indices

        // Reset all the letter views to their initial state
        for (TextView letterView : scrambledLetterViews) {
            resetSingleLetterView(letterView);
        }
    }


    /**
     * Checks the user's guess against the current word.
     */
    private void checkGuess() {
        if (isBusy() || isPaused || isGameOver || isBusyWithFeedback) return; // Prevent input during feedback

        String guess = guessDisplayText.getText().toString().trim().toUpperCase(Locale.getDefault());

        if (guess.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a guess!", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Debugging for "sarcasm" issue ---
        Log.d(TAG, "User Guess: '" + guess + "', Correct Word: '" + currentWord + "'");
        // --- End Debugging ---

        isBusyWithFeedback = true; // Set busy flag

        if (guess.equals(currentWord)) {
            score += 10; // Award points for correct guess
            correctWordsList.add(currentWord); // Add to correct words list
            Toast.makeText(getContext(), "Correct! Well well!", Toast.LENGTH_SHORT).show();
            animateFeedback(true); // Animate for correct guess
            shakeLettersContainer(); // Shake the container on correct guess
            timerHandler.postDelayed(() -> {
                currentRound++; // Increment round only on correct guess
                generateNewWord(); // Move to the next word
                isBusyWithFeedback = false; // Reset busy flag after delay
            }, NEXT_WORD_DELAY);
        } else {
            Toast.makeText(getContext(), "Incorrect guess. Try again!", Toast.LENGTH_SHORT).show();
            animateFeedback(false); // Animate for incorrect guess
            timerHandler.postDelayed(() -> {
                isBusyWithFeedback = false; // Reset busy flag after animation
                clearAllLettersFromGuess(); // Reset guess on incorrect attempt
            }, FEEDBACK_ANIMATION_DURATION);
        }
    }

    /**
     * Provides a clue by revealing one unselected letter of the current word.
     */
    private void giveClue() {
        if (isGameOver || isPaused || isBusyWithFeedback) return;

        if (cluesRemaining <= 0) {
            Toast.makeText(getContext(), "No clues left for this game session!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cluesUsedForCurrentWord >= MAX_CLUES_PER_WORD) {
            Toast.makeText(getContext(), "Already used " + MAX_CLUES_PER_WORD + " clues for this word!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find an unselected letter from the scrambled letters that matches a character in the current word
        int foundScrambledIndex = -1;
        char nextCorrectChar = '\0';

        // Iterate through the correct word to find the next character needed in the guess
        String currentGuess = guessDisplayText.getText().toString();
        for (int i = 0; i < currentWord.length(); i++) {
            char targetChar = currentWord.charAt(i);
            if (i >= currentGuess.length() || currentGuess.charAt(i) != targetChar) {
                nextCorrectChar = targetChar;
                break;
            }
        }

        if (nextCorrectChar == '\0') {
            // All letters are already correctly placed or guess is complete
            Toast.makeText(getContext(), "Word is already complete or no more clues can be given for this word!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Now find this 'nextCorrectChar' in the scrambled letters that hasn't been selected yet
        for (int i = 0; i < scrambledWordChars.length; i++) {
            if (scrambledWordChars[i] == nextCorrectChar && !selectedLetterIndices.contains(i)) {
                foundScrambledIndex = i;
                break;
            }
        }

        if (foundScrambledIndex != -1) {
            TextView letterViewToClue = scrambledLetterViews.get(foundScrambledIndex);
            handleLetterClick(letterViewToClue, foundScrambledIndex); // Simulate a click to add it to guess
            cluesRemaining--;
            cluesUsedForCurrentWord++; // Increment clues used for current word
            updateClueButtonText();
            Toast.makeText(getContext(), "Clue used! " + cluesRemaining + " session clues left, " + (MAX_CLUES_PER_WORD - cluesUsedForCurrentWord) + " for this word.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Could not find a suitable clue. Try guessing!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Animates the background color of the scrambled letters to provide feedback.
     * It iterates through each individual letter TextView and animates its background.
     * @param isCorrect True for correct guess (green), false for incorrect (red).
     */
    private void animateFeedback(boolean isCorrect) {
        if (getContext() == null) return;

        @ColorInt int startColor = ContextCompat.getColor(getContext(), R.color.lightgray); // Default letter background
        @ColorInt int endColor = isCorrect ? ContextCompat.getColor(getContext(), R.color.md_theme_tertiaryContainer) : // Greenish for correct
                ContextCompat.getColor(getContext(), R.color.md_theme_errorContainer); // Reddish for incorrect

        float cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        // Iterate through all child TextViews in the GridLayout and animate their backgrounds
        for (int i = 0; i < scrambledLettersContainer.getChildCount(); i++) {
            View child = scrambledLettersContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView letterView = (TextView) child;

                // Create a new GradientDrawable for the animation to ensure rounded corners
                GradientDrawable animatedDrawable = new GradientDrawable();
                animatedDrawable.setShape(GradientDrawable.RECTANGLE);
                animatedDrawable.setCornerRadius(cornerRadius);

                ObjectAnimator colorAnimator = ObjectAnimator.ofObject(
                        animatedDrawable, // Target the drawable itself
                        "color", // Animate the color property of the drawable
                        new ArgbEvaluator(),
                        startColor,
                        endColor
                );
                colorAnimator.setDuration(FEEDBACK_ANIMATION_DURATION);
                colorAnimator.setRepeatCount(1); // Play once to target color, then once back
                colorAnimator.setRepeatMode(ObjectAnimator.REVERSE); // Reverse to go back to original color

                // Set the drawable as the background of the TextView
                letterView.setBackground(animatedDrawable);
                colorAnimator.start();
            }
        }
    }

    /**
     * Shakes the scrambled letters container.
     */
    private void shakeLettersContainer() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(scrambledLettersContainer, "translationX", 0, 25, -25, 20, -20, 15, -15, 10, -10, 0);
        animator.setDuration(SHAKE_ANIMATION_DURATION);
        animator.start();
    }


    /**
     * Updates the score and round display TextViews.
     */
    private void updateScoreAndRoundDisplay() {
        scoreTextView.setText("Score: " + score);
        if (currentGameMode == GameMode.ENDLESS) {
            roundTextView.setText("Round: " + currentRound + " (Endless)");
        } else {
            roundTextView.setText("Round: " + currentRound + "/" + totalRounds);
        }
    }

    /**
     * Updates the text and enabled state of the clue button.
     */
    private void updateClueButtonText() {
        // Clue button is enabled if there are session clues left AND clues left for the current word
        clueButton.setText("(" + cluesRemaining + ")"); // Only show the count of session clues
        clueButton.setEnabled(cluesRemaining > 0 && cluesUsedForCurrentWord < MAX_CLUES_PER_WORD && !isPaused && !isGameOver);
    }

    /**
     * Starts the game timer.
     */
    private void startTimer() {
        startTime = System.currentTimeMillis() - timeWhenPaused;
        timerRunning = true;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPaused && !isGameOver) {
                    long millis = System.currentTimeMillis() - startTime;
                    timerTextView.setText("Time: " + formatTime(millis / 1000));
                }
                if (!isGameOver) {
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

    /**
     * Stops the game timer.
     */
    private void stopTimer() {
        if (timerRunning && timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunning = false;
        }
    }

    /**
     * Formats total seconds into MM:SS string.
     * @param totalSeconds The total number of seconds.
     * @return Formatted time string.
     */
    private String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * Saves the current game's high score if it qualifies.
     * High scores are sorted by score (descending), then time (ascending).
     */
    private void saveHighScore() {
        if (getContext() == null || score == 0) return;

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(HIGH_SCORES_KEY, null);
        Type type = new TypeToken<ArrayList<HighScoreEntry>>() {}.getType();
        List<HighScoreEntry> highScores = json == null ? new ArrayList<>() : gson.fromJson(json, type);

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;

        // Create HighScoreEntry with the current game mode
        highScores.add(new HighScoreEntry(currentDate, score, timeTaken, currentGameMode));

        // Sort: primary by score (descending), secondary by time (ascending)
        Collections.sort(highScores, (e1, e2) -> {
            int scoreCompare = Integer.compare(e2.score, e1.score); // Descending score
            if (scoreCompare == 0) {
                return Long.compare(e1.timeTaken, e2.timeTaken); // Ascending time for same score
            }
            return scoreCompare;
        });

        // Keep only top 5 scores
        if (highScores.size() > 5) {
            highScores = highScores.subList(0, 5);
        }

        String updatedJson = gson.toJson(highScores);
        prefs.edit().putString(HIGH_SCORES_KEY, updatedJson).apply();

        Toast.makeText(getContext(), "High score saved!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Loads high scores from SharedPreferences.
     * @return List of HighScoreEntry objects.
     */
    private List<HighScoreEntry> loadHighScores() {
        if (getContext() == null) return new ArrayList<>();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(HIGH_SCORES_KEY, null);
        Type type = new TypeToken<ArrayList<HighScoreEntry>>() {}.getType();
        return json == null ? new ArrayList<>() : gson.fromJson(json, type);
    }

    /**
     * Displays the high scores dialog.
     */
    private void showHighScoresDialog() {
        // Pause the game logic and show the UI, but without the toast for this specific action
        if (!isPaused && !isGameOver) {
            pauseGameAndShowUI(false); // Pause without toast
        }
        // Ensure paused card is hidden if it was shown by pauseGameAndShowUI(false)
        pausedCard.setVisibility(View.GONE); // Explicitly hide the paused card
        pauseButton.setIconResource(R.drawable.ic_play_arrow_white_24dp); // Show play icon
        overlayContainer.setVisibility(View.GONE); // Hide overlay
        Toast.makeText(getContext(), "Game Paused (High Scores)", Toast.LENGTH_SHORT).show(); // Specific toast for high scores
        List<HighScoreEntry> scores = loadHighScores();
        // Pass the original scores directly to the dialog's newInstance method,
        // and pass null for localUserId as it's not used in this game's HighScoreDialogFragment.
        HighScoreDialogFragment dialogFragment = HighScoreDialogFragment.newInstance(new ArrayList<>(scores), null);
        dialogFragment.show(getParentFragmentManager(), "word_scramble_high_scores_dialog");
    }

    /**
     * Pauses the game logic only (stops timer, sets flag). Does NOT affect UI or show toasts.
     */
    private void pauseGame() {
        if (isGameOver || isPaused) return; // Don't pause if already over or paused

        isPaused = true;
        stopTimer();
        timeWhenPaused = System.currentTimeMillis() - startTime;
    }

    /**
     * Pauses the game logic AND updates UI to show "Game Paused" with a toast.
     * @param showToast If true, a "Game Paused" toast will be shown.
     */
    private void pauseGameAndShowUI(boolean showToast) {
        pauseGame(); // Handle logic first

        pauseButton.setIconResource(R.drawable.ic_play_arrow_white_24dp); // Change icon to play
        overlayContainer.setVisibility(View.VISIBLE);
        pausedCard.setVisibility(View.VISIBLE);
        gameOverCard.setVisibility(View.GONE); // Explicitly hide game over card
        setGameControlsEnabled(false); // Disable controls when paused

        if (showToast) {
            Toast.makeText(getContext(), "Game Paused", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Resumes the game logic only (starts timer, sets flag). Does NOT affect UI or show toasts.
     */
    private void resumeGame() {
        if (isGameOver || !isPaused) return; // Don't resume if game over or not paused

        isPaused = false;
        startTimer();
    }

    /**
     * Resumes the game logic AND hides "Game Paused" UI with a toast.
     * @param showToast If true, a "Game Resumed" toast will be shown.
     */
    private void resumeGameAndHideUI(boolean showToast) {
        resumeGame(); // Handle logic first

        pauseButton.setIconResource(R.drawable.ic_pause_white_24dp); // Change icon to pause
        overlayContainer.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);
        setGameControlsEnabled(true); // Re-enable controls when resumed

        if (showToast) {
            Toast.makeText(getContext(), "Game Resumed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets the enabled state of game control buttons.
     * @param enabled True to enable, false to disable.
     */
    private void setGameControlsEnabled(boolean enabled) {
        submitButton.setEnabled(enabled);
        nextWordButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
        highScoresButton.setEnabled(enabled);
        pauseButton.setEnabled(true); // Pause button is always available unless game is over
        backspaceButton.setEnabled(enabled);
        infoGameButton.setEnabled(true); // Info button is always available
        updateClueButtonText();

        // Set visibility of saveAndQuitButton based on game mode
        if (currentGameMode == GameMode.ENDLESS) {
            saveAndQuitButton.setVisibility(View.VISIBLE);
        } else {
            saveAndQuitButton.setVisibility(View.GONE);
        }
    }

    /**
     * Checks if the game is currently busy (e.g., during an animation or delay).
     * @return True if busy with feedback animation, false otherwise.
     */
    private boolean isBusy() {
        return isBusyWithFeedback;
    }

    /**
     * Handles the game over state, stopping the timer, saving score, and showing the game over screen.
     */
    private void handleGameOver() {
        isGameOver = true;
        stopTimer();
        saveHighScore(); // Save score when game is over

        // Populate summary details in the existing game_over_card
        summaryScoreTextView.setText("Final Score: " + score);

        if (correctWordsList.isEmpty()) {
            correctWordsSummaryTextView.setText("Correct Words: None");
        } else {
            // Changed from ", \n" to ", " to display words on the same line
            correctWordsSummaryTextView.setText("Correct Words: " + String.join(", ", correctWordsList));
        }

        if (missedWordsList.isEmpty()) {
            missedWordsSummaryTextView.setText("Missed Words: None");
        } else {
            // Changed from ", \n" to ", " to display words on the same line
            missedWordsSummaryTextView.setText("Missed Words: " + String.join(", ", missedWordsList));
        }

        // --- Dynamic background color for game over card based on score and mode ---
        if (getContext() != null) {
            if (currentGameMode == GameMode.ENDLESS) {
                // For endless mode, keep it neutral (white or a light color)
                gameOverCard.setCardBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.white));
            } else {
                // For fixed-round modes, change color based on performance
                int maxPossibleScore = totalRounds * 10; // 10 points per correct word
                @ColorInt int backgroundColor;
                if (score >= maxPossibleScore * 0.75) { // Excellent performance (75% or more)
                    backgroundColor = ContextCompat.getColor(getContext(), R.color.md_theme_tertiaryContainer); // Greenish
                } else if (score >= maxPossibleScore * 0.50) { // Good performance (50% or more)
                    backgroundColor = ContextCompat.getColor(getContext(), R.color.md_theme_secondaryContainer); // Bluish
                } else { // Needs improvement (less than 50%)
                    backgroundColor = ContextCompat.getColor(getContext(), R.color.md_theme_errorContainer); // Reddish
                }
                gameOverCard.setCardBackgroundColor(backgroundColor);
            }
        }
        // --- End dynamic background color logic ---

        overlayContainer.setVisibility(View.VISIBLE);
        gameOverCard.setVisibility(View.VISIBLE);
        pausedCard.setVisibility(View.GONE); // Explicitly hide paused card
        setGameControlsEnabled(false);
        pauseButton.setIconResource(R.drawable.ic_pause_white_24dp); // Reset icon for next game
        updateClueButtonText(); // Update clue button state for game over

        Toast.makeText(getContext(), "Game Over! Your final score: " + score, Toast.LENGTH_LONG).show();
    }

    /**
     * Resets the game to its initial state.
     */
    private void resetGame() {
        Log.d(TAG, "Resetting game...");
        initializeGame();
        Toast.makeText(getContext(), "Game Reset!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mainActivity != null) {
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }
        // Pause the game logic only (no UI, no toast) when fragment is minimized/exited
        if (!isPaused && !isGameOver) {
            pauseGame();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer(); // Ensure timer is stopped when view is destroyed
    }
    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("Word Scramble Game");
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }
        // If the game was paused by onPause (e.g., app minimized) and the UI is NOT visible,
        // automatically resume it without a toast.
        if (isPaused && !isGameOver && overlayContainer.getVisibility() != View.VISIBLE) {
            resumeGame(); // Auto-resume logic only, no UI or toast
        }
    }
}
