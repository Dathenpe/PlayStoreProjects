package com.f9ld3.Zion.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.FragmentListNoToolbarBinding; // IMPORTANT: Use the new binding

public class NotificationsFragment extends Fragment {

    private FragmentListNoToolbarBinding binding; // IMPORTANT: Use the new binding

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListNoToolbarBinding.inflate(inflater, container, false); // IMPORTANT: Use the new binding
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.textPlaceholder.setText(R.string.notifications_empty_text);
        binding.textPlaceholder.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_notifications_24dp, 0, 0);

        // For now, always show the empty state.
        binding.recyclerView.setVisibility(View.GONE);
        binding.textPlaceholder.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}