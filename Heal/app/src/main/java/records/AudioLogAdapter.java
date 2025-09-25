package records;

import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.R; // Ensure this is the correct package for R

import java.io.IOException;
import java.util.List;

public class AudioLogAdapter extends RecyclerView.Adapter<AudioLogAdapter.AudioLogViewHolder> {

    private List<AudioLog> audioLogs;
    private OnLogActionListener listener;

    public interface OnLogActionListener {
        void onDeleteLog(AudioLog log);
    }

    public AudioLogAdapter(List<AudioLog> audioLogs, OnLogActionListener listener) {
        this.audioLogs = audioLogs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AudioLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio_log, parent, false);
        return new AudioLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioLogViewHolder holder, int position) {
        AudioLog log = audioLogs.get(position);
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return audioLogs.size();
    }

    public void updateData(List<AudioLog> newLogs) {
        this.audioLogs = newLogs;
        notifyDataSetChanged();
    }

    class AudioLogViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView timestampTextView;
        ImageView playButton;
        ImageButton deleteButton;

        MediaPlayer mediaPlayer;
        boolean isPlaying = false;

        AudioLogViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.logTitleTextView);
            timestampTextView = itemView.findViewById(R.id.logTimestampTextView);
            playButton = itemView.findViewById(R.id.playButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);

            playButton.setOnClickListener(v -> togglePlayback(audioLogs.get(getAdapterPosition())));
            deleteButton.setOnClickListener(v -> listener.onDeleteLog(audioLogs.get(getAdapterPosition())));
        }

        void bind(AudioLog log) {
            titleTextView.setText(log.getTitle());
            timestampTextView.setText(log.getTimestamp());
            // Reset state
            stopPlayback();
            playButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        }

        private void togglePlayback(AudioLog log) {
            if (isPlaying) {
                stopPlayback();
            } else {
                startPlayback(log.getFilePath());
            }
        }

        private void startPlayback(String filePath) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(filePath);
                mediaPlayer.prepare();
                mediaPlayer.start();
                isPlaying = true;
                playButton.setImageResource(R.drawable.ic_stop); // You need an ic_stop resource
                Toast.makeText(itemView.getContext(), "Playing: " + titleTextView.getText(), Toast.LENGTH_SHORT).show();

                mediaPlayer.setOnCompletionListener(mp -> stopPlayback());
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(itemView.getContext(), "Error playing audio file.", Toast.LENGTH_SHORT).show();
                stopPlayback();
            }
        }

        private void stopPlayback() {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            isPlaying = false;
            playButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        }
    }
}