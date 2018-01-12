package korablique.recipecalculator.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.ui.calculator.CalculatorActivity;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.InstantMainThreadExecutor;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_WEIGHT;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DatabaseWorkerTest {
    private DatabaseWorker databaseWorker;

    @Rule
    public ActivityTestRule<CalculatorActivity> mActivityRule =
            new ActivityTestRule<>(CalculatorActivity.class);

    @Before
    public void setUp() {
        databaseWorker = new DatabaseWorker(
                new InstantMainThreadExecutor(), new InstantDatabaseThreadExecutor());
        clearTable(HISTORY_TABLE_NAME);
        clearTable(FOODSTUFFS_TABLE_NAME);
    }

    @Test
    public void requestListedFoodstuffsFromDbWorks() throws InterruptedException {
        Foodstuff foodstuff1 = new Foodstuff("продукт1", 1, 1, 1, 1, 1);
        Foodstuff foodstuff2 = new Foodstuff("продукт2", 1, 1, 1, 1, 1);
        Foodstuff foodstuff3 = new Foodstuff("продукт3", 1, 1, 1, 1, 1);
        Foodstuff foodstuff4 = new Foodstuff("продукт4", 1, 1, 1, 1, 1);
        databaseWorker.saveFoodstuff(
                mActivityRule.getActivity(),
                foodstuff1,
                new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {}
            @Override
            public void onDuplication() {
                throw new RuntimeException("Видимо, продукт уже существует");
            }
        });
        databaseWorker.saveFoodstuff(mActivityRule.getActivity(), foodstuff2, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {}

            @Override
            public void onDuplication() {
                throw new RuntimeException("Видимо, продукт уже существует");
            }
        });
        //сохраняем два unlisted foodstuff'а
        databaseWorker.saveUnlistedFoodstuff(mActivityRule.getActivity(), foodstuff3, null);
        databaseWorker.saveUnlistedFoodstuff(mActivityRule.getActivity(), foodstuff4, null);

        final int[] listedFoodstuffsCount = new int[1];
        databaseWorker.requestListedFoodstuffsFromDb(
                mActivityRule.getActivity(),
                20,
                new DatabaseWorker.FoodstuffsRequestCallback() {
            @Override
            public void onResult(ArrayList<Foodstuff> foodstuffs) {
                listedFoodstuffsCount[0] = foodstuffs.size();
            }
        });

        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
        Cursor unlistedFoodstuffs = database.rawQuery(
                "SELECT * FROM " + FOODSTUFFS_TABLE_NAME + " WHERE " + COLUMN_NAME_IS_LISTED + "=0", null);
        int unlistedFoodstuffsCount = unlistedFoodstuffs.getCount();
        unlistedFoodstuffs.close();

        Assert.assertEquals(2, unlistedFoodstuffsCount);
        Assert.assertEquals(2, listedFoodstuffsCount[0]);
    }

    @Test
    public void savingToHistoryWorks() throws InterruptedException {
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

        Cursor cursorBeforeSaving = database.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME, null);
        int entriesCountBeforeSaving = cursorBeforeSaving.getCount();
        Assert.assertTrue(cursorBeforeSaving.getCount() == 0);
        cursorBeforeSaving.close();

        Foodstuff foodstuff = getAnyFoodstuffFromDb();

        Date date = new Date();
        databaseWorker.saveFoodstuffToHistory(
                mActivityRule.getActivity(),
                date,
                foodstuff.getId(),
                100,
                null);

        Cursor cursorAfterSaving = database.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME, null);
        long dateInt = -1, foodstuffId = -1;
        while (cursorAfterSaving.moveToNext()) {
            dateInt = cursorAfterSaving.getLong(cursorAfterSaving.getColumnIndex(COLUMN_NAME_DATE));
            foodstuffId = cursorAfterSaving.getLong(cursorAfterSaving.getColumnIndex(COLUMN_NAME_FOODSTUFF_ID));
        }
        int entriesCountAfterSaving = cursorAfterSaving.getCount();
        cursorAfterSaving.close();
        Assert.assertTrue(entriesCountAfterSaving - entriesCountBeforeSaving == 1);
        Assert.assertEquals(foodstuff.getId(), foodstuffId);
        Assert.assertEquals(date.getTime(), dateInt);
    }

    @Test
    public void requestAllHistoryFromDbWorks() throws InterruptedException {
        Foodstuff foodstuff = getAnyFoodstuffFromDb();
        double weight = 100;
        Date date = new Date();
        databaseWorker.saveFoodstuffToHistory(
                mActivityRule.getActivity(),
                date,
                foodstuff.getId(),
                weight,
                null);

        final ArrayList<HistoryEntry> historyList = new ArrayList<>();
        int batchSize = 100;
        databaseWorker.requestAllHistoryFromDb(
                mActivityRule.getActivity(),
                batchSize,
                new DatabaseWorker.RequestHistoryCallback() {
            @Override
            public void onResult(ArrayList<HistoryEntry> historyEntries) {
                historyList.addAll(historyEntries);
            }
        });
        Assert.assertEquals(1, historyList.size());
        Assert.assertEquals(historyList.get(0).getFoodstuff().getId(), foodstuff.getId());
        Assert.assertEquals(historyList.get(0).getTime(), date);
    }

    @Test
    public void checkRequestHistoryCallbackCallsCount() {
        int foodstuffsNumber = 10;
        Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        for (int index = 0; index < foodstuffsNumber; index++) {
            foodstuffs[index] = new Foodstuff("foodstuff" + index, -1, 5, 5, 5, 5);
        }
        ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                mActivityRule.getActivity(),
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(ArrayList<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                }
        );

        NewHistoryEntry[] historyEntries = new NewHistoryEntry[foodstuffsNumber];
        double weight = 100;
        for (int index = 0; index < historyEntries.length; index++) {
            historyEntries[index] = new NewHistoryEntry(
                    foodstuffs[index].getId(),
                    weight,
                    new Date(118, 0, index));
        }
        ArrayList<Long> entriesIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffsToHistory(
                mActivityRule.getActivity(),
                historyEntries,
                new DatabaseWorker.AddHistoryEntriesCallback() {
                    @Override
                    public void onResult(ArrayList<Long> historyEntriesIds) {
                        entriesIds.addAll(historyEntriesIds);
                    }
                });

        int batchSize = 3;
        final int[] counter = {0};
        databaseWorker.requestAllHistoryFromDb(
                mActivityRule.getActivity(),
                batchSize,
                new DatabaseWorker.RequestHistoryCallback() {
                    @Override
                    public void onResult(ArrayList<HistoryEntry> historyEntries) {
                        ++counter[0];
                    }
                }
        );
        Assert.assertEquals(4, counter[0]);
    }

    @Test
    public void updatesFoodstuffWeightInDb() throws InterruptedException {
        Foodstuff foodstuff = getAnyFoodstuffFromDb();
        double weight = 100;
        Date date = new Date();
        final long[] historyId = new long[1];
        databaseWorker.saveFoodstuffToHistory(
                mActivityRule.getActivity(),
                date,
                foodstuff.getId(),
                weight,
                new DatabaseWorker.AddHistoryEntriesCallback() {
                    @Override
                    public void onResult(ArrayList<Long> historyEntriesIds) {
                        historyId[0] = historyEntriesIds.get(0);
                    }
                });

        double newWeight = 200;
        databaseWorker.editWeightInHistoryEntry(mActivityRule.getActivity(), historyId[0], 200, null);

        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
        Cursor cursor2 = database.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME +
                " WHERE " + HistoryContract.ID + "=" + historyId[0], null);
        double updatedWeight = -1;
        while (cursor2.moveToNext()) {
            updatedWeight = cursor2.getDouble(cursor2.getColumnIndex(COLUMN_NAME_WEIGHT));
        }
        Assert.assertEquals(newWeight, updatedWeight);
    }

    @Test
    public void updatesFoodstuffIdInHistory() throws InterruptedException {
        // вставить в таблицу foodstuffs 2 фудстаффа
        final Foodstuff foodstuff1 = new Foodstuff("продукт1", 1, 1, 1, 1, 1);
        Foodstuff foodstuff2 = new Foodstuff("продукт2", 1, 1, 1, 1, 1);
        final long[] foodstuff1Id = {-1};
        final long[] foodstuff2Id = {-1};
        databaseWorker.saveFoodstuff(mActivityRule.getActivity(), foodstuff1, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {
                foodstuff1Id[0] = id;
            }
            @Override
            public void onDuplication() {
                throw new RuntimeException("Видимо, продукт уже существует");
            }
        });
        databaseWorker.saveFoodstuff(
                mActivityRule.getActivity(),
                foodstuff2,
                new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {
                foodstuff2Id[0] = id;
            }

            @Override
            public void onDuplication() {
                throw new RuntimeException("Видимо, продукт уже существует");
            }
        });

        // добавить в историю первый продукт
        double weight = 100;
        Date date = new Date();
        final long[] historyId = new long[1];
        databaseWorker.saveFoodstuffToHistory(
                mActivityRule.getActivity(),
                date,
                foodstuff1Id[0],
                weight,
                new DatabaseWorker.AddHistoryEntriesCallback() {
                    @Override
                    public void onResult(ArrayList<Long> historyEntriesIds) {
                        historyId[0] = historyEntriesIds.get(0);
                    }
                });

        // заменить foodstuff_id в записи истории с 1 на 2
        databaseWorker.updateFoodstuffIdInHistory(
                mActivityRule.getActivity(), historyId[0], foodstuff2Id[0], null);

        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = database.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME +
                " WHERE " + HistoryContract.ID + "=" + historyId[0], null);
        long updatedId = -1;
        while (cursor.moveToNext()) {
            updatedId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_ID));
        }
        Assert.assertEquals(foodstuff2Id[0], updatedId);
    }

    @Test
    public void canSaveListedProductSameAsUnlisted() throws InterruptedException {
        Foodstuff foodstuff = new Foodstuff("falafel", -1, 10, 10, 10, 100);
        final long[] id = {-1};
        databaseWorker.saveUnlistedFoodstuff(
                mActivityRule.getActivity(),
                foodstuff,
                new DatabaseWorker.SaveUnlistedFoodstuffCallback() {
            @Override
            public void onResult(long foodstuffId) {
                id[0] = foodstuffId;
            }
        });

        databaseWorker.makeFoodstuffUnlisted(mActivityRule.getActivity(), id[0], null);

        final boolean[] containsListedFoodstuff = new boolean[1];
        databaseWorker.saveFoodstuff(
                mActivityRule.getActivity(),
                foodstuff,
                new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {
                containsListedFoodstuff[0] = false;
            }

            @Override
            public void onDuplication() {
                containsListedFoodstuff[0] = true;
            }
        });
        Assert.assertEquals(false, containsListedFoodstuff[0]);
    }

    @Test
    public void requestFoodstuffsIdsFromHistoryForPeriodWorks() throws InterruptedException {
        // создаем 20 продуктов
        int foodstuffsNumber = 20;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        for (int index = 0; index < foodstuffsNumber; index++) {
            foodstuffs[index] = new Foodstuff("foodstuff" + index, -1, 5, 5, 5, 5);
        }

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                mActivityRule.getActivity(),
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
            @Override
            public void onResult(ArrayList<Long> ids) {
                foodstuffsIds.addAll(ids);
            }
        });
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // сохраняем продукты в историю
        NewHistoryEntry[] newEntries = new NewHistoryEntry[foodstuffsIds.size()];
        for (int index = 0; index < foodstuffsIds.size(); index++) {
            double weight = 100;
            newEntries[index] = new NewHistoryEntry(
                    foodstuffsIds.get(index), weight, new Date(117, 0, index + 1));
        }

        final ArrayList<Long> historyIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffsToHistory(
                mActivityRule.getActivity(),
                newEntries,
                new DatabaseWorker.AddHistoryEntriesCallback() {
                    @Override
                    public void onResult(ArrayList<Long> historyEntriesIds) {
                        historyIds.addAll(historyEntriesIds);
                    }
                }
        );
        Assert.assertEquals(foodstuffsNumber, historyIds.size());

        // запрашиваем продукты с 3 по 5 января (д.б. три продукта - 3, 4, 5)
        final ArrayList<Long> foodstuffsForPeriodIds = new ArrayList<>();
        databaseWorker.requestFoodstuffsIdsFromHistoryForPeriod(
                new Date(117, 0, 3).getTime(),
                new Date(117, 0, 5).getTime(),
                mActivityRule.getActivity(),
                new DatabaseWorker.RequestFoodstuffsIdsFromHistoryCallback() {
            @Override
            public void onResult(ArrayList<Long> ids) {
                foodstuffsForPeriodIds.addAll(ids);
            }
        });
        Assert.assertEquals(3, foodstuffsForPeriodIds.size());
        Assert.assertTrue(foodstuffsForPeriodIds.contains(foodstuffsIds.get(2)));
        Assert.assertTrue(foodstuffsForPeriodIds.contains(foodstuffsIds.get(3)));
        Assert.assertTrue(foodstuffsForPeriodIds.contains(foodstuffsIds.get(4)));
    }

    @Test
    public void checkSaveGroupOfFoodstuffsCallbackCallsCount() throws InterruptedException {
        // создаем 10 продуктов
        int foodstuffsNumber = 10;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        for (int index = 0; index < foodstuffsNumber; index++) {
            foodstuffs[index] = new Foodstuff("foodstuff" + index, -1, 5, 5, 5, 5);
        }

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                mActivityRule.getActivity(),
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(ArrayList<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // Запрашиваем все фудстафы с размером батча 3
        int batchSize = 3;
        final int[] counter = {0};
        databaseWorker.requestListedFoodstuffsFromDb(
                mActivityRule.getActivity(),
                batchSize,
                new DatabaseWorker.FoodstuffsRequestCallback() {
                    @Override
                    public void onResult(ArrayList<Foodstuff> foodstuffs) {
                        ++counter[0];
                    }
                });
        Assert.assertEquals(4, counter[0]);
    }

    @Test
    public void checkReturnedFoodstuffs() throws InterruptedException {
        // создаем 10 продуктов
        int foodstuffsNumber = 10;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        for (int index = 0; index < foodstuffsNumber; index++) {
            foodstuffs[index] = new Foodstuff("foodstuff" + index, -1, 5, 5, 5, 5);
        }

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                mActivityRule.getActivity(),
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(ArrayList<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // Запрашиваем все фудстафы с размером батча 3
        int batchSize = 3;
        final ArrayList<Foodstuff> returnedFoodstuffs = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                mActivityRule.getActivity(),
                batchSize,
                new DatabaseWorker.FoodstuffsRequestCallback() {
                    @Override
                    public void onResult(ArrayList<Foodstuff> foodstuffs) {
                        returnedFoodstuffs.addAll(foodstuffs);
                    }
                });
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
    public void listOfRequestedFoodstuffsReturnedInCorrectOrder() throws InterruptedException {
        // создаем продукт с названиями с маленькой и заглавной букв
        int foodstuffsNumber = 3;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        String apple = "Яблоко";
        foodstuffs[0] = new Foodstuff("абрикос", -1, 5, 5, 5, 5);
        foodstuffs[1] = new Foodstuff("Абрикос", -1, 5, 5, 5, 5);
        foodstuffs[2] = new Foodstuff(apple, -1, 5, 5, 5, 5);

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                mActivityRule.getActivity(),
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(ArrayList<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // Запрашиваем все фудстафы
        int batchSize = 4;
        final ArrayList<Foodstuff> returnedFoodstuffs = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                mActivityRule.getActivity(),
                batchSize,
                new DatabaseWorker.FoodstuffsRequestCallback() {
                    @Override
                    public void onResult(ArrayList<Foodstuff> foodstuffs) {
                        returnedFoodstuffs.addAll(foodstuffs);
                    }
                });
        Assert.assertEquals(apple, returnedFoodstuffs.get(2).getName());
    }

    private void clearTable(String tableName) {
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
        database.delete(tableName, null, null);
        Cursor cursor = database.rawQuery("SELECT * FROM " + tableName, null);
        Assert.assertTrue(cursor.getCount() == 0);
        cursor.close();
    }

    public Foodstuff getAnyFoodstuffFromDb() throws InterruptedException {
        final ArrayList<Foodstuff> foodstuffArrayList = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                mActivityRule.getActivity(),
                20,
                new DatabaseWorker.FoodstuffsRequestCallback() {
            @Override
            public void onResult(ArrayList<Foodstuff> foodstuffs) {
                foodstuffArrayList.addAll(foodstuffs);
            }
        });

        if (foodstuffArrayList.size() != 0) {
            return foodstuffArrayList.get(0);
        }

        Foodstuff foodstuff = new Foodstuff("apricot", -1, 10, 10, 10, 10);
        databaseWorker.saveFoodstuff(mActivityRule.getActivity(), foodstuff, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {}
            @Override
            public void onDuplication() {}
        });
        return getAnyFoodstuffFromDb();
    }
}
