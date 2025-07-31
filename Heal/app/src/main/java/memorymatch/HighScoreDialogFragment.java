package memorymatch;

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

import funcorner.MemoryMatchGameFragment;

// Implement DialogInterface.OnDismissListener
public class HighScoreDialogFragment extends DialogFragment implements DialogInterface.OnDismissListener {

    private static final String ARG_HIGH_SCORES = "high_scores";

    private RecyclerView highScoresRecyclerView;
    private TextView emptyStateTextView;
    private HighScoreAdapter adapter;
    private List<MemoryMatchGameFragment.HighScoreEntry> highScores;

    // Interface to communicate dismissal back to the calling Fragment
    public interface OnDismissListener {
        void onDismiss();
    }

    private OnDismissListener dismissListener;

    public HighScoreDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param highScores List of high scores to display.
     * @param localUserId (Optional) User ID to highlight in the list.
     * @return A new instance of fragment HighScoreDialogFragment.
     */
    public static HighScoreDialogFragment newInstance(List<MemoryMatchGameFragment.HighScoreEntry> highScores, String localUserId) {
        HighScoreDialogFragment fragment = new HighScoreDialogFragment();
        Bundle args = new Bundle();
        ArrayList<MemoryMatchGameFragment.HighScoreEntry> serializableScores = new ArrayList<>(highScores);
        args.putSerializable(ARG_HIGH_SCORES, serializableScores);
        // localUserId parameter is no longer used for display/highlighting, but kept for method signature compatibility
        fragment.setArguments(args);
        return fragment;
    }

    // Setter for the dismiss listener
    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            highScores = (List<MemoryMatchGameFragment.HighScoreEntry>) getArguments().getSerializable(ARG_HIGH_SCORES);
        } else {
            highScores = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the new dialog layout
        return inflater.inflate(R.layout.dialog_high_scores, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        highScoresRecyclerView = view.findViewById(R.id.high_scores_recycler_view);
        emptyStateTextView = view.findViewById(R.id.empty_state_text_view);
        Button closeButton = view.findViewById(R.id.button_close_dialog);

        highScoresRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Pass null for localUserId since it's no longer used in adapter for highlighting
        adapter = new HighScoreAdapter(highScores, null);
        highScoresRecyclerView.setAdapter(adapter);

        if (highScores == null || highScores.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            highScoresRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            highScoresRecyclerView.setVisibility(View.VISIBLE);
        }

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
                // Set the dialog window's background to transparent
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

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog); // Call superclass method
        if (dismissListener != null) {
            dismissListener.onDismiss(); // Call our custom listener
        }
    }
}
