package ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

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
    private static final int MAX_BARS = 50;

    // --- State Management ---
    private enum RecordingState {
        IDLE,
        RECORDING,
        PAUSED
    }
    private RecordingState currentState = RecordingState.IDLE;
    private MediaRecorder recorder = null;
    private String outputFilePath = null;
    private boolean wasRecordingInterrupted = false;
    private long timeWhenStopped = 0; // For chronometer pause/resume

    // --- UI Elements ---
    private Chronometer chronometer;
    private MaterialButton recordButton;
    private MaterialButton stopButton; // New stop button
    private EditText titleInput;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;
    private LinearLayout waveformVisualizer;
    private Handler visualizerHandler = new Handler(Looper.getMainLooper());
    private int calculatedBarWidthPx = 0;
    private int calculatedMarginPx = 0;

    private OnAudioLogSavedListener listener;

    public interface OnAudioLogSavedListener {
        void onAudioLogSaved(String title, String timestamp, String filePath, long durationMillis);
    }

    public void setOnAudioLogSavedListener(OnAudioLogSavedListener listener) {
        this.listener = listener;
    }

    public AudioRecordingDialogFragment() {
        // Required empty public constructor
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        if (getDialog() != null) {
            getDialog().setCancelable(false);
            getDialog().setCanceledOnTouchOutside(false);
        }

        View view = inflater.inflate(R.layout.dialog_audio_recording, container, false);

        chronometer = view.findViewById(R.id.chronometer);
        recordButton = view.findViewById(R.id.recordButton);
        stopButton = view.findViewById(R.id.stopButton); // Initialize new button
        titleInput = view.findViewById(R.id.audioTitleInput);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        waveformVisualizer = view.findViewById(R.id.waveformVisualizer);

        recordButton.setOnClickListener(v -> handleRecordPauseResume());
        stopButton.setOnClickListener(v -> stopRecordingAndFinalize()); // Set listener for stop
        saveButton.setOnClickListener(v -> saveLog());
        cancelButton.setOnClickListener(v -> handleCancel());

        updateUiForState(RecordingState.IDLE);
        wasRecordingInterrupted = false;
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        waveformVisualizer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                waveformVisualizer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                calculateBarDimensions();
                drawSilentWaveform();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (getDialog() != null && getDialog().getWindow() != null) {
            int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
            int dialogWidth = (int) (screenWidth * 0.95);
            getDialog().getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Central handler for the main record button.
     * Decides whether to start, pause, or resume recording based on the current state.
     */
    private void handleRecordPauseResume() {
        if (!hasRecordPermission()) {
            Log.d(TAG, "handleRecordPauseResume: Requesting audio permission.");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        switch (currentState) {
            case IDLE:
                startRecording();
                break;
            case RECORDING:
                pauseRecording();
                break;
            case PAUSED:
                resumeRecording();
                break;
        }
    }


    private void handleCancel() {
        Log.d(TAG, "Cancel button clicked. currentState: " + currentState);
        if (currentState != RecordingState.IDLE || (outputFilePath != null && new File(outputFilePath).exists())) {
            // If recording, paused, or if there's a recorded file, show confirmation
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Discard Recording?",
                    "Are you sure you want to discard this recording?",
                    "Discard",
                    "Keep"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    Log.d(TAG, "Discard confirmation: Positive click");
                    dialogFragment.dismiss(); // Dismiss the confirmation dialog
                    cleanupAndDismiss();     // Then dismiss the recording dialog
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    Log.d(TAG, "Discard confirmation: Negative click");
                    dialogFragment.dismiss(); // Just dismiss the confirmation dialog
                }
            });
            FragmentManager fm = getChildFragmentManager();
            dialog.show(fm, "ConfirmCancelRecordingDialog");
        } else {
            // If idle and no file, just dismiss
            Log.d(TAG, "No recording or file to discard. Dismissing directly.");
            dismiss();
        }
    }
    private void cleanupAndDismiss() {
        Log.d(TAG, "cleanupAndDismiss called. currentState: " + currentState);
        if (currentState != RecordingState.IDLE) {
            wasRecordingInterrupted = true; // Mark as interrupted so it doesn't try to save
            stopRecordingAndFinalize(); // This will also handle recorder cleanup
        }

        // Delete the recorded file if it exists and was not saved
        if (outputFilePath != null) {
            File fileToDelete = new File(outputFilePath);
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.d(TAG, "cleanupAndDismiss: Deleted recording file: " + outputFilePath);
                } else {
                    Log.w(TAG, "cleanupAndDismiss: Failed to delete recording file: " + outputFilePath);
                }
            }
            outputFilePath = null; // Clear the path
        }
        dismiss();
    }


    private void startRecording() {
        Log.d(TAG, "startRecording: Initializing.");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File audioDir = new File(requireContext().getFilesDir(), "audio_logs");
        if (!audioDir.exists()) {
            if (!audioDir.mkdirs()) {
                Log.e(TAG, "startRecording: Failed to create audio_logs directory.");
                Toast.makeText(getContext(), "Error: Could not create storage directory.", Toast.LENGTH_LONG).show();
                return;
            }
            Log.d(TAG, "startRecording: Created audio_logs directory.");
        }
        outputFilePath = audioDir.getAbsolutePath() + File.separator + "AUD_" + timeStamp + FILE_EXTENSION;
        Log.d(TAG, "startRecording: outputFilePath = " + outputFilePath);

        wasRecordingInterrupted = false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            recorder = new MediaRecorder(requireContext());
        } else {
            recorder = new MediaRecorder();
        }
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(outputFilePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            Log.d(TAG, "startRecording: Calling recorder.prepare()");
            recorder.prepare();
            Log.d(TAG, "startRecording: recorder.prepare() successful. Calling recorder.start()");
            recorder.start();
            Log.d(TAG, "startRecording: recorder.start() successful.");

            updateUiForState(RecordingState.RECORDING);
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();

            if (titleInput.getText().toString().trim().isEmpty()) {
                titleInput.setText("Audio Log " + new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date()));
            }

            waveformVisualizer.removeAllViews();
            startVisualizerUpdates();
            Toast.makeText(getContext(), "Recording started...", Toast.LENGTH_SHORT).show();
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "startRecording: Failed to start recording.", e);
            Toast.makeText(getContext(), "Recording failed to start: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (recorder != null) {
                try {
                    recorder.reset();
                    recorder.release();
                } catch (Exception releaseEx) {
                    Log.e(TAG, "startRecording: Exception during cleanup release.", releaseEx);
                }
                recorder = null;
            }
            if (outputFilePath != null) {
                File file = new File(outputFilePath);
                if (file.exists() && !file.delete()) {
                    Log.w(TAG, "startRecording: Failed to delete partially created file: " + outputFilePath);
                } else if(file.exists()){
                    Log.d(TAG, "startRecording: Cleaned up partially created file: " + outputFilePath);
                }
            }
            outputFilePath = null;
            updateUiForState(RecordingState.IDLE);
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            clearVisualizer();
        }
    }

    private void pauseRecording() {
        // Pause is only available on Android N (API 24) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (currentState != RecordingState.RECORDING || recorder == null) return;
            try {
                Log.d(TAG, "Pausing recording.");
                recorder.pause();
                timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
                chronometer.stop();
                pauseVisualizerUpdates();
                updateUiForState(RecordingState.PAUSED);
                Toast.makeText(getContext(), "Recording paused.", Toast.LENGTH_SHORT).show();
            } catch (IllegalStateException e) {
                Log.e(TAG, "pauseRecording: Failed to pause.", e);
            }
        } else {
            Toast.makeText(getContext(), "Pause is not available on this Android version.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (currentState != RecordingState.PAUSED || recorder == null) return;
            try {
                Log.d(TAG, "Resuming recording.");
                recorder.resume();
                chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
                chronometer.start();
                startVisualizerUpdates();
                updateUiForState(RecordingState.RECORDING);
            } catch (IllegalStateException e) {
                Log.e(TAG, "resumeRecording: Failed to resume.", e);
            }
        }
    }


    /**
     * Stops the recording process and finalizes the file, preparing it for saving.
     */
    private void stopRecordingAndFinalize() {
        Log.d(TAG, "stopRecordingAndFinalize: Attempting to stop. Current outputFilePath: " + outputFilePath + ", currentState: " + currentState);
        if (currentState == RecordingState.IDLE && recorder == null) {
            Log.w(TAG, "stopRecordingAndFinalize: Called when not recording and recorder is null.");
            return;
        }

        if (recorder != null) {
            try {
                Log.d(TAG, "stopRecordingAndFinalize: Calling recorder.stop()");
                recorder.stop();
                Log.d(TAG, "stopRecordingAndFinalize: recorder.stop() completed.");
            } catch (RuntimeException e) {
                Log.e(TAG, "stopRecordingAndFinalize: Exception during recorder.stop(). File might not be saved correctly.", e);
                wasRecordingInterrupted = true; // Mark as faulty
            }
            try {
                Log.d(TAG, "stopRecordingAndFinalize: Calling recorder.reset() and recorder.release()");
                recorder.reset();
                recorder.release();
                Log.d(TAG, "stopRecordingAndFinalize: recorder.release() completed.");
            } catch (Exception e) {
                Log.e(TAG, "stopRecordingAndFinalize: Exception during recorder cleanup", e);
            }
            recorder = null;
        } else {
            Log.w(TAG, "stopRecordingAndFinalize: recorder was null, but state was not IDLE? State inconsistency.");
        }

        chronometer.stop();
        timeWhenStopped = 0;
        pauseVisualizerUpdates();
        updateUiForState(RecordingState.IDLE); // Visually reset controls

        // Special handling for UI after stopping
        recordButton.setEnabled(false); // Can't record again
        stopButton.setVisibility(View.GONE);

        if (!wasRecordingInterrupted && outputFilePath != null && new File(outputFilePath).exists() && new File(outputFilePath).length() > 0) {
            Log.d(TAG, "stopRecordingAndFinalize: Enabling save button. File exists: " + outputFilePath);
            saveButton.setEnabled(true);
            if (getContext() != null) Toast.makeText(getContext(), "Recording stopped. Ready to save.", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "stopRecordingAndFinalize: Save button remains disabled. wasRecordingInterrupted: " + wasRecordingInterrupted + ", outputFilePath: " + outputFilePath);
            saveButton.setEnabled(false);
            if (wasRecordingInterrupted && getContext() != null) Toast.makeText(getContext(), "Recording issue, cannot save.", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "stopRecordingAndFinalize: Method finished. currentState: " + currentState + ", wasRecordingInterrupted: " + wasRecordingInterrupted);
    }


    private void saveLog() {
        Log.d(TAG, "saveLog: Attempting to save. outputFilePath: " + outputFilePath);
        Log.d(TAG, "saveLog: wasRecordingInterrupted: " + wasRecordingInterrupted);

        if (currentState != RecordingState.IDLE) {
            Log.w(TAG, "saveLog: Attempted to save while recording/paused. Stopping first.");
            stopRecordingAndFinalize();
            if (wasRecordingInterrupted || outputFilePath == null) {
                Log.e(TAG, "saveLog: Recording was stopped due to save attempt but had issues or no path.");
                Toast.makeText(getContext(), "Error: Could not finalize recording for saving.", Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }
        }

        if (outputFilePath == null) {
            Log.e(TAG, "saveLog: outputFilePath is null.");
            Toast.makeText(getContext(), "Error: No file path to save.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        File audioFile = new File(outputFilePath);
        if (!audioFile.exists() || audioFile.length() == 0) {
            Log.e(TAG, "saveLog: File does NOT exist or is empty at path: " + outputFilePath + " (Size: " + audioFile.length() + ")");
            Toast.makeText(getContext(), "Error: Recorded file not found or is empty.", Toast.LENGTH_SHORT).show();
            outputFilePath = null;
            dismiss();
            return;
        }
        Log.d(TAG, "saveLog: File exists: " + outputFilePath + ", Size: " + audioFile.length() + " bytes");

        // --- CALCULATE DURATION ---
        long durationMs = 0;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(outputFilePath);
            String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                durationMs = Long.parseLong(durationStr);
            }
            Log.d(TAG, "saveLog: Calculated duration via MMR: " + durationMs + " ms for file: " + outputFilePath);
        } catch (Exception e) {
            Log.e(TAG, "saveLog: Error getting duration with MediaMetadataRetriever, using fallback", e);
            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(outputFilePath);
                mp.prepare();
                durationMs = mp.getDuration();
                Log.d(TAG, "saveLog: Calculated duration via MediaPlayer fallback: " + durationMs + " ms");
            } catch (IOException | IllegalStateException | SecurityException e2) {
                Log.e(TAG, "saveLog: Error getting duration with MediaPlayer fallback", e2);
            } finally {
                mp.release();
            }
        } finally {
            try {
                mmr.release();
            } catch (IOException e) {
                Log.e(TAG, "saveLog: Error releasing MediaMetadataRetriever", e);
            }
        }
        // --- END CALCULATE DURATION ---

        String title = titleInput.getText().toString().trim();
        if (title.isEmpty()) {
            title = "Audio Log " + new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date(audioFile.lastModified()));
            Log.w(TAG, "saveLog: Title was empty, defaulted to: " + title);
        }

        String timestamp = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(audioFile.lastModified()));

        if (listener != null) {
            Log.d(TAG, "saveLog: Calling listener.onAudioLogSaved with title: '" + title + "', timestamp: " + timestamp + ", path: '" + outputFilePath + "', duration: " + durationMs);
            listener.onAudioLogSaved(title, timestamp, outputFilePath, durationMs); // Pass duration
        }

        Toast.makeText(getContext(), "Audio log saved!", Toast.LENGTH_SHORT).show();
        outputFilePath = null;
        wasRecordingInterrupted = false;
        dismiss();
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: DialogFragment stopping. currentState: " + currentState + ", wasRecordingInterrupted: " + wasRecordingInterrupted + ", outputFilePath: " + outputFilePath);
        if (currentState != RecordingState.IDLE) {
            Log.w(TAG, "onStop: Dialog stopped while recording/paused. This implies an interruption.");
            wasRecordingInterrupted = true;
            stopRecordingAndFinalize();
        }
        if (wasRecordingInterrupted && outputFilePath != null) {
            Log.d(TAG, "onStop: Recording was interrupted. Deleting file: " + outputFilePath);
            File fileToDelete = new File(outputFilePath);
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.d(TAG, "onStop: Successfully deleted interrupted recording file: " + outputFilePath);
                    if (getContext() != null) Toast.makeText(getContext(), "Recording discarded.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.w(TAG, "onStop: FAILED to delete interrupted recording file: " + outputFilePath);
                }
            } else {
                Log.d(TAG, "onStop: Interrupted recording file was already missing: " + outputFilePath);
            }
            outputFilePath = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (visualizerHandler != null) {
            visualizerHandler.removeCallbacksAndMessages(null); // Clear all callbacks
        }
        if (recorder != null) {
            Log.w(TAG, "onDestroy: MediaRecorder instance was not null. Releasing now.");
            try {
                if (currentState != RecordingState.IDLE) recorder.stop();
                recorder.release();
            } catch (Exception e) {
                Log.e(TAG, "onDestroy: Error releasing MediaRecorder in onDestroy", e);
            }
            recorder = null;
        }
    }

    private boolean hasRecordPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Permission GRANTED. Starting recording.");
                startRecording();
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Permission DENIED.");
                Toast.makeText(getContext(), "Permission denied. Cannot record audio.", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        }
    }

    /**
     * Updates the UI elements based on the current recording state.
     * @param state The new state to reflect in the UI.
     */
    private void updateUiForState(RecordingState state) {
        currentState = state;
        switch (state) {
            case IDLE:
                recordButton.setIconResource(R.drawable.ic_mic);
                recordButton.setEnabled(true);
                stopButton.setVisibility(View.GONE);
                saveButton.setEnabled(false);
                break;
            case RECORDING:
                // Use a pause icon to indicate the next action
                recordButton.setIconResource(R.drawable.ic_pause_white_24dp);
                stopButton.setVisibility(View.VISIBLE);
                saveButton.setEnabled(false);
                break;
            case PAUSED:
                // Use a mic/record icon to indicate the "resume" action
                recordButton.setIconResource(R.drawable.ic_mic);
                stopButton.setVisibility(View.VISIBLE);
                saveButton.setEnabled(false);
                break;
        }
    }


    // --- Waveform Visualizer Logic ---
    private void calculateBarDimensions() {
        if (waveformVisualizer == null || getContext() == null || getResources() == null) return;
        final int BAR_SPACING_DP = 1;
        int containerWidthPx = waveformVisualizer.getWidth();
        float density = getResources().getDisplayMetrics().density;

        if (containerWidthPx == 0) {
            Log.w(TAG, "calculateBarDimensions: containerWidthPx is 0.");
            return;
        }

        int totalBarWidth = containerWidthPx / MAX_BARS;
        calculatedBarWidthPx = Math.max(1, totalBarWidth - (int) (BAR_SPACING_DP * density));
        calculatedMarginPx = (totalBarWidth - calculatedBarWidthPx) / 2;
        Log.d(TAG, "calculateBarDimensions: width=" + calculatedBarWidthPx + ", margin=" + calculatedMarginPx);
    }

    private Runnable visualizerRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentState == RecordingState.RECORDING && recorder != null && getContext() != null) {
                try {
                    int amplitude = recorder.getMaxAmplitude();
                    updateWaveform(amplitude);
                    visualizerHandler.postDelayed(this, AMPLITUDE_UPDATE_DELAY);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "visualizerRunnable: IllegalStateException from recorder.getMaxAmplitude(). Might have been released.", e);
                    pauseVisualizerUpdates();
                }
            }
        }
    };

    private void startVisualizerUpdates() {
        Log.d(TAG, "startVisualizerUpdates");
        if (visualizerHandler != null && getContext() != null) {
            visualizerHandler.removeCallbacks(visualizerRunnable);
            visualizerHandler.post(visualizerRunnable);
        }
    }

    private void pauseVisualizerUpdates() {
        Log.d(TAG, "pauseVisualizerUpdates");
        if (visualizerHandler != null) {
            visualizerHandler.removeCallbacksAndMessages(null);
        }
    }

    private void clearVisualizer() {
        Log.d(TAG, "clearVisualizer");
        pauseVisualizerUpdates();
        drawSilentWaveform();
    }

    private void updateWaveform(int amplitude) {
        if (getContext() == null || getResources() == null || waveformVisualizer == null) return;
        final float MAX_AMPLITUDE = 32767.0f;
        final int MAX_BAR_HEIGHT_DP = 40;
        float density = getResources().getDisplayMetrics().density;

        if (calculatedBarWidthPx == 0) {
            Log.w(TAG, "updateWaveform: calculatedBarWidthPx is 0, attempting to recalculate.");
            calculateBarDimensions();
            if (calculatedBarWidthPx == 0) {
                Log.e(TAG, "updateWaveform: calculatedBarWidthPx is still 0. Cannot draw waveform bars.");
                return;
            }
        }

        float normalizedAmplitude = Math.min(1.0f, (float) amplitude / MAX_AMPLITUDE);
        int maxBarHeightPx = (int) (MAX_BAR_HEIGHT_DP * density);
        int barHeightPx = (int) (normalizedAmplitude * maxBarHeightPx);
        if (barHeightPx < (int) (4 * density)) barHeightPx = (int) (4 * density);

        int activeColor = MaterialColors.getColor(waveformVisualizer, com.google.android.material.R.attr.colorPrimary, Color.DKGRAY );

        View bar = new View(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                calculatedBarWidthPx,
                barHeightPx
        );
        params.leftMargin = calculatedMarginPx;
        params.rightMargin = calculatedMarginPx;
        params.gravity = android.view.Gravity.CENTER_VERTICAL;

        bar.setLayoutParams(params);
        bar.setBackgroundColor(activeColor);

        waveformVisualizer.addView(bar); // Add to the end

        if (waveformVisualizer.getChildCount() > MAX_BARS) {
            waveformVisualizer.removeViewAt(0); // Remove from the beginning
        }
    }

    private void drawSilentWaveform() {
        if (getContext() == null || getResources() == null || waveformVisualizer == null) return;
        waveformVisualizer.removeAllViews();
        Log.d(TAG, "drawSilentWaveform: Drawing silent waveform. Calculated width: " + calculatedBarWidthPx);

        final int SILENT_BAR_HEIGHT_DP = 4;
        float density = getResources().getDisplayMetrics().density;

        if (calculatedBarWidthPx == 0) {
            Log.w(TAG, "drawSilentWaveform: calculatedBarWidthPx is 0. Attempting to recalculate.");
            calculateBarDimensions();
            if (calculatedBarWidthPx == 0) {
                Log.e(TAG, "drawSilentWaveform: calculatedBarWidthPx is still 0. Skipping silent waveform draw.");
                return;
            }
        }

        int silentBarHeightPx = (int) (SILENT_BAR_HEIGHT_DP * density);
        int primaryColor = MaterialColors.getColor(waveformVisualizer, com.google.android.material.R.attr.colorPrimary, Color.LTGRAY);
        int silentColor = Color.argb(
                (int) (255 * 0.3f),
                Color.red(primaryColor),
                Color.green(primaryColor),
                Color.blue(primaryColor)
        );

        for (int i = 0; i < MAX_BARS; i++) {
            View bar = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    calculatedBarWidthPx,
                    silentBarHeightPx
            );
            params.leftMargin = calculatedMarginPx;
            params.rightMargin = calculatedMarginPx;
            params.gravity = android.view.Gravity.CENTER_VERTICAL;
            bar.setLayoutParams(params);
            bar.setBackgroundColor(silentColor);
            waveformVisualizer.addView(bar);
        }
    }
}