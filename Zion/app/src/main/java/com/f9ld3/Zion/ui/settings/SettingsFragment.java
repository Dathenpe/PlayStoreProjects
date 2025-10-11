package com.f9ld3.Zion.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference;
import androidx.lifecycle.ViewModelProvider;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.auth.LoginActivity;
import com.f9ld3.Zion.auth.ChangePasswordActivity;
import com.f9ld3.Zion.ui.profile.EditProfileActivity;

/**
 * Fragment to display application settings using PreferenceFragmentCompat.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private AuthViewModel authViewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // Initialize AuthViewModel scoped to activity for consistent auth state
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Set up all preference click listeners
        setupEditProfilePreference();
        setupChangePasswordPreference();
        setupSignOutPreference();
        setupFeedbackPreference();
        setupPrivacyPolicyPreference();
    }

    /**
     * Set up Edit Profile preference
     */
    private void setupEditProfilePreference() {
        Preference editProfilePreference = findPreference("edit_profile_shortcut");
        if (editProfilePreference != null) {
            editProfilePreference.setOnPreferenceClickListener(preference -> {
                // Launch Edit Profile Activity
                Intent intent = new Intent(requireActivity(), EditProfileActivity.class);
                startActivity(intent);
                return true;
            });
        }
    }

    /**
     * Set up Change Password preference
     * FIXED: Now uses correct key "change_password" instead of "change_password_shortcut"
     */
    private void setupChangePasswordPreference() {
        Preference changePasswordPreference = findPreference("change_password");
        if (changePasswordPreference != null) {
            changePasswordPreference.setOnPreferenceClickListener(preference -> {
                // Launch Change Password Activity
                Intent intent = new Intent(requireActivity(), ChangePasswordActivity.class);
                startActivity(intent);
                return true;
            });
        }
    }

    /**
     * Set up Sign Out preference
     * FIXED: Now uses correct key "sign_out_shortcut"
     */
    private void setupSignOutPreference() {
        Preference signOutPreference = findPreference("sign_out_shortcut");
        if (signOutPreference != null) {
            signOutPreference.setOnPreferenceClickListener(preference -> {
                // Sign out user
                authViewModel.signOut();

                // Navigate to LoginActivity and clear back stack
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                requireActivity().finish();
                return true;
            });
        }
    }

    /**
     * Set up Feedback preference
     */
    private void setupFeedbackPreference() {
        Preference feedbackPreference = findPreference("feedback_key");
        if (feedbackPreference != null) {
            feedbackPreference.setOnPreferenceClickListener(preference -> {
                // TODO: Implement actual feedback mechanism
                // Example: Open email client or feedback form
                openFeedbackForm();
                return true;
            });
        }
    }

    /**
     * Set up Privacy Policy preference
     */
    private void setupPrivacyPolicyPreference() {
        Preference privacyPolicyPreference = findPreference("privacy_policy");
        if (privacyPolicyPreference != null) {
            privacyPolicyPreference.setOnPreferenceClickListener(preference -> {
                // TODO: Open privacy policy (web page or in-app viewer)
                openPrivacyPolicy();
                return true;
            });
        }
    }

    /**
     * Open feedback form - implement based on your needs
     */
    private void openFeedbackForm() {
        // Example: Open email client
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@zion.app"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Zion App Feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send feedback via..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "No email client installed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open privacy policy - implement based on your needs
     */
    private void openPrivacyPolicy() {
        // Example: Open web browser
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://your-website.com/privacy-policy"));

        try {
            startActivity(browserIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "No browser installed", Toast.LENGTH_SHORT).show();
        }
    }
}