package wordscramble;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.R; // Assuming R is accessible from your main app module

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying HighScoreEntry objects in a RecyclerView for the Word Scramble game.
 * This adapter is styled to match the MemoryMatch HighScoreAdapter's display fields.
 */
public class HighScoreAdapter extends RecyclerView.Adapter<HighScoreAdapter.HighScoreViewHolder> {

    private List<HighScoreEntry> highScores;

    /**
     * Constructor for the HighScoreAdapter.
     * @param highScores The list of HighScoreEntry objects to display.
     */
    public HighScoreAdapter(List<HighScoreEntry> highScores) {
        this.highScores = highScores;
        // Sort the high scores for display: highest score first, then lowest time for ties.
        // This sorting matches the logic in WordScrambleGameFragment's saveHighScore.
        Collections.sort(this.highScores, (e1, e2) -> {
            int scoreCompare = Integer.compare(e2.score, e1.score); // Descending score
            if (scoreCompare == 0) {
                return Long.compare(e1.timeTaken, e2.timeTaken); // Ascending time for same score
            }
            return scoreCompare;
        });
    }

    @NonNull
    @Override
    public HighScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_high_score_memory_match layout, as requested to match its structure
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_high_score_memory_match, parent, false);
        return new HighScoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HighScoreViewHolder holder, int position) {
        HighScoreEntry entry = highScores.get(position);
        int rank = position + 1;

        holder.rankTextView.setText(String.valueOf(rank) + ".");

        // Set the date to the new TextView, matching MemoryMatch's dateTextView
        holder.dateTextView.setText(entry.date);

        // Format time and combine with score/mode info, matching MemoryMatch's timeThemeTextView
        long minutes = entry.timeTaken / 60;
        long seconds = entry.timeTaken % 60;
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        // The "theme" part will now display the score and game mode
        // Updated to include the game mode from HighScoreEntry, with a null check
        String modeDisplayName = (entry.gameMode != null) ? entry.gameMode.displayName : "N/A"; // Handle null gameMode
        String scoreAndMode = String.format(Locale.getDefault(), "Score: %d (%s)", entry.score, modeDisplayName);
        holder.timeThemeTextView.setText(String.format(Locale.getDefault(), "%s (%s)", formattedTime, scoreAndMode));
    }

    @Override
    public int getItemCount() {
        return highScores.size();
    }

    /**
     * ViewHolder for the HighScoreAdapter.
     * Holds references to the TextViews in item_high_score_memory_match.xml.
     * Renamed to match the MemoryMatch ViewHolder structure.
     */
    static class HighScoreViewHolder extends RecyclerView.ViewHolder {
        TextView rankTextView;
        TextView dateTextView; // Matches MemoryMatch's dateTextView
        TextView timeThemeTextView; // Matches MemoryMatch's timeThemeTextView

        public HighScoreViewHolder(@NonNull View itemView) {
            super(itemView);
            rankTextView = itemView.findViewById(R.id.rank_text_view);
            dateTextView = itemView.findViewById(R.id.date_text_view); // Matches MemoryMatch's ID
            timeThemeTextView = itemView.findViewById(R.id.time_theme_text_view); // Matches MemoryMatch's ID
        }
    }
}
