package korablique.recipecalculator.database;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import korablique.recipecalculator.TestEnvironmentDetector;

import android.content.Context;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseHolder {
    public static final int DATABASE_VERSION = 4;
    public static final String DATABASE_NAME = "Main.db";
    private AppDatabase db;
    private Context context;
    private DatabaseThreadExecutor databaseThreadExecutor;
    private DbHelper dbHelper;

    enum InitializationType {
        None, // БД находится в самом свежем состоянии, никакая особая инициализация не требовалась
        Creation, // БД отсутствовала - мы скопировали её из Ассетов и проинициализировали
        Update // БД присутствовала, но была старой версии - пришлось её обновить
    }

    @Inject
    public DatabaseHolder(Context context, DatabaseThreadExecutor databaseThreadExecutor) {
        this.context = context;
        this.databaseThreadExecutor = databaseThreadExecutor;
        this.dbHelper = new DbHelper(context);
        try {
            dbHelper.initializeDatabase();
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't initialize db", e);
        }
    }

    public class InitializationResult {
        private InitializationType performedInitialization; // Какая инициализация была произведена
        private int oldVersion; // Версия БД до инициализации (-1 если БД не было)
        private int newVersion; // Версия БД после инициализации

        public InitializationResult(InitializationType performedInitialization, int oldVersion, int newVersion) {
            this.performedInitialization = performedInitialization;
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }

        public InitializationType getPerformedInitialization() {
            return performedInitialization;
        }

        public int getOldVersion() {
            return oldVersion;
        }

        public int getNewVersion() {
            return newVersion;
        }
    }

    public AppDatabase getDatabase() {

        if (db == null) {
            String pathToDb = DbHelper.getDbFile(context).getAbsolutePath();
            RoomDatabase.Builder<AppDatabase> builder =
                    Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, pathToDb)
                    .setQueryExecutor((runnable) -> {
                        databaseThreadExecutor.execute(runnable);
                    });
            if (TestEnvironmentDetector.isInTests()) {
                builder.allowMainThreadQueries();
            }
            db = builder.build();
        }
        return db;
    }








//    InitializationResult initializeDatabase() throws IOException { // TODO: 22.01.19 зачем IOException?
//        if (!dbExists()) {
//            createDatabase();
//            return new InitializationResult(InitializationType.Creation, -1, DATABASE_VERSION);
//        } else {
//            return tryToUpgradeDatabase();
//        }
//    }
//
//    public static synchronized void deinitializeDatabase(Context context) {
//        File dbFile = getDbFile(context);
//        if (!dbFile.exists()) {
//            return;
//        }
//        boolean deleted = dbFile.delete();
//        if (!deleted) {
//            throw new Error("Couldn't delete database");
//        }
//        initialized = false;
//    }
//
//    /**
//     * При отсутствии базы данных, копирует её из assets
//     */
//    private void createDatabase() throws IOException {
//        copyDatabaseFromAssets();
//        File path = getDbFile(context);
////        SQLiteDatabase database = SQLiteDatabase.openDatabase(path.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
////        createTableHistory(database);
////        createTableDatabaseVersion(database);
////        createTableUserParameters(database);
//        // TODO: 22.01.19 как открыть? заполнять таблицами или изменить бд в ассетах?
//    }

//    private void createTableHistory(SQLiteDatabase database) {
//        database.execSQL("CREATE TABLE " + HISTORY_TABLE_NAME + " (" +
//                HistoryContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                COLUMN_NAME_DATE + " INTEGER, " +
//                COLUMN_NAME_FOODSTUFF_ID + " INTEGER, " +
//                COLUMN_NAME_WEIGHT + " REAL, " +
//                "FOREIGN KEY (" + COLUMN_NAME_FOODSTUFF_ID + ") " +
//                "REFERENCES " + FOODSTUFFS_TABLE_NAME + "(" + FoodstuffsContract.ID + "))");
//    }
//
//    private void createTableDatabaseVersion(SQLiteDatabase database) {
//        database.execSQL("CREATE TABLE " + TABLE_DATABASE_VERSION + " (" + COLUMN_NAME_VERSION + " INTEGER)");
//        database.execSQL("INSERT INTO " + TABLE_DATABASE_VERSION + " VALUES (" + DATABASE_VERSION + ")");
//    }
//
//    private void createTableUserParameters(SQLiteDatabase database) {
//        database.execSQL("CREATE TABLE " + USER_PARAMETERS_TABLE_NAME + " (" +
//                UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                COLUMN_NAME_GOAL + " INTEGER, " +
//                COLUMN_NAME_GENDER + " INTEGER, " +
//                COLUMN_NAME_AGE + " INTEGER, " +
//                COLUMN_NAME_HEIGHT + " INTEGER, " +
//                COLUMN_NAME_USER_WEIGHT + " INTEGER, " +
//                COLUMN_NAME_LIFESTYLE + " INTEGER, " +
//                COLUMN_NAME_FORMULA + " INTEGER)");
//    }


















//    private void setDatabaseVersion(SQLiteDatabase database, int newVersion) {
//        ContentValues values = new ContentValues(1);
//        values.put(COLUMN_NAME_VERSION, newVersion);
//        database.update(TABLE_DATABASE_VERSION, values, null, null);
//    }
//
//    private void addColumnIsListed(SQLiteDatabase database) {
//        database.execSQL("ALTER TABLE " + FOODSTUFFS_TABLE_NAME + " ADD COLUMN " + COLUMN_NAME_IS_LISTED
//                + " INTEGER DEFAULT 1 NOT NULL");
//    }
//
//    private boolean dbExists() {
//        SQLiteDatabase db = null;
//        try {
//            db = SQLiteDatabase.openDatabase(getDbFile(context).getPath(), null, SQLiteDatabase.OPEN_READONLY);
//        } catch (SQLiteException e) {
//            Crashlytics.log("openDatabase выбросил исключение: " + e.getMessage());
//            //база еще не существует
//        }
//
//        if (db != null) {
//            db.close();
//        }
//        return db != null;
//    }
//
//    /**
//     * Копирует БД из папки assets вместо созданной локальной БД
//     * Выполняется путем копирования потока байтов.
//     * */
//    private void copyDatabaseFromAssets() throws IOException {
//        //Открываем локальную БД как входящий поток
//        InputStream myInput = context.getAssets().open(DATABASE_NAME);
//
//        //Путь ко вновь созданной БД
//        File outFile = getDbFile(context);
//
//        //Открываем пустую базу данных как исходящий поток
//        OutputStream myOutput = new FileOutputStream(outFile);
//
//        //перемещаем байты из входящего файла в исходящий
//        byte[] buffer = new byte[1024];
//        int length;
//        while ((length = myInput.read(buffer)) > 0) {
//            myOutput.write(buffer, 0, length);
//        }
//
//        //закрываем потоки
//        myOutput.flush();
//        myOutput.close();
//        myInput.close();
//    }
//
//    public synchronized SQLiteDatabase openDatabase(int flag) throws SQLException {
//        if (!initialized) {
//            try {
//                initializeDatabase();
//            } catch (IOException e) {
//                throw new Error(e);
//            }
//            initialized = true;
//        }
//        return SQLiteDatabase.openDatabase(getDbFile(context).getPath(), null, flag);
//    }
//
//    public static File getDbFile(Context context) {
//        return new File(context.getFilesDir(), DATABASE_NAME);
//    }
//
//    private int getDatabaseVersion(SQLiteDatabase database) {
//        Cursor cursor = database.query(TABLE_DATABASE_VERSION, null, null, null, null, null, null);
//        int databaseVersion = -1;
//        while (cursor.moveToNext()) {
//            databaseVersion = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_VERSION));
//        }
//        cursor.close();
//        return databaseVersion;
//    }
}
