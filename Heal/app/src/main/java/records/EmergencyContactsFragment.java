package records;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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

import com.bumptech.glide.Glide;
import com.example.heal.MainActivity;
import com.example.heal.R;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsFragment extends Fragment implements EmergencyContactsAdapter.OnContactActionListener {

    private RecyclerView recyclerViewEmergencyContacts;
    private EmergencyContactsAdapter adapter;
    List<EmergencyContact> contactList;

    private ActivityResultLauncher<String> requestCallPermissionLauncher;
    private EmergencyContact contactToCall;

    private TextView emptyStateTextView;

    private AddEditContactDialogFragment.OnContactSavedListener onContactSavedListener;

    private MainActivity mainActivity;

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
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
        updateEmptyStateVisibility();
        adapter.notifyDataSetChanged();
    }

    public void refreshContactsFromActivity(List<EmergencyContact> updatedList) {

        if (this.contactList == null) {
            this.contactList = new ArrayList<>();
        }
        this.contactList.clear();
        this.contactList.addAll(updatedList);
        adapter.updateContacts(this.contactList);
        updateEmptyStateVisibility();
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
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("Are you sure you want to delete " + contact.getName() + "?, this action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Corrected approach for deletion: Delegate to MainActivity
                    if (getContext() instanceof MainActivity) {
                        ((MainActivity) getContext()).removeEmergencyContact(contact.getId());
                    } else if (getContext() != null) {
                        Toast.makeText(getContext(), "Error: Host activity not found for deletion.", Toast.LENGTH_SHORT).show();
                    }

                    if (getContext() != null) {
                        updateEmptyStateVisibility();
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
    @Override
    public void onResume(){
        super.onResume();
        updateEmptyStateVisibility();
    }
    private void updateEmptyStateVisibility() {
        if (contactList.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            emptyStateTextView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            recyclerViewEmergencyContacts.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            recyclerViewEmergencyContacts.setVisibility(View.VISIBLE);
        }
    }
}