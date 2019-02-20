package korablique.recipecalculator.ui.usergoal;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

import androidx.fragment.app.DialogFragment;
import korablique.recipecalculator.model.DateOfBirth;

import static korablique.recipecalculator.ui.usergoal.UserParametersActivity.DATE_OF_BIRTH;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    public interface MyOnDateSetListener {
        void onDateSet(int year, int month, int dayOfMonth);
    }

    private MyOnDateSetListener onDateSetListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        int day, month, year;

        Bundle args = getArguments();
        if (args != null && args.containsKey(DATE_OF_BIRTH)) {
            DateOfBirth dateOfBirth = args.getParcelable(DATE_OF_BIRTH);
            day = dateOfBirth.getDay();
            month = dateOfBirth.getMonth() - 1;
            year = dateOfBirth.getYear();
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
        // Do something with the date chosen by the user
        onDateSetListener.onDateSet(year, month, dayOfMonth);
    }

    public void setOnDateSetListener(MyOnDateSetListener listener) {
        onDateSetListener = listener;
    }
}
