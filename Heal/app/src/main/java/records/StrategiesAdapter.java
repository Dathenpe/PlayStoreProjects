package records;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heal.R; // Adjust your package name

import java.util.List;

public class StrategiesAdapter extends RecyclerView.Adapter<StrategiesAdapter.StrategyViewHolder> {

    private List<String> strategiesList;
    private Context context;
    private OnStrategyDeleteListener deleteListener;
    private OnStrategyEditListener editListener;
    private OnStrategyClickListener clickListener; // <-- NEW: Declare OnStrategyClickListener

    // NEW INTERFACE: For item clicks
    public interface OnStrategyClickListener {
        void onStrategyClick(String strategyText);
    }

    // Your existing interfaces
    public interface OnStrategyDeleteListener {
        void onDeleteStrategy(int position);
    }

    public interface OnStrategyEditListener {
        void onEditStrategy(int position, String currentStrategy);
    }

    // Updated Constructor: Accepts the new OnStrategyClickListener
    public StrategiesAdapter(Context context, List<String> strategiesList,
                             OnStrategyDeleteListener deleteListener,
                             OnStrategyEditListener editListener,
                             OnStrategyClickListener clickListener) { // <-- NEW: Add clickListener parameter
        this.context = context;
        this.strategiesList = strategiesList;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
        this.clickListener = clickListener; // <-- NEW: Initialize clickListener
    }

    @NonNull
    @Override
    public StrategyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_strategy, parent, false);
        return new StrategyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StrategyViewHolder holder, int position) {
        String strategy = strategiesList.get(position);
        holder.strategyTextView.setText(strategy);

        holder.editStrategyButton.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEditStrategy(holder.getAdapterPosition(), strategiesList.get(holder.getAdapterPosition()));
            }
        });

        holder.deleteStrategyButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteStrategy(holder.getAdapterPosition());
            }
        });

        // <-- NEW: Set click listener for the entire item view (CardView)
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onStrategyClick(strategiesList.get(holder.getAdapterPosition()));
            }
        });
    }

    @Override
    public int getItemCount() {
        return strategiesList.size();
    }

    public void updateStrategies(List<String> newStrategies) {
        this.strategiesList = newStrategies;
        notifyDataSetChanged();
    }

    public static class StrategyViewHolder extends RecyclerView.ViewHolder {
        TextView strategyTextView;
        ImageButton editStrategyButton;
        ImageButton deleteStrategyButton;

        public StrategyViewHolder(@NonNull View itemView) {
            super(itemView);
            strategyTextView = itemView.findViewById(R.id.strategyTextView);
            editStrategyButton = itemView.findViewById(R.id.editStrategyButton);
            deleteStrategyButton = itemView.findViewById(R.id.deleteStrategyButton);
        }
    }
}