package korablique.recipecalculator.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.ui.calculator.CalculatorActivity;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_WEIGHT;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DatabaseWorkerTest {
    @Rule
    public ActivityTestRule<CalculatorActivity> mActivityRule =
            new ActivityTestRule<>(CalculatorActivity.class);

    @Test
    public void requestListedFoodstuffsFromDbWorks() throws InterruptedException {
        clearTable(FOODSTUFFS_TABLE_NAME);

        final CountDownLatch mutex = new CountDownLatch(4);
        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        Foodstuff foodstuff1 = new Foodstuff("продукт1", 1, 1, 1, 1, 1);
        Foodstuff foodstuff2 = new Foodstuff("продукт2", 1, 1, 1, 1, 1);
        Foodstuff foodstuff3 = new Foodstuff("продукт3", 1, 1, 1, 1, 1);
        Foodstuff foodstuff4 = new Foodstuff("продукт4", 1, 1, 1, 1, 1);
        databaseWorker.saveFoodstuff(mActivityRule.getActivity(), foodstuff1, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(boolean hasAlreadyContainsFoodstuff, long id) {
                mutex.countDown();
            }
        });
        databaseWorker.saveFoodstuff(mActivityRule.getActivity(), foodstuff2, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(boolean hasAlreadyContainsFoodstuff, long id) {
                mutex.countDown();
            }
        });
        //сохраняем два unlisted foodstuff'а
        databaseWorker.saveUnlistedFoodstuff(mActivityRule.getActivity(), foodstuff3, new DatabaseWorker.SaveUnlistedFoodstuffCallback() {
            @Override
            public void onResult(long foodstuffId) {
                mutex.countDown();
            }
        });
        databaseWorker.saveUnlistedFoodstuff(mActivityRule.getActivity(), foodstuff4, new DatabaseWorker.SaveUnlistedFoodstuffCallback() {
            @Override
            public void onResult(long foodstuffId) {
                mutex.countDown();
            }
        });
        mutex.await();

        final CountDownLatch mutex2 = new CountDownLatch(1);
        final int[] listedFoodstuffsCount = new int[1];
        databaseWorker.requestListedFoodstuffsFromDb(mActivityRule.getActivity(), new DatabaseWorker.FoodstuffsRequestCallback() {
            @Override
            public void onResult(ArrayList<Foodstuff> foodstuffs) {
                listedFoodstuffsCount[0] = foodstuffs.size();
                mutex2.countDown();
            }
        });
        mutex2.await();

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

        database.delete(HISTORY_TABLE_NAME, null, null);
        Cursor cursorBeforeSaving = database.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME, null);
        int entriesCountBeforeSaving = cursorBeforeSaving.getCount();
        Assert.assertTrue(cursorBeforeSaving.getCount() == 0);
        cursorBeforeSaving.close();

        Foodstuff foodstuff = getAnyFoodstuffFromDb();

        final CountDownLatch mutex = new CountDownLatch(1);
        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        Date date = new Date();
        databaseWorker.saveFoodstuffToHistory(
                mActivityRule.getActivity(), date, foodstuff.getId(), 100, new DatabaseWorker.AddHistoryEntryCallback() {
            @Override
            public void onResult(long historyEntryId) {
                mutex.countDown();
            }
        });
        mutex.await();
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
        clearTable(HISTORY_TABLE_NAME);

        final CountDownLatch mutex = new CountDownLatch(1);
        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        Foodstuff foodstuff = getAnyFoodstuffFromDb();
        double weight = 100;
        Date date = new Date();
        databaseWorker.saveFoodstuffToHistory(
                mActivityRule.getActivity(), date, foodstuff.getId(), weight, new DatabaseWorker.AddHistoryEntryCallback() {
                    @Override
                    public void onResult(long historyEntryId) {
                        mutex.countDown();
                    }
                });
        mutex.await();

        final CountDownLatch mutex1 = new CountDownLatch(1);
        final ArrayList<HistoryEntry> historyList = new ArrayList<>();
        databaseWorker.requestAllHistoryFromDb(mActivityRule.getActivity(), new DatabaseWorker.RequestHistoryCallback() {
            @Override
            public void onResult(ArrayList<HistoryEntry> historyEntries) {
                historyList.addAll(historyEntries);
                mutex1.countDown();
            }
        });
        mutex1.await();
        Assert.assertEquals(1, historyList.size());
        Assert.assertEquals(historyList.get(0).getFoodstuff().getId(), foodstuff.getId());
        Assert.assertEquals(historyList.get(0).getTime(), date);
    }

    @Test
    public void updatesFoodstuffWeightInDb() throws InterruptedException {
        clearTable(HISTORY_TABLE_NAME);

        final CountDownLatch mutex = new CountDownLatch(1);
        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        Foodstuff foodstuff = getAnyFoodstuffFromDb();
        double weight = 100;
        Date date = new Date();
        final long[] historyId = new long[1];
        databaseWorker.saveFoodstuffToHistory(
                mActivityRule.getActivity(), date, foodstuff.getId(), weight, new DatabaseWorker.AddHistoryEntryCallback() {
                    @Override
                    public void onResult(long historyEntryId) {
                        historyId[0] = historyEntryId;
                        mutex.countDown();
                    }
                });
        mutex.await();

        final CountDownLatch mutex2 = new CountDownLatch(1);
        double newWeight = 200;
        databaseWorker.editWeightInHistoryEntry(mActivityRule.getActivity(), historyId[0], 200, new Runnable() {
            @Override
            public void run() {
                mutex2.countDown();
            }
        });
        mutex2.await();

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
        clearTable(HISTORY_TABLE_NAME);
        clearTable(FOODSTUFFS_TABLE_NAME);

        // вставить в таблицу foodstuffs 2 фудстаффа
        final CountDownLatch mutex1 = new CountDownLatch(2);
        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        final Foodstuff foodstuff1 = new Foodstuff("продукт1", 1, 1, 1, 1, 1);
        Foodstuff foodstuff2 = new Foodstuff("продукт2", 1, 1, 1, 1, 1);
        final long[] foodstuff1Id = {-1};
        final long[] foodstuff2Id = {-1};
        databaseWorker.saveFoodstuff(mActivityRule.getActivity(), foodstuff1, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(boolean hasAlreadyContainsFoodstuff, long id) {
                foodstuff1Id[0] = id;
                mutex1.countDown();
            }
        });
        databaseWorker.saveFoodstuff(mActivityRule.getActivity(), foodstuff2, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(boolean hasAlreadyContainsFoodstuff, long id) {
                foodstuff2Id[0] = id;
                mutex1.countDown();
            }
        });
        mutex1.await();

        // добавить в историю первый продукт
        final CountDownLatch mutex2 = new CountDownLatch(1);
        double weight = 100;
        Date date = new Date();
        final long[] historyId = new long[1];
        databaseWorker.saveFoodstuffToHistory(
                mActivityRule.getActivity(), date, foodstuff1Id[0], weight, new DatabaseWorker.AddHistoryEntryCallback() {
                    @Override
                    public void onResult(long historyEntryId) {
                        historyId[0] = historyEntryId;
                        mutex2.countDown();
                    }
                });
        mutex2.await();

        // заменить foodstuff_id в записи истории с 1 на 2
        final CountDownLatch mutex3 = new CountDownLatch(1);
        databaseWorker.updateFoodstuffIdInHistory(
                mActivityRule.getActivity(), historyId[0], foodstuff2Id[0], new Runnable() {
            @Override
            public void run() {
                mutex3.countDown();
            }
        });
        mutex3.await();

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

    private void clearTable(String tableName) {
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
        database.delete(tableName, null, null);
        Cursor cursor = database.rawQuery("SELECT * FROM " + tableName, null);
        Assert.assertTrue(cursor.getCount() == 0);
        cursor.close();
    }

    public Foodstuff getAnyFoodstuffFromDb() throws InterruptedException {
        final CountDownLatch mutex = new CountDownLatch(1);
        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        final ArrayList<Foodstuff> foodstuffArrayList = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(mActivityRule.getActivity(), new DatabaseWorker.FoodstuffsRequestCallback() {
            @Override
            public void onResult(ArrayList<Foodstuff> foodstuffs) {
                foodstuffArrayList.addAll(foodstuffs);
                mutex.countDown();
            }
        });
        mutex.await();
        return foodstuffArrayList.get(0);
    }
}
