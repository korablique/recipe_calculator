package korablique.recipecalculator.model;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;

@Singleton
public class FoodstuffsTopList {
    private static final int TOP_LIMIT = 5;
    private final HistoryWorker historyWorker;
    private final TimeProvider timeProvider;
    private final List<Observer> observers = new ArrayList<>();

    public interface Observer {
        void onFoodstuffsTopPossiblyChanged();
    }

    @Inject
    public FoodstuffsTopList(
            HistoryWorker historyWorker,
            TimeProvider timeProvider) {
        this.historyWorker = historyWorker;
        this.timeProvider = timeProvider;
        historyWorker.addObserver(new HistoryWorker.Observer() {
            @Override
            public void onHistoryChange() {
                for (Observer observer : observers) {
                    observer.onFoodstuffsTopPossiblyChanged();
                }
            }
        });
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void getTopList(Callback<List<Foodstuff>> resultCallback) {
        requestTopFoodstuffs(TOP_LIMIT, (foodstuffs) -> {
            resultCallback.onResult(foodstuffs);
        });
    }

    private void requestTopFoodstuffs(int limit, Callback<List<Foodstuff>> callback) {
        // Сначала делаем запросы в БД, в коллбеках сохраняем результаты,
        // а затем уже добавляем в адаптеры элементы.
        // Это нужно для того, чтобы элементы на экране загружались все сразу
        List<Foodstuff> foodstuffs = new ArrayList<>(); // это продукты за период
        DateTime now = timeProvider.now();
        DateTime monthAgo = now.minusMonths(1).withTimeAtStartOfDay();
        DateTime tomorrowMidnight = now.plusDays(1).withTimeAtStartOfDay();
        historyWorker.requestListedFoodstuffsFromHistoryForPeriod(
                monthAgo.toDate().getTime(),
                tomorrowMidnight.toDate().getTime(),
                (listedFoodstuffs) -> {
                    foodstuffs.addAll(listedFoodstuffs);
                    List<PopularProductsUtils.FoodstuffFrequency> topList = PopularProductsUtils.getTop(foodstuffs); // это топ из них
                    List<Foodstuff> topFoodstuffs = new ArrayList<>();
                    for (int index = 0; index < topList.size() && index < limit; index++) {
                        topFoodstuffs.add(topList.get(index).getFoodstuff());
                    }
                    callback.onResult(topFoodstuffs);
                });
    }
}
