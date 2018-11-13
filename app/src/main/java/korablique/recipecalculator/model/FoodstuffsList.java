package korablique.recipecalculator.model;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.database.DatabaseWorker;

@Singleton
public class FoodstuffsList {
    public final static int BATCH_SIZE = 100;
    private List<Foodstuff> all = new ArrayList<>();
    private final Context context;
    private final DatabaseWorker databaseWorker;
    private boolean allLoaded;
    private boolean inProcess;
    private List<Callback<List<Foodstuff>>> batchCallbacks = new ArrayList<>();
    private List<Callback<List<Foodstuff>>> finishCallbacks = new ArrayList<>();

    @Inject
    public FoodstuffsList(Context context, DatabaseWorker databaseWorker) {
        this.context = context;
        this.databaseWorker = databaseWorker;
    }

    /**
     * Контракт:
     * - Если клиент вызвал метод, когда фудстаффы ещё не были загружены,
     * в его коллбеки через неопределенное время будут приходить фудстафы частями,
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
                    batchCallbacks.clear();
                    finishCallbacks.clear();
                    inProcess = false;
                });
    }
}
