package korablique.recipecalculator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.HistoryEntry;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_WEIGHT;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_AGE;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_COEFFICIENT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_FORMULA;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GENDER;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GOAL;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_HEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_USER_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;

public class DatabaseWorker {
    private static DatabaseWorker databaseWorker;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface FoodstuffsRequestCallback {
        void onResult(ArrayList<Foodstuff> foodstuffs);
    }
    public interface SaveFoodstuffCallback {
        void onResult(boolean hasAlreadyContainsFoodstuff);
    }
    public interface RequestHistoryCallback {
        void onResult(ArrayList<HistoryEntry> historyEntries);
    }
    public interface SaveUnlistedFoodstuffCallback {
        void onResult(long foodstuffId);
    }
    public interface AddHistoryEntryCallback {
        void onResult(long historyEntryId);
    }
    public interface RequestCurrentUserParametersCallback {
        void onResult(UserParameters userParameters);
    }

    private DatabaseWorker() {}

    public static synchronized DatabaseWorker getInstance() {
        if (databaseWorker == null) {
            databaseWorker = new DatabaseWorker();
        }
        return databaseWorker;
    }

    public void saveFoodstuff(final Context context, final Foodstuff foodstuff, @NonNull final SaveFoodstuffCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                Cursor cursor = database.rawQuery("SELECT * FROM " + FOODSTUFFS_TABLE_NAME
                        + " WHERE " + COLUMN_NAME_FOODSTUFF_NAME + " = '" + foodstuff.getName() + "' AND "
                        + COLUMN_NAME_PROTEIN + " = " + foodstuff.getProtein() + " AND "
                        + COLUMN_NAME_FATS + " = " + foodstuff.getFats() + " AND "
                        + COLUMN_NAME_CARBS + " = " + foodstuff.getCarbs() + " AND "
                        + COLUMN_NAME_CALORIES + " = " + foodstuff.getCalories() + ";", null);
                //если такого продукта нет в БД:
                boolean hasAlreadyContainsFoodstuff = false;
                if (cursor.getCount() == 0) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
                    values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
                    values.put(COLUMN_NAME_FATS, foodstuff.getFats());
                    values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
                    values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
                    database.insert(FOODSTUFFS_TABLE_NAME, null, values);
                } else {
                    hasAlreadyContainsFoodstuff = true;
                }
                cursor.close();
                callback.onResult(hasAlreadyContainsFoodstuff);
            }
        });
    }

    public void saveUnlistedFoodstuff(
            final Context context,
            final Foodstuff foodstuff,
            @NonNull final SaveUnlistedFoodstuffCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
                values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
                values.put(COLUMN_NAME_FATS, foodstuff.getFats());
                values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
                values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
                values.put(COLUMN_NAME_IS_LISTED, 0);
                long foodstuffId = database.insert(FOODSTUFFS_TABLE_NAME, null, values);
                callback.onResult(foodstuffId);
            }
        });
    }

    public void editFoodstuff(final Context context, final long editedFoodstuffId, final Foodstuff newFoodstuff) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_NAME_FOODSTUFF_NAME, newFoodstuff.getName());
                contentValues.put(COLUMN_NAME_PROTEIN, newFoodstuff.getProtein());
                contentValues.put(COLUMN_NAME_FATS, newFoodstuff.getFats());
                contentValues.put(COLUMN_NAME_CARBS, newFoodstuff.getCarbs());
                contentValues.put(COLUMN_NAME_CALORIES, newFoodstuff.getCalories());
                database.update(
                        FOODSTUFFS_TABLE_NAME,
                        contentValues,
                        FoodstuffsContract.ID + " = ?",
                        new String[]{String.valueOf(editedFoodstuffId)});
            }
        });
    }

    public void deleteFoodstuff(final Context context, final long foodstuffsId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                database.delete(
                        FOODSTUFFS_TABLE_NAME,
                        FoodstuffsContract.ID + " = ?",
                        new String[]{String.valueOf(foodstuffsId)});
            }
        });
    }

    public void makeFoodstuffUnlisted(final Context context, final long foodstuffId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_IS_LISTED, 0);
                database.update(
                        FOODSTUFFS_TABLE_NAME,
                        values,
                        FoodstuffsContract.ID + "=?",
                        new String[]{String.valueOf(foodstuffId)});
            }
        });
    }

    public void requestListedFoodstuffsFromDb(final Context context, @NonNull final FoodstuffsRequestCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase db = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = db.rawQuery(
                        "SELECT * FROM " + FOODSTUFFS_TABLE_NAME + " WHERE " + COLUMN_NAME_IS_LISTED + "=1", null);
                ArrayList<Foodstuff> allFoodstuffsFromDb = new ArrayList<>();
                while (cursor.moveToNext()) {
                    Foodstuff foodstuff = new Foodstuff(
                            cursor.getLong(cursor.getColumnIndex(ID)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME)),
                            -1,
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES)));
                    allFoodstuffsFromDb.add(foodstuff);
                }
                cursor.close();
                Collections.sort(allFoodstuffsFromDb);
                callback.onResult(allFoodstuffsFromDb);
            }
        });
    }

    public void requestAllHistoryFromDb(final Context context, @NonNull final RequestHistoryCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase db = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = db.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME + " JOIN " + FOODSTUFFS_TABLE_NAME
                        + " ON " + HISTORY_TABLE_NAME + "." + COLUMN_NAME_FOODSTUFF_ID
                        + "=" + FOODSTUFFS_TABLE_NAME + "." + FoodstuffsContract.ID, null);
                ArrayList<HistoryEntry> historyEntries = new ArrayList<>();
                while (cursor.moveToNext()) {
                    long foodstuffId = cursor.getLong(
                            cursor.getColumnIndex(HISTORY_TABLE_NAME + "." + COLUMN_NAME_FOODSTUFF_ID));
                    double weight = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_WEIGHT));
                    String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
                    double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
                    double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
                    double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
                    double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
                    Foodstuff foodstuff = new Foodstuff(foodstuffId, name, weight, protein, fats, carbs, calories);

                    long time = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_DATE));
                    long historyId = cursor.getLong(cursor.getColumnIndex(HISTORY_TABLE_NAME + "." + HistoryContract.ID));
                    HistoryEntry historyEntry = new HistoryEntry(historyId, foodstuff, new Date(time));
                    historyEntries.add(historyEntry);
                }
                cursor.close();
                callback.onResult(historyEntries);
            }
        });
    }

    public void saveFoodstuffToHistory(
            final Context context,
            final Date date,
            final long foodstuffId,
            final double foodstuffWeight,
            final AddHistoryEntryCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_DATE, date.getTime());
                values.put(COLUMN_NAME_FOODSTUFF_ID, foodstuffId);
                values.put(COLUMN_NAME_WEIGHT, foodstuffWeight);
                long historyEntryId = database.insert(HISTORY_TABLE_NAME, null, values);
                if (callback != null) {
                    callback.onResult(historyEntryId);
                }
            }
        });
    }

    public void deleteEntryFromHistory(final Context context, final long historyId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                database.delete(
                        HISTORY_TABLE_NAME,
                        HistoryContract.ID + " = ?",
                        new String[]{String.valueOf(historyId)});
            }
        });
    }

    public void editWeightInHistoryEntry(
            final Context context,
            final long historyId,
            final double newWeight,
            final Runnable callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_WEIGHT, newWeight);
                database.update(
                        HISTORY_TABLE_NAME,
                        values,
                        HistoryContract.ID + "=?",
                        new String[]{String.valueOf(historyId)});
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    public void saveUserParameters(
            final Context context, final UserParameters userParameters, final Runnable callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_GOAL, userParameters.getGoal());
                values.put(COLUMN_NAME_GENDER, userParameters.getGender());
                values.put(COLUMN_NAME_AGE, userParameters.getAge());
                values.put(COLUMN_NAME_HEIGHT, userParameters.getHeight());
                values.put(COLUMN_NAME_USER_WEIGHT, userParameters.getWeight());
                values.put(COLUMN_NAME_COEFFICIENT, userParameters.getPhysicalActivityCoefficient());
                values.put(COLUMN_NAME_FORMULA, userParameters.getFormula());
                database.insert(USER_PARAMETERS_TABLE_NAME, null, values);
                callback.run();
            }
        });
    }

    public void requestCurrentUserParameters(Context context, RequestCurrentUserParametersCallback callback) {
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = database.rawQuery("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME +
                " ORDER BY " + UserParametersContract.ID + " DESC LIMIT 1", null);
        UserParameters userParameters = null;
        if (cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                String goal = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GOAL));
                String gender = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GENDER));
                int age = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_AGE));
                int height = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_HEIGHT));
                int weight = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_USER_WEIGHT));
                float coefficient = cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_COEFFICIENT));
                String formula = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FORMULA));
                userParameters = new UserParameters(goal, gender, age, height, weight, coefficient, formula);
            }
        }
        cursor.close();
        callback.onResult(userParameters);
    }
}
