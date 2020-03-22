package korablique.recipecalculator.z;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import korablique.recipecalculator.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseUtils;
import korablique.recipecalculator.database.FoodstuffsContract;
import korablique.recipecalculator.database.HistoryContract;
import korablique.recipecalculator.database.LegacyDatabaseValues;
import korablique.recipecalculator.database.UserParametersContract;
import korablique.recipecalculator.database.room.AppDatabase;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Goal;
import korablique.recipecalculator.model.Lifestyle;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME_NOCASE;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_WEIGHT;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;
import static korablique.recipecalculator.database.LegacyDatabaseValues.COLUMN_NAME_COEFFICIENT;
import static korablique.recipecalculator.database.LegacyDatabaseValues.COLUMN_NAME_GOAL;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_AGE;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_FORMULA;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GENDER;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_HEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_LIFESTYLE;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_USER_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;
import static korablique.recipecalculator.database.room.LegacyDatabaseUpdater.COLUMN_NAME_VERSION;
import static korablique.recipecalculator.database.room.LegacyDatabaseUpdater.TABLE_DATABASE_VERSION;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_1_2;

/**
 * NOTE: файл лежит в пакете "z" намеренно - эти тесты должны выполняться самыми последними,
 * т.к. они корраптят БД.
 * По-хорошему нужно выпилить legacy updater, заменив его на апдейты через room, но это
 * дольше, чем просто переименовать пакет в "z".
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LegacyDatabaseUpdaterTest {
    private Context context;
    private File dbFile;
    private MigrationTestHelper helper;

    public LegacyDatabaseUpdaterTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                AppDatabase.class.getCanonicalName(),
                new FrameworkSQLiteOpenHelperFactory());
    }

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        DatabaseThreadExecutor databaseThreadExecutor = new InstantDatabaseThreadExecutor();
        DatabaseHolder databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);

        dbFile = databaseHolder.getDBFile();
        deleteDatabase(databaseHolder);
    }

    public void deleteDatabase(DatabaseHolder databaseHolder) {
        // Закроем соединение с БД.
        databaseHolder.closeDatabaseConnection();
        // Удалим всё содержимое папки с БД.
        File dbDir = dbFile.getParentFile();
        for (File file : dbDir.listFiles()) {
            if (file.isFile()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    throw new Error("Couldn't delete file " + file.getAbsolutePath());
                }
            }
        }
        // Помолимся Богу о том, что это спасёт нас от испорченного БД-файла.
    }

    @After
    public void tearDown() {
        DatabaseThreadExecutor databaseThreadExecutor = new InstantDatabaseThreadExecutor();
        DatabaseHolder databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);
        deleteDatabase(databaseHolder);
    }

    @Test
    public void databaseUpgradesFrom1toAtLeast2version() throws IOException {
        // Создадим файл базы данных НЕ используя DatabaseHolder
        try (SQLiteDatabase database1 = SQLiteDatabase.openOrCreateDatabase(dbFile, null)) {
            database1.setVersion(1);

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
            Assert.assertFalse(DatabaseUtils.tableExists(database1, USER_PARAMETERS_TABLE_NAME));
        }

        SupportSQLiteDatabase database2 = helper.runMigrationsAndValidate(dbFile.getAbsolutePath(), 2, true, MIGRATION_1_2);

        // Убедиться, что БД имеет как минимум 2 версию
        Assert.assertTrue(DatabaseUtils.tableExists(database2, FOODSTUFFS_TABLE_NAME));
        Assert.assertTrue(isColumnExist(database2, FOODSTUFFS_TABLE_NAME, COLUMN_NAME_IS_LISTED));
        Assert.assertTrue(DatabaseUtils.tableExists(database2, HISTORY_TABLE_NAME));
        Assert.assertTrue(DatabaseUtils.tableExists(database2, USER_PARAMETERS_TABLE_NAME));
    }

    @Test
    public void databaseUpgradesFrom2toAtLeast3version() throws IOException {
        long foodstuffId = -1;
        String foodstuffName = "Apple";
        // Создадим файл базы данных НЕ используя DatabaseHolder
        try (SQLiteDatabase database1 = SQLiteDatabase.openOrCreateDatabase(dbFile, null)) {
            database1.setVersion(1);

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
            values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuffName);
            values.put(COLUMN_NAME_PROTEIN, 10);
            values.put(COLUMN_NAME_FATS, 10);
            values.put(COLUMN_NAME_CARBS, 10);
            values.put(COLUMN_NAME_CALORIES, 10);
            values.put(COLUMN_NAME_IS_LISTED, 1);
            foodstuffId = database1.insert(FOODSTUFFS_TABLE_NAME, null, values);
        }

        SupportSQLiteDatabase database2 = helper.runMigrationsAndValidate(dbFile.getAbsolutePath(), 2, true, MIGRATION_1_2);

        // Убедиться, что БД имеет 3 версию, т.е. проверить наличие столбца name_nocase
        Assert.assertTrue(isColumnExist(database2, FOODSTUFFS_TABLE_NAME, COLUMN_NAME_FOODSTUFF_NAME_NOCASE));

        // Проверить, что значения в no-case столбце такие же, как в обычном, но в нижнем регистре.
        Cursor cursor = database2.query(
                "SELECT * FROM " + FOODSTUFFS_TABLE_NAME +
                " WHERE " + FoodstuffsContract.ID + "=?",
                new String[]{String.valueOf(foodstuffId)});
        String nocaseFoodstuffName = "";
        while (cursor.moveToNext()) {
            nocaseFoodstuffName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME_NOCASE));
        }
        cursor.close();
        Assert.assertEquals(foodstuffName.toLowerCase(), nocaseFoodstuffName);
    }

    @Test
    public void databaseUpgradesFrom3to4version() throws IOException {
        int userAge = 25;
        int userHeight = 158;
        int userWeight = 48;
        // Создадим файл базы данных НЕ используя DatabaseHolder
        try (SQLiteDatabase database1 = SQLiteDatabase.openOrCreateDatabase(dbFile, null)) {
            database1.setVersion(1);

            // Заполнить файл табличками для 3 версии:
            // foodstuffs
            database1.execSQL("CREATE TABLE " + FOODSTUFFS_TABLE_NAME + "(" +
                    FoodstuffsContract.ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_NAME_FOODSTUFF_NAME + " TEXT, " +
                    COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " TEXT, " +
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
            int oldVersion = 3;
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

            // добавляем параметры пользоваетля в БД
            String userGoal = LegacyDatabaseValues.GOAL_LOSING_WEIGHT;
            String userGender = LegacyDatabaseValues.GENDER_FEMALE;
            float coefficient = LegacyDatabaseValues.COEFFICIENT_INSIGNIFICANT_ACTIVITY;
            String userFormula = LegacyDatabaseValues.FORMULA_HARRIS_BENEDICT;
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_GOAL, userGoal);
            values.put(COLUMN_NAME_GENDER, userGender);
            values.put(COLUMN_NAME_AGE, userAge);
            values.put(COLUMN_NAME_HEIGHT, userHeight);
            values.put(COLUMN_NAME_USER_WEIGHT, userWeight);
            values.put(COLUMN_NAME_COEFFICIENT, coefficient);
            values.put(COLUMN_NAME_FORMULA, userFormula);
            database1.insert(USER_PARAMETERS_TABLE_NAME, null, values);
        }

        SupportSQLiteDatabase database2 = helper.runMigrationsAndValidate(dbFile.getAbsolutePath(), 2, true, MIGRATION_1_2);

        // Убедиться, что БД имеет 4 версию
        Cursor cursor = database2.query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME, null);
        int goalId = -1, genderId = -1, age = -1, height = -1, weight = -1, lifestyleId = -1, formulaId = -1;
        while (cursor.moveToNext()) {
            goalId = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_GOAL));
            genderId = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_GENDER));
            age = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_AGE));
            height = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_HEIGHT));
            weight = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_USER_WEIGHT));
            lifestyleId = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_LIFESTYLE));
            formulaId = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_FORMULA));
        }
        Assert.assertEquals(Goal.LOSING_WEIGHT.getId(), goalId);
        Assert.assertEquals(Gender.FEMALE.getId(), genderId);
        Assert.assertEquals(userAge, age);
        Assert.assertEquals(userHeight, height);
        Assert.assertEquals(userWeight, weight);
        Assert.assertEquals(Lifestyle.INSIGNIFICANT_ACTIVITY.getId(), lifestyleId);
        Assert.assertEquals(Formula.HARRIS_BENEDICT.getId(), formulaId);
    }

    public boolean isColumnExist(SupportSQLiteDatabase database, String tableName, String columnName) {
        try (Cursor cursor = database.query("PRAGMA table_info(" + tableName + ")", null)) {
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
