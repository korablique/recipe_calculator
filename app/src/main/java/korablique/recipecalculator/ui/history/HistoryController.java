package korablique.recipecalculator.ui.history;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.joda.time.DateTime;

import java.util.ArrayList;
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
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.card.NewCard;
import korablique.recipecalculator.ui.mainscreen.MainScreenFragment;

import static korablique.recipecalculator.ui.bucketlist.BucketListActivity.EXTRA_FOODSTUFFS_LIST;


@FragmentScope
public class HistoryController extends FragmentCallbacks.Observer {
    private static final int CARD_BUTTON_TEXT_RES = R.string.save;
    private BaseActivity context;
    private BaseFragment fragment;
    private HistoryWorker historyWorker;
    private UserParametersWorker userParametersWorker;
    private RxFragmentSubscriptions subscriptions;
    private FoodstuffsList foodstuffsList;

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

    @Inject
    public HistoryController(
            BaseActivity context,
            BaseFragment fragment,
            FragmentCallbacks fragmentCallbacks,
            HistoryWorker historyWorker,
            UserParametersWorker userParametersWorker,
            RxFragmentSubscriptions subscriptions,
            FoodstuffsList foodstuffsList) {
        fragmentCallbacks.addObserver(this);
        this.context = context;
        this.fragment = fragment;
        this.historyWorker = historyWorker;
        this.userParametersWorker = userParametersWorker;
        this.subscriptions = subscriptions;
        this.foodstuffsList = foodstuffsList;
    }

    @Override
    public void onFragmentViewCreated(View fragmentView) {
        FloatingActionButton fab = fragmentView.findViewById(R.id.history_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainScreenFragment.show(context);
            }
        });

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

        CardDialog existingCardDialog = CardDialog.findCard(context);
        if (existingCardDialog != null) {
            existingCardDialog.setUpAddFoodstuffButton(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
            existingCardDialog.setOnDeleteButtonClickListener(onDeleteButtonClickListener);
        }

        // request todays history
        DateTime today = DateTime.now();
        DateTime todayMidnight = new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 0, 0, 0);
        DateTime todayEnd = new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 23, 59, 59);
        Observable<HistoryEntry> todaysHistoryObservable = historyWorker.requestHistoryForPeriod(
                todayMidnight.getMillis(),
                todayEnd.getMillis());

        // обёртки заголовка с БЖУК (значений и прогрессов БЖУК)
        ViewGroup nutritionHeaderParentLayout = fragmentView.findViewById(R.id.nutrition_parent_layout);
        nutritionValuesWrapper = new HistoryNutritionValuesWrapper(
                context, nutritionHeaderParentLayout);
        nutritionProgressWrapper = new NutritionProgressWrapper(nutritionHeaderParentLayout);

        Single<Optional<UserParameters>> currentUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
        Single<Pair<Optional<UserParameters>, List<HistoryEntry>>> currentUserParamsWithTodaysHistorySingle =
                currentUserParamsSingle.zipWith(todaysHistoryObservable.toList(), Pair::create);
        subscriptions.subscribe(currentUserParamsWithTodaysHistorySingle, new Consumer<Pair<Optional<UserParameters>, List<HistoryEntry>>>() {
            @Override
            public void accept(Pair<Optional<UserParameters>, List<HistoryEntry>> currentUserParamsWithTodaysHistory) {
                UserParameters currentUserParams = currentUserParamsWithTodaysHistory.first.get();
                List<HistoryEntry> todaysHistory = currentUserParamsWithTodaysHistory.second;
                Nutrition totalNutrition = Nutrition.zero();
                for (HistoryEntry entry : todaysHistory) {
                    WeightedFoodstuff foodstuff = entry.getFoodstuff();
                    totalNutrition = totalNutrition.plus(Nutrition.of(foodstuff));
                }

                // заполнение заголовка с БЖУК
                Rates rates = RateCalculator.calculate(currentUserParams);
                nutritionValuesWrapper.setNutrition(totalNutrition, rates);
                nutritionProgressWrapper.setProgresses(totalNutrition, rates);

                // заполнение адаптера истории
                adapter.addItems(todaysHistory);

                // листенер на нажатия на элемент адаптера
                adapter.setOnItemClickObserver(new NewHistoryAdapter.Observer() {
                    @Override
                    public void onItemClicked(HistoryEntry historyEntry, int displayedPosition) {
                        CardDialog card = CardDialog.showCard(context, historyEntry.getFoodstuff());
                        card.prohibitEditing(true);
                        card.setUpAddFoodstuffButton(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
                        card.setOnDeleteButtonClickListener(onDeleteButtonClickListener);
                    }
                });
            }
        });

        Bundle args = fragment.getArguments();
        if (args != null && args.containsKey(EXTRA_FOODSTUFFS_LIST)) {
            // фудстаффы из бакет листа
            List<WeightedFoodstuff> foodstuffsFromBucketList = args.getParcelableArrayList(EXTRA_FOODSTUFFS_LIST);
            NewHistoryEntry[] newHistoryEntries = new NewHistoryEntry[foodstuffsFromBucketList.size()];
            for (int index = 0; index < foodstuffsFromBucketList.size(); index++) {
                WeightedFoodstuff foodstuff = foodstuffsFromBucketList.get(index);
                NewHistoryEntry entry = new NewHistoryEntry(
                        foodstuff.getId(), foodstuff.getWeight(), DateTime.now().toDate());
                newHistoryEntries[index] = entry;
            }
            // сохраняем в историю
            foodstuffsList.saveFoodstuffsToHistory(newHistoryEntries, new Callback<List<Long>>() {
                @Override
                public void onResult(List<Long> historyEntriesIds) {
                    List<HistoryEntry> historyEntries = new ArrayList<>();
                    for (int index = 0; index < historyEntriesIds.size(); index++) {
                        HistoryEntry entry = new HistoryEntry(
                                historyEntriesIds.get(index),
                                foodstuffsFromBucketList.get(index),
                                newHistoryEntries[index].getDate());
                        historyEntries.add(entry);
                    }
                    // добавляем в адаптер
                    adapter.addItems(historyEntries);
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
            });
        }
    }
}
