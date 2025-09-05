package com.f9ld3.heal;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
// Ensure you have this in your build.gradle (Module: app)
// implementation 'com.google.android.material:material:1.12.0' // Or your current version

public class TypeToConfirmDialogFragment extends DialogFragment {

    private static final String ARG_DIALOG_TITLE = "arg_dialog_title";
    private static final String ARG_MESSAGE = "arg_message";
    private static final String ARG_CHALLENGE_TEXT = "arg_challenge_text";
    private static final String ARG_POSITIVE_BUTTON_TEXT = "arg_positive_button_text";
    private static final String ARG_NEGATIVE_BUTTON_TEXT = "arg_negative_button_text";

    private TextInputEditText editTextConfirmText;
    private TextInputLayout textInputLayoutConfirmText;
    private TextView textViewChallenge;
    private Button positiveButton; // To enable/disable it
    private String challengeTextActual; // The actual text to be matched

    public interface OnTypeConfirmListener {
        void onTextConfirmed();
        void onTextConfirmationCancelled();
    }

    private OnTypeConfirmListener listener;

    public static TypeToConfirmDialogFragment newInstance(String dialogTitle, String message, String challengeText, String positiveButtonText, String negativeButtonText) {
        TypeToConfirmDialogFragment fragment = new TypeToConfirmDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DIALOG_TITLE, dialogTitle);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_CHALLENGE_TEXT, challengeText);
        args.putString(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
        args.putString(ARG_NEGATIVE_BUTTON_TEXT, negativeButtonText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure the host activity or parent fragment implements the callback interface
        try {
            if (getParentFragment() != null) {
                listener = (OnTypeConfirmListener) getParentFragment();
            } else {
                listener = (OnTypeConfirmListener) getActivity();
            }
        } catch (ClassCastException e) {
            throw new ClassCastException((getParentFragment() != null ? getParentFragment().toString() : context.toString())
                    + " must implement OnTypeConfirmListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        // You need a layout file: res/layout/dialog_type_to_confirm.xml
        View view = inflater.inflate(R.layout.dialog_type_to_confirm, null);

        TextView dialogTitleView = view.findViewById(R.id.dialog_type_title);
        TextView messageView = view.findViewById(R.id.dialog_type_message);
        textViewChallenge = view.findViewById(R.id.dialog_type_challenge_text);
        editTextConfirmText = view.findViewById(R.id.dialog_type_edittext_confirm);
        textInputLayoutConfirmText = view.findViewById(R.id.dialog_type_textinputlayout_confirm);

        Bundle args = getArguments();
        String dialogTitle = "Confirm Action";
        String message = "Please type the text below to confirm.";
        String positiveButtonText = "Confirm";
        String negativeButtonText = "Cancel";

        if (args != null) {
            dialogTitle = args.getString(ARG_DIALOG_TITLE, dialogTitle);
            message = args.getString(ARG_MESSAGE, message);
            challengeTextActual = args.getString(ARG_CHALLENGE_TEXT); // This is crucial
            positiveButtonText = args.getString(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
            negativeButtonText = args.getString(ARG_NEGATIVE_BUTTON_TEXT, negativeButtonText);

            dialogTitleView.setText(dialogTitle);
            messageView.setText(message);
            if (challengeTextActual != null) {
                textViewChallenge.setText(challengeTextActual);
            } else {
                Log.e("TypeToConfirmDialog", "Challenge text is null!");
                textViewChallenge.setText("Error: Phrase not set"); // Fallback
            }
        } else {
            Log.e("TypeToConfirmDialog", "Arguments are null!");
            textViewChallenge.setText("Error: Phrase not set"); // Fallback
        }


        builder.setView(view)
                .setPositiveButton(positiveButtonText, null) // Click listener handled manually
                .setNegativeButton(negativeButtonText, (dialog, id) -> {
                    if (listener != null) {
                        listener.onTextConfirmationCancelled();
                    }
                    dialog.dismiss();
                });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // User must explicitly interact
        dialog.setCanceledOnTouchOutside(false);

        // We need to get the button after the dialog is shown to control its enabled state
        dialog.setOnShowListener(dialogInterface -> {
            positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton == null) {
                Log.e("TypeToConfirmDialog", "Positive button not found!");
                return;
            }
            positiveButton.setEnabled(false); // Initially disabled

            positiveButton.setOnClickListener(v -> {
                String enteredText = editTextConfirmText.getText().toString();
                // Note: Some users might add a space at the end. Trim for robustness if desired.
                // if (enteredText.trim().equals(challengeTextActual.trim())) {
                if (enteredText.equals(challengeTextActual)) {
                    if (listener != null) {
                        listener.onTextConfirmed();
                    }
                    dismiss(); // Dismiss the dialog on success
                } else {
                    textInputLayoutConfirmText.setError("The typed text does not match the phrase above.");
                    editTextConfirmText.requestFocus();
                    // Do NOT dismiss the dialog here, let the user correct it.
                }
            });

            editTextConfirmText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    textInputLayoutConfirmText.setError(null); // Clear error when user types
                    if (challengeTextActual != null) {
                        positiveButton.setEnabled(s.toString().equals(challengeTextActual));
                    } else {
                        positiveButton.setEnabled(false); // Should not happen if args are passed correctly
                    }
                }
            });
        });
        return dialog;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null; // Clean up listener to prevent memory leaks
    }
}
