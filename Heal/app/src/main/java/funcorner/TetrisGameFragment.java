package funcorner;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

public class TetrisGameFragment extends Fragment implements HighScoresDialogFragment.OnDismissListener { // Removed TetrisView.OnPieceRenderedListener

    // Game Board Constants
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final String PREFS_NAME = "TetrisHighScores";
    private static final String SCORES_KEY = "scores";
    private static final int LEVEL_UP_SCORE_THRESHOLD = 200;
    private static final long INITIAL_FALL_DELAY = 1000;
    private static final long MIN_FALL_DELAY = 150;

    // Game State Variables
    private int[][] gameBoard;
    private TetrisView.Tetromino currentPiece;
    private int currentPieceX, currentPieceY;
    private int score;
    private int level;
    private boolean isGameOver;
    private boolean isPaused;
    private int lastGameScore = 0;

    // UI Elements
    private TetrisView tetrisView;
    private TextView scoreTextView;
    private TextView levelTextView;
    private TextView gameOverTextView;
    private TextView pauseTextView;
    private MaterialButton restartButton;
    private MaterialButton showHighScoresButton;
    private FrameLayout overlayContainer;
    private View pausedCard;
    private View gameOverCard;
    // Removed: private ScrollView gameBoardScrollView; // No longer needed

    // FAB Controls
    private FloatingActionButton fabMainToggle;
    private FloatingActionButton fabPausePlay;
    private FloatingActionButton fabRotate;
    private FloatingActionButton fabLeft;
    private FloatingActionButton fabRight;
    private FloatingActionButton fabDown;
    private LinearLayout gamepadControlsLayout;
    private boolean isFabMenuOpen = false;

    // Game Loop
    private Handler gameHandler;
    private Runnable gameRunnable;
    private long fallDelay;

    private Random random;

    // High Scores
    private ArrayList<HighScore> highScores;
    private SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();

    public TetrisGameFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        random = new Random();
        gameHandler = new Handler(Looper.getMainLooper());
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadHighScores();
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
        // Removed: tetrisView.setOnPieceRenderedListener(this);
        startGame();
    }

    private void initializeUI(View view) {
        tetrisView = view.findViewById(R.id.tetris_view);
        scoreTextView = view.findViewById(R.id.score_text_view);
        levelTextView = view.findViewById(R.id.level_text_view);
        gameOverTextView = view.findViewById(R.id.game_over_text);
        pauseTextView = view.findViewById(R.id.pause_text);

        restartButton = view.findViewById(R.id.button_restart);
        showHighScoresButton = view.findViewById(R.id.button_show_high_scores);
        overlayContainer = view.findViewById(R.id.overlay_container);
        pausedCard = view.findViewById(R.id.paused_card);
        gameOverCard = view.findViewById(R.id.game_over_card);

        // FABs
        fabMainToggle = view.findViewById(R.id.fab_main_toggle);
        fabPausePlay = view.findViewById(R.id.fab_pause_play);
        fabRotate = view.findViewById(R.id.fab_rotate);
        fabLeft = view.findViewById(R.id.fab_left);
        fabRight = view.findViewById(R.id.fab_right);
        fabDown = view.findViewById(R.id.fab_down);
        gamepadControlsLayout = view.findViewById(R.id.fab_menu_actions);

        overlayContainer.setOnClickListener(v -> {
            if (isPaused && !isGameOver) {
                togglePause();
            }
        });
    }

    private void setupButtonListeners() {
        // FAB Menu Toggle
        fabMainToggle.setOnClickListener(v -> {
            if (isGameOver) return;
            if (isFabMenuOpen) {
                closeFabMenu();
            } else {
                openFabMenu();
            }
        });

        // Pause/Play FAB
        fabPausePlay.setOnClickListener(v -> togglePause());

        // Action FABs (inside the menu)
        fabRotate.setOnClickListener(v -> {
            if (!isGameOver && !isPaused) rotatePiece();
        });
        fabLeft.setOnClickListener(v -> {
            if (!isGameOver && !isPaused) movePiece(-1, 0);
        });
        fabRight.setOnClickListener(v -> {
            if (!isGameOver && !isPaused) movePiece(1, 0);
        });
        fabDown.setOnClickListener(v -> {
            if (!isGameOver && !isPaused) movePiece(0, 1);
        });

        // Other Buttons
        restartButton.setOnClickListener(v -> startGame());
        showHighScoresButton.setOnClickListener(v -> showHighScoresDialog());
    }

    private void openFabMenu() {
        if (isGameOver || isPaused) return;
        isFabMenuOpen = true;
        gamepadControlsLayout.setVisibility(View.VISIBLE);
        fabRotate.setVisibility(View.VISIBLE);
        fabLeft.setVisibility(View.VISIBLE);
        fabRight.setVisibility(View.VISIBLE);
        fabDown.setVisibility(View.VISIBLE);
        fabPausePlay.setVisibility(View.GONE);
        fabMainToggle.setImageResource(R.drawable.ic_close);
        fabMainToggle.setContentDescription("Close Game Controls");
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;
        gamepadControlsLayout.setVisibility(View.GONE);
        fabPausePlay.setVisibility(View.VISIBLE);
        fabRotate.setVisibility(View.GONE);
        fabLeft.setVisibility(View.GONE);
        fabRight.setVisibility(View.GONE);
        fabDown.setVisibility(View.GONE);

        fabMainToggle.setImageResource(R.drawable.ic_gamepad);
        fabMainToggle.setContentDescription("Open Game Controls");
    }

    private void startGame() {
        if (lastGameScore >= LEVEL_UP_SCORE_THRESHOLD) {
            level++;
            Toast.makeText(getContext(), "Difficulty Increased! Level: " + level, Toast.LENGTH_SHORT).show();
        } else {
            level = 1;
        }

        gameBoard = new int[BOARD_HEIGHT][BOARD_WIDTH];
        score = 0;
        isGameOver = false;
        isPaused = false;
        fallDelay = calculateFallDelay(level);

        updateScoreAndLevelDisplay();
        overlayContainer.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);

        fabPausePlay.setImageResource(R.drawable.ic_pause_white_24dp);
        fabPausePlay.setContentDescription("Pause");
        setGameControlsEnabled(true);
        if (isFabMenuOpen) {
            closeFabMenu();
        }

        spawnNewPiece();
        startGameLoop();
    }

    private long calculateFallDelay(int currentLevel) {
        return Math.max(MIN_FALL_DELAY, INITIAL_FALL_DELAY - ((long)(currentLevel - 1) * 75L));
    }

    private void updateScoreAndLevelDisplay() {
        scoreTextView.setText("Score: " + score);
        levelTextView.setText("Level: " + level);
    }

    private void startGameLoop() {
        gameHandler.removeCallbacks(gameRunnable);
        gameRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isGameOver && !isPaused) {
                    if (!movePiece(0, 1)) {
                        landPiece();
                        clearLines();
                        if (!isGameOver) {
                            spawnNewPiece();
                        }
                    }
                }
                if (!isGameOver) {
                    gameHandler.postDelayed(this, fallDelay);
                }
            }
        };
        gameHandler.postDelayed(gameRunnable, fallDelay);
    }

    private void spawnNewPiece() {
        currentPiece = TetrisView.Tetromino.ALL_TETROMINOES[random.nextInt(TetrisView.Tetromino.ALL_TETROMINOES.length)];
        currentPieceX = BOARD_WIDTH / 2 - currentPiece.getShape()[0].length / 2;
        currentPieceY = 0;

        if (!isValidPosition(currentPiece, currentPieceX, currentPieceY)) {
            handleGameOver();
        } else {
            tetrisView.setCurrentPiece(currentPiece, currentPieceX, currentPieceY);
            tetrisView.setGameBoard(gameBoard);
        }
    }

    private boolean isValidPosition(TetrisView.Tetromino piece, int newX, int newY) {
        int[][] shape = piece.getShape();
        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] != 0) {
                    int boardX = newX + x;
                    int boardY = newY + y;
                    if (boardX < 0 || boardX >= BOARD_WIDTH || boardY >= BOARD_HEIGHT) {
                        return false;
                    }
                    if (boardY >= 0 && gameBoard[boardY][boardX] != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean movePiece(int deltaX, int deltaY) {
        if (isGameOver || isPaused) return false;

        int newX = currentPieceX + deltaX;
        int newY = currentPieceY + deltaY;

        if (isValidPosition(currentPiece, newX, newY)) {
            currentPieceX = newX;
            currentPieceY = newY;
            tetrisView.setCurrentPiece(currentPiece, currentPieceX, currentPieceY);
            return true;
        }
        return false;
    }

    private void rotatePiece() {
        if (isGameOver || isPaused) return;

        TetrisView.Tetromino originalPiece = currentPiece;
        TetrisView.Tetromino rotatedPiece = currentPiece.rotate();

        if (isValidPosition(rotatedPiece, currentPieceX, currentPieceY)) {
            currentPiece = rotatedPiece;
        } else if (isValidPosition(rotatedPiece, currentPieceX + 1, currentPieceY)) {
            currentPieceX++;
            currentPiece = rotatedPiece;
        } else if (isValidPosition(rotatedPiece, currentPieceX - 1, currentPieceY)) {
            currentPieceX--;
            currentPiece = rotatedPiece;
        }
        if (currentPiece != originalPiece || isValidPosition(currentPiece, currentPieceX, currentPieceY)) {
            tetrisView.setCurrentPiece(currentPiece, currentPieceX, currentPieceY);
        }
    }

    private void landPiece() {
        int[][] shape = currentPiece.getShape();
        int color = currentPiece.getColor();
        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] != 0) {
                    if (currentPieceY + y >= 0 && currentPieceY + y < BOARD_HEIGHT &&
                            currentPieceX + x >= 0 && currentPieceX + x < BOARD_WIDTH) {
                        gameBoard[currentPieceY + y][currentPieceX + x] = color;
                    } else {
                        handleGameOver();
                        return;
                    }
                }
            }
        }
        tetrisView.setGameBoard(gameBoard);
    }

    private void clearLines() {
        int linesCleared = 0;
        for (int y = BOARD_HEIGHT - 1; y >= 0; y--) {
            boolean lineFull = true;
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (gameBoard[y][x] == 0) {
                    lineFull = false;
                    break;
                }
            }
            if (lineFull) {
                linesCleared++;
                for (int rowToMove = y; rowToMove > 0; rowToMove--) {
                    System.arraycopy(gameBoard[rowToMove - 1], 0, gameBoard[rowToMove], 0, BOARD_WIDTH);
                }
                for (int x = 0; x < BOARD_WIDTH; x++) {
                    gameBoard[0][x] = 0;
                }
                y++;
            }
        }
        if (linesCleared > 0) {
            score += (100 * linesCleared * level);
            int newLevel = (score / 1000) + 1;
            if (newLevel > level) {
                level = newLevel;
                fallDelay = calculateFallDelay(level);
                Toast.makeText(getContext(), "Level Up! Level: " + level, Toast.LENGTH_SHORT).show();
                gameHandler.removeCallbacks(gameRunnable);
                startGameLoop();
            }
            updateScoreAndLevelDisplay();
            tetrisView.setGameBoard(gameBoard);
        }
    }

    private void handleGameOver() {
        isGameOver = true;
        gameHandler.removeCallbacks(gameRunnable);
        lastGameScore = score;
        saveHighScore();

        overlayContainer.setVisibility(View.VISIBLE);
        gameOverCard.setVisibility(View.VISIBLE);
        pausedCard.setVisibility(View.GONE);
        setGameControlsEnabled(false);
        if(isFabMenuOpen) closeFabMenu();

        Toast.makeText(getContext(), "Game Over! Score: " + score, Toast.LENGTH_LONG).show();
    }

    private void togglePause() {
        if (isGameOver) return;

        isPaused = !isPaused;
        if (isPaused) {
            gameHandler.removeCallbacks(gameRunnable);
            overlayContainer.setVisibility(View.VISIBLE);
            pausedCard.setVisibility(View.VISIBLE);
            gameOverCard.setVisibility(View.GONE);
            fabPausePlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
            fabPausePlay.setContentDescription("Play");
            if (isFabMenuOpen) closeFabMenu();
            setGameControlsEnabled(false);
            fabPausePlay.setEnabled(true);
            Toast.makeText(getContext(), "Game Paused", Toast.LENGTH_SHORT).show();
        } else {
            overlayContainer.setVisibility(View.GONE);
            pausedCard.setVisibility(View.GONE);
            fabPausePlay.setImageResource(R.drawable.ic_pause_white_24dp);
            fabPausePlay.setContentDescription("Pause");
            setGameControlsEnabled(true);
            startGameLoop();
            Toast.makeText(getContext(), "Game Resumed", Toast.LENGTH_SHORT).show();
        }
    }

    // New method to pause game logic only, without showing the pause UI
    private void pauseGameLogicOnly() {
        if (isGameOver || isPaused) return;
        isPaused = true;
        gameHandler.removeCallbacks(gameRunnable);
        setGameControlsEnabled(false);
        fabPausePlay.setEnabled(true);
        if (isFabMenuOpen) closeFabMenu();
    }


    private void setGameControlsEnabled(boolean enabled) {
        fabMainToggle.setEnabled(enabled);
        fabRotate.setEnabled(enabled);
        fabLeft.setEnabled(enabled);
        fabRight.setEnabled(enabled);
        fabDown.setEnabled(enabled);
        fabPausePlay.setEnabled(!isGameOver);

        if (!enabled && isFabMenuOpen) {
            closeFabMenu();
        }
    }

    private void showHighScoresDialog() {
        pauseGameLogicOnly();
        pausedCard.setVisibility(View.GONE);
        fabPausePlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        overlayContainer.setVisibility(View.GONE);
        Toast.makeText(getContext(), "Game Paused", Toast.LENGTH_SHORT).show();
        HighScoresDialogFragment dialog = HighScoresDialogFragment.newInstance(highScores);
        dialog.setOnDismissListener(this);
        dialog.show(getParentFragmentManager(), "HighScoresDialog");
    }

    @Override
    public void onDismiss() {
        if (isPaused && !isGameOver) {
            togglePause();
        }
    }

    // Removed onPieceRendered method as automatic scrolling is no longer desired.

    private void loadHighScores() {
        String jsonScores = sharedPreferences.getString(SCORES_KEY, "[]");
        Type type = new TypeToken<ArrayList<HighScore>>() {}.getType();
        highScores = gson.fromJson(jsonScores, type);
        if (highScores == null) {
            highScores = new ArrayList<>();
        }
        Collections.sort(highScores);
    }

    private void saveHighScore() {
        if (score == 0) return;
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        highScores.add(new HighScore(score, level, date));
        Collections.sort(highScores);
        if (highScores.size() > 20) {
            highScores = new ArrayList<>(highScores.subList(0, 20));
        }
        String jsonScores = gson.toJson(highScores);
        sharedPreferences.edit().putString(SCORES_KEY, jsonScores).apply();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!isGameOver && !isPaused) {
            togglePause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        gameHandler.removeCallbacks(gameRunnable);
    }
}
