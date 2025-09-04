package funcorner;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ui.CustomMessageDialogFragment;

public class TicTacToeGameFragment extends Fragment {

    private static final String TAG = "TicTacToeGameFragment";

    private MainActivity mainActivity;
    private RecyclerView boardRecyclerView;
    private TextView statusTextView, summaryTextView, pauseText;
    private MaterialButton onePlayerModeButton, twoPlayerModeButton, pauseButton, gameInfoButton;
    private FrameLayout overlayContainer;
    private MaterialCardView pausedCard, gameOverCard;
    private Spinner difficultySpinner;
    private RadioGroup symbolRadioGroup;
    private LinearLayout onePlayerControls;

    private TicTacToeAdapter adapter;
    private String[] board;
    private String currentPlayer;
    private String playerSymbol;
    private String xavierSymbol;
    private boolean isGameOver;
    private boolean isPaused;
    private boolean isTwoPlayerMode = false;

    private enum Difficulty { EASY, MEDIUM, HARD }
    private Difficulty currentDifficulty = Difficulty.EASY;
    private int selectedDifficultyPosition = 0;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tic_tac_toe_game, container, false);
        initViews(view);
        setupListeners();
        setupDifficultySpinner();
        restartGame();
        updateModeButtonStates();
        return view;
    }

    private void initViews(View view) {
        boardRecyclerView = view.findViewById(R.id.tic_tac_toe_board);
        statusTextView = view.findViewById(R.id.status_text_view);
        onePlayerModeButton = view.findViewById(R.id.one_player_mode_button);
        twoPlayerModeButton = view.findViewById(R.id.two_player_mode_button);
        pauseButton = view.findViewById(R.id.pause_button);
        overlayContainer = view.findViewById(R.id.overlay_container);
        pausedCard = view.findViewById(R.id.paused_card);
        gameOverCard = view.findViewById(R.id.game_over_card);
        pauseText = view.findViewById(R.id.pause_text);
        summaryTextView = view.findViewById(R.id.summary_text_view);
        difficultySpinner = view.findViewById(R.id.difficulty_spinner);
        symbolRadioGroup = view.findViewById(R.id.symbol_radio_group);
        onePlayerControls = view.findViewById(R.id.one_player_controls);

        // Buttons inside overlays
        MaterialButton resumeButton = view.findViewById(R.id.button_resume_game);
        MaterialButton restartPausedButton = view.findViewById(R.id.button_restart_game_paused);
        MaterialButton restartOverButton = view.findViewById(R.id.button_restart_game_over);
        gameInfoButton = view.findViewById(R.id.button_info_game);

        resumeButton.setOnClickListener(v -> {
            pauseButton.setIconResource(R.drawable.ic_pause_white_24dp);
            resumeGame();
        });
        restartPausedButton.setOnClickListener(v -> showConfirmationDialog(
                "Restart Game?",
                "Are you sure you want to restart the game? All progress will be lost.",
                this::restartGame,
                () -> { pausedCard.setVisibility(View.VISIBLE); }
        ));

        restartOverButton.setOnClickListener(v -> restartGame());
        gameInfoButton.setOnClickListener(v -> showGameInfoDialog());
    }

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
        dialog.show(getParentFragmentManager(), "TicTacToeConfirmationDialog");
    }

    private void setupListeners() {
        onePlayerModeButton.setOnClickListener(v -> {
            if (isTwoPlayerMode) {
                showConfirmationDialog(
                        "Switch to One Player?",
                        "Are you sure you want to switch to one player mode? This will restart the game.",
                        () -> {
                            isTwoPlayerMode = false;
                            updateModeButtonStates();
                            restartGame();
                        },
                        null
                );
            }
        });

        twoPlayerModeButton.setOnClickListener(v -> {
            if (!isTwoPlayerMode) {
                showConfirmationDialog(
                        "Switch to Two Players?",
                        "Are you sure you want to switch to two player mode? This will restart the game.",
                        () -> {
                            isTwoPlayerMode = true;
                            updateModeButtonStates();
                            restartGame();
                        },
                        null
                );
            }
        });

        pauseButton.setOnClickListener(v -> {
            pauseButton.setIconResource(R.drawable.ic_play_arrow_white_24dp);
            pauseGameAndShowDialog();
        });

        symbolRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            final int previousCheckedId = (checkedId == R.id.radio_x) ? R.id.radio_o : R.id.radio_x;
            showConfirmationDialog(
                    "Switch Symbol?",
                    "Are you sure you want to switch your symbol? This will restart the game.",
                    () -> {
                        if (checkedId == R.id.radio_x) {
                            playerSymbol = "X";
                            xavierSymbol = "O";
                        } else {
                            playerSymbol = "O";
                            xavierSymbol = "X";
                        }
                        Toast.makeText(getContext(), "Symbol changed to " + playerSymbol, Toast.LENGTH_SHORT).show();
                        restartGame();
                    },
                    () -> {
                        // Revert back to the previous selection on negative action
                        symbolRadioGroup.check(previousCheckedId);
                    }
            );
        });
    }

    private void updateModeButtonStates() {
        onePlayerModeButton.setEnabled(isTwoPlayerMode);
        twoPlayerModeButton.setEnabled(!isTwoPlayerMode);
        onePlayerControls.setVisibility(isTwoPlayerMode ? View.GONE : View.VISIBLE);

        // Get resolved colors from resources
        @ColorInt int primaryColor = getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary);
        @ColorInt int silverColor = ContextCompat.getColor(requireContext(), R.color.silver);

        if (isTwoPlayerMode) {
            // Two-player mode is active
            onePlayerModeButton.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            twoPlayerModeButton.setBackgroundTintList(ColorStateList.valueOf(silverColor));
        } else {
            // One-player mode is active
            onePlayerModeButton.setBackgroundTintList(ColorStateList.valueOf(silverColor));
            twoPlayerModeButton.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        }
    }

    // Helper method to resolve an attribute color
    private int getThemeColor(@NonNull Context context, int resId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(resId, typedValue, true);
        return typedValue.data;
    }

    private void setupDifficultySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.tictactoe_difficulties,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);
        difficultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != selectedDifficultyPosition) {
                    showConfirmationDialog(
                            "Change Difficulty?",
                            "Are you sure you want to change the difficulty? This will restart the game.",
                            () -> {
                                selectedDifficultyPosition = position;
                                switch (position) {
                                    case 0:
                                        currentDifficulty = Difficulty.EASY;
                                        break;
                                    case 1:
                                        currentDifficulty = Difficulty.MEDIUM;
                                        break;
                                    case 2:
                                        currentDifficulty = Difficulty.HARD;
                                        break;
                                }
                                Toast.makeText(getContext(), "Difficulty changed to " + currentDifficulty.name(), Toast.LENGTH_SHORT).show();
                                restartGame();
                            },
                            () -> {
                                // Revert spinner selection on negative action
                                difficultySpinner.setSelection(selectedDifficultyPosition);
                            }
                    );
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void restartGame() {
        isGameOver = false;
        isPaused = false;
        board = new String[9];
        Arrays.fill(board, "");

        if (!isTwoPlayerMode) {
            if (symbolRadioGroup.getCheckedRadioButtonId() == R.id.radio_x) {
                playerSymbol = "X";
                xavierSymbol = "O";
            } else {
                playerSymbol = "O";
                xavierSymbol = "X";
            }
            // If Xavier is 'X', he always plays first
            if (xavierSymbol.equals("X")) {
                currentPlayer = xavierSymbol;
            } else {
                currentPlayer = playerSymbol;
            }
        } else {
            currentPlayer = "X";
        }

        if (adapter == null) {
            adapter = new TicTacToeAdapter(board);
            boardRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
            boardRecyclerView.setAdapter(adapter);
        } else {
            adapter.board = board;
            adapter.notifyDataSetChanged();
        }

        updateStatusText();
        overlayContainer.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        setUIEnabled(true);

        if (!isTwoPlayerMode && currentPlayer.equals(xavierSymbol)) {
            new Handler(Looper.getMainLooper()).postDelayed(this::xavierMove, 1000);
        }
    }

    private void updateStatusText() {
        String turnText;
        if (isTwoPlayerMode) {
            turnText = "Player " + currentPlayer + "'s Turn";
        } else {
            turnText = currentPlayer.equals(playerSymbol) ? "Your Turn (" + playerSymbol + ")" : "Xavier's Turn (" + xavierSymbol + ")";
        }
        statusTextView.setText(turnText);
    }

    private void checkGameStatus() {
        String winner = checkWinner();
        if (winner != null) {
            isGameOver = true;
            setUIEnabled(false);
            String winnerText = isTwoPlayerMode ? "Player " + winner + " wins!" : (winner.equals(playerSymbol) ? "You win!" : "Xavier wins!");
            statusTextView.setText(winnerText);
            summaryTextView.setText(winnerText);
            new Handler(Looper.getMainLooper()).postDelayed(() -> showGameOver(winnerText), 2000);
        } else if (isBoardFull()) {
            isGameOver = true;
            setUIEnabled(false);
            statusTextView.setText("It's a draw!");
            summaryTextView.setText("It's a draw!");
            new Handler(Looper.getMainLooper()).postDelayed(() -> showGameOver("Draw"), 2000);
        } else {
            currentPlayer = currentPlayer.equals("X") ? "O" : "X";
            updateStatusText();
            if (!isTwoPlayerMode && currentPlayer.equals(xavierSymbol) && !isGameOver) {
                new Handler(Looper.getMainLooper()).postDelayed(this::xavierMove, 1000);
            }
        }
    }

    private void xavierMove() {
        if (isGameOver) return;
        int bestMove = -1;
        switch (currentDifficulty) {
            case EASY:
                bestMove = getRandomMove();
                break;
            case MEDIUM:
                bestMove = getMediumMove();
                break;
            case HARD:
                bestMove = getHardMove();
                break;
        }
        if (bestMove != -1) {
            board[bestMove] = xavierSymbol;
            adapter.notifyItemChanged(bestMove);
            checkGameStatus();
        }
    }

    private int getRandomMove() {
        List<Integer> availableMoves = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i].isEmpty()) {
                availableMoves.add(i);
            }
        }
        if (!availableMoves.isEmpty()) {
            Collections.shuffle(availableMoves);
            return availableMoves.get(0);
        }
        return -1;
    }

    private int getMediumMove() {
        for (int i = 0; i < 9; i++) {
            if (board[i].isEmpty()) {
                board[i] = xavierSymbol;
                if (checkWinner() != null) {
                    board[i] = "";
                    return i;
                }
                board[i] = "";
            }
        }
        for (int i = 0; i < 9; i++) {
            if (board[i].isEmpty()) {
                board[i] = playerSymbol;
                if (checkWinner() != null) {
                    board[i] = "";
                    return i;
                }
                board[i] = "";
            }
        }
        return getRandomMove();
    }

    private int getHardMove() {
        String[] boardCopy = Arrays.copyOf(board, board.length);
        return minimax(boardCopy, xavierSymbol).index;
    }

    private static class Move {
        int index;
        int score;
    }

    private Move minimax(String[] currentBoard, String player) {
        List<Integer> availableSpots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (currentBoard[i].isEmpty()) {
                availableSpots.add(i);
            }
        }

        String winner = checkWinner(currentBoard);
        if (winner != null) {
            Move move = new Move();
            if (winner.equals(playerSymbol)) {
                move.score = -10;
            } else if (winner.equals(xavierSymbol)) {
                move.score = 10;
            }
            return move;
        } else if (availableSpots.isEmpty()) {
            Move move = new Move();
            move.score = 0;
            return move;
        }

        List<Move> moves = new ArrayList<>();
        for (Integer spot : availableSpots) {
            Move move = new Move();
            move.index = spot;
            currentBoard[spot] = player;

            if (player.equals(xavierSymbol)) {
                move.score = minimax(currentBoard, playerSymbol).score;
            } else {
                move.score = minimax(currentBoard, xavierSymbol).score;
            }
            currentBoard[spot] = "";
            moves.add(move);
        }

        int bestMoveIndex = -1;
        if (player.equals(xavierSymbol)) {
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < moves.size(); i++) {
                if (moves.get(i).score > bestScore) {
                    bestScore = moves.get(i).score;
                    bestMoveIndex = i;
                }
            }
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < moves.size(); i++) {
                if (moves.get(i).score < bestScore) {
                    bestScore = moves.get(i).score;
                    bestMoveIndex = i;
                }
            }
        }
        return moves.get(bestMoveIndex);
    }

    private String checkWinner() {
        return checkWinner(board);
    }

    private static String checkWinner(String[] b) {
        int[][] winPositions = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
                {0, 4, 8}, {2, 4, 6}
        };
        for (int[] pos : winPositions) {
            if (!b[pos[0]].isEmpty() &&
                    b[pos[0]].equals(b[pos[1]]) &&
                    b[pos[1]].equals(b[pos[2]])) {
                return b[pos[0]];
            }
        }
        return null;
    }

    private boolean isBoardFull() {
        for (String cell : board) {
            if (cell.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void pauseGameAndShowDialog() {
        isPaused = true;
        setUIEnabled(false);
        overlayContainer.setVisibility(View.VISIBLE);
        pausedCard.setVisibility(View.VISIBLE);
        gameOverCard.setVisibility(View.GONE);
        Toast.makeText(getContext(), "Game Paused", Toast.LENGTH_SHORT).show();
    }

    private void resumeGame() {
        isPaused = false;
        overlayContainer.setVisibility(View.GONE);
        pausedCard.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.GONE);
        setUIEnabled(true);
        Toast.makeText(getContext(), "Game Resumed", Toast.LENGTH_SHORT).show();
    }

    private void showGameOver(String result) {
        isGameOver = true;
        setUIEnabled(false);
        overlayContainer.setVisibility(View.VISIBLE);
        pausedCard.setVisibility(View.GONE);
        gameOverCard.setVisibility(View.VISIBLE);
    }

    private void showGameInfoDialog() {
        CustomMessageDialogFragment infoDialog = CustomMessageDialogFragment.newInstance(
                "Welcome to Tic-Tac-Toe!",
                getTicTacToeInfoContent(),
                "Close",
                null
        );
        infoDialog.show(getParentFragmentManager(), "TicTacToeInfoDialog");
    }

    private void setUIEnabled(boolean enabled) {
        boardRecyclerView.setEnabled(enabled);
        pauseButton.setEnabled(enabled);
        onePlayerModeButton.setEnabled(enabled && isTwoPlayerMode);
        twoPlayerModeButton.setEnabled(enabled && !isTwoPlayerMode);
        difficultySpinner.setEnabled(enabled);
        symbolRadioGroup.setEnabled(enabled);
    }

    private String getTicTacToeInfoContent() {
        return "Welcome to Tic-Tac-Toe!\n\n" +
                "How to Play:\n" +
                "Take turns clicking on an empty cell to place your mark. The first player to get three of their marks in a row (horizontally, vertically, or diagonally) wins the game.\n\n" +
                "Game Modes:\n" +
                "- One Player: You play against Xavier. You can choose your symbol (X or O) and Xavier's difficulty.\n" +
                "- Two Players: Play against a friend on the same device.\n\n" +
                "Difficulties:\n" +
                "- Easy: Xavier makes random moves.\n" +
                "- Medium: Xavier will try to win if it can, and block your winning moves otherwise.\n" +
                "- Hard: Xavier is unbeatable. It uses the minimax algorithm to find the optimal move every time.\n\n" +
                "Good luck and have fun!";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("Tic-Tac-Toe");
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }
        if (isPaused && overlayContainer.getVisibility() != View.VISIBLE) {
            isPaused = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mainActivity != null) {
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }
        if (!isPaused && !isGameOver) {
            isPaused = true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private class TicTacToeAdapter extends RecyclerView.Adapter<TicTacToeAdapter.ViewHolder> {

        private String[] board;

        public TicTacToeAdapter(String[] board) {
            this.board = board;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tic_tac_toe_cell, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.cellText.setText(board[position]);

            if (board[position].equals("X")) {
                holder.cellText.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
            } else if (board[position].equals("O")) {
                holder.cellText.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
            } else {
                holder.cellText.setTextColor(ContextCompat.getColor(getContext(), R.color.silver));
            }

            holder.cardView.setOnClickListener(v -> {
                boolean isPlayersTurn = isTwoPlayerMode || currentPlayer.equals(playerSymbol);
                if (!isGameOver && !isPaused && board[position].isEmpty() && isPlayersTurn) {
                    board[position] = currentPlayer;
                    notifyItemChanged(position);
                    checkGameStatus();
                }
            });
        }

        @Override
        public int getItemCount() {
            return 9;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView cellText;
            MaterialCardView cardView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cellText = itemView.findViewById(R.id.cell_text);
                cardView = itemView.findViewById(R.id.cell_card_view);
            }
        }
    }
}