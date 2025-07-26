package drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import ui.CustomInputDialogFragment;
import ui.CustomMessageDialogFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
    private HorizontalScrollView colorPaletteContainer;
    private SeekBar brushSizeSeekBar;
    private TextView brushSizeTextView;

    private View selectedColorCircle;

    private String loadedImageUri = null;
    private String loadedArtworkName = null;
    private Set<String> existingArtworkNames = new HashSet<>();

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

        Fragment targetFragment = getTargetFragment();
        if (targetFragment instanceof OnDrawingSavedListener) {
            listener = (OnDrawingSavedListener) targetFragment;
        } else if (context instanceof OnDrawingSavedListener) {
            listener = (OnDrawingSavedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " or target fragment must implement OnDrawingSavedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drawing_canvas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupInitialState();
        setupClickListeners();
        setupColorPalette();
    }

    private void initializeViews(View view) {
        drawingView = view.findViewById(R.id.drawingView);
        buttonPen = view.findViewById(R.id.buttonPen);
        buttonEraser = view.findViewById(R.id.buttonEraser);
        buttonUndo = view.findViewById(R.id.buttonUndo);
        buttonRedo = view.findViewById(R.id.buttonRedo);
        buttonClearCanvas = view.findViewById(R.id.buttonClearCanvas);
        buttonSaveDrawing = view.findViewById(R.id.buttonSaveDrawing);
        colorPalette = view.findViewById(R.id.colorPalette);
        colorPaletteContainer = view.findViewById(R.id.color_palette_container);
        brushSizeSeekBar = view.findViewById(R.id.brushSizeSeekBar);
        brushSizeTextView = view.findViewById(R.id.brushSizeTextView);
    }

    private void setupInitialState() {
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("New Artwork");
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }

        Bundle args = getArguments();
        if (args != null) {
            loadedImageUri = args.getString("imageUriToLoad");
            loadedArtworkName = args.getString("artworkNameToLoad");
            if (args.containsKey("existingArtworkNames")) {
                existingArtworkNames = (Set<String>) args.getSerializable("existingArtworkNames");
                if (existingArtworkNames == null) existingArtworkNames = new HashSet<>();
            }
            if (loadedImageUri != null) {
                if (mainActivity != null) mainActivity.toolbar.setTitle("Editing Artwork");
                loadArtworkForEditing(loadedImageUri);
            }
        }

        drawingView.setDrawingMode(DrawingView.DrawingMode.PEN);
        updateToolButtonStyles(buttonPen, buttonEraser);

        brushSizeTextView.setText("Size: " + (int) drawingView.getBrushSize());
        brushSizeSeekBar.setProgress((int) drawingView.getBrushSize());
    }

    private void setupClickListeners() {
        buttonPen.setOnClickListener(v -> {
            drawingView.setDrawingMode(DrawingView.DrawingMode.PEN);
            updateToolButtonStyles(buttonPen, buttonEraser);
            colorPaletteContainer.setVisibility(View.VISIBLE);
            final int defaultBrushSize = 20;
            drawingView.setBrushSize(defaultBrushSize);
            brushSizeSeekBar.setProgress(defaultBrushSize);
            brushSizeTextView.setText("Size: " + defaultBrushSize);
        });

        buttonEraser.setOnClickListener(v -> {
            drawingView.setDrawingMode(DrawingView.DrawingMode.ERASER);
            updateToolButtonStyles(buttonEraser, buttonPen);
            colorPaletteContainer.setVisibility(View.GONE);
            final int defaultBrushSize = 20;
            drawingView.setBrushSize(defaultBrushSize);
            brushSizeSeekBar.setProgress(defaultBrushSize);
            brushSizeTextView.setText("Size: " + defaultBrushSize);
        });

        brushSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actualProgress = Math.max(1, progress);
                brushSizeTextView.setText("Size: " + actualProgress);
                drawingView.setBrushSize(actualProgress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        buttonUndo.setOnClickListener(v -> drawingView.undo());
        buttonRedo.setOnClickListener(v -> drawingView.redo());

        buttonClearCanvas.setOnClickListener(v -> showClearCanvasConfirmation());
        buttonSaveDrawing.setOnClickListener(v -> showSaveDialog());
    }

    private void showClearCanvasConfirmation() {
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Clear Canvas",
                "Are you sure you want to clear the entire canvas? This action cannot be undone.",
                "Clear",
                "Cancel"
        );
        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                drawingView.clearCanvas();
            }
            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                dialogFragment.dismiss();
            }
        });
        dialog.show(getParentFragmentManager(), "ClearCanvasConfirmationDialog");
    }

    private void loadArtworkForEditing(String imageUri) {
        try {
            File imgFile = new File(Uri.parse(imageUri).getPath());
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bitmap != null) {
                    drawingView.post(() -> drawingView.loadBitmap(bitmap));
                } else {
                    Toast.makeText(getContext(), "Failed to load image for editing.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Image file not found.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error loading image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateToolButtonStyles(ImageButton selectedButton, ImageButton unselectedButton) {
        if (getContext() == null) return;
        GradientDrawable selectedDrawable = new GradientDrawable();
        selectedDrawable.setShape(GradientDrawable.OVAL);
        selectedDrawable.setColor(ContextCompat.getColor(getContext(), R.color.selected_tool_background));
        selectedButton.setBackground(selectedDrawable);
        selectedButton.setEnabled(false);

        GradientDrawable unselectedDrawable = new GradientDrawable();
        unselectedDrawable.setShape(GradientDrawable.OVAL);
        unselectedDrawable.setColor(Color.TRANSPARENT);
        unselectedButton.setBackground(unselectedDrawable);
        unselectedButton.setEnabled(true);
    }

    private void setupColorPalette() {
        if (getContext() == null) return;
        int[] colors = {
                Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.GRAY,
                Color.parseColor("#FFA500"), Color.parseColor("#800080")
        };

        for (final int color : colors) {
            View colorCircle = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.color_circle_size),
                    (int) getResources().getDimension(R.dimen.color_circle_size)
            );
            params.setMargins(8, 0, 8, 0);
            colorCircle.setLayoutParams(params);

            GradientDrawable unselectedDrawable = new GradientDrawable();
            unselectedDrawable.setShape(GradientDrawable.OVAL);
            unselectedDrawable.setColor(color);
            colorCircle.setBackground(unselectedDrawable);
            colorCircle.setTag(color);

            colorCircle.setOnClickListener(v -> {
                if (selectedColorCircle != null) {
                    int previousColor = (int) selectedColorCircle.getTag();
                    GradientDrawable prevUnselectedDrawable = new GradientDrawable();
                    prevUnselectedDrawable.setShape(GradientDrawable.OVAL);
                    prevUnselectedDrawable.setColor(previousColor);
                    selectedColorCircle.setBackground(prevUnselectedDrawable);
                }

                GradientDrawable selectedDrawable = new GradientDrawable();
                selectedDrawable.setShape(GradientDrawable.OVAL);
                selectedDrawable.setColor(color);
                selectedDrawable.setStroke(6, ContextCompat.getColor(getContext(), R.color.text_color_secondary));
                v.setBackground(selectedDrawable);

                selectedColorCircle = v;
                drawingView.setCurrentColor(color);
            });
            colorPalette.addView(colorCircle);
        }

        if (colorPalette.getChildCount() > 0) {
            colorPalette.getChildAt(0).performClick();
        }
    }

    private void showSaveDialog() {
        if (getContext() == null) return;

        if (loadedImageUri == null && !drawingView.hasDrawnSomething()) {
            Toast.makeText(getContext(), "Canvas is empty. Nothing to save!", Toast.LENGTH_SHORT).show();
            return;
        }

        String defaultArtworkName = (loadedArtworkName != null)
                ? loadedArtworkName
                : getUniqueArtworkName("My Artwork", existingArtworkNames);

        CustomInputDialogFragment dialog = CustomInputDialogFragment.newInstance(
                "Save Artwork",
                null, // No message needed
                "Enter artwork name",
                "Save",
                "Cancel"
        );

        dialog.setListener(new CustomInputDialogFragment.OnInputDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment, String inputText) {
                String artworkName = inputText.isEmpty() ? "My Artwork" : inputText;
                if (loadedImageUri == null) {
                    artworkName = getUniqueArtworkName(artworkName, existingArtworkNames);
                }
                performSave(drawingView.getBitmap(), artworkName);
            }
            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                dialogFragment.dismiss();
            }
        });
        dialog.show(getParentFragmentManager(), "SaveArtworkDialog");
    }

    private String getUniqueArtworkName(String baseName, Set<String> existingNames) {
        String uniqueName = baseName;
        int counter = 1;
        Set<String> namesToCheck = new HashSet<>(existingNames);
        if (loadedArtworkName != null) {
            namesToCheck.remove(loadedArtworkName);
        }
        while (namesToCheck.contains(uniqueName)) {
            counter++;
            uniqueName = baseName + " (" + counter + ")";
        }
        return uniqueName;
    }

    private void performSave(Bitmap bitmap, String artworkName) {
        if (getContext() == null || bitmap == null) {
            Toast.makeText(getContext(), "Error saving artwork.", Toast.LENGTH_SHORT).show();
            return;
        }

        File picturesDir = getContext().getExternalFilesDir("Artwork");
        if (picturesDir == null || (!picturesDir.exists() && !picturesDir.mkdirs())) {
            Toast.makeText(getContext(), "Could not access storage.", Toast.LENGTH_SHORT).show();
            return;
        }

        File fileToSave;
        if (loadedImageUri != null) {
            fileToSave = new File(Uri.parse(loadedImageUri).getPath());
        } else {
            String sanitizedName = artworkName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
            String fileName = sanitizedName + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png";
            fileToSave = new File(picturesDir, fileName);
        }

        try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            if (listener != null) {
                listener.onDrawingSaved(Uri.fromFile(fileToSave).toString(), artworkName);
            }
            Toast.makeText(getContext(), "Artwork saved!", Toast.LENGTH_SHORT).show();
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Failed to save artwork.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle(loadedImageUri != null ? "Editing Artwork" : "New Artwork");
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }
    }
}
