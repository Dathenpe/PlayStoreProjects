package Slider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.heal.R; // Replace with your actual R class path

public class SliderThree extends Fragment {

    public SliderThree() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_slider_two, container, false);
        // Assuming you have a layout file named fragment_slide_one.xml
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView titleTextView = view.findViewById(R.id.slideOneTitle);
        TextView descriptionTextView = view.findViewById(R.id.slideOneDescription);
        ImageView imageView = view.findViewById(R.id.slideOneBackground);

        // You can set data or perform other UI operations here
        if (titleTextView != null) {
            titleTextView.setText("Third Slide");
        }
        if (descriptionTextView != null) {
            descriptionTextView.setText("This is the content of the third slider page.");
        }
//        if (imageView != null) {
//            imageView.setImageResource(R.drawable.your_first_slide_image); // Replace with your image resource
//        }
    }
}