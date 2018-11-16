package korablique.recipecalculator.database;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;

@Singleton
public class FoodstuffsList {
    public interface Observer {
        void onFoodstuffSaved(Foodstuff savedFoodstuff);
        void onFoodstuffEdited(Foodstuff edited);
        void onFoodstuffDeleted(Foodstuff deleted);
    }
    public interface SaveFoodstuffCallback {
        void onResult(long id);
        void onDuplication();
    }
    public final static int BATCH_SIZE = 100;
    private List<Foodstuff> all = new ArrayList<>();
    private final Context context;
    private final DatabaseWorker databaseWorker;
    private boolean allLoaded;
    private boolean inProcess;
    private List<Callback<List<Foodstuff>>> batchCallbacks = new ArrayList<>();
    private List<Callback<List<Foodstuff>>> finishCallbacks = new ArrayList<>();
    private List<Observer> observers = new ArrayList<>();

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

    public void saveFoodstuff(Context context, Foodstuff foodstuff, SaveFoodstuffCallback callback) {
        databaseWorker.saveFoodstuff(context, foodstuff, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {
                Foodstuff foodstuffWithId = Foodstuff.withId(id).withName(foodstuff.getName()).withNutrition(
                        foodstuff.getProtein(), foodstuff.getFats(), foodstuff.getCarbs(), foodstuff.getCalories());
                all.add(foodstuffWithId);
                callback.onResult(id);
                for (Observer observer : observers) {
                    observer.onFoodstuffSaved(foodstuffWithId);
                }
            }

            @Override
            public void onDuplication() {
                callback.onDuplication();
            }
        });
    }

    public void editFoodstuff(Context context, long id, Foodstuff editedFoodstuff) {
        databaseWorker.editFoodstuff(context, id, editedFoodstuff, new Runnable() {
            @Override
            public void run() {
                Foodstuff editingFoodstuff = null;
                for (Foodstuff foodstuff : all) {
                    if (foodstuff.getId() == id) {
                        editingFoodstuff = foodstuff;
                        break;
                    }
                }
                Foodstuff editedFoodstuffWithId = Foodstuff.withId(id).withName(editedFoodstuff.getName())
                        .withNutrition(Nutrition.of100gramsOf(editedFoodstuff));
                all.set(all.indexOf(editingFoodstuff), editedFoodstuffWithId);
                for (Observer observer : observers) {
                    observer.onFoodstuffEdited(editedFoodstuffWithId);
                }
            }
        });
    }

    public void requestListedFoodstuffsFromDb(Context context, int batchSize, Callback<List<Foodstuff>> batchCallback) {
        databaseWorker.requestListedFoodstuffsFromDb(context, batchSize, new DatabaseWorker.FoodstuffsBatchReceiveCallback() {
            @Override
            public void onReceive(List<Foodstuff> foodstuffs) {
                batchCallback.onResult(foodstuffs);
            }
        });
    }

    public void requestFoodstuffsLike(Context context, String nameQuery, int limit, Callback<List<Foodstuff>> callback) {
        databaseWorker.requestFoodstuffsLike(context, nameQuery, limit, new DatabaseWorker.FoodstuffsBatchReceiveCallback() {
            @Override
            public void onReceive(List<Foodstuff> foodstuffs) {
                callback.onResult(foodstuffs);
            }
        });
    }

    public void removeFoodstuff(Context context, long foodstuffId, Runnable callback) {
        Foodstuff deleted = null;
        for (Foodstuff f : all) {
            if (f.getId() == foodstuffId) {
                deleted = f;
                break;
            }
        }
        Foodstuff finalDeleted = deleted;
        databaseWorker.makeFoodstuffUnlisted(context, foodstuffId, new Runnable() {
            @Override
            public void run() {
                all.remove(finalDeleted);
                for (Observer observer : observers) {
                    observer.onFoodstuffDeleted(finalDeleted);
                }
            }
        });
    }

    public void addObserver(Observer o) {
        observers.add(o);
    }

    public void removeObserver(Observer o) {
        observers.remove(o);
    }
}
