package korablique.recipecalculator.database.room;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.sqlite.db.SupportSQLiteDatabase;
import korablique.recipecalculator.database.FoodstuffsContract;
import korablique.recipecalculator.database.HistoryContract;
import korablique.recipecalculator.database.LegacyDatabaseValues;
import korablique.recipecalculator.database.UserParametersContract;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Goal;
import korablique.recipecalculator.model.Lifestyle;

import static korablique.recipecalculator.database.DatabaseUtils.tableExists;
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

public class LegacyDatabaseUpdater {
    public static final String TABLE_DATABASE_VERSION = "database_version";
    public static final String COLUMN_NAME_VERSION = "version";

    private LegacyDatabaseUpdater() {}

    public static void upgradeIfNeeded(SupportSQLiteDatabase database) {
        // В первой версии приложени не было таблицы TABLE_DATABASE_VERSION
        // обновление с 1 на 2 версию
        if (!tableExists(database, TABLE_DATABASE_VERSION)) {
            updateToVersion2(database);
        }

        if (getDatabaseVersion(database) == 2) {
            updateToVersion3(database);
        }

        if (getDatabaseVersion(database) == 3) {
            updateToVersion4(database);
        }
    }

    private static void updateToVersion2(SupportSQLiteDatabase database) {
        database.beginTransaction();
        try {
            createTableHistory(database);
            createTableDatabaseVersion(database, 2);
            addColumnIsListed(database);
            createTableUserParameters(database);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private static void updateToVersion3(SupportSQLiteDatabase database) {
        // добавляется новый столбец с названием продукта без заглавных букв
        database.beginTransaction();
        String tmpTableName = "foodstuffs_tmp";
        try {
            database.execSQL("CREATE TABLE " + tmpTableName + " (" +
                    FoodstuffsContract.ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_NAME_FOODSTUFF_NAME + " TEXT, " +
                    COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " TEXT, " +
                    COLUMN_NAME_PROTEIN + " REAL, " +
                    COLUMN_NAME_FATS + " REAL, " +
                    COLUMN_NAME_CARBS + " REAL, " +
                    COLUMN_NAME_CALORIES + " REAL, " +
                    COLUMN_NAME_IS_LISTED + " INTEGER DEFAULT 1 NOT NULL)");
            database.execSQL("INSERT INTO " + tmpTableName +
                    " SELECT " + FoodstuffsContract.ID + ", " +
                    COLUMN_NAME_FOODSTUFF_NAME + ", " +
                    COLUMN_NAME_FOODSTUFF_NAME + " AS " + COLUMN_NAME_FOODSTUFF_NAME_NOCASE + ", " +
                    COLUMN_NAME_PROTEIN + ", " +
                    COLUMN_NAME_FATS + ", " +
                    COLUMN_NAME_CARBS + ", " +
                    COLUMN_NAME_CALORIES + ", " +
                    COLUMN_NAME_IS_LISTED +
                    " FROM " + FOODSTUFFS_TABLE_NAME);
            database.execSQL("DROP TABLE " + FOODSTUFFS_TABLE_NAME);
            database.execSQL("ALTER TABLE " + tmpTableName + " RENAME TO " + FOODSTUFFS_TABLE_NAME);
            Cursor cursor = database.query("SELECT * FROM " + FOODSTUFFS_TABLE_NAME);
            while (cursor.moveToNext()) {
                String foodstuffName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
                long foodstuffId = cursor.getLong(cursor.getColumnIndex(FoodstuffsContract.ID));
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, foodstuffName.toLowerCase());
                database.update(
                        FOODSTUFFS_TABLE_NAME,
                        SQLiteDatabase.CONFLICT_NONE,
                        values,
                        FoodstuffsContract.ID + "=?",
                        new String[]{String.valueOf(foodstuffId)});
            }
            cursor.close();
            setDatabaseVersion(database, 3);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private static void updateToVersion4(SupportSQLiteDatabase database) {
        // goal, gender, lifestyle, formula хранятся в виде их id элементов enum'ов
        database.beginTransaction();
        String tmpTableName = USER_PARAMETERS_TABLE_NAME + "_tmp";
        try {
            database.execSQL("CREATE TABLE " + tmpTableName + " (" +
                    UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME_GOAL + " INTEGER, " +
                    COLUMN_NAME_GENDER + " INTEGER, " +
                    COLUMN_NAME_AGE + " INTEGER, " +
                    COLUMN_NAME_HEIGHT + " INTEGER, " +
                    COLUMN_NAME_USER_WEIGHT + " INTEGER, " +
                    COLUMN_NAME_LIFESTYLE + " INTEGER, " +
                    COLUMN_NAME_FORMULA + " INTEGER)");

            Cursor cursor = database.query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(UserParametersContract.ID));

                String goalStr = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GOAL));
                Goal goal = LegacyDatabaseValues.convertGoal(goalStr);
                int goalId = goal.getId();

                String genderStr = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GENDER));
                Gender gender = LegacyDatabaseValues.convertGender(genderStr);
                int genderId = gender.getId();

                int age = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_AGE));
                int height = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_HEIGHT));
                int weight = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_USER_WEIGHT));

                float coefficient = cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_COEFFICIENT));
                Lifestyle lifestyle = LegacyDatabaseValues.convertCoefficient(coefficient);
                int lifestyleId = lifestyle.getId();

                String formulaStr = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FORMULA));
                Formula formula = LegacyDatabaseValues.convertFormula(formulaStr);
                int formulaId = formula.getId();

                ContentValues values = new ContentValues();
                values.put(UserParametersContract.ID, id);
                values.put(COLUMN_NAME_GOAL, goalId);
                values.put(COLUMN_NAME_GENDER, genderId);
                values.put(COLUMN_NAME_AGE, age);
                values.put(COLUMN_NAME_HEIGHT, height);
                values.put(COLUMN_NAME_USER_WEIGHT, weight);
                values.put(COLUMN_NAME_LIFESTYLE, lifestyleId);
                values.put(COLUMN_NAME_FORMULA, formulaId);

                database.insert(tmpTableName, SQLiteDatabase.CONFLICT_NONE, values);
            }
            cursor.close();

            database.execSQL("DROP TABLE " + USER_PARAMETERS_TABLE_NAME);
            database.execSQL("ALTER TABLE " + tmpTableName + " RENAME TO " + USER_PARAMETERS_TABLE_NAME);
            setDatabaseVersion(database, 4);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private static int getDatabaseVersion(SupportSQLiteDatabase database) {
        Cursor cursor = database.query("SELECT * FROM " + TABLE_DATABASE_VERSION);
        int databaseVersion = -1;
        while (cursor.moveToNext()) {
            databaseVersion = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_VERSION));
        }
        cursor.close();
        return databaseVersion;
    }

    private static void setDatabaseVersion(SupportSQLiteDatabase database, int newVersion) {
        ContentValues values = new ContentValues(1);
        values.put(COLUMN_NAME_VERSION, newVersion);
        database.update(TABLE_DATABASE_VERSION, SQLiteDatabase.CONFLICT_NONE, values, null, null);
    }

    private static void addColumnIsListed(SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE " + FOODSTUFFS_TABLE_NAME + " ADD COLUMN " + COLUMN_NAME_IS_LISTED
                + " INTEGER DEFAULT 1 NOT NULL");
    }

    private static void createTableHistory(SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + HISTORY_TABLE_NAME + " (" +
                HistoryContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_DATE + " INTEGER, " +
                COLUMN_NAME_FOODSTUFF_ID + " INTEGER, " +
                COLUMN_NAME_WEIGHT + " REAL, " +
                "FOREIGN KEY (" + COLUMN_NAME_FOODSTUFF_ID + ") " +
                "REFERENCES " + FOODSTUFFS_TABLE_NAME + "(" + FoodstuffsContract.ID + "))");
    }

    private static void createTableDatabaseVersion(SupportSQLiteDatabase database, int version) {
        database.execSQL("CREATE TABLE " + TABLE_DATABASE_VERSION + " (" + COLUMN_NAME_VERSION + " INTEGER)");
        database.execSQL("INSERT INTO " + TABLE_DATABASE_VERSION + " VALUES (" + version + ")");
    }

    private static void createTableUserParameters(SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + USER_PARAMETERS_TABLE_NAME + " (" +
                UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_GOAL + " INTEGER, " +
                COLUMN_NAME_GENDER + " INTEGER, " +
                COLUMN_NAME_AGE + " INTEGER, " +
                COLUMN_NAME_HEIGHT + " INTEGER, " +
                COLUMN_NAME_USER_WEIGHT + " INTEGER, " +
                COLUMN_NAME_LIFESTYLE + " INTEGER, " +
                COLUMN_NAME_FORMULA + " INTEGER)");
    }
}
