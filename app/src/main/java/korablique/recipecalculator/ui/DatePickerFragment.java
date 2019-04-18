package korablique.recipecalculator.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import org.joda.time.LocalDate;

import java.util.Calendar;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    private static final String DATE_PICKER_FRAGMENT_TAG = "DATE_PICKER";
    public static final String DATE = "DATE";
    public interface DateSetListener {
        void onDateSet(LocalDate date);
    }
    @Nullable
    private DateSetListener onDateSetListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        int day, month, year;

        Bundle args = getArguments();
        if (args != null && args.containsKey(DATE)) {
            long dateLong = args.getLong(DATE);
            LocalDate date = new LocalDate(dateLong);
            day = date.getDayOfMonth();
            month = date.getMonthOfYear() - 1;
            year = date.getYear();
        } else {
            final Calendar c = Calendar.getInstance();
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
        }

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        LocalDate simpleDate = new LocalDate(year, month + 1, dayOfMonth);
        if (onDateSetListener != null) {
            onDateSetListener.onDateSet(simpleDate);
        }
    }

    public void setOnDateSetListener(DateSetListener listener) {
        onDateSetListener = listener;
    }

    public static DatePickerFragment showDialog(FragmentManager fragmentManager) {
        DatePickerFragment datePickerFragment = new DatePickerFragment();
        datePickerFragment.show(fragmentManager, DATE_PICKER_FRAGMENT_TAG);
        return datePickerFragment;
    }

    public static DatePickerFragment showDialog(FragmentManager fragmentManager, LocalDate date) {
        DatePickerFragment datePickerFragment = new DatePickerFragment();
        Bundle bundle = new Bundle();
        long dateLong = date.toDate().getTime();
        bundle.putLong(DATE, dateLong);
        datePickerFragment.setArguments(bundle);
        datePickerFragment.show(fragmentManager, DATE_PICKER_FRAGMENT_TAG);
        return datePickerFragment;
    }
}
