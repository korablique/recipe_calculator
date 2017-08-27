package korablique.recipecalculator;

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

        Assert.assertFalse(DatabaseUtils.tableExists(database1, "foodstuffs"));

        // Заполнить файл табличками для 1 версии
        database1.execSQL("CREATE TABLE foodstuffs " +
                "(ID INTEGER PRIMARY KEY, foodstuff_name TEXT, protein REAL, " +
                "fats REAL, carbs REAL, calories REAL)");
        Assert.assertTrue(DatabaseUtils.tableExists(database1, "foodstuffs"));
        Assert.assertFalse(DatabaseUtils.tableExists(database1, "history"));
        Assert.assertFalse(DatabaseUtils.tableExists(database1, "database_version"));
        database1.close();

        // Создать FoodstuffsDbHelper и сделать open
        FoodstuffsDbHelper helper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database2 = helper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

        // Убедиться, что БД имеет 2 версию
        Assert.assertTrue(DatabaseUtils.tableExists(database2, "foodstuffs"));
        Assert.assertTrue(DatabaseUtils.tableExists(database2, "history"));
        Assert.assertTrue(DatabaseUtils.tableExists(database2, "database_version"));
    }
}
