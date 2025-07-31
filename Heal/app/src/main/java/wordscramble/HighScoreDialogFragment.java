package wordscramble;

import android.app.Dialog;
import android.content.DialogInterface; // Import DialogInterface
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.R;

import java.util.ArrayList;
import java.util.List;

/**
 * DialogFragment to display high scores for the Word Scramble game.
 * It uses a RecyclerView with HighScoreAdapter to list the scores,
 * matching the structure and behavior of the MemoryMatch HighScoreDialogFragment.
 */
// Implement DialogInterface.OnDismissListener
public class HighScoreDialogFragment extends DialogFragment implements DialogInterface.OnDismissListener {

    private static final String ARG_HIGH_SCORES = "high_scores";
    private static final String ARG_LOCAL_USER_ID = "local_user_id"; // Added for signature compatibility

    private RecyclerView highScoresRecyclerView;
    private TextView emptyStateTextView;
    private HighScoreAdapter adapter; // Using the wordscramble.HighScoreAdapter
    private List<HighScoreEntry> highScores; // Using wordscramble.HighScoreEntry
    // private String localUserId; // Declared but not used for display/highlighting in this specific adapter

    // Interface to communicate dismissal back to the calling Fragment
    public interface OnDismissListener {
        void onDismiss();
    }

    private OnDismissListener dismissListener; // Member variable for the listener

    public HighScoreDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance of this dialog fragment.
     * This signature now matches the MemoryMatch HighScoreDialogFragment's newInstance.
     *
     * @param highScores A list of HighScoreEntry objects to display.
     * @param localUserId This parameter is kept for signature compatibility, but is not used for highlighting.
     * @return A new instance of HighScoreDialogFragment.
     */
    public static HighScoreDialogFragment newInstance(List<HighScoreEntry> highScores, String localUserId) {
        HighScoreDialogFragment fragment = new HighScoreDialogFragment();
        Bundle args = new Bundle();
        // Ensure the list is Serializable for passing via Bundle
        ArrayList<HighScoreEntry> serializableScores = new ArrayList<>(highScores);
        args.putSerializable(ARG_HIGH_SCORES, serializableScores);
        args.putString(ARG_LOCAL_USER_ID, localUserId); // Pass the localUserId for compatibility
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Setter for the dismiss listener.
     * @param listener The listener to be called when the dialog is dismissed.
     */
    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Cast the Serializable object back to List<HighScoreEntry>
            highScores = (List<HighScoreEntry>) getArguments().getSerializable(ARG_HIGH_SCORES);
            // localUserId = getArguments().getString(ARG_LOCAL_USER_ID); // Retrieve for compatibility, but not used in this dialog's logic
        } else {
            highScores = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the dialog_high_scores layout provided by the user
        return inflater.inflate(R.layout.dialog_word_scramble_highscore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        highScoresRecyclerView = view.findViewById(R.id.high_scores_recycler_view);
        emptyStateTextView = view.findViewById(R.id.empty_state_text_view);
        Button closeButton = view.findViewById(R.id.button_close_dialog);

        highScoresRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HighScoreAdapter(highScores); // Initialize with the highScores list
        highScoresRecyclerView.setAdapter(adapter);

        // Show/hide empty state message based on whether there are high scores
        if (highScores == null || highScores.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            highScoresRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            highScoresRecyclerView.setVisibility(View.VISIBLE);
        }

        // Set click listener for the close button
        closeButton.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            // Set the dialog's dismiss listener to this fragment
            dialog.setOnDismissListener(this); // Crucial change here!

            Window window = dialog.getWindow();
            if (window != null) {
                // Set the dialog window's background to transparent for rounded corners to show
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                // Ensure the width is MATCH_PARENT and height is WRAP_CONTENT
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                // Set horizontal margin to 0 to prevent any default system margins
                layoutParams.horizontalMargin = 0; // This is key for full width
                window.setAttributes(layoutParams);
            }
        }
    }

    // Correctly override the onDismiss method from DialogInterface.OnDismissListener
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog); // Call superclass method
        if (dismissListener != null) {
            dismissListener.onDismiss(); // Call our custom listener
        }
    }
}
