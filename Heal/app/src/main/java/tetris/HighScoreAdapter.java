package tetris;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.f9ld3.heal.R;
import java.util.List;

public class HighScoreAdapter extends RecyclerView.Adapter<HighScoreAdapter.ViewHolder> {

    private List<HighScore> highScores;

    public HighScoreAdapter(List<HighScore> highScores) {
        this.highScores = highScores;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_high_score, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HighScore highScore = highScores.get(position);
        holder.scoreTextView.setText("Score: " + highScore.getScore() + " - Level: " + highScore.getLevel());
        holder.dateTextView.setText(highScore.getDate());
    }

    @Override
    public int getItemCount() {
        return highScores.size();
    }

    public void updateScores(List<HighScore> newScores) {
        this.highScores = newScores;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView scoreTextView;
        TextView dateTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            scoreTextView = itemView.findViewById(R.id.high_score_text_view);
            dateTextView = itemView.findViewById(R.id.high_score_date_text_view);
        }
    }
}