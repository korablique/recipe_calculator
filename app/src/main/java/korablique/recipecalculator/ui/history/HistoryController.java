package korablique.recipecalculator.ui.history;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.DatePickerFragment;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.card.NewCard;
import korablique.recipecalculator.ui.mainscreen.MainScreenFragment;

@FragmentScope
public class HistoryController extends FragmentCallbacks.Observer {
    private static final int CARD_BUTTON_TEXT_RES = R.string.save;
    private static final String SELECTED_DATE = "SELECTED_DATE";
    private BaseActivity context;
    private HistoryWorker historyWorker;
    private UserParametersWorker userParametersWorker;
    private RxFragmentSubscriptions subscriptions;
    private NewHistoryAdapter adapter;
    private HistoryNutritionValuesWrapper nutritionValuesWrapper;
    private NutritionProgressWrapper nutritionProgressWrapper;
    private NewCard.OnAddFoodstuffButtonClickListener onAddFoodstuffButtonClickListener
            = new NewCard.OnAddFoodstuffButtonClickListener() {
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
                public void accept(Optional<UserParameters> userParametersOptional) throws Exception {
                    UserParameters currentUserParams = userParametersOptional.get();
                    Rates rates = RateCalculator.calculate(currentUserParams);
                    nutritionProgressWrapper.setProgresses(finalUpdatedNutrition, rates);
                    nutritionValuesWrapper.setNutrition(finalUpdatedNutrition, rates);
                }
            });
        }
    };
    private NewCard.OnDeleteButtonClickListener onDeleteButtonClickListener = new NewCard.OnDeleteButtonClickListener() {
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
                public void accept(Optional<UserParameters> userParametersOptional) throws Exception {
                    UserParameters currentUserParams = userParametersOptional.get();
                    Rates rates = RateCalculator.calculate(currentUserParams);
                    nutritionProgressWrapper.setProgresses(updatedNutrition, rates);
                    nutritionValuesWrapper.setNutrition(updatedNutrition, rates);
                }
            });
        }
    };
    private LocalDate selectedDate;

    @Inject
    public HistoryController(
            BaseActivity context,
            FragmentCallbacks fragmentCallbacks,
            HistoryWorker historyWorker,
            UserParametersWorker userParametersWorker,
            RxFragmentSubscriptions subscriptions) {
        fragmentCallbacks.addObserver(this);
        this.context = context;
        this.historyWorker = historyWorker;
        this.userParametersWorker = userParametersWorker;
        this.subscriptions = subscriptions;
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        restoreInstanceState(savedInstanceState);

        FloatingActionButton fab = fragmentView.findViewById(R.id.history_fab);
        fab.setOnClickListener(v -> MainScreenFragment.show(context.getSupportFragmentManager()));

        // обёртки заголовка с БЖУК (значений и прогрессов БЖУК)
        ViewGroup nutritionHeaderParentLayout = fragmentView.findViewById(R.id.nutrition_parent_layout);
        nutritionValuesWrapper = new HistoryNutritionValuesWrapper(
                context, nutritionHeaderParentLayout);
        nutritionProgressWrapper = new NutritionProgressWrapper(nutritionHeaderParentLayout);

        initHistoryList(fragmentView);

        initCard();

        DateTime today = DateTime.now();
        DateTime todayMidnight = new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 0, 0, 0);
        DateTime todayEnd = new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 23, 59, 59);
        if (selectedDate == null) {
            fillHistory(todayMidnight, todayEnd);
        } else {
            fillHistory(selectedDate.toDateTimeAtStartOfDay(), new DateTime(
                    selectedDate.getYear(), selectedDate.getMonthOfYear(), selectedDate.getDayOfMonth(), 23, 59, 59));
        }
        initCalendarButton(fragmentView);
    }

    public void onFragmentSaveInstanceState(Bundle outState) {
        if (selectedDate != null) {
            outState.putSerializable(SELECTED_DATE, selectedDate);
        }
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_DATE)) {
            selectedDate = (LocalDate) savedInstanceState.getSerializable(SELECTED_DATE);
        }
    }

    private void initCalendarButton(View fragmentView) {
        View calendarButton = fragmentView.findViewById(R.id.calendar_button);
        calendarButton.setOnClickListener(v -> {
            DatePickerFragment datePickerFragment;
            if (selectedDate != null) {
                datePickerFragment = DatePickerFragment.showDialog(context.getSupportFragmentManager(), selectedDate);
            } else {
                datePickerFragment = DatePickerFragment.showDialog(context.getSupportFragmentManager());
            }
            datePickerFragment.setOnDateSetListener(date -> {
                selectedDate = date;
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
                    updateWrappers(historyEntries, currentUserParams);
                });
            });

        });
    }

    private void fillHistory(DateTime periodStart, DateTime periodEnd) {
        Observable<HistoryEntry> todaysHistoryObservable = historyWorker.requestHistoryForPeriod(
                periodStart.getMillis(),
                periodEnd.getMillis());

        Single<Optional<UserParameters>> currentUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
        Single<Pair<Optional<UserParameters>, List<HistoryEntry>>> currentUserParamsWithTodaysHistorySingle =
                currentUserParamsSingle.zipWith(todaysHistoryObservable.toList(), Pair::create);
        subscriptions.subscribe(currentUserParamsWithTodaysHistorySingle, new Consumer<Pair<Optional<UserParameters>, List<HistoryEntry>>>() {
            @Override
            public void accept(Pair<Optional<UserParameters>, List<HistoryEntry>> currentUserParamsWithTodaysHistory) {
                UserParameters currentUserParams = currentUserParamsWithTodaysHistory.first.get();
                List<HistoryEntry> todaysHistory = currentUserParamsWithTodaysHistory.second;
                updateWrappers(todaysHistory, currentUserParams);

                // заполнение адаптера истории
                adapter.addItems(todaysHistory);

                // листенер на нажатия на элемент адаптера
                adapter.setOnItemClickObserver(new NewHistoryAdapter.Observer() {
                    @Override
                    public void onItemClicked(HistoryEntry historyEntry, int displayedPosition) {
                        CardDialog card = CardDialog.showCard(context, historyEntry.getFoodstuff());
                        card.prohibitEditing(false);
                        card.setUpAddFoodstuffButton(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
                        card.setOnDeleteButtonClickListener(onDeleteButtonClickListener);
                    }
                });
            }
        });
    }

    private void initCard() {
        CardDialog existingCardDialog = CardDialog.findCard(context);
        if (existingCardDialog != null) {
            existingCardDialog.setUpAddFoodstuffButton(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
            existingCardDialog.setOnDeleteButtonClickListener(onDeleteButtonClickListener);
        }
    }

    private void initHistoryList(View fragmentView) {
        RecyclerView recyclerView = fragmentView.findViewById(R.id.history_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(fragmentView.getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new NewHistoryAdapter(fragmentView.getContext());
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
}
