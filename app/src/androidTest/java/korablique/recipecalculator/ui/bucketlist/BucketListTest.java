package korablique.recipecalculator.ui.bucketlist;

import android.content.Context;

import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import korablique.recipecalculator.base.executors.ComputationThreadsExecutor;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.base.prefs.PrefsCleaningHelper;
import korablique.recipecalculator.base.prefs.SharedPrefsManager;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.util.InstantComputationsThreadsExecutor;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BucketListTest {
    private Context context;
    MainThreadExecutor mainThreadExecutor;
    private SharedPrefsManager prefsManager;
    private FoodstuffsList foodstuffsList;
    private BucketList bucketList;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PrefsCleaningHelper.INSTANCE.cleanAllPrefs(context);

        mainThreadExecutor = new SyncMainThreadExecutor();
        DatabaseThreadExecutor databaseThreadExecutor = new InstantDatabaseThreadExecutor();
        DatabaseHolder databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);
        DatabaseWorker databaseWorker = new DatabaseWorker(databaseHolder, mainThreadExecutor, databaseThreadExecutor);
        databaseHolder.getDatabase().clearAllTables();

        ComputationThreadsExecutor computationThreadsExecutor = new InstantComputationsThreadsExecutor();

        prefsManager = new SharedPrefsManager(context);
        foodstuffsList =
                new FoodstuffsList(
                        databaseWorker,
                        mainThreadExecutor,
                        computationThreadsExecutor);
        bucketList = new BucketList(prefsManager, foodstuffsList);
    }

    @Test
    public void bucketListRestoresFromPrefs() {
        Foodstuff savedFoodstuff =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4))
                        .blockingGet();

        WeightedFoodstuff wf = savedFoodstuff.withWeight(123);
        bucketList.add(wf);

        // Новый бакетлист со старым prefs manager'ом
        BucketList bucketList2 = new BucketList(prefsManager, foodstuffsList);
        Assert.assertEquals(Collections.singletonList(wf), bucketList2.getList());
    }

    @Test
    public void bucketListCannotRestoreFromEmptyPrefs() {
        Foodstuff savedFoodstuff =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4))
                        .blockingGet();

        WeightedFoodstuff wf = savedFoodstuff.withWeight(123);
        bucketList.add(wf);

        // Чистим преференсы!
        PrefsCleaningHelper.INSTANCE.cleanAllPrefs(context);

        // Новый бакетлист со старым prefs manager'ом, но преференсы очищены
        BucketList bucketList2 = new BucketList(prefsManager, foodstuffsList);
        Assert.assertEquals(Collections.emptyList(), bucketList2.getList());
    }

    @Test
    public void bucketListRestoresAsEmpty_ifFoodstuffRemoved() {
        Foodstuff savedFoodstuff1 =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4))
                        .blockingGet();
        Foodstuff savedFoodstuff2 =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("carrot").withNutrition(1, 2, 3, 4))
                        .blockingGet();

        WeightedFoodstuff wf1 = savedFoodstuff1.withWeight(123);
        WeightedFoodstuff wf2 = savedFoodstuff2.withWeight(123);
        bucketList.add(wf1);
        bucketList.add(wf2);

        // Удаляем продукт!
        bucketList.remove(wf1);

        // Новый бакетлист со старым prefs manager'ом
        BucketList bucketList2 = new BucketList(prefsManager, foodstuffsList);
        Assert.assertEquals(Collections.singletonList(wf2), bucketList2.getList());
    }

    @Test
    public void bucketListRestoresAsEmpty_ifFoodstuffsCleared() {
        Foodstuff savedFoodstuff1 =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4))
                        .blockingGet();
        Foodstuff savedFoodstuff2 =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("carrot").withNutrition(1, 2, 3, 4))
                        .blockingGet();

        WeightedFoodstuff wf1 = savedFoodstuff1.withWeight(123);
        WeightedFoodstuff wf2 = savedFoodstuff2.withWeight(123);
        bucketList.add(wf1);
        bucketList.add(wf2);

        // Удаляем все продукты!
        bucketList.clear();

        // Новый бакетлист со старым prefs manager'ом
        BucketList bucketList2 = new BucketList(prefsManager, foodstuffsList);
        Assert.assertEquals(Collections.emptyList(), bucketList2.getList());
    }
}
