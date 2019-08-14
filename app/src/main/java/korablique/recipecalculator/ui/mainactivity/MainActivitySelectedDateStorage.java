package korablique.recipecalculator.ui.mainactivity;

import android.os.Bundle;

import androidx.annotation.Nullable;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.dagger.ActivityScope;

/**
 * Utility class to store selected by user date and pass it across all fragments of main activity.
 */
@ActivityScope
public class MainActivitySelectedDateStorage implements ActivityCallbacks.Observer {
    private static final String EXTRA_SELECTED_DATE = "EXTRA_SELECTED_DATE";
    @Nullable
    private LocalDate selectedDate;

    @Inject
    public MainActivitySelectedDateStorage(ActivityCallbacks activityCallbacks) {
        activityCallbacks.addObserver(this);
    }

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
    }

    public boolean hasSelectedDate() {
        return selectedDate != null;
    }

    @Nullable
    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState) {
        if (selectedDate != null) {
            outState.putString(EXTRA_SELECTED_DATE, selectedDate.toString());
        }
    }

    @Override
    public void onActivityCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_SELECTED_DATE)) {
            selectedDate = LocalDate.parse(savedInstanceState.getString(EXTRA_SELECTED_DATE));
        }
    }
}
