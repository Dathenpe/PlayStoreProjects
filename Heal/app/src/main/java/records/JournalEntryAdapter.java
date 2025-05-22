package records;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heal.R;
import java.util.List;

import ui.HomeFragment; // Import your HomeFragment to access the JournalEntry inner class

public class JournalEntryAdapter extends RecyclerView.Adapter<JournalEntryViewHolder> {

    private List<HomeFragment.JournalEntry> journalEntries;
    private OnJournalEntryClickListener listener;

    // Interface for click events to communicate back to the Fragment
    public interface OnJournalEntryClickListener {
        void onJournalEntryClick(HomeFragment.JournalEntry entry);
    }

    public JournalEntryAdapter(List<HomeFragment.JournalEntry> journalEntries, OnJournalEntryClickListener listener) {
        this.journalEntries = journalEntries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JournalEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journal_entry, parent, false);
        return new JournalEntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalEntryViewHolder holder, int position) {
        HomeFragment.JournalEntry entry = journalEntries.get(position);
        holder.timestampTextView.setText(entry.getTimestamp());
        holder.journalTextTextView.setText(entry.getText());

        // Set the click listener for the entire item view (the CardView)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onJournalEntryClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return journalEntries.size();
    }

    // Method to update the data set and refresh the RecyclerView
    public void updateData(List<HomeFragment.JournalEntry> newEntries) {
        this.journalEntries = newEntries;
        notifyDataSetChanged(); // Notifies the adapter that the data has changed
    }
}