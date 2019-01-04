package korablique.recipecalculator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.model.Foodstuff;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME_NOCASE;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.ID;

public class DatabaseWorker {
    public static final int NO_LIMIT = -1;
    private DatabaseThreadExecutor databaseThreadExecutor;
    private MainThreadExecutor mainThreadExecutor;

    public interface FoodstuffsBatchReceiveCallback {
        void onReceive(List<Foodstuff> foodstuffs);
    }

    public interface FinishCallback {
        void onFinish();
    }

    public interface SaveFoodstuffCallback {
        void onResult(long id);
        void onDuplication();
    }

    public interface SaveGroupOfFoodstuffsCallback {
        void onResult(ArrayList<Long> ids);
    }

    public interface SaveUnlistedFoodstuffCallback {
        void onResult(long foodstuffId);
    }

    public DatabaseWorker(MainThreadExecutor mainThreadExecutor, DatabaseThreadExecutor databaseThreadExecutor) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.databaseThreadExecutor = databaseThreadExecutor;
    }

    public void saveFoodstuff(
            final Context context,
            final Foodstuff foodstuff) {
        saveFoodstuff(context, foodstuff, null);
    }

    public void saveFoodstuff(
            final Context context,
            final Foodstuff foodstuff,
            final SaveFoodstuffCallback callback) {
        databaseThreadExecutor.execute(() -> {
            DbHelper dbHelper = new DbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
            String whereClause = COLUMN_NAME_FOODSTUFF_NAME + "=? AND " +
                    COLUMN_NAME_PROTEIN + "=? AND " +
                    COLUMN_NAME_FATS + "=? AND " +
                    COLUMN_NAME_CARBS + "=? AND " +
                    COLUMN_NAME_CALORIES + "=? AND " +
                    COLUMN_NAME_IS_LISTED + "=?";
            String[] selectionArgs = new String[] {
                    String.valueOf(foodstuff.getName()),
                    String.valueOf(foodstuff.getProtein()),
                    String.valueOf(foodstuff.getFats()),
                    String.valueOf(foodstuff.getCarbs()),
                    String.valueOf(foodstuff.getCalories()),
                    String.valueOf(1)}; // listed
            Cursor cursor = database.query(
                    FOODSTUFFS_TABLE_NAME,
                    null,
                    whereClause,
                    selectionArgs,
                    null, null, null);
            //если такого продукта нет в БД:
            boolean alreadyContainsListedFoodstuff = false;
            long id = -1;
            if (cursor.getCount() == 0) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
                values.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, foodstuff.getName().toLowerCase());
                values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
                values.put(COLUMN_NAME_FATS, foodstuff.getFats());
                values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
                values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
                id = database.insert(FOODSTUFFS_TABLE_NAME, null, values);
            } else {
                alreadyContainsListedFoodstuff = true;
            }
            cursor.close();

            if (callback != null) {
                if (alreadyContainsListedFoodstuff) {
                    mainThreadExecutor.execute(() -> callback.onDuplication());
                } else {
                    long finalId = id;
                    mainThreadExecutor.execute(() -> callback.onResult(finalId));
                }
            }
        });
    }

    /**
     * Порядок возвращаемых айдишников гарантированно идентичен порядку переданных фудстаффов.
     */
    public void saveGroupOfFoodstuffs(
            final Context context,
            final Foodstuff[] foodstuffs,
            final SaveGroupOfFoodstuffsCallback callback) {
        databaseThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                DbHelper dbHelper = new DbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                final ArrayList<Long> ids = new ArrayList<>();

                database.beginTransaction();
                try {
                    for (Foodstuff foodstuff : foodstuffs) {
                        ContentValues values = new ContentValues();
                        values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
                        values.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, foodstuff.getName().toLowerCase());
                        values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
                        values.put(COLUMN_NAME_FATS, foodstuff.getFats());
                        values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
                        values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
                        long id = database.insert(FOODSTUFFS_TABLE_NAME, null, values);
                        ids.add(id);
                    }
                    database.setTransactionSuccessful();
                } finally {
                    database.endTransaction();
                }
                if (callback != null) {
                    mainThreadExecutor.execute(() -> callback.onResult(ids));
                }
            }
        });
    }

    public void saveUnlistedFoodstuff(
            final Context context,
            final Foodstuff foodstuff,
            @NonNull final SaveUnlistedFoodstuffCallback callback) {
        databaseThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                DbHelper dbHelper = new DbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
                values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
                values.put(COLUMN_NAME_FATS, foodstuff.getFats());
                values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
                values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
                values.put(COLUMN_NAME_IS_LISTED, 0);
                long foodstuffId = database.insert(FOODSTUFFS_TABLE_NAME, null, values);
                if (callback != null) {
                    mainThreadExecutor.execute(() -> callback.onResult(foodstuffId));
                }
            }
        });
    }

    public void editFoodstuff(final Context context, final long editedFoodstuffId, final Foodstuff newFoodstuff) {
        editFoodstuff(context, editedFoodstuffId, newFoodstuff, null);
    }

    public void editFoodstuff(
            final Context context,
            final long editedFoodstuffId,
            final Foodstuff newFoodstuff,
            Runnable callback) {
        databaseThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                DbHelper dbHelper = new DbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_NAME_FOODSTUFF_NAME, newFoodstuff.getName());
                contentValues.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, newFoodstuff.getName().toLowerCase());
                contentValues.put(COLUMN_NAME_PROTEIN, newFoodstuff.getProtein());
                contentValues.put(COLUMN_NAME_FATS, newFoodstuff.getFats());
                contentValues.put(COLUMN_NAME_CARBS, newFoodstuff.getCarbs());
                contentValues.put(COLUMN_NAME_CALORIES, newFoodstuff.getCalories());
                database.update(
                        FOODSTUFFS_TABLE_NAME,
                        contentValues,
                        FoodstuffsContract.ID + " = ?",
                        new String[]{String.valueOf(editedFoodstuffId)});
                if (callback != null) {
                    mainThreadExecutor.execute(callback);
                }
            }
        });
    }


    public void deleteFoodstuff(final Context context, final long foodstuffsId) {
        databaseThreadExecutor.execute(() -> {
            DbHelper dbHelper = new DbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
            database.delete(
                    FOODSTUFFS_TABLE_NAME,
                    FoodstuffsContract.ID + "=?",
                    new String[]{String.valueOf(foodstuffsId)});
        });
    }

    public void makeFoodstuffUnlisted(final Context context, final long foodstuffId, final Runnable callback) {
        databaseThreadExecutor.execute(() -> {
            DbHelper dbHelper = new DbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_IS_LISTED, 0);
            database.update(
                    FOODSTUFFS_TABLE_NAME,
                    values,
                    FoodstuffsContract.ID + "=?",
                    new String[]{String.valueOf(foodstuffId)});
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }
        });
    }

    public void requestListedFoodstuffsFromDb(
            final Context context,
            final int batchSize,
            @NonNull final FoodstuffsBatchReceiveCallback foodstuffsBatchReceiveCallback,
            @Nullable FinishCallback finishCallback) {
        databaseThreadExecutor.execute(() -> {
            DbHelper dbHelper = new DbHelper(context);
            SQLiteDatabase db = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = db.query(
                    FOODSTUFFS_TABLE_NAME,
                    null,
                    COLUMN_NAME_IS_LISTED + "=?",
                    new String[]{ String.valueOf(1) },
                    null,
                    null,
                    COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " ASC");
            ArrayList<Foodstuff> batchOfFoodstuffs = new ArrayList<>();
            int index = 0;
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(ID));
                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
                double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
                double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
                double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
                double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
                Foodstuff foodstuff = Foodstuff
                        .withId(id)
                        .withName(name)
                        .withNutrition(protein, fats, carbs, calories);
                batchOfFoodstuffs.add(foodstuff);
                ++index;
                if (index >= batchSize) {
                    ArrayList<Foodstuff> batchCopy = new ArrayList<>(batchOfFoodstuffs);
                    mainThreadExecutor.execute(() -> foodstuffsBatchReceiveCallback.onReceive(batchCopy));
                    batchOfFoodstuffs.clear();
                    index = 0;
                }
            }
            if (batchOfFoodstuffs.size() > 0) {
                mainThreadExecutor.execute(() -> foodstuffsBatchReceiveCallback.onReceive(batchOfFoodstuffs));
            }
            cursor.close();
            if (finishCallback != null) {
                mainThreadExecutor.execute(finishCallback::onFinish);
            }
        });
    }

    public void requestListedFoodstuffsFromDb(
            final Context context,
            final int batchSize,
            @NonNull final FoodstuffsBatchReceiveCallback foodstuffsBatchReceiveCallback) {
        requestListedFoodstuffsFromDb(context, batchSize, foodstuffsBatchReceiveCallback, null);
    }

    /**
     * Метод получает фудстаффы по их id.
     * Порядок возвращенных из метода фудстаффов соответствует порядку переданных id. (Для этого
     * мы запрашиваем продукты по одному).
     * Если передать несуществующие id, то метод вернёт пустой List.
     * @param context
     * @param ids - id продуктов, которые мы хотим получить.
     * @param callback - возвращает продукты.
     */
    public void requestFoodstuffsByIds(
            Context context,
            List<Long> ids,
            FoodstuffsBatchReceiveCallback callback) {
        databaseThreadExecutor.execute(() -> {
            DbHelper dbHelper = new DbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
            List<Foodstuff> result = new ArrayList<>();
            String[] columns = new String[] {
                    COLUMN_NAME_FOODSTUFF_NAME,
                    COLUMN_NAME_PROTEIN,
                    COLUMN_NAME_FATS,
                    COLUMN_NAME_CARBS,
                    COLUMN_NAME_CALORIES };

            for (Long id : ids) {
                Cursor cursor = database.query(
                        FOODSTUFFS_TABLE_NAME,
                        columns,
                        FoodstuffsContract.ID + "=?",
                        new String[]{ String.valueOf(id) },
                        null, null, null);
                if (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
                    double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
                    double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
                    double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
                    double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
                    Foodstuff foodstuff =
                            Foodstuff
                                    .withId(id)
                                    .withName(name)
                                    .withNutrition(protein, fats, carbs, calories);
                    result.add(foodstuff);
                }
                cursor.close();
            }
            if (callback != null) {
                mainThreadExecutor.execute(() -> callback.onReceive(result));
            }
        });
    }

    /**
     * Получает фудстаффы с названием, похожим на запрос.
     * @param context
     * @param nameQuery - поисковый запрос
     * @param limit - требуемое количество результатов поиска
     * @param callback
     */
    public void requestFoodstuffsLike(Context context, String nameQuery, int limit, FoodstuffsBatchReceiveCallback callback) {
        databaseThreadExecutor.execute(() -> {
            DbHelper dbHelper = new DbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
            List<Foodstuff> result = new ArrayList<>();
            String limitString = null;
            if (limit != NO_LIMIT) {
                limitString = String.valueOf(limit);
            }
            Cursor cursor = database.query(
                    FOODSTUFFS_TABLE_NAME,
                    null,
                    COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " LIKE ?",
                    new String[]{"%" + nameQuery.toLowerCase() + "%"},
                    null,
                    null,
                    COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " ASC",
                    limitString);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(FoodstuffsContract.ID));
                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
                double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
                double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
                double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
                double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
                Foodstuff foodstuff =
                        Foodstuff.withId(id).withName(name)
                                .withNutrition(protein, fats, carbs, calories);
                result.add(foodstuff);
            }
            cursor.close();
            mainThreadExecutor.execute(() -> callback.onReceive(result));
        });
    }
}
