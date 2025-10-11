package com.f9ld3.Zion.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private Uri selectedImageUri = null;

    // Image picker launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    // Display selected image
                    Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(binding.imageProfile);

                    Toast.makeText(this, "Image selected. Click Save to upload.", Toast.LENGTH_SHORT).show();
                }
            }
    );

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

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        // Observe user profile
        editProfileViewModel.getUserProfile().observe(this, this::populateUi);

        // Observe save status
        editProfileViewModel.getSaveStatus().observe(this, status -> {
            switch (status) {
                case LOADING:
                    binding.buttonSave.setEnabled(false);
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
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

        // Observe re-authentication requirement
        editProfileViewModel.getReauthRequired().observe(this, message -> {
            if (message != null) {
                showReauthDialog(message);
                editProfileViewModel.clearReauthRequired();
            }
        });

        // Observe email update status
        editProfileViewModel.getEmailUpdateStatus().observe(this, statusMessage -> {
            if (statusMessage != null) {
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show();
                editProfileViewModel.clearEmailUpdateStatus();
            }
        });

        // Observe upload progress
        editProfileViewModel.getUploadProgress().observe(this, progress -> {
            if (progress != null && progress > 0) {
                // You could show a progress bar here
                // binding.uploadProgressBar.setProgress(progress);
            }
        });
    }

    private void setupClickListeners() {
        // Profile image edit button
        binding.fabEditImage.setOnClickListener(v -> openImagePicker());

        // Save button
        binding.buttonSave.setOnClickListener(v -> attemptSaveProfile());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void attemptSaveProfile() {
        String newUsername = binding.inputUsername.getText().toString().trim();
        String newEmail = binding.inputEmail.getText().toString().trim();

        // Validate username
        if (newUsername.isEmpty()) {
            binding.layoutUsername.setError("Username cannot be empty");
            return;
        }
        binding.layoutUsername.setError(null);

        // Validate email if provided
        if (!newEmail.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            binding.layoutEmailInput.setError("Invalid email format");
            return;
        }
        binding.layoutEmailInput.setError(null);

        // Check if email is being changed
        UserProfile currentProfile = editProfileViewModel.getUserProfile().getValue();
        boolean emailChanged = currentProfile != null &&
                !newEmail.isEmpty() &&
                !newEmail.equals(currentProfile.getEmail());

        if (emailChanged) {
            // Email is changing, will need password via dialog
            editProfileViewModel.saveProfile(newUsername, newEmail, selectedImageUri, null, this);
        } else {
            // No email change, proceed without password
            editProfileViewModel.saveProfile(newUsername, null, selectedImageUri, null, this);
        }
    }

    private void populateUi(UserProfile profile) {
        if (profile != null) {
            binding.inputUsername.setText(profile.getUsername());
            binding.inputEmail.setText(profile.getEmail());

            // Load profile image
            String imageUrl = profile.getProfileImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(binding.imageProfile);
            } else {
                binding.imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }
    }

    private void showReauthDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Re-authentication Required");
        builder.setMessage(message);

        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Current Password");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        passwordInput.setLayoutParams(lp);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 0, 50, 0);
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
            editProfileViewModel.saveProfile(newUsername, newEmail, selectedImageUri, currentPassword, this);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}