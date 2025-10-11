package com.f9ld3.Zion.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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

        // Check if we're being recreated after navigation
        if (savedInstanceState != null) {
            hasNavigated = savedInstanceState.getBoolean("hasNavigated", false);
            if (hasNavigated) {
                Log.d(TAG, "Activity recreated after navigation, finishing immediately");
                finish();
                return;
            }
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Check if already authenticated on startup
        Boolean isAuth = authViewModel.isAuthenticated().getValue();
        if (Boolean.TRUE.equals(isAuth) && !hasNavigated) {
            Log.d(TAG, "Already authenticated on create, navigating immediately");
            navigateToMainActivity();
            return;
        }

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        // CRITICAL: Observe authentication state changes
        authViewModel.isAuthenticated().observe(this, isAuth -> {
            Log.d(TAG, "isAuthenticated: " + isAuth + ", hasNavigated: " + hasNavigated + ", isFinishing: " + isFinishing());

            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                Log.d(TAG, "Activity not in valid state, skipping navigation");
                return;
            }


            if (Boolean.TRUE.equals(isAuth) && !hasNavigated && !isFinishing()) {
                Log.d(TAG, "Navigating to MainActivity");
                navigateToMainActivity();
            }
        });

        // Error observer
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

        // Message observer
        authViewModel.getAuthMessage().observe(this, message -> {
            if (message != null && !isFinishing()) {
                showDialog("Success", message, "OK", null);
                authViewModel.clearMessages();
            }
        });

        // Password reset observer
        authViewModel.getPasswordResetSent().observe(this, sent -> {
            if (Boolean.TRUE.equals(sent) && !isFinishing()) {
                showDialog("Password Reset", "A reset link has been sent to your inbox.", "OK", null);
                authViewModel.clearMessages();
            }
        });
    }

    private void setupClickListeners() {
        // Login button
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

        // Forgot password
        binding.textViewForgotPassword.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                showDialog("Input Required", "Please enter your email address first.", "OK", null);
                return;
            }
            authViewModel.sendPasswordResetEmail(email);
        });

        // Sign up navigation
        binding.textViewSignUp.setOnClickListener(v -> {
            if (!hasNavigated) {
                startActivity(new Intent(this, SignUpActivity.class));
            }
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
        dialog.show(getSupportFragmentManager(), "VerificationDialog");
    }

    private void navigateToMainActivity() {
        if (hasNavigated || isFinishing()) {
            Log.d(TAG, "Navigation blocked - hasNavigated: " + hasNavigated + ", isFinishing: " + isFinishing());
            return;
        }

        hasNavigated = true;
        Log.d(TAG, "navigateToMainActivity executing");

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("hasNavigated", hasNavigated);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        binding = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called, hasNavigated: " + hasNavigated);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called, hasNavigated: " + hasNavigated);
    }
}