package korablique.recipecalculator.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.util.DbUtil;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.InstantMainThreadExecutor;

import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_WEIGHT;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HistoryWorkerTest {
    private Context context;
    private DatabaseWorker databaseWorker;
    private HistoryWorker historyWorker;
    
    @Before
    public void setUp() throws IOException {
        context = InstrumentationRegistry.getTargetContext();

        FoodstuffsDbHelper.deinitializeDatabase(context);
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
        dbHelper.initializeDatabase();

        DbUtil.clearTable(context, HISTORY_TABLE_NAME);
        DbUtil.clearTable(context, FOODSTUFFS_TABLE_NAME);

        databaseWorker =
                new DatabaseWorker(
                        new InstantMainThreadExecutor(), new InstantDatabaseThreadExecutor());
        historyWorker =
                new HistoryWorker(
                    context, new InstantMainThreadExecutor(), new InstantDatabaseThreadExecutor());
    }

    @Test
    public void savingToHistoryWorks() throws InterruptedException {
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

        Cursor cursorBeforeSaving = database.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME, null);
        int entriesCountBeforeSaving = cursorBeforeSaving.getCount();
        Assert.assertTrue(cursorBeforeSaving.getCount() == 0);
        cursorBeforeSaving.close();

        Foodstuff foodstuff = getAnyFoodstuffFromDb();

        Date date = new Date();
        historyWorker.saveFoodstuffToHistory(date, foodstuff.getId(), 100);

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
        historyWorker.saveFoodstuffToHistory(date, foodstuff.getId(), weight);

        final ArrayList<HistoryEntry> historyList = new ArrayList<>();
        historyWorker.requestAllHistoryFromDb(
                (List<HistoryEntry> historyEntries) -> {
                    historyList.addAll(historyEntries);
                });
        Assert.assertEquals(1, historyList.size());
        Assert.assertEquals(historyList.get(0).getFoodstuff().getId(), foodstuff.getId());
        Assert.assertEquals(historyList.get(0).getTime(), date);
    }

    @Test
    public void checkRequestHistoryCallbackCallsCount() {
        int foodstuffsNumber = HistoryWorker.BATCH_SIZE * 3 + HistoryWorker.BATCH_SIZE / 2; // 3.5 батча
        Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        for (int index = 0; index < foodstuffsNumber; index++) {
            foodstuffs[index] = new Foodstuff("foodstuff" + index, -1, 5, 5, 5, 5);
        }
        ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                context,
                foodstuffs,
                ids -> foodstuffsIds.addAll(ids));

        NewHistoryEntry[] historyEntries = new NewHistoryEntry[foodstuffsNumber];
        double weight = 100;
        for (int index = 0; index < historyEntries.length; index++) {
            historyEntries[index] = new NewHistoryEntry(
                    foodstuffs[index].getId(),
                    weight,
                    new Date(118, 0, index));
        }
        ArrayList<Long> entriesIds = new ArrayList<>();
        historyWorker.saveGroupOfFoodstuffsToHistory(
                historyEntries,
                (historyEntriesIds) -> entriesIds.addAll(historyEntriesIds));

        final int[] counter = {0};
        historyWorker.requestAllHistoryFromDb((historyEntries1) -> {
            ++counter[0];
        });

        int expectedCallsCount = 4; // 4 потому что мы сохранили 3.5 батча фудстафов
        Assert.assertEquals(expectedCallsCount, counter[0]);
    }

    @Test
    public void updatesFoodstuffWeightInDb() throws InterruptedException {
        Foodstuff foodstuff = getAnyFoodstuffFromDb();
        double weight = 100;
        Date date = new Date();
        final long[] historyId = new long[1];
        historyWorker.saveFoodstuffToHistory(
                date,
                foodstuff.getId(),
                weight,
                (historyEntriesIds) -> historyId[0] = historyEntriesIds.get(0));

        double newWeight = 200;
        historyWorker.editWeightInHistoryEntry(historyId[0], 200);

        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
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
        databaseWorker.saveFoodstuff(context, foodstuff1, new DatabaseWorker.SaveFoodstuffCallback() {
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
                context,
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
        historyWorker.saveFoodstuffToHistory(
                date,
                foodstuff1Id[0],
                weight,
                (historyEntriesIds) -> historyId[0] = historyEntriesIds.get(0));

        // заменить foodstuff_id в записи истории с 1 на 2
        historyWorker.updateFoodstuffIdInHistory(historyId[0], foodstuff2Id[0]);

        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
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
                context,
                foodstuffs,
                (ids) -> foodstuffsIds.addAll(ids));
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // сохраняем продукты в историю
        NewHistoryEntry[] newEntries = new NewHistoryEntry[foodstuffsIds.size()];
        for (int index = 0; index < foodstuffsIds.size(); index++) {
            double weight = 100;
            newEntries[index] = new NewHistoryEntry(
                    foodstuffsIds.get(index), weight, new Date(117, 0, index + 1));
        }

        final ArrayList<Long> historyIds = new ArrayList<>();
        historyWorker.saveGroupOfFoodstuffsToHistory(
                newEntries,
                (historyEntriesIds) -> historyIds.addAll(historyEntriesIds)
        );
        Assert.assertEquals(foodstuffsNumber, historyIds.size());

        // запрашиваем продукты с 3 по 5 января (д.б. три продукта - 3, 4, 5)
        final ArrayList<Long> foodstuffsForPeriodIds = new ArrayList<>();
        historyWorker.requestFoodstuffsIdsFromHistoryForPeriod(
                new Date(117, 0, 3).getTime(),
                new Date(117, 0, 5).getTime(),
                (ids) -> foodstuffsForPeriodIds.addAll(ids));
        Assert.assertEquals(3, foodstuffsForPeriodIds.size());
        Assert.assertTrue(foodstuffsForPeriodIds.contains(foodstuffsIds.get(2)));
        Assert.assertTrue(foodstuffsForPeriodIds.contains(foodstuffsIds.get(3)));
        Assert.assertTrue(foodstuffsForPeriodIds.contains(foodstuffsIds.get(4)));
    }

    public Foodstuff getAnyFoodstuffFromDb() throws InterruptedException {
        final ArrayList<Foodstuff> foodstuffArrayList = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                context,
                20,
                (foodstuffs) -> foodstuffArrayList.addAll(foodstuffs));

        if (foodstuffArrayList.size() != 0) {
            return foodstuffArrayList.get(0);
        }

        Foodstuff foodstuff = new Foodstuff("apricot", -1, 10, 10, 10, 10);
        databaseWorker.saveFoodstuff(context, foodstuff);
        return getAnyFoodstuffFromDb();
    }
}
