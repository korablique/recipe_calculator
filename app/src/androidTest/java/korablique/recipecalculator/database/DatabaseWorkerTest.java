package korablique.recipecalculator.database;

import android.content.Context;
import android.database.Cursor;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;
import korablique.recipecalculator.database.room.AppDatabase;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.InstantMainThreadExecutor;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DatabaseWorkerTest {
    private Context context;
    private DatabaseHolder databaseHolder;
    private DatabaseWorker databaseWorker;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DatabaseThreadExecutor databaseThreadExecutor = new InstantDatabaseThreadExecutor();
        databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);
        databaseWorker = new DatabaseWorker(
                databaseHolder, new InstantMainThreadExecutor(), databaseThreadExecutor);
        databaseHolder.getDatabase().clearAllTables();
    }

    @Test
    public void requestListedFoodstuffsFromDbWorks() {
        Foodstuff foodstuff1 = Foodstuff.withName("продукт1").withNutrition(1, 1, 1, 1);
        Foodstuff foodstuff2 = Foodstuff.withName("продукт2").withNutrition(1, 1, 1, 1);
        Foodstuff foodstuff3 = Foodstuff.withName("продукт3").withNutrition(1, 1, 1, 1);
        Foodstuff foodstuff4 = Foodstuff.withName("продукт4").withNutrition(1, 1, 1, 1);
        databaseWorker.saveFoodstuff(
                foodstuff1,
                new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {}
            @Override
            public void onDuplication() {
                throw new RuntimeException("Видимо, продукт уже существует");
            }
        });
        databaseWorker.saveFoodstuff(foodstuff2, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {}

            @Override
            public void onDuplication() {
                throw new RuntimeException("Видимо, продукт уже существует");
            }
        });
        //сохраняем два unlisted foodstuff'а
        databaseWorker.saveUnlistedFoodstuff(foodstuff3, null);
        databaseWorker.saveUnlistedFoodstuff(foodstuff4, null);

        final int[] listedFoodstuffsCount = new int[1];
        databaseWorker.requestListedFoodstuffsFromDb(
                20,
                foodstuffs -> listedFoodstuffsCount[0] = foodstuffs.size());

        AppDatabase database = databaseHolder.getDatabase();
        Cursor unlistedFoodstuffs = database.query(
                "SELECT * FROM " + FOODSTUFFS_TABLE_NAME + " WHERE " + COLUMN_NAME_IS_LISTED + "=0", null);
        int unlistedFoodstuffsCount = unlistedFoodstuffs.getCount();
        unlistedFoodstuffs.close();

        Assert.assertEquals(2, unlistedFoodstuffsCount);
        Assert.assertEquals(2, listedFoodstuffsCount[0]);
    }

    @Test
    public void canSaveListedProductSameAsUnlisted() {
        Foodstuff foodstuff = Foodstuff.withName("falafel").withNutrition(10, 10, 10, 100);
        AtomicBoolean savedAsUnlisted = new AtomicBoolean(false);
        databaseWorker.saveUnlistedFoodstuff(
                foodstuff,
                foodstuffId -> {
                    Assert.assertTrue(foodstuffId > 0);
                    savedAsUnlisted.set(true);
                });

        AtomicBoolean savedAsListed = new AtomicBoolean(false);
        databaseWorker.saveFoodstuff(
                foodstuff,
                new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {
                Assert.assertTrue(id > 0);
                savedAsListed.set(true);
            }

            @Override
            public void onDuplication() {
            }
        });
        Assert.assertTrue(savedAsUnlisted.get());
        Assert.assertTrue(savedAsListed.get());
    }

    @Test
    public void checkSaveGroupOfFoodstuffsCallbackCallsCount() throws InterruptedException {
        // создаем 10 продуктов
        int foodstuffsNumber = 10;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        for (int index = 0; index < foodstuffsNumber; index++) {
            foodstuffs[index] = Foodstuff.withName("foodstuff" + index).withNutrition(5, 5, 5, 5);
        }

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(List<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // Запрашиваем все фудстафы с размером батча 3
        int batchSize = 3;
        final int[] counter = {0};
        databaseWorker.requestListedFoodstuffsFromDb(batchSize, unused -> ++counter[0]);
        Assert.assertEquals(4, counter[0]);
    }

    @Test
    public void checkReturnedFoodstuffs() throws InterruptedException {
        // создаем 10 продуктов
        int foodstuffsNumber = 10;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        for (int index = 0; index < foodstuffsNumber; index++) {
            foodstuffs[index] = Foodstuff.withName("foodstuff" + index).withNutrition(5, 5, 5, 5);
        }

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(List<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // Запрашиваем все фудстафы с размером батча 3
        int batchSize = 3;
        final ArrayList<Foodstuff> returnedFoodstuffs = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                batchSize,
                foodstuffs1 -> returnedFoodstuffs.addAll(foodstuffs1));
        Collections.sort(foodstuffsIds);
        Collections.sort(returnedFoodstuffs, new Comparator<Foodstuff>() {
            @Override
            public int compare(Foodstuff lhs, Foodstuff rhs) {
                return Long.compare(lhs.getId(), rhs.getId());
            }
        });
        for (int index = 0; index < returnedFoodstuffs.size(); index++) {
            Assert.assertEquals(foodstuffsIds.get(index).longValue(), returnedFoodstuffs.get(index).getId());
        }
    }

    @Test
    public void listOfRequestedFoodstuffsReturnedInCorrectOrder() {
        // создаем продукт с названиями с маленькой и заглавной букв
        int foodstuffsNumber = 3;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        String apple = "Яблоко";
        foodstuffs[0] = Foodstuff.withName("абрикос").withNutrition(5, 5, 5, 5);
        foodstuffs[1] = Foodstuff.withName("Абрикос").withNutrition(5, 5, 5, 5);
        foodstuffs[2] = Foodstuff.withName(apple).withNutrition(5, 5, 5, 5);

        // сохраняем продукты в список
        final List<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(List<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // Запрашиваем все фудстафы
        int batchSize = 4;
        final List<Foodstuff> returnedFoodstuffs = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                batchSize,
                foodstuffs1 -> returnedFoodstuffs.addAll(foodstuffs1));
        Assert.assertEquals(apple, returnedFoodstuffs.get(2).getName());
    }

    /**
     * Проверяет, что количество фудстаффов, возвращенных методом requestFoodstuffsByIds равно
     * количеству айдишников, переданных в него
     */
    @Test
    public void requestFoodstuffsByIdsReturnsRequestedNumberOfFoodstuffs() {
        int foodstuffsNumber = 3;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        foodstuffs[0] = Foodstuff.withName("Абрикос").withNutrition(5, 5, 5, 5);
        foodstuffs[1] = Foodstuff.withName("Банан").withNutrition(5, 5, 5, 5);
        foodstuffs[2] = Foodstuff.withName("Яблоко").withNutrition(5, 5, 5, 5);

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(List<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });

        List<Foodstuff> returnedFoodstuffs = new ArrayList<>();
        databaseWorker.requestFoodstuffsByIds(
                foodstuffsIds,
                foodstuffs1 -> {
                    returnedFoodstuffs.addAll(foodstuffs1);
                });
        Assert.assertEquals(foodstuffsNumber, returnedFoodstuffs.size());
    }

    /**
     * Проверяет, что requestFoodstuffsByIds() возвращает именно те продукты, которые мы запросили,
     * и в том же порядке.
     */
    @Test
    public void requestFoodstuffsByIdsReturnsRequestedFoodstuffs() {
        int foodstuffsNumber = 3;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        foodstuffs[0] = Foodstuff.withName("Абрикос").withNutrition(5, 5, 5, 5);
        foodstuffs[1] = Foodstuff.withName("Банан").withNutrition(5, 5, 5, 5);
        foodstuffs[2] = Foodstuff.withName("Яблоко").withNutrition(5, 5, 5, 5);

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                foodstuffs, new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(List<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });

        foodstuffs[0] = Foodstuff.withId(foodstuffsIds.get(0))
                .withName(foodstuffs[0].getName())
                .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]));

        foodstuffs[1] = Foodstuff.withId(foodstuffsIds.get(1))
                .withName(foodstuffs[1].getName())
                .withNutrition(Nutrition.of100gramsOf(foodstuffs[1]));

        foodstuffs[2] = Foodstuff.withId(foodstuffsIds.get(2))
                .withName(foodstuffs[2].getName())
                .withNutrition(Nutrition.of100gramsOf(foodstuffs[2]));

        List<Foodstuff> returnedFoodstuffs = new ArrayList<>();
        databaseWorker.requestFoodstuffsByIds(
                foodstuffsIds,
                foodstuffs1 -> {
                    returnedFoodstuffs.addAll(foodstuffs1);
                });
        Assert.assertEquals(returnedFoodstuffs.get(0), foodstuffs[0]);
        Assert.assertEquals(returnedFoodstuffs.get(1), foodstuffs[1]);
        Assert.assertEquals(returnedFoodstuffs.get(2), foodstuffs[2]);

        // Делаем reverse, чтобы проверить, что фудстаффы тоже вернутся в обратном порядке.
        Collections.reverse(foodstuffsIds);
        returnedFoodstuffs.clear();
        databaseWorker.requestFoodstuffsByIds(
                foodstuffsIds,
                foodstuffs1 -> returnedFoodstuffs.addAll(foodstuffs1));
        Assert.assertEquals(returnedFoodstuffs.get(0), foodstuffs[2]);
        Assert.assertEquals(returnedFoodstuffs.get(1), foodstuffs[1]);
        Assert.assertEquals(returnedFoodstuffs.get(2), foodstuffs[0]);
    }

    @Test
    public void requestFoodstuffsLikeWorks() throws IOException {
        Foodstuff[] foodstuffs = new Foodstuff[10];
        foodstuffs[0] = Foodstuff.withName("варенье из абрикосов").withNutrition(1, 1, 1, 1);
        foodstuffs[1] = Foodstuff.withName("вареники").withNutrition(1, 1, 1, 1);
        foodstuffs[2] = Foodstuff.withName("вареная картошка").withNutrition(1, 1, 1, 1);
        foodstuffs[3] = Foodstuff.withName("варенье из груш").withNutrition(1, 1, 1, 1);
        foodstuffs[4] = Foodstuff.withName("абрикос").withNutrition(1, 1, 1, 1);
        foodstuffs[5] = Foodstuff.withName("варенье из смородины").withNutrition(1, 1, 1, 1);
        foodstuffs[6] = Foodstuff.withName("варенье из клубники").withNutrition(1, 1, 1, 1);
        foodstuffs[7] = Foodstuff.withName("шоколад").withNutrition(1, 1, 1, 1);
        foodstuffs[8] = Foodstuff.withName("хлеб").withNutrition(1, 1, 1, 1);
        foodstuffs[9] = Foodstuff.withName("варенье из черники").withNutrition(1, 1, 1, 1);

        databaseWorker.saveGroupOfFoodstuffs(foodstuffs, null);

        String query = "варенье";
        List<Foodstuff> searchResult = new ArrayList<>();
        databaseWorker.requestFoodstuffsLike(
                query,
                3,
                foodstuffs1 -> searchResult.addAll(foodstuffs1));
        Assert.assertEquals(searchResult.get(0).getName(), "варенье из абрикосов");
        Assert.assertEquals(searchResult.get(1).getName(), "варенье из груш");
        Assert.assertEquals(searchResult.get(2).getName(), "варенье из клубники");
    }

    @Test
    public void returnedIdsOfSavedFoodstuffsGroupHasSameOrder() {
        // Order 1
        Foodstuff[] foodstuffs = new Foodstuff[] {
                Foodstuff.withName("a").withNutrition(1, 2, 3, 4),
                Foodstuff.withName("b").withNutrition(4, 3, 2, 1),
                Foodstuff.withName("c").withNutrition(3, 3, 2, 2),
        };
        databaseHolder.getDatabase().clearAllTables();
        returnedIdsOfSavedFoodstuffsGroupHasSameOrderImpl(foodstuffs);

        // Order reversed
        foodstuffs = new Foodstuff[] {
                Foodstuff.withName("c").withNutrition(3, 3, 2, 2),
                Foodstuff.withName("b").withNutrition(4, 3, 2, 1),
                Foodstuff.withName("a").withNutrition(1, 2, 3, 4),
        };
        databaseHolder.getDatabase().clearAllTables();
        returnedIdsOfSavedFoodstuffsGroupHasSameOrderImpl(foodstuffs);
    }

    private void returnedIdsOfSavedFoodstuffsGroupHasSameOrderImpl(Foodstuff[] foodstuffs) {
        List<Long> ids = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(foodstuffs, idsResult -> {
            ids.addAll(idsResult);
        });

        List<Foodstuff> receivedFoodstuffs = new ArrayList<>();
        databaseWorker.requestFoodstuffsByIds(ids, foodstuffs1 -> {
            receivedFoodstuffs.addAll(foodstuffs1);
        });

        Assert.assertTrue(Foodstuff.haveSameNutrition(foodstuffs[0], receivedFoodstuffs.get(0)));
        Assert.assertEquals(foodstuffs[0].getName(), receivedFoodstuffs.get(0).getName());
        Assert.assertEquals((long) ids.get(0), receivedFoodstuffs.get(0).getId());

        Assert.assertTrue(Foodstuff.haveSameNutrition(foodstuffs[1], receivedFoodstuffs.get(1)));
        Assert.assertEquals(foodstuffs[1].getName(), receivedFoodstuffs.get(1).getName());
        Assert.assertEquals((long) ids.get(1), receivedFoodstuffs.get(1).getId());

        Assert.assertTrue(Foodstuff.haveSameNutrition(foodstuffs[2], receivedFoodstuffs.get(2)));
        Assert.assertEquals(foodstuffs[2].getName(), receivedFoodstuffs.get(2).getName());
        Assert.assertEquals((long) ids.get(2), receivedFoodstuffs.get(2).getId());
    }

    public Foodstuff getAnyFoodstuffFromDb() throws InterruptedException {
        final ArrayList<Foodstuff> foodstuffArrayList = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                20,
                foodstuffs -> foodstuffArrayList.addAll(foodstuffs));

        if (foodstuffArrayList.size() != 0) {
            return foodstuffArrayList.get(0);
        }

        Foodstuff foodstuff = Foodstuff.withName("apricot").withNutrition(10, 10, 10, 10);
        databaseWorker.saveFoodstuff(foodstuff, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {}
            @Override
            public void onDuplication() {}
        });
        return getAnyFoodstuffFromDb();
    }
}
