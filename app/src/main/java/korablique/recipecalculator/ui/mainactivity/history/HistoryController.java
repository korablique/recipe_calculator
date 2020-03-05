package korablique.recipecalculator.ui.mainactivity.history;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.TestEnvironmentDetector;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.DatePickerFragment;
import korablique.recipecalculator.ui.mainactivity.MainActivityFragmentsController;
import korablique.recipecalculator.ui.mainactivity.MainActivitySelectedDateStorage;
import korablique.recipecalculator.ui.mainactivity.history.pages.HistoryPageFragment;
import korablique.recipecalculator.ui.mainactivity.history.pages.HistoryPagesAdapter;

@FragmentScope
public class HistoryController implements
        FragmentCallbacks.Observer,
        HistoryWorker.HistoryChangeObserver,
        MainActivitySelectedDateStorage.Observer {
    private BaseActivity context;
    private BaseFragment fragment;
    private View fragmentView;
    private HistoryWorker historyWorker;
    private TimeProvider timeProvider;
    private MainActivityFragmentsController fragmentsController;
    private MainActivitySelectedDateStorage mainActivitySelectedDateStorage;
    private ViewPager2 historyViewPager;
    private HistoryPagesAdapter historyPagesAdapter;
    private boolean initialized = false;

    private DatePickerFragment.DateSetListener dateSetListener = new DatePickerFragment.DateSetListener() {
        @Override
        public void onDateSet(LocalDate date) {
            switchToDate(date);
        }
    };

    @Inject
    public HistoryController(
            BaseActivity context,
            BaseFragment fragment,
            FragmentCallbacks fragmentCallbacks,
            HistoryWorker historyWorker,
            TimeProvider timeProvider,
            MainActivityFragmentsController fragmentsController,
            MainActivitySelectedDateStorage mainActivitySelectedDateStorage) {
        fragmentCallbacks.addObserver(this);
        historyWorker.addHistoryChangeObserver(this);
        this.context = context;
        this.fragment = fragment;
        this.historyWorker = historyWorker;
        this.timeProvider = timeProvider;
        this.fragmentsController = fragmentsController;
        this.mainActivitySelectedDateStorage = mainActivitySelectedDateStorage;
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        this.fragmentView = fragmentView;
        this.historyViewPager = fragmentView.findViewById(R.id.history_view_pager);

        FloatingActionButton fab = fragmentView.findViewById(R.id.history_fab);
        fab.setOnClickListener(v -> {
            fragmentsController.showMainScreen();
        });

        // если DatePicker уже существует (открыт) - подписываемся на него,
        // т к вообще подписка происходит в onClickListener'е кнопки календаря
        DatePickerFragment existedDatePicker = DatePickerFragment.findFragment(context.getSupportFragmentManager());
        if (existedDatePicker != null) {
            existedDatePicker.setOnDateSetListener(dateSetListener);
        }

        initViewPager();
        initCalendarButton(fragmentView);
        initReturnToCurrentDateButton(fragmentView);
        updateReturnButtonVisibility(fragmentView);

        mainActivitySelectedDateStorage.addObserver(this);
        switchToDate(mainActivitySelectedDateStorage.getSelectedDate());

        initialized = true;
    }

    private void initViewPager() {
        LocalDate now = timeProvider.now().toLocalDate();
        historyPagesAdapter = new HistoryPagesAdapter(fragment, now);
        historyViewPager.setAdapter(historyPagesAdapter);
        historyViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (initialized) {
                    switchToDate(historyPagesAdapter.positionToDate(position));
                }
            }
        });
        historyViewPager.setCurrentItem(historyPagesAdapter.dateToPosition(now), false);
        historyViewPager.setOffscreenPageLimit(5);
    }

    @Override
    public void onFragmentDestroy() {
        mainActivitySelectedDateStorage.removeObserver(this);
        historyWorker.removeHistoryChangeObserver(this);
    }

    private void switchToDate(LocalDate date) {
        mainActivitySelectedDateStorage.setSelectedDate(date);
    }

    @Override
    public void onSelectedDateChanged(LocalDate date) {
        setDateInToolbar(date, fragmentView);
        int datePosition = historyPagesAdapter.dateToPosition(date);
        if (datePosition != historyViewPager.getCurrentItem()) {
            boolean animated = !TestEnvironmentDetector.isInTests();
            historyViewPager.setCurrentItem(
                    historyPagesAdapter.dateToPosition(date),
                    animated);
        }
        updateReturnButtonVisibility(fragmentView);
    }

    private void initCalendarButton(View fragmentView) {
        View calendarButton = fragmentView.findViewById(R.id.calendar_button);
        calendarButton.setOnClickListener(v -> {
            DatePickerFragment datePickerFragment = DatePickerFragment.showDialog(
                    context.getSupportFragmentManager(),
                    mainActivitySelectedDateStorage.getSelectedDate());
            datePickerFragment.setOnDateSetListener(dateSetListener);
        });
    }

    private void setDateInToolbar(LocalDate date, View fragmentView) {
        String dateString = date.toString("dd.MM.yy");

        LocalDate today = timeProvider.now().toLocalDate();
        if (date.getDayOfMonth() == today.getDayOfMonth()
                && date.getMonthOfYear() == today.getMonthOfYear()
                && date.getYear() == today.getYear()) {
            dateString = context.getString(R.string.today);
        }

        LocalDate tomorrow = today.plusDays(1);
        if (date.getDayOfMonth() == tomorrow.getDayOfMonth()
                && date.getMonthOfYear() == tomorrow.getMonthOfYear()
                && date.getYear() == tomorrow.getYear()) {
            dateString = context.getString(R.string.tomorrow);
        }

        LocalDate dayAfterTomorrow = today.plusDays(2);
        if (date.getDayOfMonth() == dayAfterTomorrow.getDayOfMonth()
                && date.getMonthOfYear() == dayAfterTomorrow.getMonthOfYear()
                && date.getYear() == dayAfterTomorrow.getYear()) {
            dateString = context.getString(R.string.day_after_tomorrow);
        }

        LocalDate yesterday = today.minusDays(1);
        if (date.getDayOfMonth() == yesterday.getDayOfMonth()
                && date.getMonthOfYear() == yesterday.getMonthOfYear()
                && date.getYear() == yesterday.getYear()) {
            dateString = context.getString(R.string.yesterday);
        }

        LocalDate dayBeforeYesterday = today.minusDays(2);
        if (date.getDayOfMonth() == dayBeforeYesterday.getDayOfMonth()
                && date.getMonthOfYear() == dayBeforeYesterday.getMonthOfYear()
                && date.getYear() == dayBeforeYesterday.getYear()) {
            dateString = context.getString(R.string.day_before_yesterday);
        }

        TextView dateTextView = fragmentView.findViewById(R.id.title_text);
        dateTextView.setText(dateString);
    }

    public void addFoodstuffs(LocalDate date, List<WeightedFoodstuff> foodstuffs) {
        NewHistoryEntry[] newHistoryEntries = newHistoryEntriesFrom(foodstuffs, date);
        // сохраняем в историю
        historyWorker.saveGroupOfFoodstuffsToHistory(newHistoryEntries, historyEntriesIds -> {
            switchToDate(date);
        });
    }

    private NewHistoryEntry[] newHistoryEntriesFrom(List<WeightedFoodstuff> weightedFoodstuffs, LocalDate date) {
        NewHistoryEntry[] newHistoryEntries = new NewHistoryEntry[weightedFoodstuffs.size()];
        for (int index = 0; index < weightedFoodstuffs.size(); index++) {
            WeightedFoodstuff foodstuff = weightedFoodstuffs.get(index);
            NewHistoryEntry entry = new NewHistoryEntry(
                    foodstuff.getId(), foodstuff.getWeight(), date.toDate());
            newHistoryEntries[index] = entry;
        }
        return newHistoryEntries;
    }

    /**
     * Кнопка возвращения на сегодняшний день
     */
    private void initReturnToCurrentDateButton(View fragmentView) {
        ExtendedFloatingActionButton returnButton = fragmentView.findViewById(R.id.return_for_today_button);
        returnButton.setOnClickListener(v -> {
            switchToDate(timeProvider.now().toLocalDate());
        });

        LocalDate selectedDate = mainActivitySelectedDateStorage.getSelectedDate();
        if (selectedDate == null || selectedDate.toDateTimeAtStartOfDay().equals(timeProvider.now().withTimeAtStartOfDay())) {
            returnButton.setVisibility(View.GONE);
        } else {
            returnButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateReturnButtonVisibility(View fragmentView) {
        ExtendedFloatingActionButton returnButton = fragmentView.findViewById(R.id.return_for_today_button);
        DateTime now = timeProvider.now();

        LocalDate selectedDate = mainActivitySelectedDateStorage.getSelectedDate();
        if (selectedDate == null ||
                selectedDate.toDateTimeAtStartOfDay().equals(now.withTimeAtStartOfDay())) {
            returnButton.setVisibility(View.GONE);
        } else {
            returnButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onHistoryChange() {
        // Если История поменялась, но фрагмент Истории не показан - История была изменена
        // не через экран Истории - обновимся, чтобы при заходе на экран Истории были отображены
        // правильные продукты.
        if (!fragmentView.isShown()) {
            switchToDate(mainActivitySelectedDateStorage.getSelectedDate());
        }
    }
}
