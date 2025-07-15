package memorymatch;

import android.app.Dialog;
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

public class HighScoreDialogFragment extends DialogFragment {

    private static final String ARG_HIGH_SCORES = "high_scores";

    private RecyclerView highScoresRecyclerView;
    private TextView emptyStateTextView;
    private HighScoreAdapter adapter;
    private List<MemoryMatchGameFragment.HighScoreEntry> highScores;

    public HighScoreDialogFragment() {
        // Required empty public constructor
    }

    public static HighScoreDialogFragment newInstance(List<MemoryMatchGameFragment.HighScoreEntry> highScores, String localUserId) {
        HighScoreDialogFragment fragment = new HighScoreDialogFragment();
        Bundle args = new Bundle();
        ArrayList<MemoryMatchGameFragment.HighScoreEntry> serializableScores = new ArrayList<>(highScores);
        args.putSerializable(ARG_HIGH_SCORES, serializableScores);
        // localUserId parameter is no longer used for display/highlighting, but kept for method signature compatibility
        fragment.setArguments(args);
        return fragment;
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

}
