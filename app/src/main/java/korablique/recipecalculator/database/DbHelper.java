package korablique.recipecalculator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.room.util.DBUtil;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Goal;
import korablique.recipecalculator.model.Lifestyle;

import static korablique.recipecalculator.database.DatabaseUtils.tableExists;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME_NOCASE;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_WEIGHT;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_AGE;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_COEFFICIENT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_LIFESTYLE;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_FORMULA;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GENDER;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GOAL;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_HEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_USER_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;

public class DbHelper {
    public static final int DATABASE_VERSION = 4;
    public static final String TABLE_DATABASE_VERSION = "database_version";
    public static final String COLUMN_NAME_VERSION = "version";

    public static final String DATABASE_NAME = "Main.db";

    private static boolean initialized;

    private Context context;

    enum InitializationType {
        None, // БД находится в самом свежем состоянии, никакая особая инициализация не требовалась
        Creation, // БД отсутствовала - мы скопировали её из Ассетов и проинициализировали
        Update // БД присутствовала, но была старой версии - пришлось её обновить
    }

    public DbHelper(Context context) {
        this.context = context;
    }

    public class InitializationResult {
        private InitializationType performedInitialization; // Какая инициилизация была произведена
        private int oldVersion; // Версия БД до инициализации (-1 если БД не было)
        private int newVersion; // Версия БД после инициализации

        public InitializationResult(InitializationType performedInitialization, int oldVersion, int newVersion) {
            this.performedInitialization = performedInitialization;
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }

        public InitializationType getPerformedInitialization() {
            return performedInitialization;
        }

        public int getOldVersion() {
            return oldVersion;
        }

        public int getNewVersion() {
            return newVersion;
        }
    }

    InitializationResult initializeDatabase() throws IOException {
        InitializationResult result;
        if (!dbExists()) {
            createDatabase();
            result = new InitializationResult(InitializationType.Creation, -1, DATABASE_VERSION);
        } else {
            result = tryToUpgradeDatabase();
        }
        initialized = true;
        return result;
    }

    public static synchronized void deinitializeDatabase(Context context) {
        File dbFile = getDbFile(context);
        if (!dbFile.exists()) {
            return;
        }
        boolean deleted = SQLiteDatabase.deleteDatabase(dbFile);
        if (!deleted) {
            throw new Error("Couldn't delete database");
        }
        initialized = false;
    }

    /**
     * При отсутствии базы данных, копирует её из assets
     */
    private void createDatabase() throws IOException {
        copyDatabaseFromAssets();
        File path = getDbFile(context);

        SQLiteDatabase database = SQLiteDatabase.openDatabase(path.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        createTableHistory(database);
        createTableDatabaseVersion(database);
        createTableUserParameters(database);
    }

    private void createTableHistory(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + HISTORY_TABLE_NAME + " (" +
                HistoryContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_DATE + " INTEGER, " +
                COLUMN_NAME_FOODSTUFF_ID + " INTEGER, " +
                COLUMN_NAME_WEIGHT + " REAL, " +
                "FOREIGN KEY (" + COLUMN_NAME_FOODSTUFF_ID + ") " +
                "REFERENCES " + FOODSTUFFS_TABLE_NAME + "(" + FoodstuffsContract.ID + "))");
    }

    private void createTableDatabaseVersion(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE_DATABASE_VERSION + " (" + COLUMN_NAME_VERSION + " INTEGER)");
        database.execSQL("INSERT INTO " + TABLE_DATABASE_VERSION + " VALUES (" + DATABASE_VERSION + ")");
    }

    private void createTableUserParameters(SQLiteDatabase database) {
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
static 
    private InitializationResult tryToUpgradeDatabase() {
        File path = getDbFile(context);

        SQLiteDatabase database = SQLiteDatabase.openDatabase(path.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        InitializationType performedInitialization = InitializationType.None;
        int oldVersion = -1, newVersion = -1;
        // В первой версии приложени не было таблицы TABLE_DATABASE_VERSION
        // обновление с 1 на 2 версию
        if (!tableExists(database, TABLE_DATABASE_VERSION)) {
            updateToVersion2(database);
            performedInitialization = InitializationType.Update;
            oldVersion = 1;
            newVersion = 2;
        }

        if (getDatabaseVersion(database) == 2) {
            updateToVersion3(database);
            performedInitialization = InitializationType.Update;
            oldVersion = 2;
            newVersion = 3;
        }

        if (getDatabaseVersion(database) == 3) {
            updateToVersion4(database);
            performedInitialization = InitializationType.Update;
            oldVersion = 3;
            newVersion = 4;
        }

        return new InitializationResult(performedInitialization, oldVersion, newVersion);
    }

    private void updateToVersion2(SQLiteDatabase database) {
        database.beginTransaction();
        try {
            createTableHistory(database);
            createTableDatabaseVersion(database);
            addColumnIsListed(database);
            createTableUserParameters(database);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void updateToVersion3(SQLiteDatabase database) {
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
            Cursor cursor = database.query(FOODSTUFFS_TABLE_NAME, null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                String foodstuffName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
                long foodstuffId = cursor.getLong(cursor.getColumnIndex(FoodstuffsContract.ID));
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, foodstuffName.toLowerCase());
                database.update(
                        FOODSTUFFS_TABLE_NAME,
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

    private void updateToVersion4(SQLiteDatabase database) {
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

            Cursor cursor = database.query(USER_PARAMETERS_TABLE_NAME, null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(UserParametersContract.ID));

                String goalStr = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GOAL));
                Goal goal = DeprecetedDatabaseValues.convertGoal(goalStr);
                int goalId = goal.getId();

                String genderStr = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GENDER));
                Gender gender = DeprecetedDatabaseValues.convertGender(genderStr);
                int genderId = gender.getId();

                int age = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_AGE));
                int height = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_HEIGHT));
                int weight = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_USER_WEIGHT));

                float coefficient = cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_COEFFICIENT));
                Lifestyle lifestyle = DeprecetedDatabaseValues.convertCoefficient(coefficient);
                int lifestyleId = lifestyle.getId();

                String formulaStr = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FORMULA));
                Formula formula = DeprecetedDatabaseValues.convertFormula(formulaStr);
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

                database.insert(tmpTableName, null, values);
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

    private void setDatabaseVersion(SQLiteDatabase database, int newVersion) {
        ContentValues values = new ContentValues(1);
        values.put(COLUMN_NAME_VERSION, newVersion);
        database.update(TABLE_DATABASE_VERSION, values, null, null);
    }

    private void addColumnIsListed(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE " + FOODSTUFFS_TABLE_NAME + " ADD COLUMN " + COLUMN_NAME_IS_LISTED
                + " INTEGER DEFAULT 1 NOT NULL");
    }

    private boolean dbExists() {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(getDbFile(context).getPath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            Crashlytics.log("openDatabase выбросил исключение: " + e.getMessage());
            //база еще не существует
        }

        if (db != null) {
            db.close();
        }
        return db != null;
    }

    /**
     * Копирует БД из папки assets вместо созданной локальной БД
     * Выполняется путем копирования потока байтов.
     * */
    private void copyDatabaseFromAssets() throws IOException {
        //Открываем локальную БД как входящий поток
        InputStream myInput = context.getAssets().open(DATABASE_NAME);

        //Путь ко вновь созданной БД
        File outFile = getDbFile(context);

        //Открываем пустую базу данных как исходящий поток
        OutputStream myOutput = new FileOutputStream(outFile);

        //перемещаем байты из входящего файла в исходящий
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        //закрываем потоки
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    public synchronized SQLiteDatabase openDatabase(int flag) throws SQLException {
        if (!initialized) {
            try {
                initializeDatabase();
            } catch (IOException e) {
                throw new Error(e);
            }
            initialized = true;
        }
        return SQLiteDatabase.openDatabase(getDbFile(context).getPath(), null, flag);
    }

    public static File getDbFile(Context context) {
        return new File(context.getFilesDir(), DATABASE_NAME);
    }

    private int getDatabaseVersion(SQLiteDatabase database) {
        Cursor cursor = database.query(TABLE_DATABASE_VERSION, null, null, null, null, null, null);
        int databaseVersion = -1;
        while (cursor.moveToNext()) {
            databaseVersion = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_VERSION));
        }
        cursor.close();
        return databaseVersion;
    }
}
