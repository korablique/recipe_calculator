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
public class FoodstuffsList {
    public final static int BATCH_SIZE = 100;

    public interface OnFoodstuffBatchReceiveListener {
        void onReceive(List<Foodstuff> foodstuffs);
    }
    private List<Foodstuff> top;
    private List<Foodstuff> all = new ArrayList<>();
    private final Context context;
    private final DatabaseWorker databaseWorker;
    private final HistoryWorker historyWorker;
    private boolean allLoaded;
    private boolean inProcess;
    private List<Callback<List<Foodstuff>>> batchCallbacks = new ArrayList<>();
    private List<Callback<List<Foodstuff>>> finishCallbacks = new ArrayList<>();

    @Inject
    public FoodstuffsList(Context context, DatabaseWorker databaseWorker, HistoryWorker historyWorker) {
        this.context = context;
        this.databaseWorker = databaseWorker;
        this.historyWorker = historyWorker;
    }

    /**
     * Контракт:
     * - Если клиент вызвал метод, в его коллбеки через неопределенное время будут приходить фудстафы частями,
     * затем будет вызван finishCallback.
     * - Если клиент вызовет метод второй раз, он сразу получит все фудстафы (они кешируются).
     * - Если второй клиент вызовет метод во время загрузки данных для первого клиента, то он
     * получит уже загруженную для первого часть фудстаффов, а затем будет получать остальные батчи
     * через неопределенное время.
     * - Фудстаффы никогда не загружаются из БД повторно.
     * */
    public void getAllFoodstuffs(Callback<List<Foodstuff>> batchCallback, Callback<List<Foodstuff>> resultCallback) {
        if (allLoaded) {
            batchCallback.onResult(all);
            resultCallback.onResult(all);
            return;
        }

        batchCallbacks.add(batchCallback);
        finishCallbacks.add(resultCallback);
        if (inProcess) {
            batchCallback.onResult(all);
            return;
        }
        inProcess = true;
        databaseWorker.requestListedFoodstuffsFromDb(
                context,
                BATCH_SIZE,
                foodstuffs -> {
                    all.addAll(foodstuffs);
                    for (Callback<List<Foodstuff>> callback : batchCallbacks) {
                        // это текущий батч
                        callback.onResult(foodstuffs);
                    }
                },
                () -> {
                    allLoaded = true;
                    for (Callback<List<Foodstuff>> callback : finishCallbacks) {
                        callback.onResult(all);
                    }
                    finishCallbacks.clear();
                    inProcess = false;
                });
    }

    // на самом деле тут не батчами, а всё сразу возвращается
    public void getTopOfFoodstuffs(
            Context context,
            int topLimit,
            OnFoodstuffBatchReceiveListener onFoodstuffBatchReceiveListener) {
        requestTopFoodstuffs(context, topLimit, (foodstuffs) -> {
            top = new ArrayList<>();
            top.addAll(foodstuffs);
            onFoodstuffBatchReceiveListener.onReceive(foodstuffs);
        });
    }

    // TODO: 26.10.18 где здесь хоть один адаптер?
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
                    List<PopularProductsUtils.FoodstuffFrequency> topList =
                            PopularProductsUtils.getTop(foodstuffsIds); // это топ из них
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
