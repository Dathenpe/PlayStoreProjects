package records;

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
            if (canvasBitmap == null) {
                canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                drawCanvas = new Canvas(canvasBitmap);
                drawCanvas.drawColor(Color.WHITE); // Initial fill with white for the bitmap
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
                undonePaths.clear();
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                // When the finger is lifted, save the path with the *actual* drawing paint settings
                PathData newPath = new PathData(new Path(drawPath), new Paint(drawPaint));
                paths.add(newPath);
                drawCanvas.drawPath(newPath.path, newPath.paint);
                drawPath.reset();
                break;
            default:
                return false;
        }
        invalidate();
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
            lastBrushSize = brushSize;
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
            redrawCanvas();
        }
    }

    public void redo() {
        if (!undonePaths.isEmpty()) {
            paths.add(undonePaths.remove(undonePaths.size() - 1));
            redrawCanvas();
        }
    }

    private void redrawCanvas() {
        // Clear the canvasBitmap to transparent
        drawCanvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
        // Do NOT fill it with white immediately. The transparent parts will show
        // the DrawingView's white background.

        for (PathData pathData : paths) {
            drawCanvas.drawPath(pathData.path, pathData.paint);
        }
        invalidate();
    }

    public void clearCanvas() {
        paths.clear();
        undonePaths.clear();
        if (drawCanvas != null) {
            // Clear the canvasBitmap to transparent
            drawCanvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
            // Do NOT fill it with white immediately.
        }
        invalidate();
    }

    public Bitmap getBitmap() {
        // It's crucial to return a copy, otherwise external modifications might affect the view.
        // If paths is empty, return a blank white bitmap of current view size.
        if (canvasBitmap == null || paths.isEmpty()) {
            Bitmap blankBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas blankCanvas = new Canvas(blankBitmap);
            blankCanvas.drawColor(Color.WHITE); // Fill blank bitmap with white background
            return blankBitmap;
        }
        return canvasBitmap.copy(Bitmap.Config.ARGB_8888, false);
    }

    public void loadBitmap(Bitmap bitmap) {
        if (bitmap == null) return;

        // Ensure the loaded bitmap matches the current view dimensions, or scale it.
        // For simplicity, let's just use the loaded bitmap if dimensions are compatible,
        // or re-create canvasBitmap if needed.
        if (canvasBitmap == null || canvasBitmap.getWidth() != getWidth() || canvasBitmap.getHeight() != getHeight()) {
            canvasBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            drawCanvas = new Canvas(canvasBitmap);
        }

        // Clear existing content
        drawCanvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
        drawCanvas.drawColor(Color.WHITE); // Ensure the background is white before drawing new content

        // Draw the loaded bitmap onto the internal canvas
        drawCanvas.drawBitmap(bitmap, 0, 0, null);

        // Clear paths and undonePaths as the canvas state is now defined by the loaded bitmap
        paths.clear();
        undonePaths.clear();
        invalidate();
    }

    /**
     * Checks if anything has been drawn on the canvas.
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
