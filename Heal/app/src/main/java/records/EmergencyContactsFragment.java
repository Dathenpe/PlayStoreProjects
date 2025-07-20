package records;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ui.CustomMessageDialogFragment;

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
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: CopingExercisesFragment attached to wrong activity", Toast.LENGTH_SHORT).show();
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
        sortContactsAlphabetically();
        adapter.notifyDataSetChanged();
    }

    public void refreshContactsFromActivity(List<EmergencyContact> updatedList) {

        if (this.contactList == null) {
            this.contactList = new ArrayList<>();
        }
        this.contactList.clear();
        this.contactList.addAll(updatedList);
        sortContactsAlphabetically();
        adapter.updateContacts(this.contactList);
        updateEmptyStateVisibility();
    }

    private void sortContactsAlphabetically(){
        if (contactList != null && !contactList.isEmpty()) {
            Collections.sort(contactList, new Comparator<EmergencyContact>() {
                @Override
                public int compare(EmergencyContact c1, EmergencyContact c2) {
                    String name1 = c1.getName() != null ? c1.getName() : "";
                    String name2 = c2.getName() != null ? c2.getName() : "";
                    return name1.compareToIgnoreCase(name2);
                }
            });
        }
    }

    // --- OnContactActionListener methods (from RecyclerView adapter) ---
    @Override
    public void onEditClick(EmergencyContact contact) {
        AddEditContactDialogFragment dialogFragment = AddEditContactDialogFragment.newInstance(contact);
        dialogFragment.show(getParentFragmentManager(), "AddEditContactDialog");
    }

    @Override
    public void onDeleteClick(EmergencyContact contact) {
        // Replaced AlertDialog with CustomMessageDialogFragment
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Delete Contact",
                "Are you sure you want to delete " + contact.getName() + "? This action cannot be undone.",
                "Delete",
                "Cancel"
        );
        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).removeEmergencyContact(contact.getId());
                } else if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: Host activity not found for deletion.", Toast.LENGTH_SHORT).show();
                }

                if (getContext() != null) {
                    updateEmptyStateVisibility();
                    Toast.makeText(getContext(), contact.getName() + " deleted.", Toast.LENGTH_SHORT).show();
                }
                dialogFragment.dismiss();
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                dialogFragment.dismiss();
            }
        });
        dialog.show(getParentFragmentManager(), "DeleteContactDialog");
    }


    @Override
    public void onImageClick(EmergencyContact contact) {
        if (contact.getImageUrl() != null && !contact.getImageUrl().isEmpty()) {
            // Use the updated newInstance method of CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Image Viewer", // Title for the image viewer dialog
                    null, // No message text needed, as the image and name are in the custom view
                    "Close",
                    null, // No negative button
                    R.layout.dialog_image_viewer, // Pass the custom layout resource ID
                    contact.getImageUrl(), // Pass image URL
                    contact.getName() // Pass image name
            );

            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    dialogFragment.dismiss();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    // Not applicable for this dialog
                }
            });
            dialog.show(getParentFragmentManager(), "ImageViewerDialog");

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
        mainActivity.toolbar.setTitle("My Emergency Contacts");
        mainActivity.navigationView.setCheckedItem(R.id.nav_records);
        mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
        mainActivity.Fab.setVisibility(View.VISIBLE);
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
