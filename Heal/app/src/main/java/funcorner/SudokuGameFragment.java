package funcorner;

import android.content.Context;
import android.graphics.Color;
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
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.MainActivity; // Assuming this is correct
import com.f9ld3.heal.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import ui.CustomMessageDialogFragment; // Assuming this is correct
import wordscramble.InfoDialogFragment;

public class SudokuGameFragment extends Fragment implements CustomMessageDialogFragment.OnMessageDialogListener {

    private static final String TAG = "SudokuGameFragment"; // For logging

    // UI Components
    private MainActivity mainActivity;
    private View overlayContainer, pausedCard, gameOverCard;
    private TextView timerTextView, pauseText, gameOverSummary;
    private MaterialButton pauseButton, restartPausedButton, checkButton, clearButton, restartGameOverButton, buttonInfoGame;
    private GridLayout sudokuGrid;
    private Spinner difficultySpinner;
    private FloatingActionButton undoButton, redoButton; // Changed MaterialButton to FloatingActionButton

    // Number Pad Buttons (from XML)
    private MaterialButton buttonNum1, buttonNum2, buttonNum3, buttonNum4, buttonNum5, buttonNum6, buttonNum7, buttonNum8, buttonNum9;

    private int[][] board;
    private int[][] solution;
    private TextView selectedCell = null;
    private int selectedRow = -1;
    private int selectedCol = -1;

    // Game state variables
    private boolean isPaused = false;
    private boolean isGameOver = false;
    private long startTime;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private String selectedDifficulty = "Easy"; // Default difficulty
    private long timeWhenPaused = 0;

    // Tags for dialogs
    private static final String TAG_RESTART_GAME_DIALOG = "restart_game";
    private static final String TAG_CHANGE_DIFFICULTY_DIALOG = "change_difficulty";

    // RGB for Tan color (D2B48C)
    private static final int COLOR_TAN = Color.rgb(210, 180, 140);

    // Data structures for undo/redo
    private Stack<SudokuMove> undoStack = new Stack<>();
    private Stack<SudokuMove> redoStack = new Stack<>();


    public SudokuGameFragment() {
        // Required empty public constructor
    }

    // Private class to represent a single user move
    private static class SudokuMove {
        final int row;
        final int col;
        final String oldValue;
        final String newValue;

        SudokuMove(int row, int col, String oldValue, String newValue) {
            this.row = row;
            this.col = col;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }


    /**
     * Helper method to resolve a color attribute from the current theme.
     * @param colorAttr The color attribute to resolve (e.g., R.attr.colorPrimary).
     * @return The resolved color integer.
     */
    @ColorInt
    private int getThemeColor(@AttrRes int colorAttr) {
        TypedValue typedValue = new TypedValue();
        Context context = requireContext();
        // Ensure you are using the correct attribute ID (e.g., com.google.android.material.R.attr.colorPrimary
        // or android.R.attr.colorPrimary or your own app's R.attr.colorPrimary)
        if (context.getTheme().resolveAttribute(colorAttr, typedValue, true)) {
            return typedValue.data;
        }
        // Fallback color if attribute is not found, though this shouldn't happen for standard attributes
        Log.w(TAG, "Theme attribute " + context.getResources().getResourceName(colorAttr) + " not found. Falling back to black.");
        return Color.BLACK; // Fallback
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sudoku_game, container, false);

        timerTextView = view.findViewById(R.id.timer_text_view);
        sudokuGrid = view.findViewById(R.id.sudoku_grid);
        checkButton = view.findViewById(R.id.check_button);
        clearButton = view.findViewById(R.id.clear_button);
        difficultySpinner = view.findViewById(R.id.difficulty_spinner);
        pauseButton = view.findViewById(R.id.pause_button);
        overlayContainer = view.findViewById(R.id.overlay_container);
        pausedCard = view.findViewById(R.id.paused_card);
        gameOverCard = view.findViewById(R.id.game_over_card);
        MaterialButton resumeButton = view.findViewById(R.id.button_resume_game); // Keep reference but its listener will be managed by the single pause button logic below
        restartPausedButton = view.findViewById(R.id.button_restart_game_paused);
        restartGameOverButton = view.findViewById(R.id.button_restart_game_over);
        pauseText = view.findViewById(R.id.pause_text);
        gameOverSummary = view.findViewById(R.id.game_over_summary);
        undoButton = view.findViewById(R.id.undo_button);
        redoButton = view.findViewById(R.id.redo_button);
        buttonInfoGame = view.findViewById(R.id.button_info_game); // Now this button exists and will be used
        pauseButton.setIconResource(R.drawable.ic_pause_white_24dp);


        if (getActivity() instanceof MainActivity) {
            mainActivity = (MainActivity) getActivity();
        }

        timerHandler = new Handler(Looper.getMainLooper());

        setupGrid();
        initializeNumberButtons(view);
        setupDifficultySpinner();

        if (difficultySpinner.getSelectedItem() != null) {
            selectedDifficulty = difficultySpinner.getSelectedItem().toString();
        }
        startNewGame(selectedDifficulty);

        checkButton.setOnClickListener(v -> checkSolution());
        clearButton.setOnClickListener(v -> {
            if (selectedCell != null && selectedCell.isClickable()) {
                String oldValue = selectedCell.getText().toString();
                if (!oldValue.isEmpty()) {
                    SudokuMove move = new SudokuMove(selectedRow, selectedCol, oldValue, "");
                    undoStack.push(move);
                    redoStack.clear();
                    updateFabState();

                    selectedCell.setText("");
                    selectedCell.setTextColor(COLOR_TAN);
                }
            } else if (selectedCell == null) {
                Toast.makeText(getContext(), "Please select a cell first.", Toast.LENGTH_SHORT).show();
            } else if (!selectedCell.isClickable()){
                Toast.makeText(getContext(), "This cell cannot be changed.", Toast.LENGTH_SHORT).show();
            }
        });

        // Use a single button for pause/resume functionality
        pauseButton.setOnClickListener(v -> {
            if (isPaused) {
                resumeGame();
            } else {
                pauseGame();
            }
        });
        resumeButton.setOnClickListener(v -> resumeGame());
        restartPausedButton.setOnClickListener(v -> showConfirmRestartDialog(TAG_RESTART_GAME_DIALOG));
        restartGameOverButton.setOnClickListener(v -> {
            overlayContainer.setVisibility(View.GONE);
            restartGame();
        });
        undoButton.setOnClickListener(v -> undoMove());
        redoButton.setOnClickListener(v -> redoMove());
        // NEW: Game Info button listener
        buttonInfoGame.setOnClickListener(v -> showGameInfoDialog());

        return view;
    }

    private void setupGrid() {
        sudokuGrid.removeAllViews();
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                TextView cell = new TextView(requireContext());
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.columnSpec = GridLayout.spec(col, 1f);
                params.rowSpec = GridLayout.spec(row, 1f);

                int rightMargin = (col % 3 == 2 && col != 8) ? 4 : 1;
                int bottomMargin = (row % 3 == 2 && row != 8) ? 4 : 1;
                params.setMargins(1, 1, rightMargin, bottomMargin);

                cell.setLayoutParams(params);
                cell.setGravity(Gravity.CENTER);
                cell.setBackgroundResource(R.drawable.sudoku_cell_background);
                cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                cell.setTag(new int[]{row, col});
                cell.setOnClickListener(v -> onCellClick((TextView) v));

                sudokuGrid.addView(cell);
            }
        }
    }

    private void initializeNumberButtons(View rootView) {
        buttonNum1 = rootView.findViewById(R.id.button_num_1);
        buttonNum2 = rootView.findViewById(R.id.button_num_2);
        buttonNum3 = rootView.findViewById(R.id.button_num_3);
        buttonNum4 = rootView.findViewById(R.id.button_num_4);
        buttonNum5 = rootView.findViewById(R.id.button_num_5);
        buttonNum6 = rootView.findViewById(R.id.button_num_6);
        buttonNum7 = rootView.findViewById(R.id.button_num_7);
        buttonNum8 = rootView.findViewById(R.id.button_num_8);
        buttonNum9 = rootView.findViewById(R.id.button_num_9);

        View.OnClickListener numberClickListener = v -> {
            if (isPaused || isGameOver || selectedCell == null || !selectedCell.isClickable()) {
                if (selectedCell == null) {
                    Toast.makeText(getContext(), "Please select a cell first.", Toast.LENGTH_SHORT).show();
                } else if (!selectedCell.isClickable()){
                    Toast.makeText(getContext(), "This cell cannot be changed.", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            MaterialButton clickedButton = (MaterialButton) v;
            String newText = clickedButton.getText().toString();
            String oldText = selectedCell.getText().toString();

            if (newText.equals(oldText)) {
                return;
            }

            // Capture the state before the change and push to undo stack
            SudokuMove move = new SudokuMove(selectedRow, selectedCol, oldText, newText);
            undoStack.push(move);
            redoStack.clear(); // Any new move clears the redo history
            updateFabState();

            selectedCell.setText(newText);
            int themedColor = getThemeColor(com.google.android.material.R.attr.colorPrimary);
            selectedCell.setTextColor(themedColor);
        };

        buttonNum1.setOnClickListener(numberClickListener);
        buttonNum2.setOnClickListener(numberClickListener);
        buttonNum3.setOnClickListener(numberClickListener);
        buttonNum4.setOnClickListener(numberClickListener);
        buttonNum5.setOnClickListener(numberClickListener);
        buttonNum6.setOnClickListener(numberClickListener);
        buttonNum7.setOnClickListener(numberClickListener);
        buttonNum8.setOnClickListener(numberClickListener);
        buttonNum9.setOnClickListener(numberClickListener);
    }


    private void setupDifficultySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);

        int initialPosition = adapter.getPosition(selectedDifficulty);
        if (initialPosition >= 0) {
            difficultySpinner.setSelection(initialPosition);
        }

        difficultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newDifficulty = parent.getItemAtPosition(position).toString();
                if (!newDifficulty.equals(selectedDifficulty)) {
                    showConfirmDifficultyChangeDialog(newDifficulty);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void showConfirmDifficultyChangeDialog(String newDifficultyToConfirm) {
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Change Difficulty?",
                "Are you sure you want to change the difficulty and restart the game?",
                "Yes",
                "No"
        );
        dialog.setListener(this);
        dialog.show(getParentFragmentManager(), TAG_CHANGE_DIFFICULTY_DIALOG);
    }

    private void showConfirmRestartDialog(String tag) {
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Restart Game?",
                "Are you sure you want to restart the current game?",
                "Yes",
                "No"
        );
        dialog.setListener(this);
        dialog.show(getParentFragmentManager(), tag);
    }

    private void showGameInfoDialog() {
        InfoDialogFragment dialog = InfoDialogFragment.newInstance(
                "How to Play Sudoku",
                "Fill the grid so that each row, column, and 3x3 box contains the digits 1 to 9 without repetition. Use the number pad to fill in the empty cells. You can clear a selected cell with the 'Clear' button. The 'Check' button will verify if your solution is correct."
        );
        dialog.show(getParentFragmentManager(), "game_info");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        String dialogTag = dialog.getTag();
        if (dialogTag == null) return;

        switch (dialogTag) {
            case TAG_RESTART_GAME_DIALOG:
                overlayContainer.setVisibility(View.GONE);
                restartGame();
                break;
            case TAG_CHANGE_DIFFICULTY_DIALOG:
                String newDifficulty = difficultySpinner.getSelectedItem().toString();
                selectedDifficulty = newDifficulty;
                Log.d(TAG, "Difficulty changed to: " + selectedDifficulty);
                overlayContainer.setVisibility(View.GONE);
                startNewGame(selectedDifficulty);
                break;
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        String dialogTag = dialog.getTag();
        if (dialogTag == null) return;

        if (TAG_CHANGE_DIFFICULTY_DIALOG.equals(dialogTag)) {
            if (difficultySpinner.getAdapter() != null) {
                ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) difficultySpinner.getAdapter();
                int currentPosition = adapter.getPosition(selectedDifficulty);
                if (currentPosition >= 0) {
                    difficultySpinner.setSelection(currentPosition);
                }
            }
            Log.d(TAG, "Difficulty change cancelled, spinner reset to: " + selectedDifficulty);
        }
    }


    private void startNewGame(String difficulty) {
        Log.d(TAG, "Starting new game with difficulty: " + difficulty);
        stopTimer();
        isGameOver = false;
        isPaused = false;
        overlayContainer.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        selectedCell = null;
        selectedRow = -1;
        selectedCol = -1;

        // Clear undo/redo history for a new game
        undoStack.clear();
        redoStack.clear();
        updateFabState();

        generateNewSudoku(difficulty);
        fillGrid();

        startTime = System.currentTimeMillis();
        timeWhenPaused = 0;
        startTimer();
        // Set the pause button icon for a new game
        pauseButton.setIconResource(R.drawable.ic_pause_white_24dp);
    }

    private void onCellClick(TextView cell) {
        if (isPaused || isGameOver || !cell.isClickable()) return;

        if (selectedCell != null) {
            selectedCell.setBackgroundResource(R.drawable.sudoku_cell_background);
        }
        selectedCell = cell;
        selectedCell.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_500)); // YOUR ORIGINAL selected cell BG color
        int[] pos = (int[]) cell.getTag();
        if (pos != null && pos.length == 2) {
            selectedRow = pos[0];
            selectedCol = pos[1];
            Log.d(TAG, "Cell selected: Row " + selectedRow + ", Col " + selectedCol);
        } else {
            Log.e(TAG, "Cell tag invalid or null!");
            selectedRow = -1;
            selectedCol = -1;
        }
    }

    private void generateNewSudoku(String difficulty) {
        Random random = new Random();
        board = new int[9][9];
        solution = new int[9][9];

        generateSolution(solution, 0, 0);

        for (int i = 0; i < 9; i++) {
            System.arraycopy(solution[i], 0, board[i], 0, 9);
        }

        int emptyCells;
        switch (difficulty.toLowerCase()) {
            case "easy":
                emptyCells = 40;
                break;
            case "medium":
                emptyCells = 50;
                break;
            case "hard":
                emptyCells = 60;
                break;
            default:
                emptyCells = 40;
                Log.w(TAG, "Unknown difficulty: " + difficulty + ", defaulting to Easy.");
                break;
        }

        int attempts = 0;
        int maxAttempts = 9 * 9 * 2;

        while (emptyCells > 0 && attempts < maxAttempts) {
            int row = random.nextInt(9);
            int col = random.nextInt(9);
            if (board[row][col] != 0) {
                board[row][col] = 0;
                emptyCells--;
            }
            attempts++;
        }
        if (attempts >= maxAttempts && emptyCells > 0) {
            Log.e(TAG, "Max attempts reached for removing cells. Puzzle might not match difficulty.");
        }
    }

    private boolean generateSolution(int[][] grid, int row, int col) {
        if (row == 9) {
            return true;
        }
        int nextRow = (col == 8) ? row + 1 : row;
        int nextCol = (col == 8) ? 0 : col + 1;

        if (grid[row][col] != 0) {
            return generateSolution(grid, nextRow, nextCol);
        }

        List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(numbers);

        for (int number : numbers) {
            if (isValidPlacement(grid, row, col, number)) {
                grid[row][col] = number;
                if (generateSolution(grid, nextRow, nextCol)) {
                    return true;
                }
                grid[row][col] = 0;
            }
        }
        return false;
    }

    private boolean isValidPlacement(int[][] grid, int row, int col, int number) {
        for (int i = 0; i < 9; i++) {
            if (grid[row][i] == number) return false;
            if (grid[i][col] == number) return false;
        }
        int boxRowStart = row - row % 3;
        int boxColStart = col - col % 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (grid[boxRowStart + i][boxColStart + j] == number) return false;
            }
        }
        return true;
    }


    private void fillGrid() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                TextView cell = (TextView) sudokuGrid.getChildAt(r * 9 + c);
                if (cell == null) {
                    Log.e(TAG, "Cell at " + r + "," + c + " is null in fillGrid!");
                    continue;
                }
                if (board[r][c] != 0) {
                    cell.setText(String.valueOf(board[r][c]));
                    cell.setTextColor(Color.BLACK); // YOUR ORIGINAL preset cell text color
                    cell.setClickable(false);
                    // cell.setBackgroundResource(R.drawable.sudoku_cell_preset_background); // Optional
                } else {
                    cell.setText("");
                    cell.setTextColor(COLOR_TAN); // User-editable empty cells are Tan
                    cell.setClickable(true);
                    cell.setBackgroundResource(R.drawable.sudoku_cell_background);
                }
            }
        }
    }

    private void checkSolution() {
        if (isGameOver || isPaused) return;

        boolean isCorrectAndComplete = true;
        boolean isComplete = true;

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                TextView cell = (TextView) sudokuGrid.getChildAt(row * 9 + col);
                String text = cell.getText().toString();
                if (text.isEmpty()) {
                    isComplete = false;
                    isCorrectAndComplete = false;
                    break;
                }
                if (Integer.parseInt(text) != solution[row][col]) {
                    isCorrectAndComplete = false;
                }
            }
            if (!isComplete) break;
        }

        if (!isComplete) {
            Toast.makeText(getContext(), "Puzzle is not complete!", Toast.LENGTH_SHORT).show();
        } else if (isCorrectAndComplete) {
            stopTimer();
            isGameOver = true;
            gameOverSummary.setText("Congratulations!\nYour time: " + timerTextView.getText().toString().replace("Time: ", ""));
            overlayContainer.setVisibility(View.VISIBLE);
            gameOverCard.setVisibility(View.VISIBLE);
            pausedCard.setVisibility(View.GONE);
            pauseButton.setIconResource(R.drawable.ic_play_arrow_white_24dp); // Reset icon for next game
        } else {
            Toast.makeText(getContext(), "Incorrect solution, try again!", Toast.LENGTH_SHORT).show();
        }
    }

    // Undo logic
    private void undoMove() {
        if (!undoStack.isEmpty()) {
            SudokuMove lastMove = undoStack.pop();
            redoStack.push(lastMove);

            TextView cell = (TextView) sudokuGrid.getChildAt(lastMove.row * 9 + lastMove.col);
            cell.setText(lastMove.oldValue);

            if (lastMove.oldValue.isEmpty()) {
                cell.setTextColor(COLOR_TAN);
            } else {
                int themedColor = getThemeColor(com.google.android.material.R.attr.colorPrimary);
                cell.setTextColor(themedColor);
            }
            updateFabState();
        }
    }

    // Redo logic
    private void redoMove() {
        if (!redoStack.isEmpty()) {
            SudokuMove undoneMove = redoStack.pop();
            undoStack.push(undoneMove);

            TextView cell = (TextView) sudokuGrid.getChildAt(undoneMove.row * 9 + undoneMove.col);
            cell.setText(undoneMove.newValue);

            int themedColor = getThemeColor(com.google.android.material.R.attr.colorPrimary);
            cell.setTextColor(themedColor);
            updateFabState();
        }
    }

    // Update FAB state
    private void updateFabState() {
        undoButton.setEnabled(!undoStack.isEmpty());
        redoButton.setEnabled(!redoStack.isEmpty());
    }


    private void startTimer() {
        stopTimer();
        if (isPaused) {
            // Correctly resume the timer by setting the start time
            // relative to the time that has already elapsed.
            startTime = System.currentTimeMillis() - timeWhenPaused;
        } else {
            // For a new game, reset the timer to 0.
            startTime = System.currentTimeMillis();
            timeWhenPaused = 0;
        }
        isPaused = false;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isGameOver || isPaused) {
                    return;
                }
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                timerTextView.setText(String.format("Time: %02d:%02d", minutes, seconds));
                timeWhenPaused = millis;
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }


    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void pauseGame() {
        if (isGameOver || isPaused) return;
        isPaused = true;
        stopTimer();
        overlayContainer.setVisibility(View.VISIBLE);
        pausedCard.setVisibility(View.VISIBLE);
        gameOverCard.setVisibility(View.GONE);
        pauseButton.setIconResource(R.drawable.ic_play_arrow_white_24dp);
        Toast.makeText(getContext(), "Game Paused", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Game Paused. Elapsed time stored: " + timeWhenPaused + "ms");
    }



    private void resumeGame() {
        if (!isPaused || isGameOver) return;
        overlayContainer.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);
        pauseButton.setIconResource(R.drawable.ic_pause_white_24dp);
        startTimer();
        Toast.makeText(getContext(), "Game Resumed", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Game Resumed.");
    }

    private void restartGame() {
        Log.d(TAG, "Restarting game with difficulty: " + selectedDifficulty);
        startNewGame(selectedDifficulty);
        Toast.makeText(getContext(), "Game Restarted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        if (mainActivity != null) {
            if (mainActivity.MenuTrigger != null) mainActivity.MenuTrigger.setVisibility(View.GONE);
            if (mainActivity.Fab != null) mainActivity.Fab.setVisibility(View.GONE);
        }
        // Pause the game state but do NOT show the UI or a toast
        if (!isPaused && !isGameOver) {
            Log.d(TAG, "onPause: Game is active, pausing game logic without showing UI.");
            isPaused = true;
            stopTimer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (mainActivity != null) {
            if (mainActivity.toolbar != null) mainActivity.toolbar.setTitle("Sudoku Game");
            if (mainActivity.navigationView != null) {
                mainActivity.navigationView.setCheckedItem(R.id.nav_fun_corner);
            }
            if (mainActivity.MenuTrigger != null) mainActivity.MenuTrigger.setVisibility(View.GONE);
            if (mainActivity.Fab != null) mainActivity.Fab.setVisibility(View.GONE);
        }
        // If the game was paused by onPause (e.g., app minimized) and the UI is NOT visible,
        // automatically resume it without a toast.
        if (isPaused && !isGameOver && overlayContainer.getVisibility() != View.VISIBLE) {
            Log.d(TAG, "onResume: Game was paused by lifecycle, resuming.");
            resumeGame(); // This will show the "Game Resumed" toast and hide the overlay
        } else if (isPaused && !isGameOver && overlayContainer.getVisibility() == View.VISIBLE && pausedCard.getVisibility() == View.VISIBLE) {
            Log.d(TAG, "onResume: Game was explicitly paused by user, keeping paused overlay.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called, stopping timer.");
        stopTimer();
        timerHandler = null;
        timerRunnable = null;
    }
}
