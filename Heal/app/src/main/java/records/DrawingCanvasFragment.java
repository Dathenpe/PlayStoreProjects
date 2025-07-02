package records;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory; // Added import
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.heal.MainActivity;
import com.example.heal.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DrawingCanvasFragment extends Fragment {

    private static final String TAG = "DrawingCanvasFragment";

    private MainActivity mainActivity;
    private DrawingView drawingView;
    private ImageButton buttonPen;
    private ImageButton buttonEraser;
    private ImageButton buttonUndo;
    private ImageButton buttonRedo;
    private Button buttonClearCanvas;
    private Button buttonSaveDrawing;
    private LinearLayout colorPalette;
    private SeekBar brushSizeSeekBar;
    private TextView brushSizeTextView;

    private View selectedColorCircle; // To keep track of the currently selected color circle

    // Variables to hold the loaded artwork URI and name if editing
    private String loadedImageUri = null;
    private String loadedArtworkName = null;

    public interface OnDrawingSavedListener {
        void onDrawingSaved(String imageUri, String artworkName);
    }

    private OnDrawingSavedListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: DrawingCanvasFragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }

        // Correctly get the target fragment, which should be ArtCornerFragment
        Fragment targetFragment = getTargetFragment();
        if (targetFragment instanceof OnDrawingSavedListener) {
            listener = (OnDrawingSavedListener) targetFragment;
        } else {
            // Fallback to parent activity if target fragment is not set or not the listener
            if (context instanceof OnDrawingSavedListener) {
                listener = (OnDrawingSavedListener) context;
            } else {
                throw new RuntimeException(context.toString() + " or target fragment must implement OnDrawingSavedListener");
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drawing_canvas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("New Artwork");
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }

        drawingView = view.findViewById(R.id.drawingView);
        buttonPen = view.findViewById(R.id.buttonPen);
        buttonEraser = view.findViewById(R.id.buttonEraser);
        buttonUndo = view.findViewById(R.id.buttonUndo);
        buttonRedo = view.findViewById(R.id.buttonRedo);
        buttonClearCanvas = view.findViewById(R.id.buttonClearCanvas);
        buttonSaveDrawing = view.findViewById(R.id.buttonSaveDrawing);
        colorPalette = view.findViewById(R.id.colorPalette);
        brushSizeSeekBar = view.findViewById(R.id.brushSizeSeekBar);
        brushSizeTextView = view.findViewById(R.id.brushSizeTextView);

        // Check for arguments (if editing an existing artwork)
        Bundle args = getArguments();
        if (args != null) {
            loadedImageUri = args.getString("imageUriToLoad");
            loadedArtworkName = args.getString("artworkNameToLoad");
            if (loadedImageUri != null) {
                mainActivity.toolbar.setTitle("Editing Artwork");
                loadArtworkForEditing(loadedImageUri);
            }
        }


        // Set initial drawing mode to PEN and update button styles
        drawingView.setDrawingMode(DrawingView.DrawingMode.PEN);
        updateToolButtonStyles(buttonPen, buttonEraser); // Apply rounded background to selected tool

        // Set initial brush size text and seekbar progress
        brushSizeTextView.setText("Size: " + (int) drawingView.getBrushSize());
        brushSizeSeekBar.setProgress((int) drawingView.getBrushSize());

        buttonPen.setOnClickListener(v -> {
            drawingView.setDrawingMode(DrawingView.DrawingMode.PEN);
            updateToolButtonStyles(buttonPen, buttonEraser); // Update styles
            // Reset brush size to 20 when pen is selected
            final int defaultBrushSize = 20;
            drawingView.setBrushSize(defaultBrushSize);
            brushSizeSeekBar.setProgress(defaultBrushSize);
            brushSizeTextView.setText("Size: " + defaultBrushSize);
        });

        buttonEraser.setOnClickListener(v -> {
            drawingView.setDrawingMode(DrawingView.DrawingMode.ERASER);
            updateToolButtonStyles(buttonEraser, buttonPen); // Update styles
            // Reset brush size to 20 when eraser is selected
            final int defaultBrushSize = 20;
            drawingView.setBrushSize(defaultBrushSize);
            brushSizeSeekBar.setProgress(defaultBrushSize);
            brushSizeTextView.setText("Size: " + defaultBrushSize);
        });

        // Set up SeekBar listener for brush size
        brushSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Ensure a minimum brush size (e.g., 1) to prevent zero size
                int actualProgress = Math.max(1, progress);
                brushSizeTextView.setText("Size: " + actualProgress);
                drawingView.setBrushSize(actualProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed for this functionality
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed for this functionality
            }
        });

        // Set up Undo and Redo button listeners
        buttonUndo.setOnClickListener(v -> drawingView.undo());
        buttonRedo.setOnClickListener(v -> drawingView.redo());

        buttonClearCanvas.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Clear Canvas")
                    .setMessage("Are you sure you want to clear the entire canvas? This action cannot be undone.")
                    .setPositiveButton("Clear", (dialog, which) -> drawingView.clearCanvas())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        buttonSaveDrawing.setOnClickListener(v -> showSaveDialog());

        setupColorPalette();
    }

    /**
     * Loads an existing artwork bitmap into the DrawingView for editing.
     * @param imageUri The URI of the image to load.
     */
    private void loadArtworkForEditing(String imageUri) {
        try {
            File imgFile = new File(Uri.parse(imageUri).getPath());
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bitmap != null) {
                    // Post the loading to ensure the DrawingView has been laid out and has dimensions
                    drawingView.post(() -> {
                        drawingView.loadBitmap(bitmap);
                        Log.d(TAG, "Loaded bitmap for editing: " + imageUri);
                    });
                } else {
                    Log.e(TAG, "BitmapFactory returned null for: " + imageUri);
                    Toast.makeText(getContext(), "Failed to load image for editing.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Image file not found for editing: " + imageUri);
                Toast.makeText(getContext(), "Image file not found for editing.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image for editing: " + imageUri, e);
            Toast.makeText(getContext(), "Error loading image for editing: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Updates the background style of the tool buttons (pen/eraser) to show which one is selected.
     * The selected button will have a rounded background, while the unselected one will be transparent.
     * @param selectedButton The ImageButton that is currently selected.
     * @param unselectedButton The ImageButton that is currently unselected.
     */
    private void updateToolButtonStyles(ImageButton selectedButton, ImageButton unselectedButton) {
        // Create a rounded background drawable for the selected state
        GradientDrawable selectedDrawable = new GradientDrawable();
        selectedDrawable.setShape(GradientDrawable.OVAL);
        selectedDrawable.setColor(ContextCompat.getColor(getContext(), R.color.selected_tool_background)); // Use your desired selected color
        selectedButton.setBackground(selectedDrawable);
        selectedButton.setEnabled(false); // Disable selected button to show it's active

        // Set the unselected button's background to transparent
        GradientDrawable unselectedDrawable = new GradientDrawable();
        unselectedDrawable.setShape(GradientDrawable.OVAL);
        unselectedDrawable.setColor(Color.TRANSPARENT);
        unselectedButton.setBackground(unselectedDrawable);
        unselectedButton.setEnabled(true);
    }

    private void setupColorPalette() {
        int[] colors = {
                Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.GRAY,
                Color.parseColor("#FFA500"), // Orange
                Color.parseColor("#800080")  // Purple
        };

        for (final int color : colors) {
            View colorCircle = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.color_circle_size),
                    (int) getResources().getDimension(R.dimen.color_circle_size)
            );
            params.setMargins(8, 0, 8, 0);
            colorCircle.setLayoutParams(params);

            // Set the initial (unselected) background using GradientDrawable
            GradientDrawable unselectedDrawable = new GradientDrawable();
            unselectedDrawable.setShape(GradientDrawable.OVAL);
            unselectedDrawable.setColor(color);
            colorCircle.setBackground(unselectedDrawable); // Set background directly
            colorCircle.setTag(color); // Store the actual color in the tag for easy retrieval

            colorCircle.setOnClickListener(v -> {
                // Clear previous selection
                if (selectedColorCircle != null) {
                    int previousColor = (int) selectedColorCircle.getTag();
                    GradientDrawable prevUnselectedDrawable = new GradientDrawable();
                    prevUnselectedDrawable.setShape(GradientDrawable.OVAL);
                    prevUnselectedDrawable.setColor(previousColor);
                    selectedColorCircle.setBackground(prevUnselectedDrawable);
                }

                // Apply new selection: add a border
                GradientDrawable selectedDrawable = new GradientDrawable();
                selectedDrawable.setShape(GradientDrawable.OVAL);
                selectedDrawable.setColor(color);
                selectedDrawable.setStroke(6, ContextCompat.getColor(getContext(), R.color.text_color_secondary)); // 6px border using text_color_secondary
                v.setBackground(selectedDrawable);

                selectedColorCircle = v; // Update the reference to the newly selected circle

                drawingView.setCurrentColor(color);
            });
            colorPalette.addView(colorCircle);
        }

        // Set initial selection to black (assuming it's the first color added)
        if (colors.length > 0 && colorPalette.getChildCount() > 0) {
            View initialColorCircle = colorPalette.getChildAt(0); // Get the View for black color
            if (initialColorCircle != null) {
                initialColorCircle.performClick(); // Simulate a click to apply selection logic
            }
        }
    }

    /**
     * Displays a dialog to allow the user to enter a name for the artwork before saving.
     * It also checks if the canvas is empty before proceeding with the save dialog.
     */
    private void showSaveDialog() {
        if (getContext() == null) return;

        // If we are editing an existing artwork, we can bypass the "empty canvas" check
        // because the user might just be saving without making further changes.
        if (loadedImageUri == null && !drawingView.hasDrawnSomething()) {
            Toast.makeText(getContext(), "Canvas is empty. Nothing to save!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Bitmap bitmapToSave = drawingView.getBitmap();
        if (bitmapToSave == null) {
            Toast.makeText(getContext(), "Error: Bitmap is null. Nothing to save!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Save Artwork");

        final EditText input = new EditText(getContext());
        // Calculate padding in pixels from dp (16dp)
        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        input.setHint("Enter artwork name");
        // Set the loaded artwork name as default if editing, otherwise "My Artwork"
        input.setText(loadedArtworkName != null ? loadedArtworkName : "My Artwork");
        input.setSingleLine(true);
        input.setPadding(paddingPx, paddingPx, paddingPx, paddingPx); // Apply padding directly to EditText

        LinearLayout dialogLayout = new LinearLayout(getContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.addView(input);

        builder.setView(dialogLayout); // Set the custom view for the dialog

        builder.setPositiveButton("Save", (dialog, which) -> {
            String artworkName = input.getText().toString().trim();
            if (artworkName.isEmpty()) {
                artworkName = "My Artwork"; // Fallback to default if user clears it
            }
            performSave(bitmapToSave, artworkName);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Performs the actual saving of the bitmap to a file with the given artwork name.
     *
     * @param bitmap The bitmap to save.
     * @param artworkName The name provided by the user for the artwork.
     */
    private void performSave(Bitmap bitmap, String artworkName) {
        if (getContext() == null) return;

        // Bitmap is already checked in showSaveDialog, but good to have a safeguard
        if (bitmap == null) {
            Toast.makeText(getContext(), "Error: Bitmap is null during save.", Toast.LENGTH_SHORT).show();
            return;
        }

        File picturesDir = getContext().getExternalFilesDir("Artwork");
        if (picturesDir == null) {
            Toast.makeText(getContext(), "Could not access storage to save artwork.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "ExternalFilesDir 'Artwork' is null.");
            return;
        }
        // Ensure the directory exists
        if (!picturesDir.exists()) {
            if (!picturesDir.mkdirs()) {
                Toast.makeText(getContext(), "Could not create artwork directory.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to create directory: " + picturesDir.getAbsolutePath());
                return;
            }
        }

        String filename;
        Uri savedUri;

        if (loadedImageUri != null) {
            // If editing, overwrite the existing file
            File existingFile = new File(Uri.parse(loadedImageUri).getPath());
            filename = existingFile.getName();
            savedUri = Uri.fromFile(existingFile);
            Log.d(TAG, "Overwriting existing artwork: " + existingFile.getAbsolutePath());
        } else {
            // If new artwork, create a new file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            // Sanitize artworkName to be safe for filenames (replace non-alphanumeric/dot/hyphen with underscore)
            String sanitizedArtworkName = artworkName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
            filename = sanitizedArtworkName + "_" + sdf.format(new Date()) + ".png";
            File newFile = new File(picturesDir, filename);
            savedUri = Uri.fromFile(newFile);
            Log.d(TAG, "Saving new artwork to: " + newFile.getAbsolutePath());
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(Uri.parse(savedUri.toString()).getPath()));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();

            Log.d(TAG, "Artwork saved to URI: " + savedUri.toString() + " with name: " + artworkName);

            if (listener != null) {
                listener.onDrawingSaved(savedUri.toString(), artworkName);
            }
            Toast.makeText(getContext(), "Artwork saved successfully!", Toast.LENGTH_SHORT).show();
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to save artwork: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Failed to save artwork: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            // Set title based on whether we are editing or creating new
            if (loadedImageUri != null) {
                mainActivity.toolbar.setTitle("Editing Artwork");
            } else {
                mainActivity.toolbar.setTitle("New Artwork");
            }
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }
    }

}
