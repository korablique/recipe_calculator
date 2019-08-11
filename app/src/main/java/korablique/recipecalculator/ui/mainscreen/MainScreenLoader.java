package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Single;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.FoodstuffsTopList;

@Singleton
public class MainScreenLoader {
    public static final int LOADED_FOODSTUFFS_COUNT = 30;
    private final Context context;
    private final FoodstuffsList foodstuffsList;
    private final FoodstuffsTopList topList;

    @Inject
    public MainScreenLoader(
            Context context,
            FoodstuffsList foodstuffsList,
            FoodstuffsTopList topList) {
        this.context = context;
        this.foodstuffsList = foodstuffsList;
        this.topList = topList;
    }

    public Completable loadMainScreenActivity() {
        Single<List<Foodstuff>> topSingle = Single.create((emitter -> {
            topList.getTopList(emitter::onSuccess);
        }));
        Single<List<Foodstuff>> allFoodstuffsFirstBatch = Single.create((emitter -> {
            // Let's get only a small number of foodstuffs and immediately finish
            List<Foodstuff> result = new ArrayList<>(LOADED_FOODSTUFFS_COUNT);
            foodstuffsList.getAllFoodstuffs(batch -> {
                if (result.size() == LOADED_FOODSTUFFS_COUNT) {
                    // All needed foodstuffs received!
                    return;
                }
                // From 0 to batch.size, while result is not full
                for (int index = 0; index < batch.size() && result.size() < LOADED_FOODSTUFFS_COUNT; ++index) {
                    result.add(batch.get(index));
                }
                // If result full - report success
                if (result.size() == LOADED_FOODSTUFFS_COUNT) {
                    emitter.onSuccess(result);
                }
            }, unused -> {
                // If we received all available foodstuffs, but result is not fully filled -
                // that means we haven't reported success yet, so report it.
                if (result.size() != LOADED_FOODSTUFFS_COUNT) {
                    emitter.onSuccess(result);
                }
            });
        }));


        // Subscribe to both foodstuffs lists, map them to MainActivity opening,
        // return a Completable.
        return Single.zip(topSingle, allFoodstuffsFirstBatch, Pair::create)
                .map((topAndFirstBatch) -> {
                    MainActivity.openMainScreen(
                            context,
                            new ArrayList<>(topAndFirstBatch.first),
                            new ArrayList<>(topAndFirstBatch.second));
                    return 1;
                }).ignoreElement();
    }
}
