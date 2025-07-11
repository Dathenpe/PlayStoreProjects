package tetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom View for drawing the Tetris game board and pieces.
 */
public class TetrisView extends View {

    // Constants for game board dimensions
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int BLOCK_SIZE_DP = 25; // Size of each Tetris block in DP

    private Paint blockPaint; // Paint for drawing individual blocks
    private Paint borderPaint; // Paint for drawing the board border
    private int[][] gameBoard; // 2D array representing the game board
    private Tetromino currentPiece; // The currently falling Tetromino
    private int pieceX, pieceY; // Position of the current piece

    private int blockSizePx; // Block size in pixels, calculated based on density

    public TetrisView(Context context) {
        super(context);
        init(context);
    }

    public TetrisView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TetrisView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Initializes paints and calculates block size based on screen density.
     * @param context The context.
     */
    private void init(Context context) {
        blockPaint = new Paint();
        blockPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(Color.DKGRAY); // Dark gray border
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2); // 2 pixel stroke width

        // Calculate block size in pixels based on device density
        blockSizePx = (int) (BLOCK_SIZE_DP * context.getResources().getDisplayMetrics().density);
    }

    /**
     * Sets the current game board state to be drawn.
     * @param board The 2D array representing the game board.
     */
    public void setGameBoard(int[][] board) {
        this.gameBoard = board;
        invalidate(); // Request a redraw
    }

    /**
     * Sets the current falling Tetromino and its position.
     * @param piece The Tetromino object.
     * @param x The X-coordinate (column) of the piece's top-left corner.
     * @param y The Y-coordinate (row) of the piece's top-left corner.
     */
    public void setCurrentPiece(Tetromino piece, int x, int y) {
        this.currentPiece = piece;
        this.pieceX = x;
        this.pieceY = y;
        invalidate(); // Request a redraw
    }

    /**
     * Overrides onMeasure to set the view's dimensions based on board size.
     * @param widthMeasureSpec The width measure specification.
     * @param heightMeasureSpec The height measure specification.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Calculate the desired width and height based on board dimensions and block size
        int desiredWidth = BOARD_WIDTH * blockSizePx + getPaddingLeft() + getPaddingRight();
        int desiredHeight = BOARD_HEIGHT * blockSizePx + getPaddingTop() + getPaddingBottom();

        // Set the measured dimension
        setMeasuredDimension(desiredWidth, desiredHeight);
    }

    /**
     * Draws the Tetris game board and pieces.
     * @param canvas The canvas to draw on.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (gameBoard == null) {
            return; // Nothing to draw yet
        }

        // Draw the settled blocks on the board
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (gameBoard[y][x] != 0) { // If a block exists at this position
                    drawBlock(canvas, x, y, gameBoard[y][x]);
                }
            }
        }

        // Draw the current falling piece
        if (currentPiece != null) {
            int[][] shape = currentPiece.getShape();
            int color = currentPiece.getColor();
            for (int y = 0; y < shape.length; y++) {
                for (int x = 0; x < shape[y].length; x++) {
                    if (shape[y][x] != 0) { // If it's a part of the piece
                        drawBlock(canvas, pieceX + x, pieceY + y, color);
                    }
                }
            }
        }

        // Draw the board border
        canvas.drawRect(0, 0, getWidth(), getHeight(), borderPaint);
    }

    /**
     * Helper method to draw a single Tetris block.
     * @param canvas The canvas to draw on.
     * @param x The column of the block.
     * @param y The row of the block.
     * @param color The color of the block.
     */
    private void drawBlock(Canvas canvas, int x, int y, int color) {
        blockPaint.setColor(color);
        // Calculate the rectangle for the block
        Rect rect = new Rect(x * blockSizePx, y * blockSizePx,
                (x + 1) * blockSizePx, (y + 1) * blockSizePx);
        canvas.drawRect(rect, blockPaint);
        // Draw a subtle border for each block for better definition
        canvas.drawRect(rect, borderPaint);
    }

    /**
     * Defines the Tetromino (Tetris piece) properties.
     * This nested class is placed here for convenience, but could be a separate file.
     */
    public static class Tetromino {
        private int[][] shape; // The 2D array representing the piece's shape
        private int color;     // The color of the piece

        // Predefined Tetromino shapes and colors
        public static final Tetromino[] ALL_TETROMINOES = {
                // I-piece (Cyan)
                new Tetromino(new int[][]{{1, 1, 1, 1}}, Color.CYAN),
                // J-piece (Blue)
                new Tetromino(new int[][]{{0, 0, 1}, {1, 1, 1}}, Color.BLUE),
                // L-piece (Orange)
                new Tetromino(new int[][]{{1, 0, 0}, {1, 1, 1}}, Color.rgb(255, 165, 0)), // Orange
                // O-piece (Yellow)
                new Tetromino(new int[][]{{1, 1}, {1, 1}}, Color.YELLOW),
                // S-piece (Green)
                new Tetromino(new int[][]{{0, 1, 1}, {1, 1, 0}}, Color.GREEN),
                // T-piece (Magenta)
                new Tetromino(new int[][]{{0, 1, 0}, {1, 1, 1}}, Color.MAGENTA),
                // Z-piece (Red)
                new Tetromino(new int[][]{{1, 1, 0}, {0, 1, 1}}, Color.RED)
        };

        public Tetromino(int[][] shape, int color) {
            this.shape = shape;
            this.color = color;
        }

        public int[][] getShape() {
            return shape;
        }

        public int getColor() {
            return color;
        }

        /**
         * Rotates the Tetromino clockwise.
         * This method creates a new rotated shape array.
         * @return A new Tetromino object with the rotated shape.
         */
        public Tetromino rotate() {
            int rows = shape.length;
            int cols = shape[0].length;
            int[][] rotatedShape = new int[cols][rows]; // Swapped dimensions for rotation

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    rotatedShape[c][rows - 1 - r] = shape[r][c];
                }
            }
            return new Tetromino(rotatedShape, this.color);
        }
    }
}