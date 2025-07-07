package drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    // Drawing paths and paints
    private Path drawPath;
    private Paint drawPaint, canvasPaint;
    private Paint eraserPreviewPaint; // Paint for the temporary eraser visual feedback
    // Initial color
    private int paintColor = Color.BLACK;
    // Canvas and bitmap for drawing
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;

    // NEW: Stores the original loaded artwork as a base layer
    private Bitmap baseBitmap;

    // Lists to store paths for undo/redo
    private List<PathData> paths = new ArrayList<>();
    private List<PathData> undonePaths = new ArrayList<>();

    // Brush sizes
    private float brushSize;
    private float lastBrushSize;

    // Drawing modes
    public enum DrawingMode {
        PEN, ERASER
    }
    private DrawingMode currentMode = DrawingMode.PEN;

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        drawPath = new Path();
        drawPaint = new Paint();

        brushSize = 20f; // Default brush size
        lastBrushSize = brushSize;

        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);

        // Initialize the eraser preview paint
        eraserPreviewPaint = new Paint();
        eraserPreviewPaint.setAntiAlias(true);
        eraserPreviewPaint.setStrokeWidth(brushSize);
        eraserPreviewPaint.setStyle(Paint.Style.STROKE);
        eraserPreviewPaint.setStrokeJoin(Paint.Join.ROUND);
        eraserPreviewPaint.setStrokeCap(Paint.Cap.ROUND);
        // Set a very light grey color for the preview
        eraserPreviewPaint.setColor(Color.parseColor("#E0E0E0")); // Using #E0E0E0 for a light grey
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            // Only create new canvasBitmap if it's null or dimensions change significantly
            // This prevents recreating it unnecessarily during minor layout changes
            if (canvasBitmap == null || canvasBitmap.getWidth() != w || canvasBitmap.getHeight() != h) {
                canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                drawCanvas = new Canvas(canvasBitmap);
                // Initial fill with white for the bitmap if it's a new canvas (not loaded artwork)
                // The redrawCanvas method will handle drawing the baseBitmap if it exists.
                if (baseBitmap == null) {
                    drawCanvas.drawColor(Color.WHITE);
                }
            }
            // If a baseBitmap exists, ensure it's drawn onto the canvasBitmap when size changes
            if (baseBitmap != null) {
                redrawCanvas(); // Redraw to scale baseBitmap if view size changed
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvasBitmap != null) {
            canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        }
        // Draw the current path being traced based on the mode
        if (currentMode == DrawingMode.PEN) {
            canvas.drawPath(drawPath, drawPaint);
        } else if (currentMode == DrawingMode.ERASER) {
            // For eraser, draw the temporary path with the preview paint
            canvas.drawPath(drawPath, eraserPreviewPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                undonePaths.clear(); // Clear undone paths when a new stroke begins
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                // When the finger is lifted, save the path with the *actual* drawing paint settings
                // Create a new Paint object to capture the current settings (color, xfermode, brush size)
                Paint currentPaint = new Paint(drawPaint);
                PathData newPath = new PathData(new Path(drawPath), currentPaint);
                paths.add(newPath);
                // Draw the new path onto the canvasBitmap
                drawCanvas.drawPath(newPath.path, newPath.paint);
                drawPath.reset(); // Reset the current path for the next stroke
                break;
            default:
                return false;
        }
        invalidate(); // Request a redraw of the view
        return true;
    }

    public void setCurrentColor(int newColor) {
        paintColor = newColor;
        drawPaint.setColor(paintColor);
        // Ensure Xfermode is null when setting pen color or after switching from eraser
        if (currentMode == DrawingMode.PEN) {
            drawPaint.setXfermode(null);
        }
    }

    public void setBrushSize(float newSize) {
        brushSize = newSize;
        drawPaint.setStrokeWidth(brushSize);
        eraserPreviewPaint.setStrokeWidth(brushSize); // Update preview paint too
        if (currentMode == DrawingMode.PEN) {
            lastBrushSize = brushSize; // Save last pen brush size
        }
    }

    public float getBrushSize() {
        return brushSize;
    }

    public void setDrawingMode(DrawingMode mode) {
        currentMode = mode;
        if (currentMode == DrawingMode.ERASER) {
            // For actual erasing, use transparent color with PorterDuff.Mode.CLEAR
            drawPaint.setColor(Color.TRANSPARENT); // Color doesn't matter much for CLEAR mode
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            drawPaint.setColor(paintColor); // Revert to the selected paint color
            drawPaint.setXfermode(null); // Clear xfermode for normal drawing
            setBrushSize(lastBrushSize); // Restore last pen brush size
        }
    }

    public void undo() {
        if (!paths.isEmpty()) {
            undonePaths.add(paths.remove(paths.size() - 1));
            redrawCanvas(); // Redraw the canvas after undoing a path
        }
    }

    public void redo() {
        if (!undonePaths.isEmpty()) {
            paths.add(undonePaths.remove(undonePaths.size() - 1));
            redrawCanvas(); // Redraw the canvas after redoing a path
        }
    }

    /**
     * Redraws the entire canvas bitmap from scratch, including the base image (if any)
     * and all paths in the 'paths' list.
     */
    private void redrawCanvas() {
        // Clear the canvasBitmap to transparent
        drawCanvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);

        // If a base bitmap exists (for editing), draw it first
        if (baseBitmap != null) {
            // Scale baseBitmap to fit current view dimensions if necessary
            Bitmap scaledBaseBitmap = Bitmap.createScaledBitmap(baseBitmap, getWidth(), getHeight(), true);
            drawCanvas.drawBitmap(scaledBaseBitmap, 0, 0, null);
        } else {
            // Otherwise, fill with white for new drawings
            drawCanvas.drawColor(Color.WHITE);
        }

        // Now draw all the paths on top of the base or white background
        for (PathData pathData : paths) {
            drawCanvas.drawPath(pathData.path, pathData.paint);
        }
        invalidate(); // Request a redraw of the view
    }

    public void clearCanvas() {
        paths.clear();
        undonePaths.clear();
        if (drawCanvas != null) {
            // Clear the canvasBitmap to transparent
            drawCanvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
            // Redraw the base bitmap if it exists, or fill with white if new canvas
            if (baseBitmap != null) {
                Bitmap scaledBaseBitmap = Bitmap.createScaledBitmap(baseBitmap, getWidth(), getHeight(), true);
                drawCanvas.drawBitmap(scaledBaseBitmap, 0, 0, null);
            } else {
                drawCanvas.drawColor(Color.WHITE);
            }
        }
        invalidate(); // Request a redraw of the view
    }

    public Bitmap getBitmap() {
        // If canvasBitmap has not been created yet (e.g., view not sized), create a blank one.
        if (canvasBitmap == null) {
            int width = getWidth() > 0 ? getWidth() : 1;
            int height = getHeight() > 0 ? getHeight() : 1;
            Bitmap blankBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas blankCanvas = new Canvas(blankBitmap);
            blankCanvas.drawColor(Color.WHITE);
            return blankBitmap;
        }
        // The canvasBitmap holds the current state of the drawing (including any loaded images).
        // Return a copy so external modifications don't affect the view's internal bitmap.
        return canvasBitmap.copy(Bitmap.Config.ARGB_8888, false); // Use ARGB_8844 for better quality
    }

    /**
     * Loads an existing bitmap onto the drawing canvas. This bitmap becomes the base layer.
     * @param bitmap The bitmap to load.
     */
    public void loadBitmap(Bitmap bitmap) {
        if (bitmap == null) return;

        // Store a mutable copy of the loaded bitmap as the base layer
        this.baseBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Ensure canvasBitmap is initialized to current view dimensions
        if (canvasBitmap == null || canvasBitmap.getWidth() != getWidth() || canvasBitmap.getHeight() != getHeight()) {
            canvasBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            drawCanvas = new Canvas(canvasBitmap);
        }

        // Clear paths and undonePaths as the canvas state is now defined by the loaded bitmap
        paths.clear();
        undonePaths.clear();

        // Redraw the canvas to include the new base bitmap
        redrawCanvas();
    }

    /**
     * Checks if anything has been drawn on the canvas (excluding the base bitmap).
     * @return True if there are drawn paths, false otherwise.
     */
    public boolean hasDrawnSomething() {
        return !paths.isEmpty();
    }

    private static class PathData {
        Path path;
        Paint paint;

        PathData(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }
}
