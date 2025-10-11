package com.f9ld3.Zion.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.f9ld3.Zion.MainActivity;
import com.f9ld3.Zion.databinding.ActivitySignUpBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Check for immediate navigation if already authenticated and verified
        if (authViewModel.getCurrentUser().getValue() != null && authViewModel.getCurrentUser().getValue().isEmailVerified()) {
            navigateToMainActivity();
            return;
        }

        authViewModel.getAuthError().observe(this, error -> {
            if (error != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonSignUp.setEnabled(true);
                // UPDATED: Use custom dialog for errors
                showDialog("Registration Failed", error, "OK", null);
                authViewModel.clearMessages(); // Clear immediately
            }
        });

        authViewModel.getAuthMessage().observe(this, message -> {
            if (message != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonSignUp.setEnabled(true);
                // Show success dialog and offer to navigate to login
                showSuccessDialog(message);
                authViewModel.clearMessages();
            }
        });

        // Sign Up Button Click
        binding.buttonSignUp.setOnClickListener(v -> attemptSignUp());

        // Navigate to Login
        binding.textViewLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void attemptSignUp() {
        String username = binding.editTextUsername.getText().toString().trim();
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showDialog("Input Error", "All fields are required.", "OK", null);
            return;
        }

        if (!authViewModel.isEmailValid(email)) {
            showDialog("Input Error", "Please enter a valid email address.", "OK", null);
            return;
        }

        if (!authViewModel.isUsernameValid(username)) {
            showDialog("Input Error", "Username must be 3-20 characters long and contain only letters, numbers, '.', '_', or '-'.", "OK", null);
            return;
        }

        if (password.length() < 6) {
            showDialog("Input Error", "Password must be at least 6 characters long.", "OK", null);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showDialog("Input Error", "Password and Confirm Password do not match.", "OK", null);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonSignUp.setEnabled(false);

        authViewModel.signUp(email, password, username);
    }

    private void showDialog(String title, String message, String positiveBtn, String negativeBtn) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(title, message, positiveBtn, negativeBtn);
        dialog.show(getSupportFragmentManager(), "CustomAlertDialogFragment");
    }

    private void showSuccessDialog(String message) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance("Success!", message, "Go to Login", null);
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                navigateToLogin();
            }
            @Override
            public void onNegativeClick() {}
        });
        dialog.show(getSupportFragmentManager(), "SuccessDialog");
    }

    private void navigateToLogin() {
        // Ensure to clear the task stack
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMainActivity() {
        // Helper to navigate to Main if authenticated (e.g., if user returns while authenticated)
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
