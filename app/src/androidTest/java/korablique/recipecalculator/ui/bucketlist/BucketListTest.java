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

import korablique.recipecalculator.base.executors.ComputationThreadsExecutor;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.base.prefs.PrefsCleaningHelper;
import korablique.recipecalculator.base.prefs.SharedPrefsManager;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.InstantComputationsThreadsExecutor;
import korablique.recipecalculator.InstantDatabaseThreadExecutor;
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
        bucketList = new BucketList(prefsManager);
    }

    @Test
    public void bucketListRestoresFromPrefs() {
        Foodstuff savedFoodstuff =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4))
                        .blockingGet();
        Ingredient ingredient = Ingredient.create(savedFoodstuff, 123, "comment");
        bucketList.add(ingredient);
        bucketList.setComment("general comment");
        bucketList.setName("name");
        bucketList.setTotalWeight(1f);

        // Новый бакетлист со старым prefs manager'ом
        BucketList bucketList2 = new BucketList(prefsManager);
        Assert.assertEquals(Collections.singletonList(ingredient), bucketList2.getList());
        Assert.assertEquals("general comment", bucketList2.getComment());
        Assert.assertEquals("name", bucketList2.getName());
        Assert.assertEquals(1f, bucketList2.getTotalWeight(), 0.0001f);
    }

    @Test
    public void bucketListCannotRestoreFromEmptyPrefs() {
        Foodstuff savedFoodstuff =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4))
                        .blockingGet();
        Ingredient ingredient = Ingredient.create(savedFoodstuff, 123, "comment");
        bucketList.add(ingredient);
        bucketList.setComment("general comment");
        bucketList.setName("name");
        bucketList.setTotalWeight(1f);

        // Чистим преференсы!
        PrefsCleaningHelper.INSTANCE.cleanAllPrefs(context);

        // Новый бакетлист со старым prefs manager'ом, но преференсы очищены
        BucketList bucketList2 = new BucketList(prefsManager);
        Assert.assertEquals(Collections.emptyList(), bucketList2.getList());
        Assert.assertEquals("", bucketList2.getComment());
        Assert.assertEquals("", bucketList2.getName());
        Assert.assertEquals(0f, bucketList2.getTotalWeight(), 0.0001f);
    }

    @Test
    public void bucketListDoesNotRestoreRemovedIngredient() {
        Foodstuff savedFoodstuff1 =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4))
                        .blockingGet();
        Ingredient ingredient1 = Ingredient.create(savedFoodstuff1, 123, "comment");
        Foodstuff savedFoodstuff2 =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("carrot").withNutrition(1, 2, 3, 4))
                        .blockingGet();
        Ingredient ingredient2 = Ingredient.create(savedFoodstuff2, 123, "comment");
        bucketList.add(ingredient1);
        bucketList.add(ingredient2);

        // Удаляем рецепт!
        bucketList.remove(ingredient1);

        // Новый бакетлист со старым prefs manager'ом
        BucketList bucketList2 = new BucketList(prefsManager);
        Assert.assertEquals(Collections.singletonList(ingredient2), bucketList2.getList());
    }

    @Test
    public void bucketListRestoresAsEmpty_ifCleared() {
        Foodstuff savedFoodstuff1 =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4))
                        .blockingGet();
        Ingredient ingredient1 = Ingredient.create(savedFoodstuff1, 123, "comment");
        Foodstuff savedFoodstuff2 =
                foodstuffsList
                        .saveFoodstuff(Foodstuff.withName("pinapple").withNutrition(1, 2, 3, 4))
                        .blockingGet();
        Ingredient ingredient2 = Ingredient.create(savedFoodstuff2, 123, "comment");
        bucketList.add(ingredient1);
        bucketList.add(ingredient2);
        bucketList.setComment("general comment");
        bucketList.setName("name");
        bucketList.setTotalWeight(1f);

        // Удаляем всё!
        bucketList.clear();

        // Новый бакетлист со старым prefs manager'ом
        BucketList bucketList2 = new BucketList(prefsManager);
        Assert.assertEquals(Collections.emptyList(), bucketList2.getList());
        Assert.assertEquals("", bucketList2.getComment());
        Assert.assertEquals("", bucketList2.getName());
        Assert.assertEquals(0f, bucketList2.getTotalWeight(), 0.0001f);
    }
}
