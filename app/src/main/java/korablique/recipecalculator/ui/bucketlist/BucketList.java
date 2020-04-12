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
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.WeightedFoodstuff;

@Singleton
public class BucketList {
    private static final String PREFS_FOODSTUFFS_IDS = "PREFS_FOODSTUFFS_IDS";
    private static final String PREFS_FOODSTUFFS_WEIGHTS = "PREFS_FOODSTUFFS_WEIGHTS";
    private static final String PREFS_FOODSTUFFS_COMMENTS = "PREFS_FOODSTUFFS_COMMENTS";
    private static final String PREFS_COMMENT = "PREFS_COMMENT";
    private static final short PERSISTENT_WEIGHT_PRECISION = 3; // 3 цифры после точки хватит всем
    public interface Observer {
        default void onIngredientAdded(Ingredient ingredient) {}
        default void onIngredientRemoved(Ingredient ingredient) {}
    }
    private final SharedPrefsManager prefsManager;
    private List<Ingredient> bucketList = new ArrayList<>();
    private String comment = "";
    private List<Observer> observers = new ArrayList<>();

    @Inject
    public BucketList(
            SharedPrefsManager prefsManager,
            FoodstuffsList foodstuffsList) {
        this.prefsManager = prefsManager;
        List<Long> ids = prefsManager.getLongList(PrefsOwner.BUCKET_LIST, PREFS_FOODSTUFFS_IDS);
        List<Float> weights = prefsManager.getFloatList(PrefsOwner.BUCKET_LIST, PREFS_FOODSTUFFS_WEIGHTS);
        List<String> comments = prefsManager.getStringList(PrefsOwner.BUCKET_LIST, PREFS_FOODSTUFFS_COMMENTS);
        this.comment = prefsManager.getString(PrefsOwner.BUCKET_LIST, PREFS_COMMENT, "");
        if (ids == null || weights == null || comments == null) {
            return;
        }
        if (ids.size() != weights.size() || ids.size() != comments.size()) {
            // TODO: report an error
            return;
        }
        Disposable unusedDisposable =
                foodstuffsList
                .getFoodstuffsWithIds(ids)
                .toList()
                .subscribe((foodstuffs) -> {
                    if (foodstuffs.size() != ids.size() || ids.size() != comments.size()) {
                        // Количество восстановленных продуктов не равно
                        // количеству запрошенных - что-то пошло не так.
                        // TODO: report an error
                        return;
                    }
                    List<Ingredient> ingredients = new ArrayList<>();
                    for (int index = 0; index < foodstuffs.size(); ++index) {
                        ingredients.add(
                                Ingredient.create(
                                        foodstuffs.get(index),
                                        weights.get(index),
                                        comments.get(index)));
                    }
                    add(ingredients);
                });
    }

    public List<Ingredient> getList() {
        return Collections.unmodifiableList(bucketList);
    }

    public void add(List<Ingredient> ingredients) {
        checkCurrentThread();
        bucketList.addAll(ingredients);
        updatePersistentState();
        for (Ingredient ingredient : ingredients) {
            for (Observer observer : observers) {
                observer.onIngredientAdded(ingredient);
            }
        }
    }

    public void add(Ingredient ingredient) {
        checkCurrentThread();
        bucketList.add(ingredient);
        updatePersistentState();
        for (Observer observer : observers) {
            observer.onIngredientAdded(ingredient);
        }
    }

    public void remove(Ingredient ingredient) {
        checkCurrentThread();
        bucketList.remove(ingredient);
        updatePersistentState();
        for (Observer observer : observers) {
            observer.onIngredientRemoved(ingredient);
        }
    }

    public void setComment(String comment) {
        this.comment = comment;
        updatePersistentState();
    }

    public String getComment() {
        return comment;
    }

    public void clear() {
        checkCurrentThread();
        List<Ingredient> removedList = new ArrayList<>(bucketList);
        bucketList.clear();
        comment = "";
        updatePersistentState();
        for (Ingredient ingredient : removedList) {
            for (Observer observer : observers) {
                observer.onIngredientRemoved(ingredient);
            }
        }
    }

    private void updatePersistentState() {
        List<Long> ids = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        for (int index = 0; index < bucketList.size(); ++index) {
            ids.add(bucketList.get(index).getFoodstuff().getId());
            weights.add((float)bucketList.get(index).getWeight());
            comments.add(bucketList.get(index).getComment());
            if (ids.get(index) == -1) {
                throw new IllegalStateException("Cannot have foodstuffs without ids in bucket list");
            }
        }
        prefsManager.putLongList(PrefsOwner.BUCKET_LIST, PREFS_FOODSTUFFS_IDS, ids);
        prefsManager.putFloatList(PrefsOwner.BUCKET_LIST, PREFS_FOODSTUFFS_WEIGHTS, weights, PERSISTENT_WEIGHT_PRECISION);
        prefsManager.putStringList(PrefsOwner.BUCKET_LIST, PREFS_FOODSTUFFS_COMMENTS, comments);
        prefsManager.putString(PrefsOwner.BUCKET_LIST, PREFS_COMMENT, comment);
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
