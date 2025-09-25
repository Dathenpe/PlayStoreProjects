package ui; // Use 'ui' package as seen in JournalEntriesFragment

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver; // Import for the new listener
import android.view.Window;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.f9ld3.heal.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioRecordingDialogFragment extends DialogFragment {

    public static final String TAG = "AudioRecordingDialog";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String FILE_EXTENSION = ".3gp";
    private static final int AMPLITUDE_UPDATE_DELAY = 100;
    private static final int MAX_BARS = 50; // Constant for max bars

    private MediaRecorder recorder = null;
    private String outputFilePath = null;
    private boolean isRecording = false;

    private Chronometer chronometer;
    private MaterialButton recordButton;
    private EditText titleInput;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;

    private LinearLayout waveformVisualizer;
    private Handler visualizerHandler = new Handler(Looper.getMainLooper());

    // Calculated dimensions for wave bars
    private int calculatedBarWidthPx = 0;
    private int calculatedMarginPx = 0;


    private OnAudioLogSavedListener listener;

    public interface OnAudioLogSavedListener {
        void onAudioLogSaved(String title, String timestamp, String filePath);
    }

    public void setOnAudioLogSavedListener(OnAudioLogSavedListener listener) {
        this.listener = listener;
    }

    public AudioRecordingDialogFragment() {
        // Required empty public constructor
    }

    // Utility method to resolve a theme attribute to a concrete color integer
    private int resolveThemeColor(@AttrRes int attrId, Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrId, typedValue, true);
        return typedValue.data;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Make the dialog background transparent for rounded corners
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        View view = inflater.inflate(R.layout.dialog_audio_recording, container, false);

        chronometer = view.findViewById(R.id.chronometer);
        recordButton = view.findViewById(R.id.recordButton);
        titleInput = view.findViewById(R.id.audioTitleInput);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        waveformVisualizer = view.findViewById(R.id.waveformVisualizer);

        recordButton.setOnClickListener(v -> toggleRecording());
        saveButton.setOnClickListener(v -> saveLog());
        cancelButton.setOnClickListener(v -> dismiss());

        // Initially disable save button
        saveButton.setEnabled(false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // FIX: Use OnGlobalLayoutListener for guaranteed width measurement
        waveformVisualizer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove the listener to prevent multiple calls
                waveformVisualizer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // Calculate and store bar dimensions once the layout is ready
                calculateBarDimensions();

                // Draw the silent waveform using the calculated dimensions
                drawSilentWaveform();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
            int dialogWidth = (int) (screenWidth * 0.95);
            getDialog().getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }


    private void toggleRecording() {
        if (!hasRecordPermission()) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        // Generate a unique filename and path
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File audioDir = new File(requireContext().getFilesDir(), "audio_logs");
        if (!audioDir.exists()) audioDir.mkdirs();
        outputFilePath = audioDir.getAbsolutePath() + File.separator + "AUD_" + timeStamp + FILE_EXTENSION;

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(outputFilePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
            recordButton.setIconResource(R.drawable.ic_stop);
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            saveButton.setEnabled(false);

            if (titleInput.getText().toString().trim().isEmpty()){
                titleInput.setText("Audio Log " + new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date()));
            }

            // Clear any silent bars before starting the visualizer
            waveformVisualizer.removeAllViews();
            startVisualizerUpdates(); // Start updating the sound bars

            Toast.makeText(getContext(), "Recording started...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Recording failed to start: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            // Clean up
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }
            isRecording = false;
            if (outputFilePath != null) {
                File file = new File(outputFilePath);
                if (file.exists()) file.delete();
            }
            clearVisualizer(); // Reset visualizer on failure
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            recorder.release();
            recorder = null;
        }
        isRecording = false;
        chronometer.stop();

        pauseVisualizerUpdates();

        recordButton.setIconResource(R.drawable.ic_mic);
        saveButton.setEnabled(true);
        Toast.makeText(getContext(), "Recording stopped. Ready to save.", Toast.LENGTH_SHORT).show();
    }


    // --- Waveform Visualizer Logic ---

    /**
     * FIX: Calculates the required width and margin for a bar to ensure MAX_BARS fills the container width.
     */
    private void calculateBarDimensions() {
        final int BAR_SPACING_DP = 1; // Spacing between bars
        int containerWidthPx = waveformVisualizer.getWidth();
        float density = getResources().getDisplayMetrics().density;

        if (containerWidthPx == 0) return; // Cannot calculate if width is 0

        // Calculate the width of one bar (Bar + Spacing) to fit MAX_BARS across the container
        int totalBarWidth = containerWidthPx / MAX_BARS;

        // We want the bar to be wide, and the spacing (margin) to be small
        calculatedBarWidthPx = Math.max(1, totalBarWidth - (int)(BAR_SPACING_DP * density));
        calculatedMarginPx = (totalBarWidth - calculatedBarWidthPx) / 2; // Distribute remaining space as margin
    }

    private Runnable visualizerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording && recorder != null) {
                int amplitude = recorder.getMaxAmplitude();
                updateWaveform(amplitude);

                visualizerHandler.postDelayed(this, AMPLITUDE_UPDATE_DELAY);
            }
        }
    };

    private void startVisualizerUpdates() {
        visualizerHandler.post(visualizerRunnable);
    }

    private void pauseVisualizerUpdates() {
        visualizerHandler.removeCallbacks(visualizerRunnable);
    }

    private void clearVisualizer() {
        visualizerHandler.removeCallbacks(visualizerRunnable);
        drawSilentWaveform();
    }


    /**
     * Dynamically creates and adds a bar View based on the current audio amplitude.
     */
    private void updateWaveform(int amplitude) {
        final float MAX_AMPLITUDE = 32767.0f;
        final int MAX_BAR_HEIGHT_DP = 40;
        float density = getResources().getDisplayMetrics().density;

        // Ensure dimensions have been calculated
        if (calculatedBarWidthPx == 0) {
            calculateBarDimensions();
            if (calculatedBarWidthPx == 0) return;
        }

        // 1. Calculate the height based on amplitude
        float normalizedAmplitude = (float) amplitude / MAX_AMPLITUDE;
        int maxBarHeightPx = (int) (MAX_BAR_HEIGHT_DP * density);
        int barHeightPx = (int) (normalizedAmplitude * maxBarHeightPx);
        if (barHeightPx < (int) (4 * density)) barHeightPx = (int) (4 * density); // Ensure minimum visible height

        // 2. Resolve color
        int activeColor = MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorPrimary,
                "colorPrimary"
        );

        // 3. Create and configure the bar View using the pre-calculated dimensions
        View bar = new View(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                calculatedBarWidthPx,
                barHeightPx // Dynamic height
        );
        params.leftMargin = calculatedMarginPx;
        params.rightMargin = calculatedMarginPx;
        params.gravity = android.view.Gravity.CENTER_VERTICAL;

        bar.setLayoutParams(params);
        bar.setBackgroundColor(activeColor);

        // Add the new bar to the END of the container
        waveformVisualizer.addView(bar);

        // Limit the number of bars by removing the oldest (at index 0)
        if (waveformVisualizer.getChildCount() > MAX_BARS) {
            waveformVisualizer.removeViewAt(0);
        }
    }

    /**
     * Clears bars and draws a flat waveform when not recording.
     */
    private void drawSilentWaveform() {
        waveformVisualizer.removeAllViews();

        final int NUM_SILENT_BARS = MAX_BARS;
        final int SILENT_BAR_HEIGHT_DP = 4;
        float density = getResources().getDisplayMetrics().density;

        // Ensure dimensions have been calculated
        if (calculatedBarWidthPx == 0) {
            calculateBarDimensions();
            if (calculatedBarWidthPx == 0) return;
        }

        int silentBarHeightPx = (int) (SILENT_BAR_HEIGHT_DP * density);

        // 2. Resolve color
        int primaryColor = MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorPrimary,
                "colorPrimary"
        );

        // Reduce opacity to about 30% for the "muted" effect
        int silentColor = Color.argb(
                (int)(255 * 0.3f),
                Color.red(primaryColor),
                Color.green(primaryColor),
                Color.blue(primaryColor));

        // 3. Draw bars
        for (int i = 0; i < NUM_SILENT_BARS; i++) {
            View bar = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    calculatedBarWidthPx,
                    silentBarHeightPx
            );
            params.leftMargin = calculatedMarginPx;
            params.rightMargin = calculatedMarginPx;
            params.gravity = android.view.Gravity.CENTER_VERTICAL;

            bar.setLayoutParams(params);
            bar.setBackgroundColor(silentColor); // Set muted color

            waveformVisualizer.addView(bar);
        }
    }


    // --- Existing Save/Permission Logic ---

    private void saveLog() {
        if (outputFilePath == null || !new File(outputFilePath).exists()) {
            Toast.makeText(getContext(), "No audio recorded to save.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        String title = titleInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a title for your log.", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(new File(outputFilePath).lastModified()));

        if (listener != null) {
            listener.onAudioLogSaved(title, timestamp, outputFilePath);
        }

        Toast.makeText(getContext(), "Audio log saved!", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private boolean hasRecordPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isRecording) {
            // Stop the recorder, which calls pauseVisualizerUpdates()
            stopRecording();

            // Since the dialog is being dismissed (onStop), discard the file
            if (outputFilePath != null) {
                File file = new File(outputFilePath);
                if (file.exists()) file.delete();
            }

            // Now clear the visualizer and reset state completely
            clearVisualizer();
            Toast.makeText(getContext(), "Recording discarded.", Toast.LENGTH_SHORT).show();
        } else if (recorder == null && outputFilePath != null && new File(outputFilePath).exists()) {
            // If recording was stopped/paused and the dialog is dismissed without saving
            File file = new File(outputFilePath);
            if (file.exists()) file.delete();
            clearVisualizer();
            Toast.makeText(getContext(), "Recording discarded.", Toast.LENGTH_SHORT).show();
        }
    }
}