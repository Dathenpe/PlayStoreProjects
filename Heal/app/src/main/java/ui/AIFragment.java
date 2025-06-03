package ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.heal.MainActivity;
import com.example.heal.R;

import viewmodels.GeneralViewModel;

public class AIFragment extends Fragment {

    private MainActivity mainActivity;
    private Context context;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: Fragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.toolbar.setTitle("Game Room");
        mainActivity.navigationView.setCheckedItem(R.id.nav_ai);

        View loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        View aiCoordinatorLayout = view.findViewById(R.id.ai_chat_coordinator_layout);

        GeneralViewModel viewModel = new ViewModelProvider(this).get(GeneralViewModel.class);

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                aiCoordinatorLayout.setVisibility(View.GONE); // Hide content while loading
            } else {
                loadingProgressBar.setVisibility(View.GONE);
                aiCoordinatorLayout.setVisibility(View.VISIBLE); // Show content when loading finishes
            }
        });
    }
}