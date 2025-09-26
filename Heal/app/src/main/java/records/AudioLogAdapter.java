package records;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.R;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class AudioLogAdapter extends RecyclerView.Adapter<AudioLogAdapter.AudioLogViewHolder> {

    private List<AudioLog> audioLogs;
    private OnLogActionListener listener;
    private Activity activityContext;

    private static AudioLogViewHolder currentlyPlayingHolder = null;
    private static MediaPlayer globalMediaPlayer = null;
    private static Handler progressUpdateHandler = new Handler(Looper.getMainLooper());
    private static Runnable progressUpdateRunnable;

    private static final String TAG = "AudioLogAdapter";

    public interface OnLogActionListener {
        void onDeleteLog(AudioLog log);
    }

    public AudioLogAdapter(List<AudioLog> audioLogs, OnLogActionListener listener, Activity activityContext) {
        this.audioLogs = audioLogs;
        this.listener = listener;
        this.activityContext = activityContext;
        Log.d(TAG, "Adapter created. Initial logs count: " + (audioLogs != null ? audioLogs.size() : 0));
    }

    @NonNull
    @Override
    public AudioLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio_log, parent, false);
        return new AudioLogViewHolder(view, this.activityContext);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioLogViewHolder holder, int position) {
        AudioLog log = audioLogs.get(position);
        Log.d(TAG, "Binding ViewHolder for position " + position + ", title: " + log.getTitle());
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return audioLogs != null ? audioLogs.size() : 0;
    }

    public void updateData(List<AudioLog> newLogs) {
        // This is safe to call even if currentlyPlayingHolder is null
        clearCurrentlyPlaying(true);

        this.audioLogs = newLogs;
        notifyDataSetChanged();
    }


    private static void startProgressUpdater() {
        stopProgressUpdaterInternal();

        if (globalMediaPlayer != null && currentlyPlayingHolder != null &&
                currentlyPlayingHolder.durationTextView != null && currentlyPlayingHolder.boundLog != null) {

            final AudioLog currentLog = currentlyPlayingHolder.boundLog;
            final AudioLogViewHolder activeHolder = currentlyPlayingHolder;

            progressUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (globalMediaPlayer != null && globalMediaPlayer.isPlaying() &&
                            currentlyPlayingHolder == activeHolder && activeHolder.boundLog == currentLog) {
                        try {
                            int currentPosition = globalMediaPlayer.getCurrentPosition();

                            // UPDATED: Call the new overlay method
                            activeHolder.updateProgressOverlay(currentPosition, currentLog.getDurationMillis());

                            String timeString = AudioLog.formatDuration(currentPosition) + " / " + currentLog.getFormattedDuration();
                            activeHolder.durationTextView.setText(timeString);
                            progressUpdateHandler.postDelayed(this, 50); // Update frequently for smooth visual
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "ProgressUpdater: MediaPlayer in bad state for " + currentLog.getTitle(), e);
                            stopProgressUpdaterUIThread(activeHolder, currentLog);
                        }
                    } else {
                        // Don't auto-stop if the player is just paused.
                        // Only stop if the media player is null or not the same holder.
                        if(globalMediaPlayer == null || currentlyPlayingHolder != activeHolder || activeHolder.boundLog != currentLog) {
                            stopProgressUpdaterUIThread(activeHolder, currentLog);
                        }
                    }
                }
            };
            progressUpdateHandler.post(progressUpdateRunnable);
        }
    }

    private static void stopProgressUpdaterUIThread(final AudioLogViewHolder holderToReset, final AudioLog logToReset) {
        stopProgressUpdaterInternal();
        if (holderToReset != null && holderToReset.durationTextView != null && logToReset != null &&
                holderToReset.activityContext != null && !holderToReset.activityContext.isFinishing()) {
            if (holderToReset.boundLog == logToReset || holderToReset == currentlyPlayingHolder) {
                holderToReset.activityContext.runOnUiThread(() ->
                        holderToReset.durationTextView.setText(logToReset.getFormattedDuration()));
            }
        }
    }

    private static void stopProgressUpdaterInternal() {
        if (progressUpdateRunnable != null) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable);
            progressUpdateRunnable = null;
        }
    }


    public static void clearCurrentlyPlaying(boolean resetOldHolderUI) {
        AudioLogViewHolder oldHolder = currentlyPlayingHolder;
        AudioLog oldLog = (oldHolder != null) ? oldHolder.boundLog : null;

        stopProgressUpdaterInternal();

        if (globalMediaPlayer != null) {
            try {
                if (globalMediaPlayer.isPlaying()) {
                    globalMediaPlayer.stop();
                }
                globalMediaPlayer.release();
            } catch (IllegalStateException e) {
                Log.e(TAG, "clearCurrentlyPlaying: Error stopping/releasing globalMediaPlayer", e);
            }
            globalMediaPlayer = null;
        }

        currentlyPlayingHolder = null;

        if (resetOldHolderUI && oldHolder != null && oldHolder.activityContext != null && !oldHolder.activityContext.isFinishing()) {
            oldHolder.activityContext.runOnUiThread(() -> {
                if (oldHolder.playButton != null) {
                    oldHolder.playButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                }

                // Reset progress overlay UI
                if (oldHolder.playbackProgressOverlay != null) {
                    ViewGroup.LayoutParams params = oldHolder.playbackProgressOverlay.getLayoutParams();
                    if (params != null) {
                        params.width = 0; // Reset width to 0
                        oldHolder.playbackProgressOverlay.setLayoutParams(params);
                    }
                    oldHolder.playbackProgressOverlay.setVisibility(View.INVISIBLE); // Hide progress overlay
                }

                if (oldHolder.durationTextView != null && oldLog != null) {
                    if(oldHolder.boundLog == oldLog) {
                        oldHolder.durationTextView.setText(oldLog.getFormattedDuration());
                    }
                }
                oldHolder.isPlaying = false;
            });
        }
    }

    class AudioLogViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView timestampTextView;
        TextView durationTextView;
        FrameLayout waveformContainer; // The parent FrameLayout for touch events
        LinearLayout playbackVisualizerContainer; // Container for Waveform Bars (Unplayed - the base layer)
        LinearLayout playbackProgressOverlay;     // The actual progress overlay (Played - the mask layer)
        ImageView playButton;
        ImageButton deleteButton;

        AudioLog boundLog;
        boolean isPlaying = false;
        Activity activityContext;

        private static final int BAR_COUNT = 40;
        private final Random random = new Random();
        private final int[] staticBarHeights = new int[BAR_COUNT]; // Array to store the static height profile

        AudioLogViewHolder(View itemView, Activity context) {
            super(itemView);
            this.activityContext = context;

            titleTextView = itemView.findViewById(R.id.logTitleTextView);
            timestampTextView = itemView.findViewById(R.id.logTimestampTextView);
            durationTextView = itemView.findViewById(R.id.logDurationTextView);

            waveformContainer = itemView.findViewById(R.id.waveformContainer);
            playbackVisualizerContainer = itemView.findViewById(R.id.playbackVisualizerContainer);
            playbackProgressOverlay = itemView.findViewById(R.id.playbackProgressOverlay);

            playButton = itemView.findViewById(R.id.playButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);

            // Pre-calculate random bar heights for a consistent look across the two layers
            for (int i = 0; i < BAR_COUNT; i++) {
                // Random height between 20% and 100%
                // Storing as percentage * 100 for easy calc later
                staticBarHeights[i] = (int) ((0.2f + (random.nextFloat() * 0.8f)) * 100f);
            }


            playButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    AudioLog clickedLog = audioLogs.get(position);
                    togglePlayback(clickedLog);
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    AudioLog logToDelete = audioLogs.get(position);
                    if (currentlyPlayingHolder == this) {
                        clearCurrentlyPlaying(true);
                    }
                    listener.onDeleteLog(logToDelete);
                }
            });

            waveformContainer.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    // Only allow seeking if this item is the one currently loaded in the player
                    if (globalMediaPlayer != null && boundLog != null && boundLog.getDurationMillis() > 0 && currentlyPlayingHolder == this) {
                        float touchX = event.getX();
                        int containerWidth = v.getWidth();
                        if (containerWidth > 0) {
                            float progress = touchX / containerWidth;
                            long seekPosition = (long) (progress * boundLog.getDurationMillis());

                            // Clamp the value to be within the duration
                            seekPosition = Math.max(0, Math.min(seekPosition, boundLog.getDurationMillis()));

                            // seekTo takes an int, so we cast. For audio logs, this is fine.
                            globalMediaPlayer.seekTo((int) seekPosition);

                            // --- CHANGE: REMOVED IMMEDIATE UI UPDATE ---
                            // The periodic progress updater (progressUpdateRunnable) will now handle
                            // updating the UI. This ensures the visualizer position is always
                            // synchronized with the media player's actual position after seeking,
                            // preventing the visualizer from jumping ahead of the audio.
                        }
                    }
                    return true; // Consume the touch event to handle seeking
                }
                return false; // Don't consume other touch events
            });
        }

        /**
         * Calculates the new width for the playbackProgressOverlay based on the current playback position.
         */
        public void updateProgressOverlay(int currentPosition, long totalDuration) {
            if (playbackVisualizerContainer == null || playbackProgressOverlay == null || totalDuration <= 0) return;

            final int containerWidth = playbackVisualizerContainer.getWidth();
            if (containerWidth <= 0) {
                return;
            }

            float progress = (float) currentPosition / (float) totalDuration;
            progress = Math.min(1.0f, Math.max(0.0f, progress));

            int newWidth = (int) (containerWidth * progress);

            // This must run on the UI thread as it modifies the View hierarchy
            if (activityContext != null && !activityContext.isFinishing()) {
                activityContext.runOnUiThread(() -> {
                    if (playbackProgressOverlay != null) {
                        ViewGroup.LayoutParams params = playbackProgressOverlay.getLayoutParams();
                        if (params != null && params.width != newWidth) {
                            params.width = newWidth;
                            playbackProgressOverlay.setLayoutParams(params);
                        }
                    }
                });
            }
        }


        /**
         * Generates the waveform bars into the specified container with the given color.
         * R.color.silver is used for the base/unplayed layer.
         * ?attr/colorPrimary is used for the overlay/played layer.
         */
        private void generateWaveformBars(LinearLayout container, int barColor) {
            if (container == null || container.getChildCount() > 0) return;

            int marginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, activityContext.getResources().getDisplayMetrics());
            int barWidthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, activityContext.getResources().getDisplayMetrics());

            // Wait for the container to be drawn to get its final height
            container.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    container.getViewTreeObserver().removeOnPreDrawListener(this);
                    final int containerHeightPx = container.getHeight();
                    if (containerHeightPx == 0) return true;

                    for (int i = 0; i < BAR_COUNT; i++) {
                        View bar = new View(activityContext);

                        // Use pre-calculated height profile (percentage * 100)
                        float heightRatio = (float) staticBarHeights[i] / 100f;
                        int barHeightPx = (int) (containerHeightPx * heightRatio);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                barWidthPx,
                                barHeightPx
                        );

                        // Use margin to separate bars
                        if (i > 0) {
                            params.leftMargin = marginPx;
                        }
                        params.rightMargin = marginPx;

                        params.gravity = android.view.Gravity.CENTER_VERTICAL; // Align bars to center
                        bar.setLayoutParams(params);
                        bar.setBackgroundColor(barColor);
                        container.addView(bar);
                    }
                    return true;
                }
            });
        }

        void bind(AudioLog log) {
            this.boundLog = log;
            titleTextView.setText(log.getTitle());
            timestampTextView.setText(log.getTimestamp());

            // 1. Resolve Colors
            int unplayedColor = ContextCompat.getColor(activityContext, R.color.silver);

            int playedColor;
            try {
                // Resolve ?attr/colorPrimary from the theme
                TypedValue typedValue = new TypedValue();
                activityContext.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
                playedColor = typedValue.data;
            } catch (Exception e) {
                // Fallback to a default primary color if theme resolution fails
                playedColor = ContextCompat.getColor(activityContext, R.color.coral);
            }

            // 2. Setup Waveform Layers (Base and Overlay)
            playbackVisualizerContainer.removeAllViews();
            playbackProgressOverlay.removeAllViews();

            // Generate the base layer (Unplayed Color)
            generateWaveformBars(playbackVisualizerContainer, unplayedColor);

            // Generate the overlay layer (Played Color - primary color)
            generateWaveformBars(playbackProgressOverlay, playedColor);

            // 3. Set Playback State
            if (currentlyPlayingHolder == this && globalMediaPlayer != null) {
                isPlaying = globalMediaPlayer.isPlaying();
                playButton.setImageResource(isPlaying ? R.drawable.ic_stop : R.drawable.ic_play_arrow_white_24dp);
                playbackProgressOverlay.setVisibility(View.VISIBLE);

                try {
                    int currentPosition = globalMediaPlayer.getCurrentPosition();
                    updateProgressOverlay(currentPosition, log.getDurationMillis());
                    String timeString = AudioLog.formatDuration(currentPosition) + " / " + boundLog.getFormattedDuration();
                    durationTextView.setText(timeString);
                } catch (IllegalStateException e) {
                    durationTextView.setText(boundLog.getFormattedDuration());
                    playbackProgressOverlay.setVisibility(View.INVISIBLE);
                }
                if (isPlaying && progressUpdateRunnable == null) {
                    startProgressUpdater();
                }

            } else {
                playButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                durationTextView.setText(log.getFormattedDuration());
                isPlaying = false;
                playbackProgressOverlay.setVisibility(View.INVISIBLE);

                // Ensure the progress overlay is reset to 0 width when not playing/recycled
                if (playbackProgressOverlay != null) {
                    ViewGroup.LayoutParams params = playbackProgressOverlay.getLayoutParams();
                    if (params != null && params.width != 0) {
                        params.width = 0;
                        playbackProgressOverlay.setLayoutParams(params);
                    }
                }
            }
        }

        private void togglePlayback(AudioLog logToPlay) {
            // Case 1: This item is playing, user clicks to pause/stop
            if (currentlyPlayingHolder == this && globalMediaPlayer != null && globalMediaPlayer.isPlaying()) {
                globalMediaPlayer.pause();
                isPlaying = false;
                playButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                stopProgressUpdaterInternal();
            }
            // Case 2: This item is paused, user clicks to resume
            else if (currentlyPlayingHolder == this && globalMediaPlayer != null && !globalMediaPlayer.isPlaying()) {
                globalMediaPlayer.start();
                isPlaying = true;
                playButton.setImageResource(R.drawable.ic_stop);
                startProgressUpdater();
            }
            // Case 3: A different item is playing (or none), user clicks to start this one
            else {
                startPlayback(logToPlay);
            }
        }

        private void startPlayback(AudioLog logToPlay) {
            if (logToPlay.getFilePath() == null || logToPlay.getFilePath().isEmpty()) {
                Toast.makeText(itemView.getContext(), "Error: Audio file path is missing.", Toast.LENGTH_SHORT).show();
                return;
            }

            clearCurrentlyPlaying(true);

            // Reset and show the overlay before playback starts
            if (playbackProgressOverlay != null) {
                ViewGroup.LayoutParams params = playbackProgressOverlay.getLayoutParams();
                if (params != null) {
                    params.width = 0;
                    playbackProgressOverlay.setLayoutParams(params);
                }
                playbackProgressOverlay.setVisibility(View.VISIBLE);
            }

            globalMediaPlayer = new MediaPlayer();
            try {
                globalMediaPlayer.setDataSource(logToPlay.getFilePath());
                globalMediaPlayer.prepareAsync();

                globalMediaPlayer.setOnPreparedListener(mp -> {
                    currentlyPlayingHolder = this; // Set holder here after prepare
                    if (boundLog == logToPlay && boundLog.getDurationMillis() <= 0 && mp.getDuration() > 0) {
                        boundLog.setDurationMillis(mp.getDuration());
                    }

                    mp.start();
                    isPlaying = true;
                    playButton.setImageResource(R.drawable.ic_stop);
                    Toast.makeText(itemView.getContext(), "Playing: " + logToPlay.getTitle(), Toast.LENGTH_SHORT).show();

                    startProgressUpdater();
                });

                globalMediaPlayer.setOnCompletionListener(mp -> {
                    if (currentlyPlayingHolder == this) {
                        clearCurrentlyPlaying(true);
                    }
                });

                globalMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Toast.makeText(itemView.getContext(), "Error during playback.", Toast.LENGTH_SHORT).show();
                    if (playbackProgressOverlay != null) {
                        activityContext.runOnUiThread(() -> playbackProgressOverlay.setVisibility(View.INVISIBLE));
                    }

                    if (currentlyPlayingHolder == this) {
                        clearCurrentlyPlaying(true);
                    }
                    return true;
                });

            } catch (IOException | IllegalStateException | SecurityException e) {
                Toast.makeText(itemView.getContext(), "Error playing audio: " + e.getMessage(), Toast.LENGTH_LONG).show();
                clearCurrentlyPlaying(true);
                if (playbackProgressOverlay != null) {
                    playbackProgressOverlay.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    public static void releaseGlobalPlayer() {
        clearCurrentlyPlaying(true);
    }
}