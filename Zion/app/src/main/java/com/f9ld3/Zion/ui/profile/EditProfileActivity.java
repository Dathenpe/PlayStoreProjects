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
    private Uri selectedBannerUri = null;
    private boolean imageRemoved = false;
    private boolean bannerRemoved = false;

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

    private final ActivityResultLauncher<Intent> bannerPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedBannerUri = result.getData().getData();
                    bannerRemoved = false;
                    Glide.with(this).load(selectedBannerUri).placeholder(R.drawable.ic_banner_placeholder).into(binding.imageBanner);
                    binding.buttonRemoveBanner.setVisibility(View.VISIBLE);
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
            if (message != null && !isFinishing()) {
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
        // CHANGED: Listen on the image itself, not the old FAB
        binding.imageProfile.setOnClickListener(v -> openImagePicker());
        // CHANGED: Listen on the image itself, not the old FAB
        binding.imageBanner.setOnClickListener(v -> openBannerPicker());

        // These are still correct
        binding.buttonRemoveImage.setOnClickListener(v -> showRemoveImageConfirmation());
        binding.buttonRemoveBanner.setOnClickListener(v -> showRemoveBannerConfirmation());
        binding.buttonSave.setOnClickListener(v -> attemptSaveProfile());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void openBannerPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        bannerPickerLauncher.launch(intent);
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

    private void showRemoveBannerConfirmation() {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Remove Banner Image?",
                "Are you sure you want to remove your banner image?",
                "Remove",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                removeBannerImage();
            }
            @Override
            public void onNegativeClick() {}
        });
        dialog.show(getSupportFragmentManager(), "RemoveBannerDialog");
    }

    private void removeProfileImage() {
        selectedImageUri = null;
        imageRemoved = true;
        binding.imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
        binding.buttonRemoveImage.setVisibility(View.GONE);
    }

    private void removeBannerImage() {
        selectedBannerUri = null;
        bannerRemoved = true;
        binding.imageBanner.setImageResource(R.drawable.ic_banner_placeholder);
        binding.buttonRemoveBanner.setVisibility(View.GONE);
    }


    private void attemptSaveProfile() {
        String newAccountName = binding.inputAccountName.getText().toString().trim();
        String newUsername = binding.inputUsername.getText().toString().trim();
        String newBio = binding.inputBio.getText().toString().trim();
        String newEmail = binding.inputEmail.getText().toString().trim();

        if (newAccountName.isEmpty()) {
            binding.layoutAccountName.setError("Account Name cannot be empty");
            return;
        }
        binding.layoutAccountName.setError(null);

        editProfileViewModel.saveProfile(newAccountName, newUsername, newEmail, newBio,
                imageRemoved ? Uri.parse("REMOVE_IMAGE") : selectedImageUri,
                bannerRemoved ? Uri.parse("REMOVE_BANNER") : selectedBannerUri,
                null, this);
    }

    private void populateUi(UserProfile profile) {
        if (profile != null) {
            binding.inputAccountName.setText(profile.getAccountName());
            binding.inputUsername.setText(profile.getUsername());
            binding.inputBio.setText(profile.getBio());
            binding.inputEmail.setText(profile.getEmail());
            if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
                Glide.with(this).load(profile.getProfileImageUrl()).placeholder(R.drawable.ic_profile_placeholder).into(binding.imageProfile);
                binding.buttonRemoveImage.setVisibility(View.VISIBLE);
            } else {
                binding.imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
                binding.buttonRemoveImage.setVisibility(View.GONE);
            }
            if (profile.getBannerImageUrl() != null && !profile.getBannerImageUrl().isEmpty()) {
                Glide.with(this).load(profile.getBannerImageUrl()).placeholder(R.drawable.ic_banner_placeholder).into(binding.imageBanner);
                binding.buttonRemoveBanner.setVisibility(View.VISIBLE);
            } else {
                binding.imageBanner.setImageResource(R.drawable.ic_banner_placeholder);
                binding.buttonRemoveBanner.setVisibility(View.GONE);
            }
        }
    }

    private void showReauthDialog(String message) {
        if (isFinishing()) return;
        CustomInputDialogFragment dialog = CustomInputDialogFragment.newInstance(
                "Re-authentication Required", message, "Current Password", "Confirm", "Cancel", true);

        dialog.setInputListener(currentPassword -> {
            String newAccountName = binding.inputAccountName.getText().toString().trim();
            String newUsername = binding.inputUsername.getText().toString().trim();
            String newBio = binding.inputBio.getText().toString().trim();
            String newEmail = binding.inputEmail.getText().toString().trim();
            editProfileViewModel.saveProfile(newAccountName, newUsername, newEmail, newBio,
                    imageRemoved ? Uri.parse("REMOVE_IMAGE") : selectedImageUri,
                    bannerRemoved ? Uri.parse("REMOVE_BANNER") : selectedBannerUri,
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
