// MultipleFiles/LoginActivity.java
package com.f9ld3.Zion.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView; // NEW: Import TextView
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.auth.SignUpActivity;
import com.f9ld3.Zion.databinding.ActivityLoginBinding;
import com.f9ld3.Zion.MainActivity;
import com.google.firebase.auth.FirebaseAuth; // NEW: Import FirebaseAuth

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private FirebaseAuth mAuth; // NEW: FirebaseAuth instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // NEW: Check if user is already logged in (from a previous session)
        if (mAuth.getCurrentUser () != null && !mAuth.getCurrentUser ().isAnonymous()) {
            // If a registered user is already logged in, navigate directly to MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return; // Prevent further execution of onCreate
        }

        authViewModel.getAuthError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Login Error: " + error, Toast.LENGTH_LONG).show();
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonLogin.setEnabled(true);
                authViewModel.clearAuthError();
            }
        });

        authViewModel.isAuthenticated().observe(this, isAuthenticated -> {
            if (isAuthenticated != null && isAuthenticated) {
                startActivity(new Intent(this, MainActivity.class));// MultipleFiles/LoginActivity.java (continued from previous)
                finish();
            }
        });

        // Observe auth messages (e.g., for password reset)
        authViewModel.getAuthMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                authViewModel.clearMessages(); // Clear after display
            }
        });

        // Login button click
        binding.buttonLogin.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonLogin.setEnabled(false);

            authViewModel.loginWithEmailAndPassword(email, password);
        });

        // Sign up link
        binding.textViewSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });

        // Forgot password link
        binding.textViewForgotPassword.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Please enter your email first.", Toast.LENGTH_SHORT).show();
                return;
            }
            binding.progressBar.setVisibility(View.VISIBLE);
            authViewModel.sendPasswordResetEmail(email);
            // Reset UI after sending
            binding.progressBar.setVisibility(View.GONE);
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding = null;
        }
    }
}