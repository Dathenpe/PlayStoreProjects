package records;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.heal.R;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder> {

    private List<EmergencyContact> contactList;
    private OnContactActionListener listener;

    public interface OnContactActionListener {
        void onEditClick(EmergencyContact contact);
        void onDeleteClick(EmergencyContact contact);
        void onImageClick(EmergencyContact contact);
        void onCallClick(EmergencyContact contact); // New: for making a call
    }

    public EmergencyContactsAdapter(List<EmergencyContact> contactList, OnContactActionListener listener) {
        this.contactList = contactList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContact contact = contactList.get(position);
        holder.nameTextView.setText(contact.getName());
        holder.phoneTextView.setText(contact.getPhoneNumber());

        if (contact.getImageUrl() != null && !contact.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(contact.getImageUrl())
                    .placeholder(R.drawable.ic_default_contact_avatar)
                    .error(R.drawable.ic_default_contact_avatar)
                    .into(holder.contactImageView);
        } else {
            holder.contactImageView.setImageResource(R.drawable.ic_default_contact_avatar);
        }

        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(contact);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(contact);
            }
        });

        holder.contactImageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(contact);
            }
        });

        // New: Set click listener for the call button
        holder.callButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCallClick(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public void updateContacts(List<EmergencyContact> newContacts) {
        this.contactList = newContacts;
        notifyDataSetChanged();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView phoneTextView;
        Button editButton;
        Button deleteButton;
        Button callButton; // Reference to the new call button
        CircleImageView contactImageView;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contactNameTextView);
            phoneTextView = itemView.findViewById(R.id.contactPhoneTextView);
            editButton = itemView.findViewById(R.id.editContactButton);
            deleteButton = itemView.findViewById(R.id.deleteContactButton);
            callButton = itemView.findViewById(R.id.callContactButton); // Initialize call button
            contactImageView = itemView.findViewById(R.id.contactImageView);
        }
    }
}