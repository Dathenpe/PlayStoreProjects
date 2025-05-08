package ui;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
// Assuming you have a binding class for your SettingsFragment layout
import com.example.heal.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private View settingsRootView; // To hold the root view of the fragment

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        settingsRootView = binding.getRoot(); // Get the root view from the binding
        return settingsRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Now you can set the OnTouchListener on the fragment's root view
        settingsRootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Check if the touch is outside the desired view(s) within the fragment
                    View targetView = binding.getRoot(); // Replace with the ID of the view you want to check against
                    if (targetView != null && !isViewInside(targetView, (int) event.getRawX(), (int) event.getRawY())) {


                        return true; // Consume the event
                    }
                }
                return false; // Propagate the event
            }
        });
    }

    private boolean isViewInside(View view, int x, int y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        Rect rect = new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
        return rect.contains(x, y);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important to avoid memory leaks with ViewBinding
    }
}