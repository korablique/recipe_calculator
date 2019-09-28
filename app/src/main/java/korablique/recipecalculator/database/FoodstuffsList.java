package korablique.recipecalculator.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.base.executors.ComputationThreadsExecutor;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.util.FuzzySearcher;

@Singleton
public class FoodstuffsList {
    public interface Observer {
        default void onFoodstuffSaved(Foodstuff savedFoodstuff, int index) {}
        default void onFoodstuffEdited(Foodstuff edited) {}
        default void onFoodstuffDeleted(Foodstuff deleted) {}
    }
    public interface SaveFoodstuffCallback {
        void onResult(Foodstuff addedFoodstuff);
        void onDuplication();
    }
    public final static int BATCH_SIZE = 100;
    private List<Foodstuff> all = new ArrayList<>();
    private final DatabaseWorker databaseWorker;
    private final MainThreadExecutor mainThreadExecutor;
    private final ComputationThreadsExecutor computationThreadsExecutor;
    private boolean allLoaded;
    private boolean inProcess;
    private List<Callback<List<Foodstuff>>> batchCallbacks = new ArrayList<>();
    private List<Callback<List<Foodstuff>>> finishCallbacks = new ArrayList<>();
    private List<Observer> observers = new ArrayList<>();

    @Inject
    public FoodstuffsList(
            DatabaseWorker databaseWorker,
            MainThreadExecutor mainThreadExecutor,
            ComputationThreadsExecutor computationThreadsExecutor) {
        this.databaseWorker = databaseWorker;
        this.mainThreadExecutor = mainThreadExecutor;
        this.computationThreadsExecutor = computationThreadsExecutor;
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
                        callback.onResult(Collections.unmodifiableList(all));
                    }
                    batchCallbacks.clear();
                    finishCallbacks.clear();
                    inProcess = false;
                });
    }

    public void saveFoodstuff(Foodstuff foodstuff, SaveFoodstuffCallback callback) {
        getAllFoodstuffs(unused -> {}, unused -> {
            databaseWorker.saveFoodstuff(foodstuff, new DatabaseWorker.SaveFoodstuffCallback() {
                @Override
                public void onResult(long id) {
                    Foodstuff foodstuffWithId = Foodstuff.withId(id).withName(foodstuff.getName()).withNutrition(
                            foodstuff.getProtein(), foodstuff.getFats(), foodstuff.getCarbs(), foodstuff.getCalories());
                    int index = addLexicographically(foodstuffWithId);
                    callback.onResult(foodstuffWithId);
                    for (Observer observer : observers) {
                        observer.onFoodstuffSaved(foodstuffWithId, index);
                    }
                }

                @Override
                public void onDuplication() {
                    callback.onDuplication();
                }
            });
        });
    }

    public void deleteFoodstuff(Foodstuff foodstuff) {
        // вызов нужен для гарантии правильного состояния кеша,
        // чтобы не удалить продукт во время формирования кеша
        getAllFoodstuffs(unused -> {}, unused -> {
            all.remove(foodstuff);
            databaseWorker.makeFoodstuffUnlisted(foodstuff, () -> {
                for (Observer observer : observers) {
                    observer.onFoodstuffDeleted(foodstuff);
                }
            });
        });
    }

    /**
     * @param id id редактированного фудстаффа (не меняется при редактировании)
     * @param editedFoodstuff отредактированный фудстафф
     */
    public void editFoodstuff(long id, Foodstuff editedFoodstuff) {
        getAllFoodstuffs(foodstuffs -> {}, foodstuffs -> {
            databaseWorker.editFoodstuff(id, editedFoodstuff, () -> {
                int editingFoodstuffIndex = -1;
                for (int index = 0; index < all.size(); index++) {
                    Foodstuff foodstuff = all.get(index);
                    if (foodstuff.getId() == id) {
                        editingFoodstuffIndex = index;
                        break;
                    }
                }
                Foodstuff editedFoodstuffWithId = Foodstuff.withId(id).withName(editedFoodstuff.getName())
                        .withNutrition(Nutrition.of100gramsOf(editedFoodstuff));
                all.set(editingFoodstuffIndex, editedFoodstuffWithId);
                for (Observer observer : observers) {
                    observer.onFoodstuffEdited(editedFoodstuffWithId);
                }
            });
        });
    }

    public Single<List<Foodstuff>> requestFoodstuffsLike(String nameQuery) {
        return requestFoodstuffsLike(nameQuery, Integer.MAX_VALUE);
    }

    public Single<List<Foodstuff>> requestFoodstuffsLike(String nameQuery, int limit) {
        Single<List<Foodstuff>> allFoodstuffsSingle = Single.create((subscription) -> {
            getAllFoodstuffs(foodstuffs -> {}, allFoodstuffs -> {
                subscription.onSuccess(allFoodstuffs);
            });
        });

        // Perform search on a computation thread and then pass the result to the main thread.
        return allFoodstuffsSingle
            .observeOn(computationThreadsExecutor.asScheduler())
            .map(allFoodstuffs -> requestFoodstuffsLikeImpl(nameQuery, allFoodstuffs, limit))
            .observeOn(mainThreadExecutor.asScheduler());
    }

    private List<Foodstuff> requestFoodstuffsLikeImpl(String nameQuery, List<Foodstuff> foodstuffs, int limit) {
        return FuzzySearcher.search(
                nameQuery.toLowerCase(),
                foodstuffs,
                (foodstuff) -> foodstuff.getName().toLowerCase(),
                limit);
    }

    public void addObserver(Observer o) {
        observers.add(o);
    }

    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    private int addLexicographically(Foodstuff newFoodstuff) {
        for (int index = 0; index < all.size(); index++) {
            Foodstuff f = all.get(index);
            if (f.getName().toLowerCase().compareTo(newFoodstuff.getName().toLowerCase()) > 0) {
                all.add(index, newFoodstuff);
                return index;
            }
        }
        all.add(newFoodstuff);
        return (all.size() - 1);
    }
}
