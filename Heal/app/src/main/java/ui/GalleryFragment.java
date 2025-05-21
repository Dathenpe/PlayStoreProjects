package ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.heal.MainActivity;
import com.example.heal.R;

import viewmodels.GeneralViewModel;

public class GalleryFragment extends Fragment {

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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity.toolbar.setTitle("Art Corner");
        mainActivity.navigationView.setCheckedItem(R.id.nav_gallery);
        View loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        View galleryScrollView = view.findViewById(R.id.gallery_scroll_view);

        GeneralViewModel viewModel = new ViewModelProvider(this).get(GeneralViewModel.class);

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                galleryScrollView.setVisibility(View.GONE); // Hide content while loading
            } else {
                loadingProgressBar.setVisibility(View.GONE);
                galleryScrollView.setVisibility(View.VISIBLE); // Show content when loading finishes
            }
        });
    }
}