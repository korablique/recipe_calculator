package korablique.recipecalculator.database;

import android.content.Context;
import android.database.Cursor;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import io.reactivex.Observable;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.database.room.AppDatabase;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.util.DBTestingUtils;
import korablique.recipecalculator.InstantComputationsThreadsExecutor;
import korablique.recipecalculator.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.InstantMainThreadExecutor;
import korablique.recipecalculator.util.TestingTimeProvider;

import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_WEIGHT;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HistoryWorkerTest {
    private Context context;
    private DatabaseHolder databaseHolder;
    private DatabaseWorker databaseWorker;
    private HistoryWorker historyWorker;
    private TimeProvider timeProvider;
    
    @Before
    public void setUp() throws IOException {
        context = InstrumentationRegistry.getTargetContext();

        DatabaseThreadExecutor databaseThreadExecutor = new InstantDatabaseThreadExecutor();
        databaseHolder = Mockito.spy(new DatabaseHolder(context, databaseThreadExecutor));
        databaseHolder.getDatabase().clearAllTables();
        databaseWorker = new DatabaseWorker(
                databaseHolder, new InstantMainThreadExecutor(), databaseThreadExecutor);
        timeProvider = new TestingTimeProvider();
        historyWorker = new HistoryWorker(
                databaseHolder, new InstantMainThreadExecutor(), databaseThreadExecutor,
                timeProvider);
    }

    @Test
    public void savingToHistoryWorks() throws InterruptedException {
        AppDatabase database = databaseHolder.getDatabase();

        Cursor cursorBeforeSaving = database.query("SELECT * FROM " + HISTORY_TABLE_NAME, null);
        int entriesCountBeforeSaving = cursorBeforeSaving.getCount();
        Assert.assertTrue(cursorBeforeSaving.getCount() == 0);
        cursorBeforeSaving.close();

        Foodstuff foodstuff = getAnyFoodstuffFromDb();

        Date date = new Date();
        historyWorker.saveFoodstuffToHistory(date, foodstuff.getId(), 100);

        Cursor cursorAfterSaving = database.query("SELECT * FROM " + HISTORY_TABLE_NAME, null);
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
            foodstuffs[index] = Foodstuff.withName("foodstuff" + index).withNutrition(5, 5, 5, 5);
        }
        ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                foodstuffs,
                ids -> foodstuffsIds.addAll(ids));

        NewHistoryEntry[] historyEntries = new NewHistoryEntry[foodstuffsNumber];
        double weight = 100;
        for (int index = 0; index < historyEntries.length; index++) {
            historyEntries[index] = new NewHistoryEntry(
                    foodstuffsIds.get(index),
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
        historyWorker.editWeightInHistoryEntry(historyId[0], newWeight);

        AppDatabase database = databaseHolder.getDatabase();
        Cursor cursor2 = database.query("SELECT * FROM " + HISTORY_TABLE_NAME +
                " WHERE " + HistoryContract.ID + "=" + historyId[0], null);
        double updatedWeight = -1;
        while (cursor2.moveToNext()) {
            updatedWeight = cursor2.getDouble(cursor2.getColumnIndex(COLUMN_NAME_WEIGHT));
        }
        Assert.assertEquals(newWeight, updatedWeight);
    }

    @Test
    public void updatesFoodstuffIdInHistory() {
        // вставить в таблицу foodstuffs 2 фудстаффа
        final Foodstuff foodstuff1 = Foodstuff.withName("продукт1").withNutrition(1, 1, 1, 1);
        Foodstuff foodstuff2 = Foodstuff.withName("продукт2").withNutrition(1, 1, 1, 1);
        final long[] foodstuff1Id = {-1};
        final long[] foodstuff2Id = {-1};
        databaseWorker.saveFoodstuff(foodstuff1, new DatabaseWorker.SaveFoodstuffCallback() {
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

        AppDatabase database = databaseHolder.getDatabase();
        Cursor cursor = database.query("SELECT * FROM " + HISTORY_TABLE_NAME +
                " WHERE " + HistoryContract.ID + "=" + historyId[0], null);
        long updatedId = -1;
        while (cursor.moveToNext()) {
            updatedId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_ID));
        }
        Assert.assertEquals(foodstuff2Id[0], updatedId);
    }

    @Test
    public void requestFoodstuffsIdsFromHistoryForPeriodWorks() {
        // создаем 20 продуктов
        int foodstuffsNumber = 20;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        for (int index = 0; index < foodstuffsNumber; index++) {
            foodstuffs[index] = Foodstuff.withName("foodstuff" + index).withNutrition(5, 5, 5, 5);
        }

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
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

    @Test
    public void cacheTest() {
        clearAllData();

        Foodstuff[] foodstuffs = new Foodstuff[] {
                Foodstuff.withName("f1").withNutrition(1, 2, 3, 4),
                Foodstuff.withName("f2").withNutrition(1, 2, 3, 4),
                Foodstuff.withName("f3").withNutrition(1, 2, 3, 4),
                Foodstuff.withName("f4").withNutrition(1, 2, 3, 4),
                Foodstuff.withName("f5").withNutrition(1, 2, 3, 4)
        };
        databaseWorker.saveGroupOfFoodstuffs(foodstuffs, ids -> {
            for (int index = 0; index < foodstuffs.length; ++index) {
                foodstuffs[index] = foodstuffs[index].recreateWithId(ids.get(index));
            }
        });

        long todayMidnight = timeProvider.now().withTimeAtStartOfDay().getMillis();
        long tomorrowMidnight = timeProvider.now().plusDays(1).withTimeAtStartOfDay().getMillis();

        historyWorker.saveFoodstuffToHistory(new Date(todayMidnight-1000), foodstuffs[0].getId(), 123);
        historyWorker.saveFoodstuffToHistory(new Date(todayMidnight-1), foodstuffs[1].getId(), 123);
        historyWorker.saveFoodstuffToHistory(new Date(todayMidnight), foodstuffs[2].getId(), 123);
        historyWorker.saveFoodstuffToHistory(new Date(todayMidnight+1000), foodstuffs[3].getId(), 123);
        historyWorker.saveFoodstuffToHistory(new Date(Long.MAX_VALUE), foodstuffs[4].getId(), 123);

        //
        // Verify today is cached
        //
        reset(databaseHolder);
        Observable<HistoryEntry> todayEntries = historyWorker.requestHistoryForPeriod(
                todayMidnight,
                tomorrowMidnight - 1);
        // Verify correct data
        Assert.assertEquals(2, todayEntries.toList().blockingGet().size());
        Assert.assertEquals(foodstuffs[3], todayEntries.toList().blockingGet().get(0).getFoodstuff().withoutWeight());
        Assert.assertEquals(foodstuffs[2], todayEntries.toList().blockingGet().get(1).getFoodstuff().withoutWeight());
        // Verify that cache was used
        verify(databaseHolder, never()).getDatabase();

        //
        // Verify all future is cached
        //
        reset(databaseHolder);
        Observable<HistoryEntry> todayAndFutureEntries = historyWorker.requestHistoryForPeriod(
                todayMidnight,
                Long.MAX_VALUE);
        // Verify correct data
        Assert.assertEquals(3, todayAndFutureEntries.toList().blockingGet().size());
        Assert.assertEquals(foodstuffs[4], todayAndFutureEntries.toList().blockingGet().get(0).getFoodstuff().withoutWeight());
        Assert.assertEquals(foodstuffs[3], todayAndFutureEntries.toList().blockingGet().get(1).getFoodstuff().withoutWeight());
        Assert.assertEquals(foodstuffs[2], todayAndFutureEntries.toList().blockingGet().get(2).getFoodstuff().withoutWeight());
        // Verify that cache was used
        verify(databaseHolder, never()).getDatabase();

        //
        // Verify past is NOT cached
        //
        reset(databaseHolder);
        Observable<HistoryEntry> pastAndToday = historyWorker.requestHistoryForPeriod(
                0,
                tomorrowMidnight - 1);
        // Verify correct data
        Assert.assertEquals(4, pastAndToday.toList().blockingGet().size());
        Assert.assertEquals(foodstuffs[3], pastAndToday.toList().blockingGet().get(0).getFoodstuff().withoutWeight());
        Assert.assertEquals(foodstuffs[2], pastAndToday.toList().blockingGet().get(1).getFoodstuff().withoutWeight());
        Assert.assertEquals(foodstuffs[1], pastAndToday.toList().blockingGet().get(2).getFoodstuff().withoutWeight());
        Assert.assertEquals(foodstuffs[0], pastAndToday.toList().blockingGet().get(3).getFoodstuff().withoutWeight());
        // Verify that cache was NOT used
        verify(databaseHolder, atLeastOnce()).getDatabase();
    }

    private void clearAllData() {
        FoodstuffsList foodstuffsList = new FoodstuffsList(
                databaseWorker,
                new InstantMainThreadExecutor(),
                new InstantComputationsThreadsExecutor());
        DBTestingUtils.clearAllData(
                foodstuffsList,
                historyWorker,
                databaseHolder);
    }

    public Foodstuff getAnyFoodstuffFromDb() throws InterruptedException {
        final ArrayList<Foodstuff> foodstuffArrayList = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                20,
                (foodstuffs) -> foodstuffArrayList.addAll(foodstuffs));

        if (foodstuffArrayList.size() != 0) {
            return foodstuffArrayList.get(0);
        }

        Foodstuff foodstuff = Foodstuff.withName("apricot").withNutrition(10, 10, 10, 10);
        databaseWorker.saveFoodstuff(foodstuff);
        return getAnyFoodstuffFromDb();
    }
}
