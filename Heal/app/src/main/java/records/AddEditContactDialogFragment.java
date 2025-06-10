package records;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.heal.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddEditContactDialogFragment extends DialogFragment {

    private TextInputEditText editTextContactName;
    private TextInputEditText editTextPhoneNumber;
    private Button buttonAddSaveContact;
    private Button buttonClearFields;
    private CircleImageView addEditContactImageView;
    private Button buttonSelectImage;

    private TextView dialogTitle;

    private EmergencyContact editingContact; // The contact being edited, or null for new
    private String currentSelectedImageUrl = null;


    public interface OnContactSavedListener {
        void onContactSaved(EmergencyContact contact); // Pass the saved/updated contact
    }

    private OnContactSavedListener contactSavedListener;

    // Static factory method to create an instance with optional arguments
    public static AddEditContactDialogFragment newInstance(EmergencyContact contactToEdit) {
        AddEditContactDialogFragment fragment = new AddEditContactDialogFragment();
        if (contactToEdit != null) {
            Bundle args = new Bundle();
            // EmergencyContact must implement Serializable or Parcelable
            args.putSerializable("contactToEdit", contactToEdit);
            fragment.setArguments(args);
        }
        return fragment;
    }

    // Use this to set the listener from the hosting fragment/activity
    // Inside AddEditContactDialogFragment.java
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Option 1: If the Activity is the listener (as we've set up above)
        if (context instanceof OnContactSavedListener) {
            contactSavedListener = (OnContactSavedListener) context;
        }
        else if (getParentFragment() instanceof OnContactSavedListener) {
            contactSavedListener = (OnContactSavedListener) getParentFragment();
        }
        else {
            // If neither the activity nor the parent fragment implements the listener, throw an error
            throw new RuntimeException(context.toString() + " or its parent fragment must implement OnContactSavedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        contactSavedListener = null;
    }

    // ActivityResultLauncher for image selection
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
                Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                // Set the width to MATCH_PARENT
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                // Set the height to WRAP_CONTENT (or MATCH_PARENT if you want it to fill height too)
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(layoutParams);

                // Optional: To remove the default dialog padding/margins if they exist
                // window.setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This ensures the dialog is full screen if desired, or can be removed for default dialog size
        // setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);

        // Initialize image picker launcher
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        currentSelectedImageUrl = uri.toString();
                        Glide.with(this)
                                .load(uri)
                                .placeholder(R.drawable.ic_default_contact_avatar)
                                .error(R.drawable.ic_default_contact_avatar)
                                .into(addEditContactImageView);
                        try {
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            if (getContext() != null) {
                                getContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                            }
                        } catch (SecurityException e) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Permission to access image denied. Please try again.", Toast.LENGTH_LONG).show();
                            }
                            e.printStackTrace();
                        }
                    } else {
                        currentSelectedImageUrl = null;
                        addEditContactImageView.setImageResource(R.drawable.ic_default_contact_avatar);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Get the contact to edit if passed in arguments
        if (getArguments() != null) {
            editingContact = (EmergencyContact) getArguments().getSerializable("contactToEdit");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for the add/edit dialog
        return inflater.inflate(R.layout.dialog_add_edit_contact, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTextContactName = view.findViewById(R.id.editTextContactName);
        editTextPhoneNumber = view.findViewById(R.id.editTextPhoneNumber);
        buttonAddSaveContact = view.findViewById(R.id.buttonAddSaveContact);
        buttonClearFields = view.findViewById(R.id.buttonClearFields);
        addEditContactImageView = view.findViewById(R.id.addEditContactImageView);
        buttonSelectImage = view.findViewById(R.id.buttonSelectImage);
        dialogTitle = view.findViewById(R.id.dialogTitle);

        if (editingContact != null) {
            editTextContactName.setText(editingContact.getName());
            editTextPhoneNumber.setText(editingContact.getPhoneNumber());
            currentSelectedImageUrl = editingContact.getImageUrl();
            if (currentSelectedImageUrl != null && !currentSelectedImageUrl.isEmpty()) {
                Glide.with(this)
                        .load(Uri.parse(currentSelectedImageUrl))
                        .placeholder(R.drawable.ic_default_contact_avatar)
                        .error(R.drawable.ic_default_contact_avatar)
                        .into(addEditContactImageView);
            } else {
                addEditContactImageView.setImageResource(R.drawable.ic_default_contact_avatar);
            }
            dialogTitle.setText("Edit Contact");
            buttonAddSaveContact.setText("Save Changes");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Editing: " + editingContact.getName(), Toast.LENGTH_SHORT).show();
            }
        } else {
            dialogTitle.setText("Add Contact");
            buttonAddSaveContact.setText("Add Contact");
            addEditContactImageView.setImageResource(R.drawable.ic_default_contact_avatar);
        }

        buttonAddSaveContact.setOnClickListener(v -> handleAddSaveContact());
        buttonClearFields.setOnClickListener(v -> clearFields());
        buttonSelectImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void handleAddSaveContact() {
        String name = editTextContactName.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phoneNumber)) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please enter both name and phone number.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        EmergencyContact contactToSave;
        if (editingContact == null) {

            String id = UUID.randomUUID().toString();
            contactToSave = new EmergencyContact(id, name, phoneNumber, currentSelectedImageUrl);
        } else {
            editingContact.setName(name);
            editingContact.setPhoneNumber(phoneNumber);
            editingContact.setImageUrl(currentSelectedImageUrl);
            contactToSave = editingContact;
        }


        if (contactSavedListener != null) {
            contactSavedListener.onContactSaved(contactToSave);
        }
        dismiss();
    }

    private void clearFields() {
        editTextContactName.setText("");
        editTextPhoneNumber.setText("");
        currentSelectedImageUrl = null;
        addEditContactImageView.setImageResource(R.drawable.ic_default_contact_avatar);
        buttonAddSaveContact.setText("Add Contact");
        editingContact = null;
        if (getContext() != null) {
            Toast.makeText(getContext(), "Fields cleared.", Toast.LENGTH_SHORT).show();
        }
    }
}