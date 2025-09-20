package records;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.R;
import com.google.android.material.textfield.TextInputEditText;

import ui.CustomMessageDialogFragment;
import ui.HomeFragment;

public class EditJournalEntryDialogFragment extends DialogFragment {

    private TextInputEditText journalEditText;
    private TextView journalTimestampTextView;
    private Button buttonSave;
    private Button buttonDelete;
    private HomeFragment.JournalEntry editingEntry;

    public interface OnJournalEntryModifiedListener {
        void onJournalEntrySaved(HomeFragment.JournalEntry updatedEntry);
        void onJournalEntryDeleted(String entryTimestamp); // Using timestamp as ID for now
    }

    private OnJournalEntryModifiedListener listener;

    public static EditJournalEntryDialogFragment newInstance(HomeFragment.JournalEntry entryToEdit) {
        EditJournalEntryDialogFragment fragment = new EditJournalEntryDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("journalEntry", entryToEdit); // JournalEntry must be Serializable
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // First, check if a target fragment is set and implements the listener
        Fragment targetFragment = getTargetFragment();
        if (targetFragment instanceof OnJournalEntryModifiedListener) {
            listener = (OnJournalEntryModifiedListener) targetFragment;
        }
        // If not, or if no target fragment, check the parent fragment
        else if (getParentFragment() instanceof OnJournalEntryModifiedListener) {
            listener = (OnJournalEntryModifiedListener) getParentFragment();
        }
        // If neither, then throw the runtime exception
        else {
            throw new RuntimeException(context.toString() + " or its parent fragment/target fragment must implement OnJournalEntryModifiedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            editingEntry = (HomeFragment.JournalEntry) getArguments().getSerializable("journalEntry");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_journal_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        journalEditText = view.findViewById(R.id.journalEditText);
        journalTimestampTextView = view.findViewById(R.id.journalTimestampTextView);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonDelete = view.findViewById(R.id.buttonDelete);

        if (editingEntry != null) {
            journalEditText.setText(editingEntry.getText());
            journalTimestampTextView.setText(editingEntry.getTimestamp());
        }

        buttonSave.setOnClickListener(v -> handleSave());
        buttonDelete.setOnClickListener(v -> handleDelete());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Get screen width
            int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
            // Set dialog width to a percentage of the screen width
            int dialogWidth = (int) (screenWidth * 0.85); // 85% of screen width
            Dialog dialog = getDialog();
            Window window = dialog.getWindow();
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
            getDialog().getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void handleSave() {
        String updatedText = journalEditText.getText().toString().trim();
        if (TextUtils.isEmpty(updatedText)) {
            Toast.makeText(getContext(), "Journal entry cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingEntry != null && listener != null) {
            // Create a new JournalEntry with updated text but original timestamp and creation timestamp
            HomeFragment.JournalEntry updatedEntry = new HomeFragment.JournalEntry(
                    editingEntry.getTimestamp(),
                    updatedText,
                    editingEntry.getCreationTimestampMillis()
            );
            listener.onJournalEntrySaved(updatedEntry);
        }
        dismiss();
    }

    private void handleDelete() {
        if (getContext() == null || getDialog() == null) return;

        // 1. Hide the current (edit) dialog before showing the new one.
        getDialog().hide();

        // This is the confirmation dialog
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Delete Entry",
                "Are you sure you want to delete this journal entry? This action cannot be undone.",
                "Yes",
                "No"
        );

        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                // User confirmed the deletion.
                if (editingEntry != null && listener != null) {
                    listener.onJournalEntryDeleted(editingEntry.getTimestamp());
                }
                Toast.makeText(getContext(), "Journal entry deleted.", Toast.LENGTH_SHORT).show();

                // Dismiss the confirmation dialog
                dialogFragment.dismiss();

                // Permanently dismiss this EditJournalEntryDialogFragment.
                // No need to show() it again, as we're getting rid of it.
                EditJournalEntryDialogFragment.this.dismiss();
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                // User cancelled. Dismiss the confirmation dialog.
                dialogFragment.dismiss();

                // IMPORTANT: Show the edit dialog again so the user can continue editing.
                if (getDialog() != null) {
                    getDialog().show();
                }
            }
        });

        // Make the confirmation dialog non-cancelable by touch outside,
        // so we can control the show/hide flow properly.
        dialog.setCancelable(false);
        dialog.show(getParentFragmentManager(), "DeleteJournalEntryDialog");
    }
}