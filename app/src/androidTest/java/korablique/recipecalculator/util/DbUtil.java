package korablique.recipecalculator.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import junit.framework.Assert;

import korablique.recipecalculator.database.FoodstuffsDbHelper;

public class DbUtil {
    public static void clearTable(Context context, String tableName) {
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
        database.delete(tableName, null, null);
        Cursor cursor = database.rawQuery("SELECT * FROM " + tableName, null);
        Assert.assertTrue(cursor.getCount() == 0);
        cursor.close();
    }
}
