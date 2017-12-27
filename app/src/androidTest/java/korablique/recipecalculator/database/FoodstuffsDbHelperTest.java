package korablique.recipecalculator.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import korablique.recipecalculator.ui.calculator.CalculatorActivity;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME_NOCASE;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.FoodstuffsDbHelper.COLUMN_NAME_VERSION;
import static korablique.recipecalculator.database.FoodstuffsDbHelper.DATABASE_VERSION;
import static korablique.recipecalculator.database.FoodstuffsDbHelper.TABLE_DATABASE_VERSION;
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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FoodstuffsDbHelperTest {
    @Rule
    public ActivityTestRule<CalculatorActivity> mActivityRule =
            new ActivityTestRule<>(CalculatorActivity.class);

    @NonNull
    private File getDbFile() {
        return FoodstuffsDbHelper.getDbFile(mActivityRule.getActivity());
    }

    @Test
    public void databaseUpgradesFrom1to2version() {
        // Удалим существующую базу данных
        FoodstuffsDbHelper.deinitializeDatabase(mActivityRule.getActivity());

        // Создадим файл базы данных НЕ используя FoodstuffsDbHelper
        SQLiteDatabase database1 = SQLiteDatabase.openOrCreateDatabase(getDbFile(), null);

        Assert.assertFalse(DatabaseUtils.tableExists(database1, FOODSTUFFS_TABLE_NAME));

        // Заполнить файл табличками для 1 версии
        database1.execSQL("CREATE TABLE " + FOODSTUFFS_TABLE_NAME + "(" +
                FoodstuffsContract.ID + " INTEGER PRIMARY KEY, " +
                COLUMN_NAME_FOODSTUFF_NAME + " TEXT, " +
                COLUMN_NAME_PROTEIN + " REAL, " +
                COLUMN_NAME_FATS + " REAL, " +
                COLUMN_NAME_CARBS + " REAL, " +
                COLUMN_NAME_CALORIES + " REAL)");
        Assert.assertTrue(DatabaseUtils.tableExists(database1, FOODSTUFFS_TABLE_NAME));
        Assert.assertFalse(DatabaseUtils.tableExists(database1, HISTORY_TABLE_NAME));
        Assert.assertFalse(DatabaseUtils.tableExists(database1, TABLE_DATABASE_VERSION));
        Assert.assertFalse(DatabaseUtils.tableExists(database1, USER_PARAMETERS_TABLE_NAME));
        database1.close();

        // Создать FoodstuffsDbHelper и сделать open
        FoodstuffsDbHelper helper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database2 = helper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

        // Убедиться, что БД имеет 2 версию
        Assert.assertTrue(DatabaseUtils.tableExists(database2, FOODSTUFFS_TABLE_NAME));
        Assert.assertTrue(isColumnExist(database2, FOODSTUFFS_TABLE_NAME, COLUMN_NAME_IS_LISTED));
        Assert.assertTrue(DatabaseUtils.tableExists(database2, HISTORY_TABLE_NAME));
        Assert.assertTrue(DatabaseUtils.tableExists(database2, TABLE_DATABASE_VERSION));
        Assert.assertTrue(DatabaseUtils.tableExists(database2, USER_PARAMETERS_TABLE_NAME));
    }

    @Test
    public void databaseUpgradesFrom2to3version() {
        // Удалим существующую базу данных
        FoodstuffsDbHelper.deinitializeDatabase(mActivityRule.getActivity());

        // Создадим файл базы данных НЕ используя FoodstuffsDbHelper
        SQLiteDatabase database1 = SQLiteDatabase.openOrCreateDatabase(getDbFile(), null);

        Assert.assertFalse(DatabaseUtils.tableExists(database1, FOODSTUFFS_TABLE_NAME));

        // Заполнить файл табличками для 2 версии:
        // foodstuffs
        database1.execSQL("CREATE TABLE " + FOODSTUFFS_TABLE_NAME + "(" +
                FoodstuffsContract.ID + " INTEGER PRIMARY KEY, " +
                COLUMN_NAME_FOODSTUFF_NAME + " TEXT, " +
                COLUMN_NAME_PROTEIN + " REAL, " +
                COLUMN_NAME_FATS + " REAL, " +
                COLUMN_NAME_CARBS + " REAL, " +
                COLUMN_NAME_CALORIES + " REAL, " +
                COLUMN_NAME_IS_LISTED + " INTEGER)");
        // history
        database1.execSQL("CREATE TABLE " + HISTORY_TABLE_NAME + " (" +
                HistoryContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_DATE + " INTEGER, " +
                COLUMN_NAME_FOODSTUFF_ID + " INTEGER, " +
                COLUMN_NAME_WEIGHT + " REAL, " +
                "FOREIGN KEY (" + COLUMN_NAME_FOODSTUFF_ID + ") " +
                "REFERENCES " + FOODSTUFFS_TABLE_NAME + "(" + FoodstuffsContract.ID + "))");
        // version
        int oldVersion = 2;
        database1.execSQL("CREATE TABLE " + TABLE_DATABASE_VERSION + " (" + COLUMN_NAME_VERSION + " INTEGER)");
        database1.execSQL("INSERT INTO " + TABLE_DATABASE_VERSION + " VALUES (" + oldVersion + ")");
        // user parameters
        database1.execSQL("CREATE TABLE " + USER_PARAMETERS_TABLE_NAME + " (" +
                UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_GOAL + " TEXT, " +
                COLUMN_NAME_GENDER + " TEXT, " +
                COLUMN_NAME_AGE + " INTEGER, " +
                COLUMN_NAME_HEIGHT + " INTEGER, " +
                COLUMN_NAME_USER_WEIGHT + " INTEGER, " +
                COLUMN_NAME_COEFFICIENT + " REAL, " +
                COLUMN_NAME_FORMULA + " TEXT)");

        // добавляем продукт в БД
        ContentValues values = new ContentValues();
        String foodstuffName = "Apple";
        values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuffName);
        values.put(COLUMN_NAME_PROTEIN, 10);
        values.put(COLUMN_NAME_FATS, 10);
        values.put(COLUMN_NAME_CARBS, 10);
        values.put(COLUMN_NAME_CALORIES, 10);
        values.put(COLUMN_NAME_IS_LISTED, 1);
        long id = database1.insert(FOODSTUFFS_TABLE_NAME, null, values);

        // Создать FoodstuffsDbHelper и сделать open
        FoodstuffsDbHelper helper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database2 = helper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

        // Убедиться, что БД имеет 3 версию, т.е. проверить наличие столбца name_nocase
        Assert.assertTrue(isColumnExist(database2, FOODSTUFFS_TABLE_NAME, COLUMN_NAME_FOODSTUFF_NAME_NOCASE));

        // Проверить, что значения в no-case столбце такие же, как в обычном, но в нижнем регистре.
        Cursor cursor = database2.query(
                FOODSTUFFS_TABLE_NAME,
                new String[]{COLUMN_NAME_FOODSTUFF_NAME_NOCASE},
                FoodstuffsContract.ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);
        String nocaseFoodstuffName = "";
        while (cursor.moveToNext()) {
            nocaseFoodstuffName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME_NOCASE));
        }
        cursor.close();
        Assert.assertEquals(foodstuffName.toLowerCase(), nocaseFoodstuffName);
    }

    @Test
    public void databaseDoesNotUpdateIfHasActualVersion() throws IOException {
        FoodstuffsDbHelper.deinitializeDatabase(mActivityRule.getActivity());

        FoodstuffsDbHelper helper1 = new FoodstuffsDbHelper(mActivityRule.getActivity());
        FoodstuffsDbHelper.InitializationResult result1 = helper1.initializeDatabase();
        Assert.assertEquals(DATABASE_VERSION, result1.getNewVersion());

        FoodstuffsDbHelper helper2 = new FoodstuffsDbHelper(mActivityRule.getActivity());
        FoodstuffsDbHelper.InitializationResult result2 = helper2.initializeDatabase();
        Assert.assertEquals(FoodstuffsDbHelper.InitializationType.None, result2.getPerformedInitialization());
    }

    public boolean isColumnExist(SQLiteDatabase database, String tableName, String columnName) {
        try (Cursor cursor = database.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
            while (cursor.moveToNext()) {
                String currentColumnName = cursor.getString(cursor.getColumnIndex("name"));
                if (currentColumnName.equals(columnName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
