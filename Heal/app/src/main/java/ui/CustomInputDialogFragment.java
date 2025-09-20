package ui;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.f9ld3.heal.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CustomInputDialogFragment extends DialogFragment {

    // Argument keys
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_HINT = "hint"; // This will now also serve as the pre-filled text
    private static final String ARG_POSITIVE_BTN_TEXT = "positive_btn_text";
    private static final String ARG_NEGATIVE_BTN_TEXT = "negative_btn_text";

    // Listener for dialog actions
    public interface OnInputDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, String inputText);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    private OnInputDialogListener listener;

    // Factory method to create a new instance
    public static CustomInputDialogFragment newInstance(String title, @Nullable String message, String hint, String positiveBtnText, String negativeBtnText) {
        CustomInputDialogFragment fragment = new CustomInputDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_HINT, hint); // Pass the hint (which is now also the pre-filled text)
        args.putString(ARG_POSITIVE_BTN_TEXT, positiveBtnText);
        args.putString(ARG_NEGATIVE_BTN_TEXT, negativeBtnText);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(OnInputDialogListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the custom layout
        View view = inflater.inflate(R.layout.dialog_custom_input, container, false);

        // Make the dialog background transparent for rounded corners
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Get screen width
            int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
            // Set dialog width to a percentage of the screen width
            int dialogWidth = (int) (screenWidth * 0.85); // 85% of screen width
            getDialog().getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find UI elements
        TextView dialogTitle = view.findViewById(R.id.dialogTitle);
        TextView dialogMessage = view.findViewById(R.id.dialogMessage);
        TextInputLayout textInputLayout = view.findViewById(R.id.textInputLayout);
        TextInputEditText textInputEditText = view.findViewById(R.id.textInputEditText);
        Button buttonPositive = view.findViewById(R.id.buttonPositive);
        Button buttonNegative = view.findViewById(R.id.buttonNegative);

        // Get arguments
        Bundle args = getArguments();
        if (args != null) {
            dialogTitle.setText(args.getString(ARG_TITLE));
            textInputLayout.setHint(args.getString(ARG_HINT)); // Still use hint for the floating label
            // Set the text of the EditText to the value passed in ARG_HINT
            String preFilledText = args.getString(ARG_HINT);
            if (preFilledText != null && !preFilledText.isEmpty() && !preFilledText.equals("Your Name")) { // Avoid pre-filling with default "Your Name"
                textInputEditText.setText(preFilledText);
                textInputEditText.setSelection(preFilledText.length()); // Place cursor at the end
            }

            buttonPositive.setText(args.getString(ARG_POSITIVE_BTN_TEXT));
            buttonNegative.setText(args.getString(ARG_NEGATIVE_BTN_TEXT));

            String message = args.getString(ARG_MESSAGE);
            if (message != null && !message.isEmpty()) {
                dialogMessage.setText(message);
                dialogMessage.setVisibility(View.VISIBLE);
            } else {
                dialogMessage.setVisibility(View.GONE);
            }
        }

        // Set click listeners
        buttonPositive.setOnClickListener(v -> {
            String inputText = textInputEditText.getText().toString().trim();
            if (listener != null) {
                if (inputText.isEmpty()) {
                    Toast.makeText(getContext(), "Input cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    listener.onDialogPositiveClick(this, inputText);
                    dismiss();
                }
            } else {
                dismiss();
            }
        });

        buttonNegative.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDialogNegativeClick(this);
            }
            dismiss();
        });
    }
}
