package korablique.recipecalculator;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static korablique.recipecalculator.DatabaseUtils.tableExists;
import static korablique.recipecalculator.HistoryContract.*;

public class FoodstuffsDbHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_DATABASE_VERSION = "database_version";

    public static final String DATABASE_NAME = "Main.db";

    private static boolean initialized;

    private Context context;

    public FoodstuffsDbHelper(Context context) {
        this.context = context;
    }

    public void initializeDatabase() throws IOException {
        if (!dbExists()) {
            createDatabase();
        } else {
            tryToUpgradeDatabase();
        }
    }

    public static synchronized void deinitializeDatabase(Context context) {
        File dbFile = getDbFile(context);
        boolean deleted = dbFile.delete();
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
        SQLiteDatabase database = openDatabase(SQLiteDatabase.OPEN_READWRITE);
        createTableHistory(database);
        createTableDatabaseVersion(database);
    }

    private void createTableHistory(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + HISTORY_TABLE_NAME + " (" +
                HistoryContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_DATE + " INTEGER, " +
                COLUMN_NAME_FOODSTUFF_ID + " INTEGER, " +
                COLUMN_NAME_WEIGHT + " REAL, " +
                "FOREIGN KEY (foodstuff_id) REFERENCES foodstuffs(ID))");
    }

    private void createTableDatabaseVersion(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE_DATABASE_VERSION + " (version INTEGER)");
        database.execSQL("INSERT INTO " + TABLE_DATABASE_VERSION + " VALUES (" + DATABASE_VERSION + ")");
    }

    private void tryToUpgradeDatabase() {
        File path = getDbFile(context);
        SQLiteDatabase database = SQLiteDatabase.openDatabase(path.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        // В первой версии приложени не было таблицы TABLE_DATABASE_VERSION
        if (!tableExists(database, TABLE_DATABASE_VERSION)) {
            createTableHistory(database);
            createTableDatabaseVersion(database);
        }
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
}
