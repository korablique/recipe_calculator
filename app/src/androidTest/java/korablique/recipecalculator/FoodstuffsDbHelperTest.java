package korablique.recipecalculator;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.FoodstuffsDbHelper.TABLE_DATABASE_VERSION;
import static korablique.recipecalculator.HistoryContract.HISTORY_TABLE_NAME;

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
        database1.close();

        // Создать FoodstuffsDbHelper и сделать open
        FoodstuffsDbHelper helper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database2 = helper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

        // Убедиться, что БД имеет 2 версию
        Assert.assertTrue(DatabaseUtils.tableExists(database2, FOODSTUFFS_TABLE_NAME));
        Assert.assertTrue(isColumnExist(database2, FOODSTUFFS_TABLE_NAME, COLUMN_NAME_IS_LISTED));
        Assert.assertTrue(DatabaseUtils.tableExists(database2, HISTORY_TABLE_NAME));
        Assert.assertTrue(DatabaseUtils.tableExists(database2, TABLE_DATABASE_VERSION));
    }

    public boolean isColumnExist(SQLiteDatabase database, String tableName, String columnName) {
        Cursor cursor = database.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        try {
            while (cursor.moveToNext()) {
                String currentColumnName = cursor.getString(cursor.getColumnIndex("name"));
                if (currentColumnName.equals(columnName)) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }
}
