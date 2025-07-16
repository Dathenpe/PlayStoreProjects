package ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;

import drawing.DrawingCanvasFragment;
import funcorner.ArtCornerFragment;
import funcorner.TetrisGameFragment;
import funcorner.MemoryMatchGameFragment;
import funcorner.WordScrambleGameFragment; // Import the new WordScrambleGameFragment
import viewmodels.GeneralViewModel;

public class FunCornerFragment extends Fragment implements DrawingCanvasFragment.OnDrawingSavedListener {

    private static final String TAG = "FunCornerFragment";
    private MainActivity mainActivity;
    private FrameLayout tetrisGameContainer;
    private FrameLayout artCornerCardContainer;
    private FrameLayout memoryMatchGameContainer;
    private FrameLayout wordScrambleGameContainer; // Declare the Word Scramble game container

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: FunCornerFragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fun_corner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("Fun Corner");
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }

        // Initialize the clickable containers (MaterialCardViews acting as FrameLayouts)
        tetrisGameContainer = view.findViewById(R.id.tetris_game_container);
        artCornerCardContainer = view.findViewById(R.id.art_corner_card_container);
        memoryMatchGameContainer = view.findViewById(R.id.memory_match_game_container);
        wordScrambleGameContainer = view.findViewById(R.id.word_scramble_game_container); // Initialize the Word Scramble game container

        ProgressBar loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        View funCornerScrollView = view.findViewById(R.id.fun_corner_scroll_view);

        GeneralViewModel viewModel = new ViewModelProvider(this).get(GeneralViewModel.class);
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                funCornerScrollView.setVisibility(View.GONE);
            } else {
                loadingProgressBar.setVisibility(View.GONE);
                funCornerScrollView.setVisibility(View.VISIBLE);
            }
        });

        tetrisGameContainer.setOnClickListener(v -> {
            mainActivity.loadFragment(new TetrisGameFragment(), R.id.nav_gallery);
            mainActivity.toolbar.setTitle("Tetris Game");
            mainActivity.addFragmentToHistory(R.id.nav_gallery, "Tetris Game");
        });

        artCornerCardContainer.setOnClickListener(v -> {
            mainActivity.loadFragment(new ArtCornerFragment(), R.id.nav_gallery);
            mainActivity.toolbar.setTitle("Art Corner");
            mainActivity.addFragmentToHistory(R.id.nav_gallery, "Art Corner");
        });

        memoryMatchGameContainer.setOnClickListener(v -> {
            mainActivity.loadFragment(new MemoryMatchGameFragment(), R.id.nav_gallery);
            mainActivity.toolbar.setTitle("Memory Match Game");
            mainActivity.addFragmentToHistory(R.id.nav_gallery, "Memory Match Game");
        });

        // Set OnClickListener for the Word Scramble game container
        wordScrambleGameContainer.setOnClickListener(v -> {
            mainActivity.loadFragment(new WordScrambleGameFragment(), R.id.nav_gallery);
            mainActivity.toolbar.setTitle("Word Scramble Game");
            mainActivity.addFragmentToHistory(R.id.nav_gallery, "Word Scramble Game");
        });
    }

    @Override
    public void onDrawingSaved(String imageUri, String artworkName) {
        if (mainActivity != null) {
            Toast.makeText(mainActivity, "Artwork saved: " + artworkName, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("Fun Corner");
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mainActivity != null) {
            mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
            mainActivity.Fab.setVisibility(View.VISIBLE);
            mainActivity.shakeView(mainActivity.Fab);
        }
    }
}
