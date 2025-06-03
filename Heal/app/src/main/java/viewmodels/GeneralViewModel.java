package viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GeneralViewModel extends ViewModel {

    private final MutableLiveData<String> _homeData = new MutableLiveData<>();
    public LiveData<String> homeData = _homeData;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    public GeneralViewModel() {
        loadHomeData();
    }

    public void loadHomeData() {
        _isLoading.setValue(true); // Indicate that loading has started

        new android.os.Handler().postDelayed(() -> {

            _isLoading.setValue(false);
        }, 2000);
    }

}