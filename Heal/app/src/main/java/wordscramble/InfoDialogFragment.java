package wordscramble; // Assuming this is in the funcorner package

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.f9ld3.heal.R; // Adjust this import based on your R file location
import com.google.android.material.button.MaterialButton;

/**
 * DialogFragment to display information about the game.
 */
public class InfoDialogFragment extends DialogFragment {

    public InfoDialogFragment() {
        // Required empty public constructor
    }

    public static InfoDialogFragment newInstance() {
        return new InfoDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the dialog_info layout
        return inflater.inflate(R.layout.dialog_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView infoTextContent = view.findViewById(R.id.info_text_content);
        // You might want to set more detailed game info here dynamically
        // For now, it uses the string resource defined in strings.xml

        MaterialButton closeButton = view.findViewById(R.id.button_close_info);
        closeButton.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.horizontalMargin = 0;
                window.setAttributes(layoutParams);
            }
        }
    }
}
