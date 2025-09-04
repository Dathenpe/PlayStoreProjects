package funcorner;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import tetris.HighScore;
import tetris.HighScoresDialogFragment;
import tetris.TetrisView;
import ui.CustomMessageDialogFragment;

public class TetrisGameFragment extends Fragment implements HighScoresDialogFragment.OnDismissListener, CustomMessageDialogFragment.OnMessageDialogListener {

    private static final String TAG = "TetrisGameFragment";

    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final String PREFS_NAME = "TetrisHighScores";
    private static final String SCORES_KEY = "scores";
    private static final long INITIAL_FALL_DELAY = 1000;
    private static final long MIN_FALL_DELAY = 150;
    private static final long DELAY_DECREMENT_PER_LEVEL = 75;

    private int[][] gameBoard;
    private TetrisView.Tetromino currentPiece;
    private int currentPieceX, currentPieceY;
    private int score;
    private int level;
    private boolean isGameOver;
    private boolean isPaused;

    private TetrisView tetrisView;
    private TextView scoreTextView;
    private TextView levelTextView;
    private TextView gameOverTitleTextView;
    private TextView summaryScoreTextView;

    private MaterialButton buttonShowHighScores;
    private MaterialButton buttonResumeGame;
    private MaterialButton buttonRestartGamePaused;
    private MaterialButton buttonInfoGame;
    private MaterialButton buttonRestartGameOver;

    private FrameLayout overlayContainer;
    private View pausedCard;
    private View gameOverCard;

    private FloatingActionButton fabMainToggle;
    private FloatingActionButton fabPausePlay;
    private FloatingActionButton fabRotate;
    private FloatingActionButton fabLeft;
    private FloatingActionButton fabRight;
    private FloatingActionButton fabDown;
    private LinearLayout gamepadControlsLayout;
    private boolean isFabMenuOpen = false;

    private Handler gameHandler;
    private Runnable gameRunnable;
    private long fallDelay;

    private Random random;

    private ArrayList<HighScore> highScores;
    private SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();
    private Context context;
    private MainActivity mainActivity;

    private static final String DIALOG_TAG_GAME_INFO = "TetrisGameInfoDialog";
    private static final String DIALOG_TAG_RESTART_CONFIRM = "TetrisRestartConfirmDialog";

    public TetrisGameFragment() { /* Required empty public constructor */ }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Log.e(TAG, "Fragment must be attached to MainActivity.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        random = new Random();
        gameHandler = new Handler(Looper.getMainLooper());
        if (this.context != null) {
            sharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        } else {
            try {
                sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            } catch (IllegalStateException e) {
                Log.e(TAG, "requireActivity() failed in onCreate", e);
                highScores = new ArrayList<>();
            }
        }
        if (sharedPreferences != null) {
            loadHighScores();
        } else {
            if (highScores == null) highScores = new ArrayList<>();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tetris_game, container, false);
        initializeUI(view);
        setupButtonListeners();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startGame();
    }

    private void initializeUI(View view) {
        tetrisView = view.findViewById(R.id.tetris_view);
        scoreTextView = view.findViewById(R.id.score_text_view);
        levelTextView = view.findViewById(R.id.level_text_view);
        overlayContainer = view.findViewById(R.id.overlay_container);
        pausedCard = view.findViewById(R.id.paused_card);
        gameOverCard = view.findViewById(R.id.game_over_card);
        gameOverTitleTextView = view.findViewById(R.id.game_over_text);
        summaryScoreTextView = view.findViewById(R.id.summary_text_view);
        buttonShowHighScores = view.findViewById(R.id.button_show_high_scores);
        buttonResumeGame = view.findViewById(R.id.button_resume_game);
        buttonRestartGamePaused = view.findViewById(R.id.button_restart_game_paused);
        buttonInfoGame = view.findViewById(R.id.button_info_game);
        buttonRestartGameOver = view.findViewById(R.id.button_restart);
        fabMainToggle = view.findViewById(R.id.fab_main_toggle);
        fabPausePlay = view.findViewById(R.id.fab_pause_play);
        fabRotate = view.findViewById(R.id.fab_rotate);
        fabLeft = view.findViewById(R.id.fab_left);
        fabRight = view.findViewById(R.id.fab_right);
        fabDown = view.findViewById(R.id.fab_down);
        gamepadControlsLayout = view.findViewById(R.id.fab_menu_actions);

        gamepadControlsLayout.setVisibility(View.GONE);
        overlayContainer.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
    }

    private void setupButtonListeners() {
        if (fabMainToggle != null) {
            fabMainToggle.setOnClickListener(v -> {
                if (isGameOver) return;
                if (isFabMenuOpen) closeFabMenu(); else openFabMenu();
            });
        } else { Log.e(TAG, "fabMainToggle is null!"); }

        if (fabPausePlay != null) {
            fabPausePlay.setOnClickListener(v -> {
                if (isGameOver) return;
                if (isPaused) resumeGameAndHideUI(true); else pauseGameAndShowPauseCard();
            });
        } else { Log.e(TAG, "fabPausePlay is null!"); }

        if (fabRotate != null) fabRotate.setOnClickListener(v -> { if (!isGameOver && !isPaused) rotatePiece(); });
        else { Log.e(TAG, "fabRotate is null!"); }
        if (fabLeft != null) fabLeft.setOnClickListener(v -> { if (!isGameOver && !isPaused) movePiece(-1, 0); });
        else { Log.e(TAG, "fabLeft is null!"); }
        if (fabRight != null) fabRight.setOnClickListener(v -> { if (!isGameOver && !isPaused) movePiece(1, 0); });
        else { Log.e(TAG, "fabRight is null!"); }
        if (fabDown != null) fabDown.setOnClickListener(v -> { if (!isGameOver && !isPaused) movePiece(0, 1); });
        else { Log.e(TAG, "fabDown is null!"); }

        if (buttonShowHighScores != null) buttonShowHighScores.setOnClickListener(v -> showHighScoresDialog());
        else { Log.e(TAG, "buttonShowHighScores is null!"); }
        if (buttonResumeGame != null) buttonResumeGame.setOnClickListener(v -> resumeGameAndHideUI(true));
        else { Log.e(TAG, "buttonResumeGame is null!"); }

        if (buttonRestartGamePaused != null) {
            buttonRestartGamePaused.setOnClickListener(v -> showConfirmationDialog(
                    "Restart Game?", "Are you sure you want to restart the current game?",
                    DIALOG_TAG_RESTART_CONFIRM // Only pass tag, action handled by listener
            ));
        } else { Log.e(TAG, "buttonRestartGamePaused is null!"); }

        if (buttonInfoGame != null) buttonInfoGame.setOnClickListener(v -> showGameInfoDialog());
        else { Log.e(TAG, "buttonInfoGame is null!"); }

        if (buttonRestartGameOver != null) {
            buttonRestartGameOver.setOnClickListener(v -> {
                if (isGameOver) { // Direct restart for game over
                    startGame();
                }
            });
        } else { Log.e(TAG, "buttonRestartGameOver is null!"); }
    }

    private void openFabMenu() {
        if (isGameOver || isPaused) return;
        isFabMenuOpen = true;
        gamepadControlsLayout.setVisibility(View.VISIBLE);
        if (fabRotate != null) fabRotate.setVisibility(View.VISIBLE);
        if (fabLeft != null) fabLeft.setVisibility(View.VISIBLE);
        if (fabRight != null) fabRight.setVisibility(View.VISIBLE);
        if (fabDown != null) fabDown.setVisibility(View.VISIBLE);
        if (fabPausePlay != null) fabPausePlay.setVisibility(View.GONE);
        if (fabMainToggle != null) {
            fabMainToggle.setImageResource(R.drawable.ic_close);
            fabMainToggle.setContentDescription(getResString(R.string.close_game_controls_desc));
        }
        setGameControlsEnabled(true);
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;
        gamepadControlsLayout.setVisibility(View.GONE);
        if (fabPausePlay != null) fabPausePlay.setVisibility(View.VISIBLE);
        if (fabMainToggle != null) {
            fabMainToggle.setImageResource(R.drawable.ic_gamepad);
            fabMainToggle.setContentDescription(getResString(R.string.open_game_controls_desc));
        }
        setGameControlsEnabled(true);
    }

    private void startGame() {
        Log.d(TAG, "startGame called");
        level = 1; score = 0; isGameOver = false; isPaused = false;
        fallDelay = calculateFallDelay(level);
        gameBoard = new int[BOARD_HEIGHT][BOARD_WIDTH];
        updateScoreAndLevelDisplay();
        overlayContainer.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);
        if (fabPausePlay != null) {
            fabPausePlay.setImageResource(R.drawable.ic_pause_white_24dp);
            fabPausePlay.setContentDescription(getResString(R.string.pause_desc));
        }
        setGameControlsEnabled(true);
        if (isFabMenuOpen) closeFabMenu();
        spawnNewPiece();
        startGameLoop();
    }

    private long calculateFallDelay(int currentLevel) {
        return Math.max(MIN_FALL_DELAY, INITIAL_FALL_DELAY - ((long)(currentLevel - 1) * DELAY_DECREMENT_PER_LEVEL));
    }

    private void updateScoreAndLevelDisplay() {
        if (scoreTextView != null) scoreTextView.setText(getResString(R.string.score_format, score));
        if (levelTextView != null) levelTextView.setText(getResString(R.string.level_format, level));
    }

    private void startGameLoop() {
        if (gameHandler == null) gameHandler = new Handler(Looper.getMainLooper());
        gameHandler.removeCallbacks(gameRunnable);
        gameRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isGameOver && !isPaused) {
                    if (!movePiece(0, 1)) {
                        landPiece(); clearLines();
                        if (!isGameOver) spawnNewPiece();
                    }
                }
                if (!isGameOver) gameHandler.postDelayed(this, fallDelay);
            }
        };
        gameHandler.postDelayed(gameRunnable, fallDelay);
    }

    private void spawnNewPiece() {
        if (TetrisView.Tetromino.ALL_TETROMINOES.length == 0) {
            Log.e(TAG, "ALL_TETROMINOES empty!"); handleGameOver(); return;
        }
        currentPiece = TetrisView.Tetromino.ALL_TETROMINOES[random.nextInt(TetrisView.Tetromino.ALL_TETROMINOES.length)];
        currentPieceX = BOARD_WIDTH / 2 - currentPiece.getShape()[0].length / 2;
        currentPieceY = 0;
        if (!isValidPosition(currentPiece, currentPieceX, currentPieceY)) {
            handleGameOver();
        } else {
            if (tetrisView != null) {
                tetrisView.setCurrentPiece(currentPiece, currentPieceX, currentPieceY);
                tetrisView.setGameBoard(gameBoard);
            }
        }
    }

    private boolean isValidPosition(TetrisView.Tetromino piece, int newX, int newY) {
        if (piece == null) return false;
        int[][] shape = piece.getShape();
        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] != 0) {
                    int boardX = newX + x; int boardY = newY + y;
                    if (boardX < 0 || boardX >= BOARD_WIDTH || boardY >= BOARD_HEIGHT) return false;
                    if (boardY >= 0 && gameBoard[boardY][boardX] != 0) return false;
                }
            }
        }
        return true;
    }

    private boolean movePiece(int deltaX, int deltaY) {
        if (isGameOver || isPaused || currentPiece == null) return false;
        int newX = currentPieceX + deltaX; int newY = currentPieceY + deltaY;
        if (isValidPosition(currentPiece, newX, newY)) {
            currentPieceX = newX; currentPieceY = newY;
            if (tetrisView != null) tetrisView.setCurrentPiece(currentPiece, currentPieceX, currentPieceY);
            return true;
        }
        return false;
    }

    private void rotatePiece() {
        if (isGameOver || isPaused || currentPiece == null) return;
        TetrisView.Tetromino rotated = currentPiece.rotate(); int originalX = currentPieceX;
        if (isValidPosition(rotated, currentPieceX, currentPieceY)) currentPiece = rotated;
        else if (isValidPosition(rotated, currentPieceX + 1, currentPieceY)) { currentPieceX++; currentPiece = rotated; }
        else if (isValidPosition(rotated, currentPieceX - 1, currentPieceY)) { currentPieceX--; currentPiece = rotated; }
        if (isValidPosition(currentPiece, currentPieceX, currentPieceY)) {
            if (tetrisView != null) tetrisView.setCurrentPiece(currentPiece, currentPieceX, currentPieceY);
        } else currentPieceX = originalX;
    }

    private void landPiece() {
        if (currentPiece == null) return;
        int[][] shape = currentPiece.getShape(); int color = currentPiece.getColor();
        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] != 0) {
                    int boardY = currentPieceY + y; int boardX = currentPieceX + x;
                    if (boardY >= 0 && boardY < BOARD_HEIGHT && boardX >= 0 && boardX < BOARD_WIDTH)
                        gameBoard[boardY][boardX] = color;
                    else { Log.e(TAG, "Land piece out of bounds."); handleGameOver(); return; }
                }
            }
        }
        if (tetrisView != null) tetrisView.setGameBoard(gameBoard);
        currentPiece = null;
    }

    private void clearLines() {
        int linesCleared = 0;
        for (int y = BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean lineFull = true;
            for (int x = 0; x < BOARD_WIDTH; x++) if (gameBoard[y][x] == 0) { lineFull = false; break; }
            if (lineFull) {
                linesCleared++;
                for (int r = y; r > 0; r--) System.arraycopy(gameBoard[r - 1], 0, gameBoard[r], 0, BOARD_WIDTH);
                for (int x = 0; x < BOARD_WIDTH; x++) gameBoard[0][x] = 0;
                y++;
            }
        }
        if (linesCleared > 0) {
            int points = 0;
            switch (linesCleared) { case 1: points = 100; break; case 2: points = 300; break; case 3: points = 500; break; case 4: points = 800; break; }
            score += (points * level);
            int newLvl = (score / 1000) + 1;
            if (newLvl > level) { level = newLvl; fallDelay = calculateFallDelay(level); if (context != null) Toast.makeText(context, "Level Up! " + level, Toast.LENGTH_SHORT).show(); }
            updateScoreAndLevelDisplay();
            if (tetrisView != null) tetrisView.setGameBoard(gameBoard);
        }
    }

    private void handleGameOver() {
        if (isGameOver) return; isGameOver = true;
        if (gameHandler != null) gameHandler.removeCallbacks(gameRunnable);
        saveHighScore();
        overlayContainer.setVisibility(View.VISIBLE); gameOverCard.setVisibility(View.VISIBLE); pausedCard.setVisibility(View.GONE);
        setGameControlsEnabled(false); if (isFabMenuOpen) closeFabMenu();
        if (summaryScoreTextView != null) summaryScoreTextView.setText(getResString(R.string.final_score_format, score));
        if (context != null) Toast.makeText(context, getResString(R.string.game_over_toast, score), Toast.LENGTH_LONG).show();
    }

    // Simplified showConfirmationDialog, Runnables are handled by onDialogPositive/NegativeClick via tag
    private void showConfirmationDialog(String title, String message, String dialogTag) {
        if (getContext() == null || getParentFragmentManager() == null) {
            Log.e(TAG, "Cannot show confirmation dialog, context or fragment manager is null.");
            return;
        }
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                title, message, getString(android.R.string.yes), getString(android.R.string.no)
        );
        dialog.setListener(this);
        dialog.show(getParentFragmentManager(), dialogTag);
    }

    private void internalPauseGame() {
        if (isGameOver || isPaused) return; isPaused = true;
        if (gameHandler != null) gameHandler.removeCallbacks(gameRunnable);
        Log.d(TAG, "Game internally paused.");
    }

    private void pauseGameAndShowPauseCard() {
        if (isGameOver || isPaused) return; internalPauseGame();
        overlayContainer.setVisibility(View.VISIBLE); pausedCard.setVisibility(View.VISIBLE); gameOverCard.setVisibility(View.GONE);
        if (fabPausePlay != null) {
            fabPausePlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
            fabPausePlay.setContentDescription(getResString(R.string.play_desc));
        }
        if (isFabMenuOpen) closeFabMenu();
        setGameControlsEnabled(false);
        if (fabPausePlay != null) fabPausePlay.setEnabled(true);
        if (context != null) Toast.makeText(context, getResString(R.string.game_paused_toast), Toast.LENGTH_SHORT).show();
    }

    private void internalResumeGame() {
        if (isGameOver || !isPaused) return; isPaused = false;
        startGameLoop(); Log.d(TAG, "Game internally resumed.");
    }

    private void resumeGameAndHideUI(boolean showToast) {
        if (isGameOver || !isPaused) return; internalResumeGame();
        overlayContainer.setVisibility(View.GONE); pausedCard.setVisibility(View.GONE);
        if (fabPausePlay != null) {
            fabPausePlay.setImageResource(R.drawable.ic_pause_white_24dp);
            fabPausePlay.setContentDescription(getResString(R.string.pause_desc));
        }
        setGameControlsEnabled(true);
        if (showToast && context != null) Toast.makeText(context, getResString(R.string.game_resumed_toast), Toast.LENGTH_SHORT).show();
    }

    private void setGameControlsEnabled(boolean enabled) {
        if (fabMainToggle != null) fabMainToggle.setEnabled(enabled);
        boolean actionFabsShouldBeEnabled = enabled && isFabMenuOpen && !isPaused && !isGameOver;
        if (fabRotate != null) fabRotate.setEnabled(actionFabsShouldBeEnabled);
        if (fabLeft != null) fabLeft.setEnabled(actionFabsShouldBeEnabled);
        if (fabRight != null) fabRight.setEnabled(actionFabsShouldBeEnabled);
        if (fabDown != null) fabDown.setEnabled(actionFabsShouldBeEnabled);
        if (fabPausePlay != null) fabPausePlay.setEnabled(!isGameOver);
        if (!enabled && isFabMenuOpen) closeFabMenu();
    }

    private void showHighScoresDialog() {
        if (!isPaused && !isGameOver) internalPauseGame();
        HighScoresDialogFragment dialog = HighScoresDialogFragment.newInstance(highScores);
        dialog.setOnDismissListener(this);
        if (getParentFragmentManager() != null) dialog.show(getParentFragmentManager(), "HighScoresDialog");
        else Log.e(TAG, "Cannot show HighScoresDialog, ParentFragmentManager is null");
    }

    @Override
    public void onDismiss() { if (isPaused && !isGameOver) resumeGameAndHideUI(true); }

    private void showGameInfoDialog() {
        if (!isPaused && !isGameOver) internalPauseGame();
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Welcome to Tetris!", getTetrisInfoContent(), "Close", null
        );
        dialog.setListener(this);
        if (getParentFragmentManager() != null) dialog.show(getParentFragmentManager(), DIALOG_TAG_GAME_INFO);
        else Log.e(TAG, "Cannot show Game Info Dialog, ParentFragmentManager is null.");
    }

    private String getTetrisInfoContent() {
        return getResString(R.string.tetris_info_how_to_play) + "\n\n" +
                getResString(R.string.tetris_info_controls_fab) + "\n\n" +
                getResString(R.string.tetris_info_controls_main) + "\n\n" +
                getResString(R.string.tetris_info_scoring);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialogFragment) {
        String tag = dialogFragment.getTag();
        if (tag == null) {
            if (dialogFragment != null) dialogFragment.dismiss();
            return;
        }
        Log.d(TAG, "onDialogPositiveClick for tag: " + tag);
        switch (tag) {
            case DIALOG_TAG_GAME_INFO:
                if (isPaused && !isGameOver) {
                    overlayContainer.setVisibility(View.VISIBLE);
                    pausedCard.setVisibility(View.VISIBLE);
                }
                break;
            case DIALOG_TAG_RESTART_CONFIRM:
                Log.d(TAG, "Restart confirmed. Calling startGame().");
                startGame();
                break;
            default:
                Log.w(TAG, "Unhandled positive dialog click for tag: " + tag);
                break;
        }
        if (dialogFragment != null && dialogFragment.getDialog() != null && dialogFragment.getDialog().isShowing()) {
            dialogFragment.dismiss();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialogFragment) {
        String tag = dialogFragment.getTag();
        if (tag == null) { if (dialogFragment != null) dialogFragment.dismiss(); return; }
        Log.d(TAG, "onDialogNegativeClick for tag: " + tag);
        switch (tag) {
            case DIALOG_TAG_RESTART_CONFIRM:
                Log.d(TAG, "Restart cancelled.");
                if (isPaused && !isGameOver) {
                    overlayContainer.setVisibility(View.VISIBLE);
                    pausedCard.setVisibility(View.VISIBLE);
                    gameOverCard.setVisibility(View.GONE);
                    // internalPauseGame(); // Already paused
                }
                break;
            default: Log.w(TAG, "Unhandled negative dialog click for tag: " + tag); break;
        }
        if (dialogFragment != null && dialogFragment.getDialog() != null && dialogFragment.getDialog().isShowing()) {
            // dialogFragment.dismiss(); // Let CustomMessageDialogFragment handle its own dismissal
        }
    }

    private void loadHighScores() {
        if (sharedPreferences == null) { Log.e(TAG, "SP null in loadHighScores."); highScores = new ArrayList<>(); return; }
        String json = sharedPreferences.getString(SCORES_KEY, "[]"); Type type = new TypeToken<ArrayList<HighScore>>(){}.getType();
        try { highScores = gson.fromJson(json, type); } catch (Exception e) { Log.e(TAG, "GSON parse error", e); highScores = new ArrayList<>(); }
        if (highScores == null) highScores = new ArrayList<>();
        Collections.sort(highScores);
    }

    private void saveHighScore() {
        if (sharedPreferences == null || score == 0) { Log.w(TAG, "Cannot save high score."); return; }
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (highScores == null) highScores = new ArrayList<>();
        highScores.add(new HighScore(score, level, date)); Collections.sort(highScores);
        if (highScores.size() > 20) highScores = new ArrayList<>(highScores.subList(0, 20));
        sharedPreferences.edit().putString(SCORES_KEY, gson.toJson(highScores)).apply();
    }

    @Override
    public void onPause() { super.onPause(); Log.d(TAG, "onPause"); if (!isPaused && !isGameOver) internalPauseGame(); }

    @Override
    public void onResume() {
        super.onResume(); Log.d(TAG, "onResume");
        if (mainActivity != null && mainActivity.toolbar != null) {
            mainActivity.toolbar.setTitle(getResString(R.string.tetris_title));
            if (mainActivity.navigationView != null) mainActivity.navigationView.setCheckedItem(R.id.nav_fun_corner);
            if (mainActivity.MenuTrigger != null) mainActivity.MenuTrigger.setVisibility(View.GONE);
            if (mainActivity.Fab != null) mainActivity.Fab.setVisibility(View.GONE);
        }
        if (isPaused && !isGameOver && overlayContainer.getVisibility() != View.VISIBLE) internalResumeGame();
        else if (isPaused && !isGameOver) Log.d(TAG, "onResume: Game paused, overlay visible.");
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); Log.d(TAG, "onDestroyView"); if (gameHandler != null) gameHandler.removeCallbacksAndMessages(null); }

    private String getResString(int resId, Object... args) {
        if (!isAdded() || context == null) { Log.w(TAG, "Ctx null for res: " + resId); return "Err:" + resId; }
        return (args.length > 0) ? context.getString(resId, args) : context.getString(resId);
    }
}
