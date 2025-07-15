package memorymatch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.R;

import java.util.List;

import funcorner.MemoryMatchGameFragment; // Keep import for HighScoreEntry

public class HighScoreAdapter extends RecyclerView.Adapter<HighScoreAdapter.HighScoreViewHolder> {

    private List<MemoryMatchGameFragment.HighScoreEntry> highScores;
    // Removed localUserId as it's no longer used for highlighting
    // private String localUserId;

    public HighScoreAdapter(List<MemoryMatchGameFragment.HighScoreEntry> highScores, String localUserId) { // localUserId parameter kept for compatibility, but not used
        this.highScores = highScores;
        // this.localUserId = localUserId; // Removed assignment
    }

    @NonNull
    @Override
    public HighScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure this inflates the correct item layout for high scores
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_high_score_memory_match, parent, false);
        return new HighScoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HighScoreViewHolder holder, int position) {
        MemoryMatchGameFragment.HighScoreEntry scoreEntry = highScores.get(position);
        int rank = position + 1;

        holder.rankTextView.setText(String.valueOf(rank) + ".");

        // Set the date to the new TextView
        holder.dateTextView.setText(scoreEntry.date); // Set date instead of user ID

        // Format time
        long minutes = scoreEntry.timeTaken / 60;
        long seconds = scoreEntry.timeTaken % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        holder.timeThemeTextView.setText(String.format("%s (%s)", formattedTime, scoreEntry.theme));
    }

    @Override
    public int getItemCount() {
        return highScores.size();
    }

    public void updateHighScores(List<MemoryMatchGameFragment.HighScoreEntry> newHighScores) {
        this.highScores = newHighScores;
        notifyDataSetChanged();
    }

    static class HighScoreViewHolder extends RecyclerView.ViewHolder {
        TextView rankTextView;
        TextView dateTextView; // Changed from userIdTextView to dateTextView
        TextView timeThemeTextView;

        public HighScoreViewHolder(@NonNull View itemView) {
            super(itemView);
            rankTextView = itemView.findViewById(R.id.rank_text_view);
            dateTextView = itemView.findViewById(R.id.date_text_view); // Changed ID
            timeThemeTextView = itemView.findViewById(R.id.time_theme_text_view);
        }
    }
}
