package tetris;

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
import android.content.DialogInterface; // Import for DialogInterface

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.R;

import java.util.ArrayList;

public class HighScoresDialogFragment extends DialogFragment {

    private static final String ARG_HIGH_SCORES = "high_scores";
    private ArrayList<HighScore> highScores;
    private OnDismissListener dismissListener; // Declare the listener

    // Interface for dismiss callback
    public interface OnDismissListener {
        void onDismiss(); // This method will be called when the dialog is dismissed
    }

    public static HighScoresDialogFragment newInstance(ArrayList<HighScore> highScores) {
        HighScoresDialogFragment fragment = new HighScoresDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_HIGH_SCORES, highScores);
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
            highScores = getArguments().getParcelableArrayList(ARG_HIGH_SCORES);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_high_scores, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.high_scores_recycler_view);
        Button closeButton = view.findViewById(R.id.button_close_dialog);

        HighScoreAdapter adapter = new HighScoreAdapter(highScores);
        recyclerView.setAdapter(adapter);

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
                layoutParams.horizontalMargin = 0;
                window.setAttributes(layoutParams);
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.onDismiss(); // Notify the listener when dismissed
        }
    }
}
