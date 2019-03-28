package korablique.recipecalculator.ui.history;

import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.joda.time.DateTime;

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
import korablique.recipecalculator.ui.mainscreen.MainScreenFragment;

@FragmentScope
public class HistoryController extends FragmentCallbacks.Observer {
    private BaseActivity context;
    private HistoryFragment fragment;
    private HistoryWorker historyWorker;
    private UserParametersWorker userParametersWorker;
    private RxFragmentSubscriptions subscriptions;

    @Inject
    public HistoryController(
            BaseActivity context,
            HistoryFragment fragment,
            FragmentCallbacks fragmentCallbacks,
            HistoryWorker historyWorker,
            UserParametersWorker userParametersWorker,
            RxFragmentSubscriptions subscriptions) {
        fragmentCallbacks.addObserver(this);
        this.context = context;
        this.fragment = fragment;
        this.historyWorker = historyWorker;
        this.userParametersWorker = userParametersWorker;
        this.subscriptions = subscriptions;
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
        NewHistoryAdapter adapter = new NewHistoryAdapter(fragmentView.getContext());
        recyclerView.setAdapter(adapter);
        DividerItemDecorationWithoutDividerAfterLastItem dividerItemDecoration =
                new DividerItemDecorationWithoutDividerAfterLastItem(
                        recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(fragmentView.getResources().getDrawable(R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);

        // request todays history
        DateTime today = DateTime.now();
        DateTime todayMidnight = new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 0, 0, 0);
        DateTime todayEnd = new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 23, 59, 59);
        Observable<HistoryEntry> todaysHistoryObservable = historyWorker.requestHistoryForPeriod(
                todayMidnight.getMillis(),
                todayEnd.getMillis());

        // обёртки заголовка с БЖУК (значений и прогрессов БЖУК)
        ViewGroup nutritionHeaderParentLayout = fragmentView.findViewById(R.id.nutrition_parent_layout);
        HistoryNutritionValuesWrapper nutritionValuesWrapper = new HistoryNutritionValuesWrapper(
                context, nutritionHeaderParentLayout);
        NutritionProgressWrapper nutritionProgressWrapper = new NutritionProgressWrapper(nutritionHeaderParentLayout);

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
            }
        });
    }
}
