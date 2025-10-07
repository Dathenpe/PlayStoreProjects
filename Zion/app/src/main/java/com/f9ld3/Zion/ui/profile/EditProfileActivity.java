// MultipleFiles/EditProfileActivity.java
package com.f9ld3.Zion.ui.profile;

import android.app.AlertDialog; // NEW
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText; // NEW
import android.widget.LinearLayout; // NEW
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.data.UserProfile;
import com.f9ld3.Zion.databinding.ActivityEditProfileBinding;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private EditProfileViewModel editProfileViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        editProfileViewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.edit_profile);
        }

        editProfileViewModel.getUserProfile().observe(this, this::populateUi);

        editProfileViewModel.getSaveStatus().observe(this, status -> {
            switch (status) {
                case LOADING:
                    binding.buttonSave.setEnabled(false);
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity on success
                    break;
                case FAILED:
                    Toast.makeText(this, "Failed to save profile.", Toast.LENGTH_SHORT).show();
                    binding.buttonSave.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    break;
                default:
                    binding.buttonSave.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    break;
            }
        });

        // NEW: Observe re-authentication requirement
        editProfileViewModel.getReauthRequired().observe(this, message -> {
            if (message != null) {
                showReauthDialog(message);
                editProfileViewModel.clearReauthRequired(); // Clear the message after showing
            }
        });

        // NEW: Observe email update status messages
        editProfileViewModel.getEmailUpdateStatus().observe(this, statusMessage -> {
            if (statusMessage != null) {
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show();
                editProfileViewModel.clearEmailUpdateStatus(); // Clear the message after showing
            }
        });


        binding.buttonSave.setOnClickListener(v -> {
            String newUsername = binding.inputUsername.getText().toString().trim();
            String newEmail = binding.inputEmail.getText().toString().trim(); // NEW: Get new email from input

            if (newUsername.isEmpty()) {
                binding.layoutUsername.setError("Username cannot be empty");
                return;
            }
            binding.layoutUsername.setError(null);

            // Pass null for newImageUri and currentPassword initially.
            // currentPassword will be requested via dialog if email changes.
            editProfileViewModel.saveProfile(newUsername, newEmail, null, null);
        });
    }

    private void populateUi(UserProfile profile) {
        if (profile != null) {
            binding.inputUsername.setText(profile.getUsername());
            binding.inputEmail.setText(profile.getEmail()); // NEW: Set current email to the input field

            Glide.with(this)
                    .load(profile.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.imageProfile);
        }
    }

    // NEW: Dialog to prompt for current password for re-authentication
    private void showReauthDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Re-authentication Required");
        builder.setMessage(message);

        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Current Password");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        passwordInput.setLayoutParams(lp);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 0, 50, 0); // Add some padding
        container.addView(passwordInput);
        builder.setView(container);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String currentPassword = passwordInput.getText().toString();
            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Password cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            String newUsername = binding.inputUsername.getText().toString().trim();
            String newEmail = binding.inputEmail.getText().toString().trim();
            editProfileViewModel.saveProfile(newUsername, newEmail, null, currentPassword);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            binding.buttonSave.setEnabled(true);
            binding.progressBar.setVisibility(View.GONE);
        });
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}