package com.f9ld3.Zion.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import com.f9ld3.Zion.MainActivity;
import com.f9ld3.Zion.databinding.ActivityLoginBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            hasNavigated = savedInstanceState.getBoolean("hasNavigated", false);
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        authViewModel.isAuthenticated().observe(this, isAuth -> {
            if (Boolean.TRUE.equals(isAuth) && !hasNavigated && !isFinishing()) {
                navigateToMainActivity();
            }
        });

        authViewModel.getAuthError().observe(this, error -> {
            if (error != null && !isFinishing()) {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonLogin.setEnabled(true);
                if (error.contains("verify your email") || error.contains("not verified")) {
                    showVerificationDialog(error);
                } else {
                    showDialog("Login Failed", error, "OK", null);
                }
                authViewModel.clearMessages();
            }
        });

        authViewModel.getAuthMessage().observe(this, message -> {
            if (message != null && !isFinishing()) {
                showDialog("Success", message, "OK", null);
                authViewModel.clearMessages();
            }
        });

        authViewModel.getPasswordResetSent().observe(this, sent -> {
            if (Boolean.TRUE.equals(sent) && !isFinishing()) {
                showDialog("Password Reset", "A reset link has been sent to your inbox.", "OK", null);
                authViewModel.clearMessages();
            }
        });
    }

    private void setupClickListeners() {
        binding.buttonLogin.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                showDialog("Input Required", "Email and password cannot be empty.", "OK", null);
                return;
            }
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonLogin.setEnabled(false);
            authViewModel.signIn(email, password);
        });

        binding.textViewForgotPassword.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                showDialog("Input Required", "Please enter your email address first.", "OK", null);
                return;
            }
            authViewModel.sendPasswordResetEmail(email);
        });

        binding.textViewSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });
    }

    private void showDialog(String title, String message, String positiveBtn, String negativeBtn) {
        if (!isFinishing()) {
            CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(title, message, positiveBtn, negativeBtn);
            dialog.show(getSupportFragmentManager(), "CustomAlertDialogFragment");
        }
    }

    private void showVerificationDialog(String message) {
        if (isFinishing()) return;

        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance("Email Not Verified", message, "Resend", "OK");
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                String email = binding.editTextEmail.getText().toString().trim();
                String password = binding.editTextPassword.getText().toString();
                if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                    authViewModel.resendVerificationFromLogin(email, password);
                } else {
                    showDialog("Error", "Please fill in email and password to resend.", "OK", null);
                }
            }
            @Override
            public void onNegativeClick() { }
        });
        if (!isFinishing()) {
            dialog.show(getSupportFragmentManager(), "VerificationDialog");
        }
    }

    private void navigateToMainActivity() {
        if (hasNavigated || isFinishing()) {
            return;
        }
        hasNavigated = true;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("hasNavigated", hasNavigated);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
