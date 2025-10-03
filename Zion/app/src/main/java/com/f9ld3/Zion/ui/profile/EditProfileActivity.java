package com.f9ld3.Zion.ui.profile;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

        binding.buttonSave.setOnClickListener(v -> {
            String newUsername = binding.inputUsername.getText().toString().trim();
            if (newUsername.isEmpty()) {
                binding.layoutUsername.setError("Username cannot be empty");
                return;
            }
            binding.layoutUsername.setError(null);
            editProfileViewModel.saveProfile(newUsername, null);
        });
    }

    private void populateUi(UserProfile profile) {
        if (profile != null) {
            binding.inputUsername.setText(profile.getUsername());
            binding.textEmail.setText(profile.getEmail());

            Glide.with(this)
                    .load(profile.getProfileImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.imageProfile);
        }
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