// MultipleFiles/SignUpActivity.java
package com.f9ld3.Zion.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.databinding.ActivitySignUpBinding;
import com.f9ld3.Zion.MainActivity;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        authViewModel.getAuthError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Sign Up Error: " + error, Toast.LENGTH_LONG).show();
                if (binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonSignUp.setEnabled(true);
                }
                authViewModel.clearAuthError();
            }
        });

        authViewModel.getAuthMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                // After registration, prompt to check email
                if (message.contains("verification")) {
                    if (binding != null) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.buttonSignUp.setEnabled(true);
                    }
                }
                authViewModel.clearMessages();
            }
        });

        authViewModel.isAuthenticated().observe(this, isAuthenticated -> {
            if (isAuthenticated != null && isAuthenticated) {
                // Note: Full access requires verification, but redirect to MainActivity for profile check
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });

        // Sign up button click - Matches XML: No confirmPassword
        if (binding != null) {
            binding.buttonSignUp.setOnClickListener(v -> {
                String username = binding.editTextUsername.getText().toString().trim();
                String email = binding.editTextEmail.getText().toString().trim();
                String password = binding.editTextPassword.getText().toString();

                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                    return;
                }

                binding.progressBar.setVisibility(View.VISIBLE);
                binding.buttonSignUp.setEnabled(false);

                authViewModel.createUserWithEmailAndPassword(email, password, username);
            });

            // Login link - Matches XML
            binding.textViewLogin.setOnClickListener(v -> {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding = null;
        }
    }
}