package korablique.recipecalculator.ui.mainscreen;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.PopularProductsUtils;

public class MainScreenModelImpl implements MainScreenModel {
    private DatabaseWorker databaseWorker;
    private HistoryWorker historyWorker;

    public MainScreenModelImpl(DatabaseWorker databaseWorker, HistoryWorker historyWorker) {
        this.databaseWorker = databaseWorker;
        this.historyWorker = historyWorker;
    }

    @Override
    public void requestTopFoodstuffs(Context context, int limit, Callback<List<Foodstuff>> callback) {
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

    @Override
    public void requestAllFoodstuffs(Context context, Callback<List<Foodstuff>> callback) {
        int batchSize = 100;
        databaseWorker.requestListedFoodstuffsFromDb(context, batchSize, (foodstuffs) -> {
            callback.onResult(foodstuffs);
        });
    }

    @Override
    public void requestFoodstuffsLike(
            Context context,
            String query,
            int suggestionsNumber,
            Callback<List<Foodstuff>> callback) {
        databaseWorker.requestFoodstuffsLike(context, query, suggestionsNumber, foodstuffs -> {
            callback.onResult(foodstuffs);
        });
    }
}
