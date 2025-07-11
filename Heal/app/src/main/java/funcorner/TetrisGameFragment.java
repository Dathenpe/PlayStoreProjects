package funcorner;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.R;

import java.util.Random;

import tetris.TetrisView;

/**
 * A simple {@link Fragment} subclass for the Tetris game.
 * This fragment manages the game logic, UI updates, and user interactions.
 */
public class TetrisGameFragment extends Fragment {

    // Game Board Constants
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;

    // Game State Variables
    private int[][] gameBoard; // 0 for empty, non-zero for block color
    private TetrisView.Tetromino currentPiece;
    private int currentPieceX, currentPieceY;
    private int score;
    private int level;
    private boolean isGameOver;
    private boolean isPaused;

    // UI Elements
    private TetrisView tetrisView;
    private TextView scoreTextView;
    private TextView levelTextView;
    private TextView gameOverTextView;
    private TextView pauseTextView;
    private ImageButton rotateButton;
    private ImageButton leftButton;
    private ImageButton rightButton;
    private ImageButton downButton;
    private ImageButton pausePlayButton;

    // Game Loop
    private Handler gameHandler;
    private Runnable gameRunnable;
    private long fallDelay = 1000; // Initial fall delay in milliseconds (1 second)

    private Random random;

    public TetrisGameFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        random = new Random();
        gameHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tetris_game, container, false);

        // Initialize UI elements
        tetrisView = view.findViewById(R.id.tetris_view);
        scoreTextView = view.findViewById(R.id.score_text_view);
        levelTextView = view.findViewById(R.id.level_text_view);
        gameOverTextView = view.findViewById(R.id.game_over_text);
        pauseTextView = view.findViewById(R.id.pause_text);
        rotateButton = view.findViewById(R.id.button_rotate);
        leftButton = view.findViewById(R.id.button_left);
        rightButton = view.findViewById(R.id.button_right);
        downButton = view.findViewById(R.id.button_down);
        pausePlayButton = view.findViewById(R.id.button_pause_play);

        // Set up button listeners
        rotateButton.setOnClickListener(v -> rotatePiece());
        leftButton.setOnClickListener(v -> movePiece(-1, 0));
        rightButton.setOnClickListener(v -> movePiece(1, 0));
        downButton.setOnClickListener(v -> movePiece(0, 1));
        pausePlayButton.setOnClickListener(v -> togglePause());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startGame(); // Start the game when the view is created
    }

    /**
     * Initializes a new game.
     */
    private void startGame() {
        gameBoard = new int[BOARD_HEIGHT][BOARD_WIDTH]; // Initialize empty board
        score = 0;
        level = 1;
        isGameOver = false;
        isPaused = false;

        updateScoreAndLevelDisplay();
        gameOverTextView.setVisibility(View.GONE);
        pauseTextView.setVisibility(View.GONE);
        pausePlayButton.setImageResource(R.drawable.ic_pause_white_24dp); // Set pause icon

        spawnNewPiece();
        startGameLoop();
    }

    /**
     * Updates the score and level TextViews.
     */
    private void updateScoreAndLevelDisplay() {
        scoreTextView.setText("Score: " + score);
        levelTextView.setText("Level: " + level);
    }

    /**
     * Starts or resumes the game loop.
     */
    private void startGameLoop() {
        gameRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isGameOver && !isPaused) {
                    if (!movePiece(0, 1)) { // Try to move piece down
                        // If piece cannot move down, it has landed
                        landPiece();
                        clearLines();
                        if (!isGameOver) { // Check game over after clearing lines
                            spawnNewPiece();
                        }
                    }
                }
                if (!isGameOver) {
                    gameHandler.postDelayed(this, fallDelay); // Schedule next fall
                }
            }
        };
        gameHandler.postDelayed(gameRunnable, fallDelay);
    }

    /**
     * Spawns a new random Tetromino at the top center of the board.
     */
    private void spawnNewPiece() {
        currentPiece = TetrisView.Tetromino.ALL_TETROMINOES[random.nextInt(TetrisView.Tetromino.ALL_TETROMINOES.length)];
        currentPieceX = BOARD_WIDTH / 2 - currentPiece.getShape()[0].length / 2;
        currentPieceY = 0;

        // Check for immediate game over condition (piece spawns on existing blocks)
        if (!isValidPosition(currentPiece, currentPieceX, currentPieceY)) {
            isGameOver = true;
            gameOverTextView.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "Game Over! Score: " + score, Toast.LENGTH_LONG).show();
            gameHandler.removeCallbacks(gameRunnable); // Stop the game loop
        }
        tetrisView.setCurrentPiece(currentPiece, currentPieceX, currentPieceY);
        tetrisView.setGameBoard(gameBoard); // Update view with the new piece
    }

    /**
     * Checks if a given piece at a given position is valid (no collisions or out of bounds).
     * @param piece The Tetromino to check.
     * @param newX The new X-coordinate.
     * @param newY The new Y-coordinate.
     * @return True if the position is valid, false otherwise.
     */
    private boolean isValidPosition(TetrisView.Tetromino piece, int newX, int newY) {
        int[][] shape = piece.getShape();
        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] != 0) {
                    int boardX = newX + x;
                    int boardY = newY + y;

                    // Check boundaries
                    if (boardX < 0 || boardX >= BOARD_WIDTH || boardY >= BOARD_HEIGHT) {
                        return false; // Out of bounds
                    }
                    // Check collision with existing blocks on the board (only if within bounds)
                    if (boardY >= 0 && gameBoard[boardY][boardX] != 0) {
                        return false; // Collision
                    }
                }
            }
        }
        return true;
    }

    /**
     * Moves the current piece by the given delta X and delta Y.
     * @param deltaX Change in X (column).
     * @param deltaY Change in Y (row).
     * @return True if the move was successful, false if it collided or was out of bounds.
     */
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

    /**
     * Rotates the current piece if the rotation is valid.
     */
    private void rotatePiece() {
        if (isGameOver || isPaused) return;

        TetrisView.Tetromino rotatedPiece = currentPiece.rotate();
        // Simple wall kick: try shifting left/right if rotation is blocked
        if (isValidPosition(rotatedPiece, currentPieceX, currentPieceY)) {
            currentPiece = rotatedPiece;
            tetrisView.setCurrentPiece(currentPiece, currentPieceX, currentPieceY);
        } else if (isValidPosition(rotatedPiece, currentPieceX + 1, currentPieceY)) { // Try shifting right
            currentPieceX++;
            currentPiece = rotatedPiece;
            tetrisView.setCurrentPiece(currentPiece, currentPieceX, currentPieceY);
        } else if (isValidPosition(rotatedPiece, currentPieceX - 1, currentPieceY)) { // Try shifting left
            currentPieceX--;
            currentPiece = rotatedPiece;
            tetrisView.setCurrentPiece(currentPiece, currentPieceX, currentPieceY);
        }
    }

    /**
     * Lands the current piece onto the game board.
     */
    private void landPiece() {
        int[][] shape = currentPiece.getShape();
        int color = currentPiece.getColor();

        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] != 0) {
                    // Place the block on the game board
                    gameBoard[currentPieceY + y][currentPieceX + x] = color;
                }
            }
        }
        tetrisView.setGameBoard(gameBoard); // Update the view with the landed piece
    }

    /**
     * Checks for and clears full lines, updating the score and level.
     */
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
                // Shift all lines above down by one
                for (int rowToMove = y; rowToMove > 0; rowToMove--) {
                    System.arraycopy(gameBoard[rowToMove - 1], 0, gameBoard[rowToMove], 0, BOARD_WIDTH);
                }
                // Clear the top line
                for (int x = 0; x < BOARD_WIDTH; x++) {
                    gameBoard[0][x] = 0;
                }
                y++; // Re-check the current line as it's now a new one
            }
        }

        if (linesCleared > 0) {
            // Update score based on lines cleared (Tetris scoring system)
            switch (linesCleared) {
                case 1: score += 100 * level; break;
                case 2: score += 300 * level; break;
                case 3: score += 500 * level; break;
                case 4: score += 800 * level; break; // Tetris!
            }

            // Increase level every 10 lines (example)
            if (score / (1000 * level) >= 1) { // Adjust this logic for level up
                level++;
                fallDelay = Math.max(100, fallDelay - 50); // Decrease fall delay, min 100ms
                Toast.makeText(getContext(), "Level Up! Level: " + level, Toast.LENGTH_SHORT).show();
                // Restart game loop with new delay
                gameHandler.removeCallbacks(gameRunnable);
                startGameLoop();
            }
            updateScoreAndLevelDisplay();
            tetrisView.setGameBoard(gameBoard); // Redraw board after clearing lines
        }
    }

    /**
     * Toggles the game's paused state.
     */
    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            gameHandler.removeCallbacks(gameRunnable); // Stop game loop
            pauseTextView.setVisibility(View.VISIBLE);
            pausePlayButton.setImageResource(R.drawable.ic_play_arrow_white_24dp); // Set play icon
            Toast.makeText(getContext(), "Game Paused", Toast.LENGTH_SHORT).show();
        } else {
            startGameLoop(); // Resume game loop
            pauseTextView.setVisibility(View.GONE);
            pausePlayButton.setImageResource(R.drawable.ic_pause_white_24dp); // Set pause icon
            Toast.makeText(getContext(), "Game Resumed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause the game when the fragment is no longer in the foreground
        if (!isGameOver && !isPaused) {
            togglePause(); // Automatically pause if not already game over or paused
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // If the game was paused and not over, resume it
        if (!isGameOver && isPaused) {
            // No need to call togglePause() here, as it will be resumed by user interaction
            // or if we want auto-resume, we can call togglePause()
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        gameHandler.removeCallbacks(gameRunnable); // Stop the game loop to prevent memory leaks
    }
}
