package com.f9ld3.Zion.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.auth.ChangePasswordActivity;
import com.f9ld3.Zion.auth.LoginActivity;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.dialogs.CustomInputDialogFragment;
import com.f9ld3.Zion.ui.profile.EditProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends PreferenceFragmentCompat {

    private AuthViewModel authViewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        setupAccountPreferences();
        setupAboutPreferences();
        // observeViewModel(); // This call is moved
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeViewModel(); // Observers are now set up here
    }

    private void setupAccountPreferences() {
        findPreference("edit_profile_shortcut").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), EditProfileActivity.class));
            return true;
        });

        findPreference("change_password").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), ChangePasswordActivity.class));
            return true;
        });

        findPreference("sign_out_shortcut").setOnPreferenceClickListener(preference -> {
            authViewModel.signOut();
            navigateToLogin();
            return true;
        });

        findPreference("delete_account").setOnPreferenceClickListener(preference -> {
            showDeleteAccountWarningDialog();
            return true;
        });
    }

    private void setupAboutPreferences() {
        findPreference("feedback_key").setOnPreferenceClickListener(preference -> {
            openFeedbackForm();
            return true;
        });

        findPreference("privacy_policy").setOnPreferenceClickListener(preference -> {
            openPrivacyPolicy();
            return true;
        });
    }

    private void observeViewModel() {
        authViewModel.getAccountDeleted().observe(getViewLifecycleOwner(), isDeleted -> {
            if (isDeleted != null && isDeleted) {
                Toast.makeText(requireContext(), "Account deleted successfully.", Toast.LENGTH_LONG).show();
                navigateToLogin();
                authViewModel.clearMessages();
            }
        });

        authViewModel.getAuthError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                authViewModel.clearMessages();
            }
        });
    }

    private void showDeleteAccountWarningDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = user != null ? user.getEmail() : "your email";

        CustomAlertDialogFragment warningDialog = CustomAlertDialogFragment.newInstance(
                "⚠️ Delete Account?",
                "This action is PERMANENT and IRREVERSIBLE.\n\n" +
                        "• All your data will be permanently deleted\n" +
                        "• Your profile and content will be removed\n" +
                        "• You cannot recover your account (" + userEmail + ")\n\n" +
                        "Are you absolutely sure you want to continue?",
                "Continue",
                "Cancel"
        );

        warningDialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                showDeleteAccountPasswordConfirmation();
            }

            @Override
            public void onNegativeClick() {
                // User cancelled
            }
        });

        warningDialog.show(getParentFragmentManager(), "DeleteAccountWarningDialog");
    }

    private void showDeleteAccountPasswordConfirmation() {
        CustomInputDialogFragment passwordDialog = CustomInputDialogFragment.newInstance(
                "Final Confirmation",
                "This is your last chance to cancel.\n\nEnter your password to permanently delete your account.",
                "Enter your password",
                "DELETE ACCOUNT",
                "Cancel",
                true // isPassword = true
        );

        passwordDialog.setInputListener(password -> {
            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Password is required.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Deleting account...", Toast.LENGTH_SHORT).show();
                authViewModel.deleteAccount(password);
            }
        });

        passwordDialog.show(getParentFragmentManager(), "DeleteAccountPasswordDialog");
    }


    private void navigateToLogin() {
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void openFeedbackForm() {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@zion.app"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Zion App Feedback");
        try {
            startActivity(Intent.createChooser(emailIntent, "Send feedback via..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "No email client installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPrivacyPolicy() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://your-website.com/privacy-policy"));
        try {
            startActivity(browserIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "No browser installed", Toast.LENGTH_SHORT).show();
        }
    }
}