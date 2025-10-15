package com.f9ld3.Zion.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
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
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.dialogs.CustomInputDialogFragment;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private EditProfileViewModel editProfileViewModel;
    private Uri selectedImageUri = null;
    private boolean imageRemoved = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imageRemoved = false;
                    Glide.with(this).load(selectedImageUri).placeholder(R.drawable.ic_profile_placeholder).into(binding.imageProfile);
                    binding.buttonRemoveImage.setVisibility(View.VISIBLE);
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
        editProfileViewModel.getUserProfile().observe(this, this::populateUi);
        editProfileViewModel.getSaveStatus().observe(this, status -> {
            binding.progressBar.setVisibility(status == EditProfileViewModel.SaveStatus.LOADING ? View.VISIBLE : View.GONE);
            binding.buttonSave.setEnabled(status != EditProfileViewModel.SaveStatus.LOADING);
            if (status == EditProfileViewModel.SaveStatus.SUCCESS) {
                Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        editProfileViewModel.getReauthRequired().observe(this, message -> {
            if (message != null) {
                showReauthDialog(message);
                editProfileViewModel.clearReauthRequired();
            }
        });
        editProfileViewModel.getEmailUpdateStatus().observe(this, statusMessage -> {
            if (statusMessage != null) {
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show();
                editProfileViewModel.clearEmailUpdateStatus();
            }
        });
        editProfileViewModel.getUploadError().observe(this, error -> {
            if(error != null){
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                editProfileViewModel.clearUploadError();
            }
        });
    }

    private void setupClickListeners() {
        binding.fabEditImage.setOnClickListener(v -> openImagePicker());
        binding.buttonRemoveImage.setOnClickListener(v -> showRemoveImageConfirmation());
        binding.buttonSave.setOnClickListener(v -> attemptSaveProfile());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void showRemoveImageConfirmation() {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Remove Profile Picture?",
                "Are you sure you want to remove your profile picture?",
                "Remove",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                removeProfileImage();
            }
            @Override
            public void onNegativeClick() {}
        });
        dialog.show(getSupportFragmentManager(), "RemoveImageDialog");
    }

    private void removeProfileImage() {
        selectedImageUri = null;
        imageRemoved = true;
        binding.imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
        binding.buttonRemoveImage.setVisibility(View.GONE);
    }

    private void attemptSaveProfile() {
        String newUsername = binding.inputUsername.getText().toString().trim();
        String newEmail = binding.inputEmail.getText().toString().trim();

        if (newUsername.isEmpty()) {
            binding.layoutUsername.setError("Username cannot be empty");
            return;
        }
        binding.layoutUsername.setError(null);

        editProfileViewModel.saveProfile(newUsername, newEmail,
                imageRemoved ? Uri.parse("REMOVE_IMAGE") : selectedImageUri,
                null, this);
    }

    private void populateUi(UserProfile profile) {
        if (profile != null) {
            binding.inputUsername.setText(profile.getUsername());
            binding.inputEmail.setText(profile.getEmail());
            if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
                Glide.with(this).load(profile.getProfileImageUrl()).placeholder(R.drawable.ic_profile_placeholder).into(binding.imageProfile);
                binding.buttonRemoveImage.setVisibility(View.VISIBLE);
            } else {
                binding.imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
                binding.buttonRemoveImage.setVisibility(View.GONE);
            }
        }
    }

    private void showReauthDialog(String message) {
        CustomInputDialogFragment dialog = CustomInputDialogFragment.newInstance(
                "Re-authentication Required", message, "Current Password", "Confirm", "Cancel", true);

        dialog.setInputListener(currentPassword -> {
            String newUsername = binding.inputUsername.getText().toString().trim();
            String newEmail = binding.inputEmail.getText().toString().trim();
            editProfileViewModel.saveProfile(newUsername, newEmail,
                    imageRemoved ? Uri.parse("REMOVE_IMAGE") : selectedImageUri,
                    currentPassword, this);
        });

        dialog.show(getSupportFragmentManager(), "ReauthInputDialog");
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