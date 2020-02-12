package korablique.recipecalculator.ui.bucketlist;

import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.disposables.Disposable;
import korablique.recipecalculator.TestEnvironmentDetector;
import korablique.recipecalculator.WrongThreadException;
import korablique.recipecalculator.base.prefs.PrefsOwner;
import korablique.recipecalculator.base.prefs.SharedPrefsManager;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;

@Singleton
public class BucketList {
    private static final String PREFS_FOODSTUFFS_IDS = "PREFS_FOODSTUFFS_IDS";
    private static final String PREFS_FOODSTUFFS_WEIGHTS = "PREFS_FOODSTUFFS_WEIGHTS";
    private static final short PERSISTENT_WEIGHT_PRECISION = 3; // 3 цифры после точки хватит всем
    public interface Observer {
        default void onFoodstuffAdded(WeightedFoodstuff wf) {}
        default void onFoodstuffRemoved(WeightedFoodstuff wf) {}
    }
    private final SharedPrefsManager prefsManager;
    private List<WeightedFoodstuff> bucketList = new ArrayList<>();
    private List<Observer> observers = new ArrayList<>();

    @Inject
    public BucketList(
            SharedPrefsManager prefsManager,
            FoodstuffsList foodstuffsList) {
        this.prefsManager = prefsManager;
        List<Long> ids = prefsManager.getLongList(PrefsOwner.BUCKET_LIST, PREFS_FOODSTUFFS_IDS);
        List<Float> weights = prefsManager.getFloatList(PrefsOwner.BUCKET_LIST, PREFS_FOODSTUFFS_WEIGHTS);
        if (ids == null || weights == null) {
            return;
        }
        if (ids.size() != weights.size()) {
            // TODO: report an error
            return;
        }
        Disposable unusedDisposable =
                foodstuffsList
                .getFoodstuffsWithIds(ids)
                .toList()
                .subscribe((foodstuffs) -> {
                    if (foodstuffs.size() != ids.size()) {
                        // Количество восстановленных продуктов не равно
                        // количеству запрошенных - что-то пошло не так.
                        // TODO: report an error
                        return;
                    }
                    List<WeightedFoodstuff> weightedFoodstuffs = new ArrayList<>();
                    for (int index = 0; index < foodstuffs.size(); ++index) {
                        weightedFoodstuffs.add(
                                foodstuffs.get(index).withWeight(weights.get(index)));
                    }
                    add(weightedFoodstuffs);
                });
    }

    public List<WeightedFoodstuff> getList() {
        return Collections.unmodifiableList(bucketList);
    }

    public void add(List<WeightedFoodstuff> wfs) {
        checkCurrentThread();
        bucketList.addAll(wfs);
        updatePersistentState();
        for (WeightedFoodstuff wf : wfs) {
            for (Observer observer : observers) {
                observer.onFoodstuffAdded(wf);
            }
        }
    }

    public void add(WeightedFoodstuff wf) {
        checkCurrentThread();
        bucketList.add(wf);
        updatePersistentState();
        for (Observer observer : observers) {
            observer.onFoodstuffAdded(wf);
        }
    }

    public void remove(WeightedFoodstuff wf) {
        checkCurrentThread();
        bucketList.remove(wf);
        updatePersistentState();
        for (Observer observer : observers) {
            observer.onFoodstuffRemoved(wf);
        }
    }

    public void clear() {
        checkCurrentThread();
        List<WeightedFoodstuff> removedList = new ArrayList<>(bucketList);
        bucketList.clear();
        updatePersistentState();
        for (WeightedFoodstuff wf : removedList) {
            for (Observer observer : observers) {
                observer.onFoodstuffRemoved(wf);
            }
        }
    }

    private void updatePersistentState() {
        List<Long> ids = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        for (int index = 0; index < bucketList.size(); ++index) {
            ids.add(bucketList.get(index).getId());
            weights.add((float)bucketList.get(index).getWeight());
            if (ids.get(index) == -1) {
                throw new IllegalStateException("Cannot have foodstuffs without ids in bucket list");
            }
        }
        prefsManager.putLongList(PrefsOwner.BUCKET_LIST, PREFS_FOODSTUFFS_IDS, ids);
        prefsManager.putFloatList(PrefsOwner.BUCKET_LIST, PREFS_FOODSTUFFS_WEIGHTS, weights, PERSISTENT_WEIGHT_PRECISION);
    }

    public void addObserver(Observer o) {
        checkCurrentThread();
        observers.add(o);
    }

    public void removeObserver(Observer o) {
        checkCurrentThread();
        observers.remove(o);
    }

    private void checkCurrentThread() {
        if (TestEnvironmentDetector.isInTests()) {
            return;
        }
        if (Thread.currentThread().getId() != Looper.getMainLooper().getThread().getId()) {
            throw new WrongThreadException("Can't invoke BucketList's methods from not UI thread");
        }
    }
}
