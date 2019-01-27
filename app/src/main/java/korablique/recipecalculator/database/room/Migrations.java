package korablique.recipecalculator.database.room;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import korablique.recipecalculator.database.FoodstuffsContract;
import korablique.recipecalculator.database.HistoryContract;
import korablique.recipecalculator.database.UserParametersContract;

public class Migrations {
    private Migrations() {}

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            LegacyDatabaseUpdater.upgradeIfNeeded(database);

            // Add NOT NULL to foodstuffs table
            database.execSQL(
                    "CREATE TABLE foodstuffs_tmp(" +
                            FoodstuffsContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME + " TEXT NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " TEXT NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_PROTEIN + " REAL NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_FATS + " REAL NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_CARBS + " REAL NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_CALORIES + " REAL NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_IS_LISTED + " INTEGER DEFAULT 1 NOT NULL)");
            replaceTable(database, "foodstuffs_tmp", FoodstuffsContract.FOODSTUFFS_TABLE_NAME);

            // Add NOT NULL to history table
            database.execSQL(
                    "CREATE TABLE history_tmp (" +
                            HistoryContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            HistoryContract.COLUMN_NAME_DATE + " INTEGER NOT NULL, " +
                            HistoryContract.COLUMN_NAME_FOODSTUFF_ID + " INTEGER NOT NULL, " +
                            HistoryContract.COLUMN_NAME_WEIGHT + " REAL NOT NULL, " +
                            "FOREIGN KEY (" + HistoryContract.COLUMN_NAME_FOODSTUFF_ID + ") " +
                            "REFERENCES " + FoodstuffsContract.FOODSTUFFS_TABLE_NAME + "(" + FoodstuffsContract.ID + "))");
            replaceTable(database, "history_tmp", HistoryContract.HISTORY_TABLE_NAME);

            // Add not null to user parameters table
            database.execSQL(
                    "CREATE TABLE user_params_tmp (" +
                            UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_GOAL + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_GENDER + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_AGE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_HEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_USER_WEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_LIFESTYLE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_FORMULA + " INTEGER NOT NULL)");
            replaceTable(database, "user_params_tmp", UserParametersContract.USER_PARAMETERS_TABLE_NAME);

            // Drop versions table - Room now controls DB versioning
            database.execSQL("DROP TABLE " + LegacyDatabaseUpdater.TABLE_DATABASE_VERSION);

            // Create an index manually - Room expects the index since it's declare in the Entity.
            database.execSQL("CREATE INDEX index_history_foodstuff_id ON history(foodstuff_id)");
        }
    };

    private static void replaceTable(
            SupportSQLiteDatabase database, String tmpTableName, String targetTableName) {
        database.execSQL("INSERT INTO " + tmpTableName + " SELECT * FROM " + targetTableName);
        database.execSQL("DROP TABLE " + targetTableName);
        database.execSQL("ALTER TABLE " + tmpTableName + " RENAME TO " + targetTableName);
    }
}
