package korablique.recipecalculator.database;

import android.database.Cursor;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.database.room.AppDatabase;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.database.room.HistoryDao;
import korablique.recipecalculator.database.room.HistoryEntity;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.WeightedFoodstuff;

import static korablique.recipecalculator.database.EntityConverter.toEntity;
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
    private DatabaseThreadExecutor databaseThreadExecutor;
    private MainThreadExecutor mainThreadExecutor;
    private TimeProvider timeProvider;

    private List<HistoryChangeObserver> observers = new ArrayList<>();

    private volatile Observable<HistoryEntry> cachedHistory;
    private volatile long cachedHistoryStart = -1;
    private volatile long cachedHistoryEnd = -1;

    public interface HistoryChangeObserver {
        void onHistoryChange();
    }

    @Inject
    public HistoryWorker(
            DatabaseHolder databaseHolder,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor,
            TimeProvider timeProvider) {
        this.databaseHolder = databaseHolder;
        this.mainThreadExecutor = mainThreadExecutor;
        this.databaseThreadExecutor = databaseThreadExecutor;
        this.timeProvider = timeProvider;
    }

    public void addHistoryChangeObserver(HistoryChangeObserver observer) {
        observers.add(observer);
    }

    public void removeHistoryChangeObserver(HistoryChangeObserver observer) {
        observers.remove(observer);
    }

    private void notifyObserversAboutHistoryChange() {
        mainThreadExecutor.execute(() -> {
            for (HistoryChangeObserver observer : observers) {
                observer.onHistoryChange();
            }
        });
    }

    @AnyThread
    public void updateCache() {
        // Синхронизируем, чтобы значения полей не разъехались
        // (если cachedHistoryStart будет иметь сегодняшний день в качестве значения,
        // но cachedHistory не будет внутри себя иметь продукты за севодняшний день, то
        // это баг).
        synchronized (this) {
            long from = timeProvider.now().withTimeAtStartOfDay().getMillis();
            long to = Long.MAX_VALUE;
            cachedHistory = requestHistoryForPeriod(from, to, true);
            cachedHistory = cachedHistory.cache();
            cachedHistory.subscribe();
            cachedHistoryStart = from;
            cachedHistoryEnd = to;
        }
    }

    public void requestAllHistoryFromDb(
            @NonNull final Callback<List<HistoryEntry>> callback) {
        requestHistoryPartFromDb(NO_LIMIT, callback);
    }

    private void requestHistoryPartFromDb(
            final int limit,
            @NonNull final Callback<List<HistoryEntry>> callback) {
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
            @NonNull final Callback<List<HistoryEntry>> historyBatchesCallback) {
        AppDatabase database = databaseHolder.getDatabase();
        HistoryDao historyDao = database.historyDao();
        Cursor cursor;
        if (limit == NO_LIMIT) {
            cursor = historyDao.loadHistory();
        } else {
            cursor = historyDao.loadHistoryWithLimit(limit);
        }

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
                historyBatchesCallback.onResult(historyBatch);
                historyBatch.clear();
                index = 0;
            }
        }
        if (historyBatch.size() > 0) {
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
            final Callback<List<Long>> callback) {
        NewHistoryEntry[] array = new NewHistoryEntry[]{new NewHistoryEntry(foodstuffId, foodstuffWeight, date)};
        saveGroupOfFoodstuffsToHistory(array, callback);
    }

    public void saveGroupOfFoodstuffsToHistory(final NewHistoryEntry[] newEntries) {
        saveGroupOfFoodstuffsToHistory(newEntries, null);
    }

    public void saveGroupOfFoodstuffsToHistory(
            final NewHistoryEntry[] newEntries,
            final Callback<List<Long>> callback) {
        databaseThreadExecutor.execute(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            List<HistoryEntity> historyEntities = new ArrayList<>();
            for (NewHistoryEntry historyEntry : newEntries) {
                historyEntities.add(toEntity(historyEntry));
            }

            List<Long> historyEntitiesIds = historyDao.insertHistoryEntities(historyEntities);
            if (callback != null) {
                mainThreadExecutor.execute(() -> callback.onResult(historyEntitiesIds));
            }
            updateCache();
            notifyObserversAboutHistoryChange();
        });
    }

    public void deleteEntryFromHistory(final HistoryEntry historyEntry) {
        databaseThreadExecutor.execute(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            historyDao.deleteHistoryEntity(historyEntry.getHistoryId());
            updateCache();
            notifyObserversAboutHistoryChange();
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
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            historyDao.updateWeight(historyId, weight);
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }
            updateCache();
            notifyObserversAboutHistoryChange();
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
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            historyDao.updateFoodstuff(historyId, foodstuffId);
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }
            updateCache();
            notifyObserversAboutHistoryChange();
        });
    }

    public void requestFoodstuffsIdsFromHistoryForPeriod(
            final long from,
            final long to,
            @NonNull final Callback<List<Long>> callback) {
        databaseThreadExecutor.execute(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            List<Long> foodstuffIdsForPeriod = historyDao.loadFoodstuffsIdsForPeriod(from, to);
            mainThreadExecutor.execute(() -> callback.onResult(foodstuffIdsForPeriod));
        });
    }

    public Observable<Foodstuff> requestListedFoodstuffsFromHistoryForPeriod(
            final long from,
            final long to) {
        Observable<Foodstuff> result = Observable.create((subscriber) -> {
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            Cursor cursor = historyDao.loadListedFoodstuffsFromHistoryForPeriod(from, to);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(
                        cursor.getColumnIndex(FoodstuffsContract.ID));
                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
                double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
                double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
                double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
                double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
                Foodstuff foodstuff = Foodstuff.withId(id).withName(name).withNutrition(protein, fats, carbs, calories);
                subscriber.onNext(foodstuff);
            }
            subscriber.onComplete();
        });
        result = result.subscribeOn(databaseThreadExecutor.asScheduler())
                .observeOn(mainThreadExecutor.asScheduler());
        return result;
    }

    public Observable<HistoryEntry> requestHistoryForPeriod(final long from, final long to) {
        return requestHistoryForPeriod(from, to, false);
    }

    private Observable<HistoryEntry> requestHistoryForPeriod(
            final long from, final long to, final boolean ignoreCache) {
        if (!ignoreCache && cachedHistoryStart <= from && to <= cachedHistoryEnd) {
            return cachedHistory.filter(entry -> {
                long entryTime = entry.getTime().getTime();
                return from <= entryTime && entryTime <= to;
            });
        }
        Observable<HistoryEntry> result = Observable.create((subscriber) -> {
            AppDatabase database = databaseHolder.getDatabase();
            HistoryDao historyDao = database.historyDao();
            Cursor cursor = historyDao.loadHistoryForPeriod(from, to);
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
                subscriber.onNext(historyEntry);
            }
            subscriber.onComplete();
        });
        result = result.subscribeOn(databaseThreadExecutor.asScheduler())
                .observeOn(mainThreadExecutor.asScheduler());
        return result;
    }
}
