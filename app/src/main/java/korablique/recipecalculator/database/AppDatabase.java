package korablique.recipecalculator.database;

import androidx.room.Database;
import androidx.room.OnConflictStrategy;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Goal;
import korablique.recipecalculator.model.Lifestyle;

import static korablique.recipecalculator.database.DatabaseHolder.DATABASE_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME_NOCASE;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;

@Database(entities = {FoodstuffEntity.class, DatabaseVersionEntity.class, UserParametersEntity.class, HistoryEntity.class},
        version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract FoodstuffsDao foodstuffsDao();
    public abstract DatabaseVersionDao databaseVersionDao();
    public abstract UserParametersDao userParametersDao();
    public abstract HistoryDao historyDao();

//    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
//        @Override
//        public void migrate(SupportSQLiteDatabase database) {
//            database.beginTransaction();
//            try {
//                // create table history
//                database.execSQL("CREATE TABLE history (" +
//                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                        "date INTEGER, " +
//                        "foodstuff_id INTEGER, " +
//                        "weight REAL, " +
//                        "FOREIGN KEY (foodstuff_id) " +
//                        "REFERENCES foodstuffs(ID))"); // TODO: 22.01.19 ID большими буквами?
//
//                // create table database_version
//                database.execSQL("CREATE TABLE database_version (version INTEGER)");
//                database.execSQL("INSERT INTO database_version  VALUES (2)");
//
//                // create column is_listed
//                database.execSQL("ALTER TABLE foodstuffs ADD COLUMN is_listed INTEGER DEFAULT 1 NOT NULL");
//                // TODO: 22.01.19 надо ли default?
//
//                // create table user_parameters
//                database.execSQL("CREATE TABLE user_parameters (" +
//                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                        "goal TEXT, " +
//                        "gender TEXT, " +
//                        "age INTEGER, " +
//                        "height INTEGER, " +
//                        "weight INTEGER, " +
//                        "coefficient REAL, " +
//                        "formula TEXT)"); // TODO: 22.01.19 до этого там были строки
//                database.setTransactionSuccessful();
//            } finally {
//                database.endTransaction();
//            }
//        }
//    };
//
//    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
//        @Override
//        public void migrate(SupportSQLiteDatabase database) {
//            // добавляется новый столбец с названием продукта без заглавных букв
//            database.beginTransaction();
//            String tmpTableName = "foodstuffs_tmp";
//            String foodstuffsTableName = "foodstuffs";
//            String id = "ID";
//            String columnNameFoodstuffName = "foodstuff_name";
//            String columnNameFoodstuffNameNoCase = "name_nocase";
//            String columnNameProtein = "protein";
//            String columnNameFats = "fats";
//            String columnNameCarbs = "carbs";
//            String columnNameCalories = "calories";
//            String columnNameIsListed = "is_listed";
//            try {
//                database.execSQL("CREATE TABLE " + tmpTableName + " (" +
//                        id + " INTEGER PRIMARY KEY, " +
//                        columnNameFoodstuffName + " TEXT, " +
//                        columnNameFoodstuffNameNoCase + " TEXT, " +
//                        columnNameProtein + " REAL, " +
//                        columnNameFats + " REAL, " +
//                        columnNameCarbs + " REAL, " +
//                        columnNameCalories + " REAL, " +
//                        columnNameIsListed + " INTEGER DEFAULT 1 NOT NULL)");
//                database.execSQL("INSERT INTO " + tmpTableName +
//                        " SELECT " + id + ", " +
//                        columnNameFoodstuffName + ", " +
//                        columnNameFoodstuffName + " AS " + columnNameFoodstuffNameNoCase + ", " +
//                        columnNameProtein + ", " +
//                        columnNameFats + ", " +
//                        columnNameCarbs + ", " +
//                        columnNameCalories + ", " +
//                        columnNameIsListed +
//                        " FROM " + foodstuffsTableName);
//                database.execSQL("DROP TABLE " + foodstuffsTableName);
//                database.execSQL("ALTER TABLE " + tmpTableName + " RENAME TO " + foodstuffsTableName);
//                Cursor cursor = database.query(foodstuffsTableName, null);
//                while (cursor.moveToNext()) {
//                    String foodstuffName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
//                    long foodstuffId = cursor.getLong(cursor.getColumnIndex(FoodstuffsContract.ID));
//                    ContentValues values = new ContentValues();
//                    values.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, foodstuffName.toLowerCase());
//                    database.update(
//                            FOODSTUFFS_TABLE_NAME,
//                            OnConflictStrategy.FAIL,
//                            values,
//                            id + "=?",
//                            new String[]{String.valueOf(foodstuffId)});
//                }
//                cursor.close();
////                setDatabaseVersion(database, 3);
//                database.setTransactionSuccessful();
//            } finally {
//                database.endTransaction();
//            }
//        }
//    };
//
//    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            // goal, gender, lifestyle, formula хранятся в виде их id элементов enum'ов
//            String columnNameId = "id";
//            String columnNameGoal = "goal";
//            String columnNameGender = "gender";
//            String columnNameAge = "age";
//            String columnNameHeight = "height";
//            String columnNameWeight = "weight";
//            String columnNameLifestyle = "lifestyle";
//            String columnNameCoefficient = "coefficient";
//            String columnNameFormula = "formula";
//            String userParametersTableName = "user_parameters";
//            String tmpTableName = userParametersTableName + "_tmp";
//
//            database.beginTransaction();
//            try {
//                database.execSQL("CREATE TABLE " + tmpTableName + " (" +
//                        columnNameId + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                        columnNameGoal + " INTEGER, " +
//                        columnNameGender + " INTEGER, " +
//                        columnNameAge + " INTEGER, " +
//                        columnNameHeight + " INTEGER, " +
//                        columnNameWeight + " INTEGER, " +
//                        columnNameLifestyle + " INTEGER, " +
//                        columnNameFormula + " INTEGER)");
//
//                Cursor cursor = database.query(userParametersTableName, null);
//                while (cursor.moveToNext()) {
//                    long id = cursor.getLong(cursor.getColumnIndex(columnNameId));
//
//                    String goalStr = cursor.getString(cursor.getColumnIndex(columnNameGoal));
//                    Goal goal = DeprecetedDatabaseValues.convertGoal(goalStr);
//                    int goalId = goal.getId();
//
//                    String genderStr = cursor.getString(cursor.getColumnIndex(columnNameGender));
//                    Gender gender = DeprecetedDatabaseValues.convertGender(genderStr);
//                    int genderId = gender.getId();
//
//                    int age = cursor.getInt(cursor.getColumnIndex(columnNameAge));
//                    int height = cursor.getInt(cursor.getColumnIndex(columnNameHeight));
//                    int weight = cursor.getInt(cursor.getColumnIndex(columnNameWeight));
//
//                    float coefficient = cursor.getFloat(cursor.getColumnIndex(columnNameCoefficient));
//                    Lifestyle lifestyle = DeprecetedDatabaseValues.convertCoefficient(coefficient);
//                    int lifestyleId = lifestyle.getId();
//
//                    String formulaStr = cursor.getString(cursor.getColumnIndex(columnNameFormula));
//                    Formula formula = DeprecetedDatabaseValues.convertFormula(formulaStr);
//                    int formulaId = formula.getId();
//
//                    ContentValues values = new ContentValues();
//                    values.put(columnNameId, id);
//                    values.put(columnNameGoal, goalId);
//                    values.put(columnNameGender, genderId);
//                    values.put(columnNameAge, age);
//                    values.put(columnNameHeight, height);
//                    values.put(columnNameWeight, weight);
//                    values.put(columnNameLifestyle, lifestyleId);
//                    values.put(columnNameFormula, formulaId);
//
//                    database.insert(tmpTableName, OnConflictStrategy.FAIL, values);
//                }
//                cursor.close();
//
//                database.execSQL("DROP TABLE " + userParametersTableName);
//                database.execSQL("ALTER TABLE " + tmpTableName + " RENAME TO " + userParametersTableName);
//                database.setTransactionSuccessful();
//            } finally {
//                database.endTransaction();
//            }
//        }
//    };
}

//@Database(entities = {FoodstuffEntity.class, UserParametersEntity.class, DatabaseVersionEntity.class, HistoryEntity.class}, version = 4)
//public abstract class AppDatabase extends RoomDatabase {
//
//    private static AppDatabase sInstance;
//
//    @VisibleForTesting
//    public static final String DATABASE_NAME = "Main.db";
//
//    public abstract ProductDao productDao();
//
//    public abstract CommentDao commentDao();
//
//    private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();
//
//    public static AppDatabase getInstance(final Context context, final AppExecutors executors) {
//        if (sInstance == null) {
//            synchronized (AppDatabase.class) {
//                if (sInstance == null) {
//                    sInstance = buildDatabase(context.getApplicationContext(), executors);
//                    sInstance.updateDatabaseCreated(context.getApplicationContext());
//                }
//            }
//        }
//        return sInstance;
//    }
//
//    /**
//     * Build the database. {@link Builder#build()} only sets up the database configuration and
//     * creates a new instance of the database.
//     * The SQLite database is only created when it's accessed for the first time.
//     */
//    private static AppDatabase buildDatabase(final Context appContext,
//                                             final AppExecutors executors) {
//        return Room.databaseBuilder(appContext, AppDatabase.class, DATABASE_NAME)
//                .addCallback(new Callback() {
//                    @Override
//                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
//                        super.onCreate(db);
//                        executors.diskIO().execute(() -> {
//                            // Add a delay to simulate a long-running operation
//                            addDelay();
//                            // Generate the data for pre-population
//                            AppDatabase database = AppDatabase.getInstance(appContext, executors);
//                            List<ProductEntity> products = DataGenerator.generateProducts();
//                            List<CommentEntity> comments =
//                                    DataGenerator.generateCommentsForProducts(products);
//
//                            insertData(database, products, comments);
//                            // notify that the database was created and it's ready to be used
//                            database.setDatabaseCreated();
//                        });
//                    }
//                })
//                .addMigrations(MIGRATION_1_2)
//                .build();
//    }
//
//    /**
//     * Check whether the database already exists and expose it via {@link #getDatabaseCreated()}
//     */
//    private void updateDatabaseCreated(final Context context) {
//        if (context.getDatabasePath(DATABASE_NAME).exists()) {
//            setDatabaseCreated();
//        }
//    }
//
//    private void setDatabaseCreated(){
//        mIsDatabaseCreated.postValue(true);
//    }
//
//    private static void insertData(final AppDatabase database, final List<ProductEntity> products,
//                                   final List<CommentEntity> comments) {
//        database.runInTransaction(() -> {
//            database.productDao().insertAll(products);
//            database.commentDao().insertAll(comments);
//        });
//    }
//
//    private static void addDelay() {
//        try {
//            Thread.sleep(4000);
//        } catch (InterruptedException ignored) {
//        }
//    }
//
//    public LiveData<Boolean> getDatabaseCreated() {
//        return mIsDatabaseCreated;
//    }
//
//    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
//
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `productsFts` USING FTS4("
//                    + "`name` TEXT, `description` TEXT, content=`products`)");
//            database.execSQL("INSERT INTO productsFts (`rowid`, `name`, `description`) "
//                    + "SELECT `id`, `name`, `description` FROM products");
//
//        }
//    };
//}
