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
        // Set a very light grey color for the preview to indicate erasing area
        eraserPreviewPaint.setColor(Color.parseColor("#E0E0E0")); // Using #E0E0E0 for a light grey
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            // Only create new canvasBitmap if it's null or dimensions change significantly
            if (canvasBitmap == null || canvasBitmap.getWidth() != w || canvasBitmap.getHeight() != h) {
                canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                drawCanvas = new Canvas(canvasBitmap);
                // Initial fill with white for the bitmap if it's a new canvas (not loaded artwork)
                // This is only done once when the bitmap is created, not on every redraw.
                if (baseBitmap == null) {
                    drawCanvas.drawColor(Color.WHITE);
                }
            }
            // If a baseBitmap exists, ensure it's drawn onto the canvasBitmap when size changes
            // This will be handled by redrawCanvas, which is called after size changes if needed.
            redrawCanvas(); // Always redraw on size change to ensure content scales correctly
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvasBitmap != null) {
            canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        }
        // Draw the current path being traced based on the mode
        // This is for the real-time preview, before the path is "committed" to the canvasBitmap
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
                // When the finger is lifted, save the path with the current drawing paint settings
                // Create a new Paint object to capture the current settings (color, xfermode, brush size)
                Paint paintForPath = new Paint(drawPaint); // Capture current drawPaint state
                PathData newPath = new PathData(new Path(drawPath), paintForPath, currentMode);
                paths.add(newPath);

                drawPath.reset(); // Reset the current path for the next stroke
                redrawCanvas(); // Redraw the entire canvas to apply the new path (including erase effects)
                break;
            default:
                return false;
        }
        invalidate(); // Request a redraw of the view to show the current path preview
        return true;
    }

    public void setCurrentColor(int newColor) {
        paintColor = newColor;
        // Only update drawPaint color if in PEN mode, otherwise it's transparent for ERASER
        if (currentMode == DrawingMode.PEN) {
            drawPaint.setColor(paintColor);
            drawPaint.setXfermode(null); // Ensure no xfermode for pen
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
            drawPaint.setColor(Color.TRANSPARENT); // Color doesn't matter for CLEAR mode
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            drawPaint.setColor(paintColor); // Revert to the selected paint color
            drawPaint.setXfermode(null); // Clear xfermode for normal drawing
            setBrushSize(lastBrushSize); // Restore last pen brush size
        }
        // Force a redraw to ensure the visual state reflects the new mode (e.g., if eraser was selected,
        // any lingering preview from pen mode is gone, or vice versa).
        invalidate();
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
     * and all paths in the 'paths' list. This is the authoritative drawing method.
     */
    private void redrawCanvas() {
        // Clear the canvasBitmap to transparent
        // This ensures a clean slate before redrawing the base image and all paths.
        drawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // If a base bitmap exists (for editing), draw it first to establish the background
        if (baseBitmap != null) {
            // Scale baseBitmap to fit current view dimensions if necessary
            Bitmap scaledBaseBitmap = Bitmap.createScaledBitmap(baseBitmap, getWidth(), getHeight(), true);
            drawCanvas.drawBitmap(scaledBaseBitmap, 0, 0, null);
        } else {
            // Otherwise, fill with white for new drawings as the default background
            drawCanvas.drawColor(Color.WHITE);
        }

        // Now draw all the paths on top of the base or white background
        for (PathData pathData : paths) {
            // Use the paint stored with the path, which already has the correct Xfermode
            drawCanvas.drawPath(pathData.path, pathData.paint);
        }
        invalidate(); // Request a redraw of the view
    }

    public void clearCanvas() {
        paths.clear();
        undonePaths.clear();
        if (drawCanvas != null) {
            // Clear the canvasBitmap to transparent
            drawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
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
        // Create a new bitmap with a solid white background
        Bitmap finalBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas finalCanvas = new Canvas(finalBitmap);
        finalCanvas.drawColor(Color.WHITE);

        // Draw the current canvasBitmap (which includes the base image and all paths) onto the new white background
        if (canvasBitmap != null) {
            finalCanvas.drawBitmap(canvasBitmap, 0, 0, null);
        }

        return finalBitmap;
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
        DrawingMode mode; // Store the drawing mode for this path

        PathData(Path path, Paint paint, DrawingMode mode) {
            this.path = path;
            this.paint = paint;
            this.mode = mode;
        }
    }
}
