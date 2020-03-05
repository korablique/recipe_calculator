package korablique.recipecalculator.ui.mainactivity.history.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;

public class HistoryPageFragment extends BaseFragment {
    private static final String EXTRA_DATE = "HistoryPageFragment.EXTRA_DATE";
    private LocalDate date;
    @Inject
    HistoryPageController historyPageController;

    public HistoryPageFragment() {
    }

    public HistoryPageFragment(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (date == null) {
            if (savedInstanceState == null) {
                throw new IllegalStateException(
                        "Expected either date from constructor or saved state, "
                                + "cannot work without a date (no saved state)");
            }
            date = (LocalDate) savedInstanceState.getSerializable(EXTRA_DATE);
            if (date == null) {
                throw new IllegalStateException(
                        "Expected either date from constructor or saved state, "
                                + "cannot work without a date (no date in saved state)");
            }
        }
        historyPageController.setDate(date);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(EXTRA_DATE, date);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_page, container, false);
    }
}
