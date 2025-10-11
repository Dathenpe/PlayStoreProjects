package com.f9ld3.Zion.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.f9ld3.Zion.databinding.ActivityChangePasswordBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup the toolbar for back navigation
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Change Password");
        }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Observer for successful password change message
        authViewModel.getAuthMessage().observe(this, message -> {
            if (message != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonChangePassword.setEnabled(true);
                showSuccessDialog(message);
                authViewModel.clearMessages();
            }
        });

        // Observer for errors
        authViewModel.getAuthError().observe(this, error -> {
            if (error != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonChangePassword.setEnabled(true);
                showDialog("Error", error, "OK", null);
                authViewModel.clearMessages();
            }
        });

        // Button click listener
        binding.buttonChangePassword.setOnClickListener(v -> attemptPasswordChange());
    }

    private void attemptPasswordChange() {
        String currentPassword = binding.editTextCurrentPassword.getText().toString();
        String newPassword = binding.editTextNewPassword.getText().toString();
        String confirmPassword = binding.editTextConfirmNewPassword.getText().toString();

        if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            showDialog("Input Error", "All fields are required.", "OK", null);
            return;
        }

        if (newPassword.length() < 6) {
            showDialog("Input Error", "New password must be at least 6 characters long.", "OK", null);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showDialog("Input Error", "New password and confirmation do not match.", "OK", null);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonChangePassword.setEnabled(false);

        authViewModel.changePassword(currentPassword, newPassword);
    }

    private void showDialog(String title, String message, String positiveBtn, String negativeBtn) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(title, message, positiveBtn, negativeBtn);
        dialog.show(getSupportFragmentManager(), "CustomAlertDialogFragment");
    }

    private void showSuccessDialog(String message) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance("Success!", message, "OK", null);
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                finish(); // Close the activity on success
            }
            @Override
            public void onNegativeClick() {
                finish();
            }
        });
        dialog.show(getSupportFragmentManager(), "SuccessDialog");
    }

    // Handle back button press in the action bar
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
