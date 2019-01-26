package korablique.recipecalculator.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.sqlite.db.SupportSQLiteDatabase;
import korablique.recipecalculator.database.room.AppDatabase;

public class DatabaseUtils {
    private DatabaseUtils() {}

    public static boolean tableExists(SupportSQLiteDatabase database, String tableName) {
        Cursor cursor = database.query(createTableExistsQuery(tableName), null);
        return cursor.getCount() > 0;
    }

    private static String createTableExistsQuery(String tableName) {
        return "SELECT name " +
                "FROM sqlite_master " +
                "WHERE type='table' " +
                "AND name='" + tableName + "'";
    }

    public static boolean tableExists(SQLiteDatabase database, String tableName) {
        Cursor cursor = database.rawQuery(createTableExistsQuery(tableName), null);
        return cursor.getCount() > 0;
    }

    public static boolean tableExists(AppDatabase database, String tableName) {
        Cursor cursor = database.query(createTableExistsQuery(tableName), null);
        return cursor.getCount() > 0;
    }
}
