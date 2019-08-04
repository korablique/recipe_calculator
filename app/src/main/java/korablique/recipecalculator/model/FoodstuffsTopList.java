package korablique.recipecalculator.model;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;

@Singleton
public class FoodstuffsTopList {
    private static final int TOP_LIMIT = 5;
    private DatabaseWorker databaseWorker;
    private HistoryWorker historyWorker;

    @Inject
    public FoodstuffsTopList(DatabaseWorker databaseWorker, HistoryWorker historyWorker) {
        this.databaseWorker = databaseWorker;
        this.historyWorker = historyWorker;
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
        historyWorker.requestListedFoodstuffsFromHistoryForPeriod(
                0,
                Long.MAX_VALUE,
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
