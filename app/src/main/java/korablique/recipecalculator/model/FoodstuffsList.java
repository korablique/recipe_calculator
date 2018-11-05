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
    private List<Callback<List<Foodstuff>>> finishCallbacks = new ArrayList<>();

    @Inject
    public FoodstuffsList(Context context, DatabaseWorker databaseWorker, HistoryWorker historyWorker) {
        this.context = context;
        this.databaseWorker = databaseWorker;
        this.historyWorker = historyWorker;
    }

    /**
     * Контракт:
     * - Если клиент вызвал метод, в его коллбеки через неопределенное время придут фудстафы, затем
     * будет вызван finishCallback.
     * - Если клиент вызовет метод второй раз, он сразу получит все фудстафы (они кешируются).
     * - Если второй клиент вызовет метод во время загрузки данных для первого клиента, то он также
     * получит все фудстафы через неопределенное время.
     * - Фудстафы никогда не загружаются из БД повторно.
     * */
    public void getAllFoodstuffs(Callback<List<Foodstuff>> resultCallback) {
        if (allLoaded) {
            resultCallback.onResult(all);
            return;
        }

        finishCallbacks.add(resultCallback);
        if (inProcess) {
            return;
        }
        inProcess = true;
        databaseWorker.requestListedFoodstuffsFromDb(
                context,
                BATCH_SIZE,
                foodstuffs -> {
                    all.addAll(foodstuffs);
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
}
