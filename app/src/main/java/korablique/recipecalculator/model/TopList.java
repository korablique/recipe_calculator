package korablique.recipecalculator.model;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;

@Singleton
public class TopList {
    private static final int TOP_LIMIT = 5;
    private Context context;
    private DatabaseWorker databaseWorker;
    private HistoryWorker historyWorker;

    @Inject
    public TopList(Context context, DatabaseWorker databaseWorker, HistoryWorker historyWorker) {
        this.context = context;
        this.databaseWorker = databaseWorker;
        this.historyWorker = historyWorker;
    }

    public void getTopList(Callback<List<Foodstuff>> resultCallback) {
        requestTopFoodstuffs(context, TOP_LIMIT, (foodstuffs) -> {
            resultCallback.onResult(foodstuffs);
        });
    }

    private void requestTopFoodstuffs(Context context, int limit, Callback<List<Foodstuff>> callback) {
        // Сначала делаем запросы в БД, в коллбеках сохраняем результаты,
        // а затем уже добавляем в адаптеры элементы.
        // Это нужно для того, чтобы элементы на экране загружались все сразу
        List<Long> foodstuffsIds = new ArrayList<>(); // это айдишники всех продуктов за период
        historyWorker.requestFoodstuffsIdsFromHistoryForPeriod(
                0,
                Long.MAX_VALUE,
                (ids) -> {
                    foodstuffsIds.addAll(ids);
                    List<PopularProductsUtils.FoodstuffFrequency> topList = PopularProductsUtils.getTop(foodstuffsIds); // это топ из них
                    List<Long> topFoodstuffIds = new ArrayList<>(); // это айдишники топа
                    for (int index = 0; index < topList.size() && index < limit; ++index) {
                        topFoodstuffIds.add(topList.get(index).getFoodstuffId());
                    }
                    databaseWorker.requestFoodstuffsByIds(context, topFoodstuffIds, (foodstuffs) -> {
                        callback.onResult(foodstuffs);
                    });
                });
    }
}
