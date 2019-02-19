package korablique.recipecalculator.database.room;

import android.database.Cursor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import korablique.recipecalculator.FloatUtils;

import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_TARGET_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_USER_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_2_3;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_3_4;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MigrationTest {
    public static final String TEST_DB = "migration-test";

    @Rule
    public MigrationTestHelper helper;

    public MigrationTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                AppDatabase.class.getCanonicalName(),
                new FrameworkSQLiteOpenHelperFactory());
    }

    @Test
    public void migrate2To3() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);

        // db has schema version 2. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
        int id = 1, goalId = 1, genderId = 1, age = 25, height = 158, weight = 48, lifestyleId = 0, formulaId = 0;
        db.execSQL("INSERT INTO " + USER_PARAMETERS_TABLE_NAME + " VALUES (" +
                + id + ", " + goalId + ", " + genderId + ", " + age + ", " + height + ", "
                + weight + ", " + lifestyleId + ", " + formulaId + ")");

        // Prepare for the next version.
        db.close();

        // Re-open the database with version 3 and provide
        // MIGRATION_2_3 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3);

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        Cursor cursor = db.query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME);
        // убеждаемся, что таблица пуста, т к при миграции мы удаляем её и создаём заново,
        // не перенося старые данные
        Assert.assertTrue(!cursor.moveToFirst());
    }

    @Test
    public void migrate3To4() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 3);
        int id = 1, targetWeight = 45, genderId = 1, age = 25, height = 158, weight = 48, lifestyleId = 0, formulaId = 0;
        db.execSQL("INSERT INTO " + USER_PARAMETERS_TABLE_NAME + " VALUES (" +
                + id + ", " + targetWeight + ", " + genderId + ", " + age + ", " + height + ", "
                + weight + ", " + lifestyleId + ", " + formulaId + ")");
        db.close();

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4);
        Cursor cursor = db.query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME);
        while (cursor.moveToNext()) {
            float targetWeightFloat = cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_TARGET_WEIGHT));
            Assert.assertTrue(FloatUtils.areFloatsEquals(targetWeight, targetWeightFloat));

            float weightFloat = cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_USER_WEIGHT));
            Assert.assertTrue(FloatUtils.areFloatsEquals(weight, weightFloat));
        }
    }
}
