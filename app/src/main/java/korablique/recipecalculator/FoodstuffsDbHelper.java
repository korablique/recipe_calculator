package korablique.recipecalculator;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FoodstuffsDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Main.db";
    private SQLiteDatabase database;
    private Context context;

    public FoodstuffsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            createDatabase();
        } catch (IOException ioe) {
            throw new IllegalStateException("Unable to create database", ioe);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        //TODO: сделать нормально
        /*db.execSQL(FoodstuffsContract.SQL_DELETE_ENTRIES);
        onCreate(db);*/
    }

    /**
     * При отсутствии базы данных, копирует её из assets
     */
    public void createDatabase() throws IOException {
        if (!dbExists()) {
            try {
                copyDatabaseFromAssets();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    private boolean dbExists() {
        SQLiteDatabase db = null;
        try {
            File dbFile = new File(getDbPath(), DATABASE_NAME);
            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
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
        File outFile = new File(getDbPath(), DATABASE_NAME);

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

    public SQLiteDatabase openDatabase(int flag) throws SQLException {
        File path = new File(getDbPath(), DATABASE_NAME);
        database = SQLiteDatabase.openDatabase(path.getPath(), null, flag);
        return database;
    }

    @Override
    public synchronized void close() {
        if (database != null) {
            database.close();
        }
        super.close();
    }

    private File getDbPath() {
        return context.getFilesDir();
    }
}
