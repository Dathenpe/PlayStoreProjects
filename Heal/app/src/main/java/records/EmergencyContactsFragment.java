package records;

import android.Manifest; // Import Manifest
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager; // Import PackageManager
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat; // Import ContextCompat
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.example.heal.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import com.bumptech.glide.Glide;

public class EmergencyContactsFragment extends Fragment implements EmergencyContactsAdapter.OnContactActionListener {

    private TextInputEditText editTextContactName;
    private TextInputEditText editTextPhoneNumber;
    private Button buttonAddSaveContact;
    private Button buttonClearFields;
    private RecyclerView recyclerViewEmergencyContacts;
    private EmergencyContactsAdapter adapter;
    private List<EmergencyContact> contactList;

    private EmergencyContact editingContact = null;

    private CircleImageView addEditContactImageView;
    private Button buttonSelectImage;
    private String currentSelectedImageUrl = null;

    private static final String PREFS_NAME = "EmergencyContactsPrefs";
    private static final String KEY_CONTACTS = "contactsList";

    private Gson gson;
    private ActivityResultLauncher<String> pickImageLauncher;

    // ActivityResultLauncher for requesting CALL_PHONE permission
    private ActivityResultLauncher<String> requestCallPermissionLauncher;

    private EmergencyContact contactToCall; // Temporary variable to hold contact for permission callback


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gson = new Gson();
        loadContacts();

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
                            getContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        } catch (SecurityException e) {
                            Toast.makeText(getContext(), "Permission to access image denied. Please try again.", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }

                    } else {
                        currentSelectedImageUrl = null;
                        addEditContactImageView.setImageResource(R.drawable.ic_default_contact_avatar);
                        Toast.makeText(getContext(), "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Initialize the ActivityResultLauncher for CALL_PHONE permission
        requestCallPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, proceed with the call
                        if (contactToCall != null) {
                            makePhoneCall(contactToCall.getPhoneNumber());
                        }
                    } else {
                        // Permission denied
                        Toast.makeText(getContext(), "Call permission denied. Cannot make call.", Toast.LENGTH_SHORT).show();
                    }
                    contactToCall = null; // Clear the temporary variable
                });
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_emergency_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTextContactName = view.findViewById(R.id.editTextContactName);
        editTextPhoneNumber = view.findViewById(R.id.editTextPhoneNumber);
        buttonAddSaveContact = view.findViewById(R.id.buttonAddSaveContact);
        buttonClearFields = view.findViewById(R.id.buttonClearFields);
        recyclerViewEmergencyContacts = view.findViewById(R.id.recyclerViewEmergencyContacts);

        addEditContactImageView = view.findViewById(R.id.addEditContactImageView);
        buttonSelectImage = view.findViewById(R.id.buttonSelectImage);

        adapter = new EmergencyContactsAdapter(contactList, this);
        recyclerViewEmergencyContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewEmergencyContacts.setAdapter(adapter);

        buttonAddSaveContact.setOnClickListener(v -> handleAddSaveContact());
        buttonClearFields.setOnClickListener(v -> clearFields());
        buttonSelectImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void handleAddSaveContact() {
        String name = editTextContactName.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(getContext(), "Please enter both name and phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingContact == null) {
            String id = UUID.randomUUID().toString();
            EmergencyContact newContact = new EmergencyContact(id, name, phoneNumber, currentSelectedImageUrl);
            contactList.add(newContact);
            Toast.makeText(getContext(), "Contact added!", Toast.LENGTH_SHORT).show();
        } else {
            editingContact.setName(name);
            editingContact.setPhoneNumber(phoneNumber);
            editingContact.setImageUrl(currentSelectedImageUrl);
            Toast.makeText(getContext(), "Contact updated!", Toast.LENGTH_SHORT).show();
            editingContact = null;
            buttonAddSaveContact.setText("Add Contact");
        }
        adapter.updateContacts(contactList);
        clearFields();
        saveContacts();
    }

    private void clearFields() {
        editTextContactName.setText("");
        editTextPhoneNumber.setText("");
        editingContact = null;
        currentSelectedImageUrl = null;
        addEditContactImageView.setImageResource(R.drawable.ic_default_contact_avatar);
        buttonAddSaveContact.setText("Add Contact");
    }

    @Override
    public void onEditClick(EmergencyContact contact) {
        editingContact = contact;
        editTextContactName.setText(contact.getName());
        editTextPhoneNumber.setText(contact.getPhoneNumber());
        currentSelectedImageUrl = contact.getImageUrl();

        if (currentSelectedImageUrl != null && !currentSelectedImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentSelectedImageUrl)
                    .placeholder(R.drawable.ic_default_contact_avatar)
                    .error(R.drawable.ic_default_contact_avatar)
                    .into(addEditContactImageView);
        } else {
            addEditContactImageView.setImageResource(R.drawable.ic_default_contact_avatar);
        }
        buttonAddSaveContact.setText("Save Changes");
        Toast.makeText(getContext(), "Editing: " + contact.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(EmergencyContact contact) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete " + contact.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    contactList.remove(contact);
                    adapter.updateContacts(contactList);
                    Toast.makeText(getContext(), contact.getName() + " deleted.", Toast.LENGTH_SHORT).show();
                    if (editingContact != null && editingContact.getId().equals(contact.getId())) {
                        clearFields();
                    }
                    saveContacts();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onImageClick(EmergencyContact contact) {
        if (contact.getImageUrl() != null && !contact.getImageUrl().isEmpty()) {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_image_viewer, null);

            ImageView fullScreenImageView = dialogView.findViewById(R.id.fullScreenImageView);
            TextView imageViewerNameTextView = dialogView.findViewById(R.id.imageViewerNameTextView);

            Glide.with(this)
                    .load(contact.getImageUrl())
                    .placeholder(R.drawable.ic_default_contact_avatar)
                    .error(R.drawable.ic_default_contact_avatar)
                    .into(fullScreenImageView);

            imageViewerNameTextView.setText(contact.getName());

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(dialogView);
            builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
            builder.show();

        } else {
            Toast.makeText(getContext(), "No image set for " + contact.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCallClick(EmergencyContact contact) {
        // Store the contact temporarily in case permission needs to be requested
        contactToCall = contact;

        if (contact.getPhoneNumber() == null || contact.getPhoneNumber().isEmpty()) {
            Toast.makeText(getContext(), "No phone number available for " + contact.getName(), Toast.LENGTH_SHORT).show();
            contactToCall = null; // Clear temporary variable
            return;
        }

        // Check if CALL_PHONE permission is already granted
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with the call
            makePhoneCall(contact.getPhoneNumber());
        } else {
            // Permission not granted, request it from the user
            requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
        }
    }

    private void makePhoneCall(String phoneNumber) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL); // ACTION_CALL for direct dial
            // Use Uri.encode to ensure the phone number is correctly formatted, especially if it contains special characters
            callIntent.setData(Uri.parse("tel:" + Uri.encode(phoneNumber)));
            startActivity(callIntent);
        } catch (SecurityException e) {
            Toast.makeText(getContext(), "Call permission not granted. Please enable it in app settings.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not initiate call: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    private void saveContacts() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(contactList);
        editor.putString(KEY_CONTACTS, json);
        editor.apply();
    }

    private void loadContacts() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_CONTACTS, null);

        if (json == null) {
            contactList = new ArrayList<>();
        } else {
            Type type = new TypeToken<ArrayList<EmergencyContact>>() {}.getType();
            contactList = gson.fromJson(json, type);
        }
    }
}