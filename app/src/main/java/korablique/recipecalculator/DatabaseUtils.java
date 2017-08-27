package korablique.recipecalculator;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseUtils {
    public static boolean tableExists(SQLiteDatabase database, String tableName) {
        Cursor cursor = database.rawQuery("SELECT name " +
                "FROM sqlite_master " +
                "WHERE type='table' " +
                "AND name='" + tableName + "'", null);
        return cursor.getCount() > 0;
    }
}
