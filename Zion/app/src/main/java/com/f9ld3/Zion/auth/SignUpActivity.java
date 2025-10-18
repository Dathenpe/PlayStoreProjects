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

        authViewModel.isAuthenticated().observe(this, authenticated -> {
            if (Boolean.TRUE.equals(authenticated)) {
                navigateToMainActivity();
            }
        });

        authViewModel.getAuthError().observe(this, error -> {
            if (error != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonSignUp.setEnabled(true);
                showDialog("Registration Failed", error, "OK", null);
                authViewModel.clearMessages();
            }
        });

        authViewModel.getAuthMessage().observe(this, message -> {
            if (message != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonSignUp.setEnabled(true);
                showSuccessDialog(message);
                authViewModel.clearMessages();
            }
        });

        binding.buttonSignUp.setOnClickListener(v -> attemptSignUp());
        binding.textViewLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void attemptSignUp() {
        String accountName = binding.editTextUsername.getText().toString().trim();
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showDialog("Input Error", "All fields are required.", "OK", null);
            return;
        }

        if (!authViewModel.isEmailValid(email)) {
            showDialog("Input Error", "Please enter a valid email address.", "OK", null);
            return;
        }

        if (!authViewModel.isUsernameValid(accountName)) {
            showDialog("Input Error", "Account Name must be 3-20 characters long and contain only letters, numbers, '.', '_', or '-'.", "OK", null);
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

        authViewModel.signUp(email, password, accountName);
    }

    private void showDialog(String title, String message, String positiveBtn, String negativeBtn) {
        if (isFinishing()) return;
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(title, message, positiveBtn, negativeBtn);
        dialog.show(getSupportFragmentManager(), "CustomAlertDialogFragment");
    }

    private void showSuccessDialog(String message) {
        if (isFinishing()) return;
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
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMainActivity() {
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
