package ui;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class CustomTimePickerDialogFragment extends DialogFragment {

    public interface OnTimeSelectedListener {
        void onTimeSelected(int hour, int minute);
    }

    private OnTimeSelectedListener listener;

    public static CustomTimePickerDialogFragment newInstance(OnTimeSelectedListener listener) {
        CustomTimePickerDialogFragment fragment = new CustomTimePickerDialogFragment();
        fragment.setListener(listener);
        return fragment;
    }

    public void setListener(OnTimeSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Use the current time as the default values for the picker.
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it.
        return new TimePickerDialog(getActivity(), (view, hourOfDay, minuteOfHour) -> {
            if (listener != null) {
                listener.onTimeSelected(hourOfDay, minuteOfHour);
            }
        }, hour, minute, DateFormat.is24HourFormat(getActivity()));
    }
}