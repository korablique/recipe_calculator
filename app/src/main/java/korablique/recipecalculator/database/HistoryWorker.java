package korablique.recipecalculator.database;

import android.database.Cursor;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.WeightedFoodstuff;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_WEIGHT;

@Singleton
public class HistoryWorker {
    public static final int BATCH_SIZE = 20;
    private static final int NO_LIMIT = -1;

    private DatabaseHolder databaseHolder;
    private DatabaseWorker databaseWorker;
    private DatabaseThreadExecutor databaseThreadExecutor;
    private MainThreadExecutor mainThreadExecutor;

    private List<HistoryEntry> cachedValues = new ArrayList<>();

    public interface RequestHistoryCallback {
        void onResult(List<HistoryEntry> historyEntries);
    }

    public interface AddHistoryEntriesCallback {
        void onResult(List<Long> historyEntriesIds);
    }

    public interface RequestFoodstuffsIdsFromHistoryCallback {
        void onResult(List<Long> ids);
    }

    @Inject
    public HistoryWorker(
            DatabaseHolder databaseHolder,
            DatabaseWorker databaseWorker,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor) {
        this.databaseHolder = databaseHolder;
        this.databaseWorker = databaseWorker;
        this.mainThreadExecutor = mainThreadExecutor;
        this.databaseThreadExecutor = databaseThreadExecutor;
    }

    public void initCache() {
        databaseThreadExecutor.execute(() -> updateCache());
    }

    /**
     * Обновляет кеш первого батча.
     * Выполняется на том же потоке, на котором вызван.
     */
    private void updateCache() {
        requestHistoryFromDbImpl(BATCH_SIZE, (historyEntries) -> {
            cachedValues.clear();
            cachedValues.addAll(historyEntries);
        });
    }

    public void requestAllHistoryFromDb(
            @NonNull final RequestHistoryCallback callback) {
        requestHistoryPartFromDb(NO_LIMIT, callback);
    }

    private void requestHistoryPartFromDb(
            final int limit,
            @NonNull final RequestHistoryCallback callback) {
        databaseThreadExecutor.execute(() -> {
            requestHistoryFromDbImpl(limit, historyEntries -> {
                List<HistoryEntry> historyEntriesCopy = new ArrayList<>(historyEntries);
                mainThreadExecutor.execute(() -> callback.onResult(historyEntriesCopy));
            });
        });
    }

    /**
     * Фактическая реализация запроса продуктов из БД.
     * Метод выполняется на том же потоке, на котором вызван. Фактически не выполняется вне
     * DatabaseThreadExecutor.
     * Сделано так, чтобы клиенты метода могли вызвать его внутри другого кода, уже работающего в
     * DatabaseThreadExecutor.
     */
    private void requestHistoryFromDbImpl(
            final int limit,
            @NonNull final RequestHistoryCallback historyBatchesCallback) {
        boolean cacheWasUsed = false;
        if (!cachedValues.isEmpty()) {
            historyBatchesCallback.onResult(cachedValues);
            cacheWasUsed = true;
        }

//        DbHelper dbHelper = new DbHelper(context);
//        SQLiteDatabase db = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
//        String joinTablesArg = HISTORY_TABLE_NAME + " LEFT OUTER" +
//                " JOIN " + FOODSTUFFS_TABLE_NAME +
//                " ON " + HISTORY_TABLE_NAME + "." + COLUMN_NAME_FOODSTUFF_ID
//                + "=" + FOODSTUFFS_TABLE_NAME + "." + FoodstuffsContract.ID;
//
//        Cursor cursor;
//        if (limit == NO_LIMIT) {
//            cursor = db.query(joinTablesArg, null, null, null, null, null, COLUMN_NAME_DATE + " DESC");
//        } else {
//            cursor = db.query(joinTablesArg, null, null, null, null, null, COLUMN_NAME_DATE + " DESC", String.valueOf(limit));
//        }
//
//        // Если кэш уже отправлен, первый батч будет такой же, как кэш
//        // - первый батч отправлять не нужно.
//        boolean shouldIgnoreNextBatch = cacheWasUsed;
//
//        ArrayList<HistoryEntry> historyBatch = new ArrayList<>();
//        int index = 0;
//        while (cursor.moveToNext()) {
//            long foodstuffId = cursor.getLong(
//                    cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_ID));
//            double weight = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_WEIGHT));
//            String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
//            double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
//            double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
//            double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
//            double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
//
//            WeightedFoodstuff foodstuff = Foodstuff
//                    .withId(foodstuffId)
//                    .withName(name)
//                    .withNutrition(protein, fats, carbs, calories)
//                    .withWeight(weight);
//
//            long time = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_DATE));
//            long historyId = cursor.getLong(cursor.getColumnIndex(HistoryContract.ID));
//            HistoryEntry historyEntry = new HistoryEntry(historyId, foodstuff, new Date(time));
//            historyBatch.add(historyEntry);
//            ++index;
//            if (index >= BATCH_SIZE) {
//                if (!shouldIgnoreNextBatch) {
//                    callback.onResult(historyBatch);
//                }
//                historyBatch.clear();
//                index = 0;
//                shouldIgnoreNextBatch = false;
//            }
//        }
//        if (historyBatch.size() > 0 && !shouldIgnoreNextBatch) {
//            callback.onResult(historyBatch);
//        }
//        cursor.close();

        AppDatabase database = databaseHolder.getDatabase();
        HistoryDao historyDao = database.historyDao();
        Cursor cursor;
        if (limit == NO_LIMIT) {
            cursor = historyDao.loadHistory();
        } else {
            cursor = historyDao.loadHistoryWithLimit(limit);
        }

        // Если кэш уже отправлен, первый батч будет такой же, как кэш
        // - первый батч отправлять не нужно.
        boolean shouldIgnoreNextBatch = cacheWasUsed;

        List<HistoryEntry> historyBatch = new ArrayList<>();
        int index = 0;
        while (cursor.moveToNext()) {
            long foodstuffId = cursor.getLong(
                    cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_ID));
            double weight = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_WEIGHT));
            String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
            double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
            double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
            double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
            double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));

            WeightedFoodstuff foodstuff = Foodstuff
                    .withId(foodstuffId)
                    .withName(name)
                    .withNutrition(protein, fats, carbs, calories)
                    .withWeight(weight);

            long time = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_DATE));
            long historyId = cursor.getLong(cursor.getColumnIndex(HistoryContract.ID));
            HistoryEntry historyEntry = new HistoryEntry(historyId, foodstuff, new Date(time));
            historyBatch.add(historyEntry);
            ++index;
            if (index >= BATCH_SIZE) {
                if (!shouldIgnoreNextBatch) {
                    historyBatchesCallback.onResult(historyBatch);
                }
                historyBatch.clear();
                index = 0;
                shouldIgnoreNextBatch = false;
            }
        }
        if (historyBatch.size() > 0 && !shouldIgnoreNextBatch) {
            historyBatchesCallback.onResult(historyBatch);
        }
        cursor.close();
    }

    public void saveFoodstuffToHistory(
            Date date,
            long foodstuffId,
            double foodstuffWeight) {
        saveFoodstuffToHistory(date, foodstuffId, foodstuffWeight, null);
    }

    public void saveFoodstuffToHistory(
            final Date date,
            final long foodstuffId,
            final double foodstuffWeight,
            final AddHistoryEntriesCallback callback) {
        NewHistoryEntry[] array = new NewHistoryEntry[]{new NewHistoryEntry(foodstuffId, foodstuffWeight, date)};
        saveGroupOfFoodstuffsToHistory(array, callback);
    }

    public void saveGroupOfFoodstuffsToHistory(final NewHistoryEntry[] newEntries) {
        saveGroupOfFoodstuffsToHistory(newEntries, null);
    }

    public void saveGroupOfFoodstuffsToHistory(
            final NewHistoryEntry[] newEntries,
            final AddHistoryEntriesCallback callback) {
        databaseThreadExecutor.execute(() -> {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
//
//            final ArrayList<Long> ids = new ArrayList<>();
//            database.beginTransaction();
//            try {
//                for (NewHistoryEntry newEntry : newEntries) {
//                    ContentValues values = new ContentValues();
//                    values.put(COLUMN_NAME_DATE, newEntry.getDate().getTime());
//                    values.put(COLUMN_NAME_FOODSTUFF_ID, newEntry.getFoodstuffId());
//                    values.put(COLUMN_NAME_WEIGHT, newEntry.getFoodstuffWeight());
//                    long historyEntryId = database.insert(HISTORY_TABLE_NAME, null, values);
//                    ids.add(historyEntryId);
//                }
//                database.setTransactionSuccessful();
//            } finally {
//                database.endTransaction();
//            }
//            if (callback != null) {
//                mainThreadExecutor.execute(() -> callback.onResult(ids));
//            }
//
//            updateCache();
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            List<HistoryEntity> historyEntities = new ArrayList<>();
            for (NewHistoryEntry historyEntry : newEntries) {
                HistoryEntity historyEntity = new HistoryEntity(
                        historyEntry.getDate().getTime(),
                        historyEntry.getFoodstuffId(),
                        (float) historyEntry.getFoodstuffWeight());
                historyEntities.add(historyEntity);
            }

            List<Long> historyEntitiesIds = historyDao.insertHistoryEntities(historyEntities);
            if (callback != null) {
                mainThreadExecutor.execute(() -> callback.onResult(historyEntitiesIds));
            }
            updateCache();
        });
    }

    public void deleteEntryFromHistory(final HistoryEntry historyEntry) {
        databaseThreadExecutor.execute(() -> {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
//            database.delete(
//                    HISTORY_TABLE_NAME,
//                    HistoryContract.ID + " = ?",
//                    new String[]{String.valueOf(historyId)});
//
//            updateCache();
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            HistoryEntity historyEntity = new HistoryEntity(
                    historyEntry.getTime().getTime(), // TODO: 21.01.19 get time get time
                    historyEntry.getHistoryId(),
                    (float) historyEntry.getFoodstuff().getWeight());
            historyDao.deleteHistoryEntity(historyEntity);
            updateCache();
        });
    }

    public void editWeightInHistoryEntry(final long historyId, final double weight) {
        editWeightInHistoryEntry(historyId, weight, null);
    }

    public void editWeightInHistoryEntry(
            final long historyId,
            final double weight,
            final Runnable callback) {
        databaseThreadExecutor.execute(() -> {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
//            ContentValues values = new ContentValues();
//            values.put(COLUMN_NAME_WEIGHT, newWeight);
//            database.update(
//                    HISTORY_TABLE_NAME,
//                    values,
//                    HistoryContract.ID + "=?",
//                    new String[]{String.valueOf(historyId)});
//            if (callback != null) {
//                mainThreadExecutor.execute(callback);
//            }
//
//            updateCache();
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            historyDao.updateWeight(historyId, weight);
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }
            updateCache();
        });
    }

    public void updateFoodstuffIdInHistory(
            final long historyId,
            final long foodstuffId) {
        updateFoodstuffIdInHistory(historyId, foodstuffId, null);
    }

    public void updateFoodstuffIdInHistory(
            final long historyId,
            final long foodstuffId,
            final Runnable callback) {
        databaseThreadExecutor.execute(() -> {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
//            ContentValues values = new ContentValues(1);
//            values.put(COLUMN_NAME_FOODSTUFF_ID, newFoodstuffId);
//            database.update(
//                    HISTORY_TABLE_NAME,
//                    values,
//                    HistoryContract.ID + "=?",
//                    new String[]{String.valueOf(historyId)});
//            if (callback != null) {
//                mainThreadExecutor.execute(callback);
//            }
//
//            updateCache();
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            historyDao.updateFoodstuff(historyId, foodstuffId);
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }
            updateCache();
        });
    }

    public void requestFoodstuffsIdsFromHistoryForPeriod(
            final long from,
            final long to,
            @NonNull final RequestFoodstuffsIdsFromHistoryCallback callback) {
        databaseThreadExecutor.execute(() -> {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
//            Cursor cursor = database.query(
//                    HISTORY_TABLE_NAME,
//                    new String[]{ COLUMN_NAME_FOODSTUFF_ID },
//                    COLUMN_NAME_DATE + " >= ? AND " + COLUMN_NAME_DATE + " <= ?",
//                    new String[]{ String.valueOf(from), String.valueOf(to) },
//                    null,
//                    null,
//                    COLUMN_NAME_DATE + " ASC");
//
//            ArrayList<Long> ids = new ArrayList<>();
//            while (cursor.moveToNext()) {
//                long id = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_ID));
//                ids.add(id);
//            }
//            cursor.close();
//            mainThreadExecutor.execute(() -> callback.onResult(ids));

            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            List<Long> foodstuffIdsForPeriod = historyDao.loadFoodstuffsIdsForPeriod(from, to);
            mainThreadExecutor.execute(() -> callback.onResult(foodstuffIdsForPeriod));
        });
    }
}
