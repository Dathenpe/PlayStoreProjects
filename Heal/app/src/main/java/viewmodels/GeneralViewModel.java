package viewmodels; // A dedicated package for ViewModels is good practice

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GeneralViewModel extends ViewModel {

    // LiveData to hold the actual data for the UI
    private final MutableLiveData<String> _homeData = new MutableLiveData<>();
    public LiveData<String> homeData = _homeData;

    // LiveData to indicate the loading state
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    public GeneralViewModel() {
        // When the ViewModel is first created, initiate data loading
        loadHomeData();
    }

    public void loadHomeData() {
        _isLoading.setValue(true); // Indicate that loading has started

        // Simulate network request or database fetch
        new android.os.Handler().postDelayed(() -> {
            // After simulating a delay, set the data and stop loading
            _isLoading.setValue(false); // Indicate that loading has finished
        }, 2000); // Simulate a 2-second loading time
    }

    // You can add more methods here to fetch different types of data or handle user interactions
}