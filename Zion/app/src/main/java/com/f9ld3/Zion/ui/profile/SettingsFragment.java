package com.f9ld3.Zion.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentFullPageListBinding; // Reusing generic list layout

/**
 * Fragment to display application settings.
 */
public class SettingsFragment extends Fragment {

    private FragmentFullPageListBinding binding; // Reusing this layout for simplicity

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFullPageListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Customize the empty state for this page (or populate with actual settings)
        binding.textPlaceholder.setText(getString(R.string.settings_empty_text));
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_dashboard_black_24dp, 0, 0); // Placeholder icon

        // In a real app, you would populate a RecyclerView or a PreferenceFragment here
        // For now, we just show the placeholder.
        binding.recyclerView.setVisibility(View.GONE);
        binding.textPlaceholder.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}