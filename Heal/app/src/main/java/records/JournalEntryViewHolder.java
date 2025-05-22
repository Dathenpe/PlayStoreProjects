package records;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heal.R;

public class JournalEntryViewHolder extends RecyclerView.ViewHolder {

    TextView timestampTextView;
    TextView journalTextTextView;

    public JournalEntryViewHolder(@NonNull View itemView) {
        super(itemView);
        timestampTextView = itemView.findViewById(R.id.timestamp_text_view);
        journalTextTextView = itemView.findViewById(R.id.journal_text_view);
    }
}