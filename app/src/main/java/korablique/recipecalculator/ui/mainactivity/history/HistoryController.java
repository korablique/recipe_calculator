package korablique.recipecalculator.ui.mainactivity.history;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.DatePickerFragment;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.card.Card;
import korablique.recipecalculator.ui.mainactivity.MainActivityFragmentsController;
import korablique.recipecalculator.ui.mainactivity.MainActivitySelectedDateStorage;

@FragmentScope
public class HistoryController implements FragmentCallbacks.Observer, HistoryWorker.Observer {
    private static final int CARD_BUTTON_TEXT_RES = R.string.save;
    private BaseActivity context;
    private BaseFragment fragment;
    private HistoryWorker historyWorker;
    private UserParametersWorker userParametersWorker;
    private RxFragmentSubscriptions subscriptions;
    private TimeProvider timeProvider;
    private MainActivityFragmentsController fragmentsController;
    private MainActivitySelectedDateStorage mainActivitySelectedDateStorage;
    private HistoryAdapter adapter;
    private HistoryNutritionValuesWrapper nutritionValuesWrapper;
    private NutritionProgressWrapper nutritionProgressWrapper;
    private Card.OnMainButtonClickListener onAddFoodstuffButtonClickListener
            = new Card.OnMainButtonClickListener() {
        @Override
        public void onClick(WeightedFoodstuff foodstuff) {
            CardDialog.hideCard(context);
            long replacedItemId = adapter.replaceItem(foodstuff);
            historyWorker.editWeightInHistoryEntry(replacedItemId, foodstuff.getWeight());

            // update wrappers
            Nutrition updatedNutrition = Nutrition.zero();
            for (HistoryEntry entry : adapter.getItems()) {
                updatedNutrition = updatedNutrition.plus(Nutrition.of(entry.getFoodstuff()));
            }
            Single<Optional<UserParameters>> currentUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
            Nutrition finalUpdatedNutrition = updatedNutrition;
            subscriptions.subscribe(currentUserParamsSingle, new Consumer<Optional<UserParameters>>() {
                @Override
                public void accept(Optional<UserParameters> userParametersOptional) {
                    UserParameters currentUserParams = userParametersOptional.get();
                    Rates rates = RateCalculator.calculate(currentUserParams);
                    nutritionProgressWrapper.setProgresses(finalUpdatedNutrition, rates);
                    nutritionValuesWrapper.setNutrition(finalUpdatedNutrition, rates);
                }
            });
        }
    };
    private Card.OnDeleteButtonClickListener onDeleteButtonClickListener = new Card.OnDeleteButtonClickListener() {
        @Override
        public void onClick(WeightedFoodstuff foodstuff) {
            CardDialog.hideCard(context);
            HistoryEntry removingItem = adapter.removeItem(foodstuff);
            historyWorker.deleteEntryFromHistory(removingItem);

            // update wrappers
            Nutrition updatedNutrition = nutritionValuesWrapper.getCurrentNutrition()
                    .minus(Nutrition.of(foodstuff));
            Single<Optional<UserParameters>> currentUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
            subscriptions.subscribe(currentUserParamsSingle, new Consumer<Optional<UserParameters>>() {
                @Override
                public void accept(Optional<UserParameters> userParametersOptional) {
                    UserParameters currentUserParams = userParametersOptional.get();
                    Rates rates = RateCalculator.calculate(currentUserParams);
                    nutritionProgressWrapper.setProgresses(updatedNutrition, rates);
                    nutritionValuesWrapper.setNutrition(updatedNutrition, rates);
                }
            });
        }
    };
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
            UserParametersWorker userParametersWorker,
            RxFragmentSubscriptions subscriptions,
            TimeProvider timeProvider,
            MainActivityFragmentsController fragmentsController,
            MainActivitySelectedDateStorage mainActivitySelectedDateStorage) {
        fragmentCallbacks.addObserver(this);
        this.context = context;
        this.fragment = fragment;
        this.historyWorker = historyWorker;
        this.userParametersWorker = userParametersWorker;
        this.subscriptions = subscriptions;
        this.timeProvider = timeProvider;
        this.fragmentsController = fragmentsController;
        this.mainActivitySelectedDateStorage = mainActivitySelectedDateStorage;
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        FloatingActionButton fab = fragmentView.findViewById(R.id.history_fab);
        fab.setOnClickListener(v -> {
            fragmentsController.showMainScreen();
        });

        // обёртки заголовка с БЖУК (значений и прогрессов БЖУК)
        ViewGroup nutritionHeaderParentLayout = fragmentView.findViewById(R.id.nutrition_parent_layout);
        nutritionValuesWrapper = new HistoryNutritionValuesWrapper(
                context, nutritionHeaderParentLayout);
        nutritionProgressWrapper = new NutritionProgressWrapper(nutritionHeaderParentLayout);

        RecyclerView recyclerView = fragmentView.findViewById(R.id.history_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(fragmentView.getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new HistoryAdapter(fragmentView.getContext());

        initHistoryList(fragmentView);
        initCard();
        switchToDate(mainActivitySelectedDateStorage.getSelectedDate(), fragmentView);
        initCalendarButton(fragmentView);

        // если DatePicker уже существует (открыт) - подписываемся на него,
        // т к вообще подписка происходит в onClickListener'е кнопки календаря
        DatePickerFragment existedDatePicker = DatePickerFragment.findFragment(context.getSupportFragmentManager());
        if (existedDatePicker != null) {
            existedDatePicker.setOnDateSetListener(dateSetListener);
        }

        initReturnToCurrentDateButton(fragmentView);

        historyWorker.addObserver(this);
    }

    @Override
    public void onFragmentDestroy() {
        historyWorker.removeObserver(this);
    }

    private void switchToDate(LocalDate date) {
        switchToDate(date, fragment.getView());
    }

    private void switchToDate(LocalDate date, View fragmentView) {
        mainActivitySelectedDateStorage.setSelectedDate(date);
        setDateInToolbar(date, fragmentView);
        // загрузить историю за выбранный день
        DateTime from = new DateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 0, 0, 0);
        DateTime to = new DateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 23, 59, 59);
        Single<List<HistoryEntry>> historySingle =
                historyWorker.requestHistoryForPeriod(from.getMillis(), to.getMillis()).toList();
        Single<Optional<UserParameters>> currentUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
        Single<Pair<List<HistoryEntry>, Optional<UserParameters>>> historyWithUserParamsSingle =
                historySingle.zipWith(currentUserParamsSingle, Pair::create);
        subscriptions.subscribe(historyWithUserParamsSingle, historyAndUserParamsPair -> {
            List<HistoryEntry> historyEntries = historyAndUserParamsPair.first;
            UserParameters currentUserParams = historyAndUserParamsPair.second.get();

            adapter.clear();
            adapter.addItems(historyEntries);
            // листенер на нажатия на элемент адаптера
            adapter.setOnItemClickObserver((historyEntry) -> {
                CardDialog card = CardDialog.showCard(context, historyEntry.getFoodstuff());
                card.prohibitEditing(true);
                card.setUpButton1(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
                card.setOnDeleteButtonClickListener(onDeleteButtonClickListener);
            });
            updateWrappers(historyEntries, currentUserParams);
            updateReturnButtonVisibility(fragmentView);
        });
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

    private void initCard() {
        CardDialog existingCardDialog = CardDialog.findCard(context);
        if (existingCardDialog != null) {
            existingCardDialog.setUpButton1(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
            existingCardDialog.setOnDeleteButtonClickListener(onDeleteButtonClickListener);
        }
    }

    private void initHistoryList(View fragmentView) {
        RecyclerView recyclerView = fragmentView.findViewById(R.id.history_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(fragmentView.getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new HistoryAdapter(fragmentView.getContext());
        recyclerView.setAdapter(adapter);
        DividerItemDecorationWithoutDividerAfterLastItem dividerItemDecoration =
                new DividerItemDecorationWithoutDividerAfterLastItem(
                        recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(fragmentView.getResources().getDrawable(R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    private void updateWrappers(List<HistoryEntry> historyEntries, UserParameters currentUserParams) {
        Nutrition totalNutrition = Nutrition.zero();
        for (HistoryEntry entry : historyEntries) {
            WeightedFoodstuff foodstuff = entry.getFoodstuff();
            totalNutrition = totalNutrition.plus(Nutrition.of(foodstuff));
        }

        // заполнение заголовка с БЖУК
        Rates rates = RateCalculator.calculate(currentUserParams);
        nutritionValuesWrapper.setNutrition(totalNutrition, rates);
        nutritionProgressWrapper.setProgresses(totalNutrition, rates);
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
        View view = fragment.getView();
        if (view == null) {
            return;
        }
        // Если История поменялась, но фрагмент Истории не показан - История была изменена
        // не через экран Истории - обновимся, чтобы при заходе на экран Истории были отображены
        // правильные продукты.
        if (!view.isShown()) {
            switchToDate(timeProvider.now().toLocalDate());
        }
    }
}
