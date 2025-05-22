package records;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
// import android.content.SharedPreferences; // Remove this if it's no longer used
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heal.MainActivity;
import com.example.heal.R;
import com.bumptech.glide.Glide;
// Remove Gson imports if no longer used here
// import com.google.gson.Gson;
// import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type; // Not needed if Gson imports are removed
import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // Not needed if AddEditContactDialogFragment handles ID generation

public class EmergencyContactsFragment extends Fragment implements EmergencyContactsAdapter.OnContactActionListener {

    private RecyclerView recyclerViewEmergencyContacts;
    private EmergencyContactsAdapter adapter;
    private List<EmergencyContact> contactList;

    private ActivityResultLauncher<String> requestCallPermissionLauncher;
    private EmergencyContact contactToCall;

    private AddEditContactDialogFragment.OnContactSavedListener onContactSavedListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AddEditContactDialogFragment.OnContactSavedListener) {
            onContactSavedListener = (AddEditContactDialogFragment.OnContactSavedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement AddEditContactDialogFragment.OnContactSavedListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            contactList = (List<EmergencyContact>) getArguments().getSerializable("contactList");
            if (contactList == null) {
                contactList = new ArrayList<>();
            }
        } else {
            contactList = new ArrayList<>();
        }

        requestCallPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if (contactToCall != null) {
                            makePhoneCall(contactToCall.getPhoneNumber());
                        }
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Call permission denied. Cannot make call.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    contactToCall = null;
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emergency_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewEmergencyContacts = view.findViewById(R.id.recyclerViewEmergencyContacts);

        adapter = new EmergencyContactsAdapter(contactList, this);
        recyclerViewEmergencyContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewEmergencyContacts.setAdapter(adapter);

        // --- ADD THIS LINE ---
        // Notify the adapter that the initial data set is ready
        adapter.notifyDataSetChanged();
        // Or, more explicitly using your custom update method:
        // adapter.updateContacts(contactList); // This would also work if updateContacts calls notifyDataSetChanged internally
        // Since updateContacts is already defined, let's use that for consistency.
        // If contactList is already holding the data from arguments, calling updateContacts here ensures it's displayed.
    }

    /**
     * Public method to refresh the contact list from MainActivity.
     * This is called by MainActivity after a contact is added/updated/deleted.
     */
    public void refreshContactsFromActivity(List<EmergencyContact> updatedList) {
        // Ensure the list is not null before clearing/adding
        if (this.contactList == null) {
            this.contactList = new ArrayList<>();
        }
        this.contactList.clear();
        this.contactList.addAll(updatedList);
        adapter.updateContacts(this.contactList); // This will call notifyDataSetChanged internally
    }

    // --- OnContactActionListener methods (from RecyclerView adapter) ---

    @Override
    public void onEditClick(EmergencyContact contact) {
        AddEditContactDialogFragment dialogFragment = AddEditContactDialogFragment.newInstance(contact);
        dialogFragment.show(getParentFragmentManager(), "AddEditContactDialog");
    }

    @Override
    public void onDeleteClick(EmergencyContact contact) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete " + contact.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Corrected approach for deletion: Delegate to MainActivity
                    if (getContext() instanceof MainActivity) {
                        ((MainActivity) getContext()).removeEmergencyContact(contact.getId());
                        // MainActivity's removeEmergencyContact will then call refreshContactsFromActivity
                        // on this fragment, so no need to manually update adapter here.
                    } else if (getContext() != null) {
                        Toast.makeText(getContext(), "Error: Host activity not found for deletion.", Toast.LENGTH_SHORT).show();
                    }

                    if (getContext() != null) {
                        Toast.makeText(getContext(), contact.getName() + " deleted.", Toast.LENGTH_SHORT).show();
                    }
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
            if (getContext() != null) {
                Toast.makeText(getContext(), "No image set for " + contact.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onCallClick(EmergencyContact contact) {
        contactToCall = contact;

        if (contact.getPhoneNumber() == null || contact.getPhoneNumber().isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "No phone number available for " + contact.getName(), Toast.LENGTH_SHORT).show();
            }
            contactToCall = null;
            return;
        }

        if (getContext() != null && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            makePhoneCall(contact.getPhoneNumber());
        } else {
            requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
        }
    }

    private void makePhoneCall(String phoneNumber) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + Uri.encode(phoneNumber)));
            startActivity(callIntent);
        } catch (SecurityException e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Call permission not granted. Please enable it in app settings.", Toast.LENGTH_LONG).show();
            }
            e.printStackTrace();
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Could not initiate call: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            e.printStackTrace();
        }
    }
}