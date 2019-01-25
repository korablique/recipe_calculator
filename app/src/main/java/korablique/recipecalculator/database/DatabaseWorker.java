package korablique.recipecalculator.database;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.model.Foodstuff;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.FoodstuffsContract.ID;

public class DatabaseWorker {
    public static final int NO_LIMIT = -1;
    public static final int UNLISTED = 0;
    public static final int LISTED = 1;
    private DatabaseHolder databaseHolder;
    private DatabaseThreadExecutor databaseThreadExecutor;
    private MainThreadExecutor mainThreadExecutor;

    public interface FoodstuffsBatchReceiveCallback {
        void onReceive(List<Foodstuff> foodstuffs);
    }

    public interface FinishCallback {
        void onFinish();
    }

    public interface SaveFoodstuffCallback {
        void onResult(long id);
        void onDuplication();
    }

    public interface SaveGroupOfFoodstuffsCallback {
        void onResult(List<Long> ids);
    }

    public interface SaveUnlistedFoodstuffCallback {
        void onResult(long foodstuffId);
    }

    public DatabaseWorker(
            DatabaseHolder databaseHolder,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor) {
        this.databaseHolder = databaseHolder;
        this.mainThreadExecutor = mainThreadExecutor;
        this.databaseThreadExecutor = databaseThreadExecutor;
    }

    public void saveFoodstuff(
            final Foodstuff foodstuff) {
        saveFoodstuff(foodstuff, null);
    }

    public void saveFoodstuff(
            final Foodstuff foodstuff,
            final SaveFoodstuffCallback callback) {
        databaseThreadExecutor.execute(() -> {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
            FoodstuffEntity foodstuffEntity = new FoodstuffEntity(
                    foodstuff.getName(),
                    foodstuff.getName().toLowerCase(),
                    (float) foodstuff.getProtein(),
                    (float) foodstuff.getFats(),
                    (float) foodstuff.getCarbs(),
                    (float) foodstuff.getCalories(),
                    LISTED);
            long id = foodstuffsDao.insertFoodstuff(foodstuffEntity);

//            String whereClause = COLUMN_NAME_FOODSTUFF_NAME + "=? AND " +
//                    COLUMN_NAME_PROTEIN + "=? AND " +
//                    COLUMN_NAME_FATS + "=? AND " +
//                    COLUMN_NAME_CARBS + "=? AND " +
//                    COLUMN_NAME_CALORIES + "=? AND " +
//                    COLUMN_NAME_IS_LISTED + "=?";
//            String[] selectionArgs = new String[] {
//                    String.valueOf(foodstuff.getName()),
//                    String.valueOf(foodstuff.getProtein()),
//                    String.valueOf(foodstuff.getFats()),
//                    String.valueOf(foodstuff.getCarbs()),
//                    String.valueOf(foodstuff.getCalories()),
//                    String.valueOf(1)}; // listed
//            Cursor cursor = database.query(
//                    FOODSTUFFS_TABLE_NAME,
//                    null,
//                    whereClause,
//                    selectionArgs,
//                    null, null, null);
//            //если такого продукта нет в БД:
//            boolean alreadyContainsListedFoodstuff = false;
//            long id = -1;
//            if (cursor.getCount() == 0) {
//                ContentValues values = new ContentValues();
//                values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
//                values.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, foodstuff.getName().toLowerCase());
//                values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
//                values.put(COLUMN_NAME_FATS, foodstuff.getFats());
//                values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
//                values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
//                id = database.insert(FOODSTUFFS_TABLE_NAME, null, values);
//            } else {
//                alreadyContainsListedFoodstuff = true;
//            }
//            cursor.close();

            if (callback != null) {
                if (id == -1) {
                    mainThreadExecutor.execute(() -> callback.onDuplication());
                } else {
                    mainThreadExecutor.execute(() -> callback.onResult(id));
                }
            }
        });
    }

    /**
     * Порядок возвращаемых айдишников гарантированно идентичен порядку переданных фудстаффов.
     */
    public void saveGroupOfFoodstuffs(
            final Foodstuff[] foodstuffs,
            final SaveGroupOfFoodstuffsCallback callback) {
        databaseThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
//                DbHelper dbHelper = new DbHelper(context);
//                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
//                final ArrayList<Long> ids = new ArrayList<>();
//
//                database.beginTransaction();
//                try {
//                    for (Foodstuff foodstuff : foodstuffs) {
//                        ContentValues values = new ContentValues();
//                        values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
//                        values.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, foodstuff.getName().toLowerCase());
//                        values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
//                        values.put(COLUMN_NAME_FATS, foodstuff.getFats());
//                        values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
//                        values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
//                        long id = database.insert(FOODSTUFFS_TABLE_NAME, null, values);
//                        ids.add(id);
//                    }
//                    database.setTransactionSuccessful();
//                } finally {
//                    database.endTransaction();
//                }

                AppDatabase database = databaseHolder.getDatabase();
                FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
                List<FoodstuffEntity> foodstuffsEntities = new ArrayList<>();
                for (int index = 0; index < foodstuffs.length; index++) {
                    FoodstuffEntity foodstuffEntity = new FoodstuffEntity(
                            foodstuffs[index].getName(),
                            foodstuffs[index].getName().toLowerCase(),
                            (float) foodstuffs[index].getProtein(),
                            (float) foodstuffs[index].getFats(),
                            (float) foodstuffs[index].getCarbs(),
                            (float) foodstuffs[index].getCalories(),
                            LISTED);
                    foodstuffsEntities.add(foodstuffEntity);
                }

                List<Long> ids = foodstuffsDao.insertFoodstuffs(foodstuffsEntities);
                if (callback != null) {
                    // TODO: 14.01.19 нет проверки, что продукт уже существует. если так, то это будет абортировано
                    mainThreadExecutor.execute(() -> callback.onResult(ids));
                }
            }
        });
    }

    public void saveUnlistedFoodstuff( // TODO: 14.01.19 метод идентичен saveFoodstuff() кроме LISTED/UNLISTED
            final Foodstuff foodstuff,
            @NonNull final SaveUnlistedFoodstuffCallback callback) {
        databaseThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
//                DbHelper dbHelper = new DbHelper(context);
//                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
//                ContentValues values = new ContentValues();
//                values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
//                values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
//                values.put(COLUMN_NAME_FATS, foodstuff.getFats());
//                values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
//                values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
//                values.put(COLUMN_NAME_IS_LISTED, 0);
//                long foodstuffId = database.insert(FOODSTUFFS_TABLE_NAME, null, values);
                AppDatabase database = databaseHolder.getDatabase();
                FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
                FoodstuffEntity foodstuffEntity = new FoodstuffEntity(
                        foodstuff.getName(),
                        foodstuff.getName().toLowerCase(),
                        (float) foodstuff.getProtein(),
                        (float) foodstuff.getFats(),
                        (float) foodstuff.getCarbs(),
                        (float) foodstuff.getCalories(),
                        UNLISTED);
                long id = foodstuffsDao.insertFoodstuff(foodstuffEntity);

                if (callback != null) {
                    mainThreadExecutor.execute(() -> callback.onResult(id));
                }
            }
        });
    }

    /**
     * @param newFoodstuffId new foodstuff id the same as old
     * @param newFoodstuff updated foodstuff
     * @param callback
     */
    public void editFoodstuff(
            final long newFoodstuffId,
            final Foodstuff newFoodstuff,
            Runnable callback) {
        databaseThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
//                DbHelper dbHelper = new DbHelper(context);
//                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
//
//                ContentValues contentValues = new ContentValues();
//                contentValues.put(COLUMN_NAME_FOODSTUFF_NAME, newFoodstuff.getName());
//                contentValues.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, newFoodstuff.getName().toLowerCase());
//                contentValues.put(COLUMN_NAME_PROTEIN, newFoodstuff.getProtein());
//                contentValues.put(COLUMN_NAME_FATS, newFoodstuff.getFats());
//                contentValues.put(COLUMN_NAME_CARBS, newFoodstuff.getCarbs());
//                contentValues.put(COLUMN_NAME_CALORIES, newFoodstuff.getCalories());
//                database.update(
//                        FOODSTUFFS_TABLE_NAME,
//                        contentValues,
//                        FoodstuffsContract.ID + " = ?",
//                        new String[]{String.valueOf(editedFoodstuffId)});
                AppDatabase database = databaseHolder.getDatabase();
                FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
                FoodstuffEntity foodstuffEntity = new FoodstuffEntity(
                        newFoodstuff.getName(),
                        newFoodstuff.getName().toLowerCase(),
                        (float) newFoodstuff.getProtein(),
                        (float) newFoodstuff.getFats(),
                        (float) newFoodstuff.getCarbs(),
                        (float) newFoodstuff.getCalories(),
                        LISTED);
                foodstuffEntity.setId(newFoodstuffId);
                foodstuffsDao.updateFoodstuffs(foodstuffEntity);

                if (callback != null) {
                    mainThreadExecutor.execute(callback);
                }
            }
        });
    }

    /**
     * @param foodstuff foodstuff containing id
     */
    public void deleteFoodstuff(Foodstuff foodstuff) {
        databaseThreadExecutor.execute(() -> {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
//            database.delete(
//                    FOODSTUFFS_TABLE_NAME,
//                    FoodstuffsContract.ID + "=?",
//                    new String[]{String.valueOf(foodstuffsId)});
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
            FoodstuffEntity foodstuffEntity = new FoodstuffEntity(
                    foodstuff.getName(),
                    foodstuff.getName().toLowerCase(),
                    (float) foodstuff.getProtein(),
                    (float) foodstuff.getFats(),
                    (float) foodstuff.getCarbs(),
                    (float) foodstuff.getCalories(),
                    LISTED); // TODO: 15.01.19 что если нужно будет удалить unlisted foodstuff?
            foodstuffEntity.setId(foodstuff.getId());
            foodstuffsDao.deleteFoodstuff(foodstuffEntity);
        });
    }

    /**
     * @param foodstuff foodstuff containing id
     * @param callback
     */
    public void makeFoodstuffUnlisted(final Foodstuff foodstuff, final Runnable callback) {
        databaseThreadExecutor.execute(() -> {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
//            ContentValues values = new ContentValues();
//            values.put(COLUMN_NAME_IS_LISTED, 0);
//            database.update(
//                    FOODSTUFFS_TABLE_NAME,
//                    values,
//                    FoodstuffsContract.ID + "=?",
//                    new String[]{String.valueOf(foodstuffId)});
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
            FoodstuffEntity foodstuffEntity = new FoodstuffEntity(
                    foodstuff.getName(),
                    foodstuff.getName().toLowerCase(),
                    (float) foodstuff.getProtein(),
                    (float) foodstuff.getFats(),
                    (float) foodstuff.getCarbs(),
                    (float) foodstuff.getCalories(),
                    UNLISTED);
            foodstuffEntity.setId(foodstuff.getId());
            foodstuffsDao.updateFoodstuffs(foodstuffEntity);
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }
        });
    }

    public void requestListedFoodstuffsFromDb(
            final int batchSize,
            @NonNull final FoodstuffsBatchReceiveCallback foodstuffsBatchReceiveCallback,
            @Nullable FinishCallback finishCallback) {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase db = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
//            Cursor cursor = db.query(
//                    FOODSTUFFS_TABLE_NAME,
//                    null,
//                    COLUMN_NAME_IS_LISTED + "=?",
//                    new String[]{ String.valueOf(1) },
//                    null,
//                    null,
//                    COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " ASC");
//            ArrayList<Foodstuff> batchOfFoodstuffs = new ArrayList<>();
//            int index = 0;
//            while (cursor.moveToNext()) {
//                long id = cursor.getLong(cursor.getColumnIndex(ID));
//                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
//                double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
//                double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
//                double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
//                double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
//                Foodstuff foodstuff = Foodstuff
//                        .withId(id)
//                        .withName(name)
//                        .withNutrition(protein, fats, carbs, calories);
//                batchOfFoodstuffs.add(foodstuff);
//                ++index;
//                if (index >= batchSize) {
//                    ArrayList<Foodstuff> batchCopy = new ArrayList<>(batchOfFoodstuffs);
//                    mainThreadExecutor.execute(() -> foodstuffsBatchReceiveCallback.onReceive(batchCopy));
//                    batchOfFoodstuffs.clear();
//                    index = 0;
//                }
//            }
//            if (batchOfFoodstuffs.size() > 0) {
//                mainThreadExecutor.execute(() -> foodstuffsBatchReceiveCallback.onReceive(batchOfFoodstuffs));
//            }
//            cursor.close();

        databaseThreadExecutor.execute(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();

            try (Cursor cursor = foodstuffsDao.loadListedFoodstuffs()) {
                ArrayList<Foodstuff> batchOfFoodstuffs = new ArrayList<>();
                int index = 0;
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndex(ID));
                    String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
                    double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
                    double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
                    double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
                    double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
                    Foodstuff foodstuff = Foodstuff
                            .withId(id)
                            .withName(name)
                            .withNutrition(protein, fats, carbs, calories);
                    batchOfFoodstuffs.add(foodstuff);
                    ++index;
                    if (index >= batchSize) {
                        ArrayList<Foodstuff> batchCopy = new ArrayList<>(batchOfFoodstuffs);
                        mainThreadExecutor.execute(() -> foodstuffsBatchReceiveCallback.onReceive(batchCopy));
                        batchOfFoodstuffs.clear();
                        index = 0;
                    }
                }
                if (batchOfFoodstuffs.size() > 0) {
                    mainThreadExecutor.execute(() -> foodstuffsBatchReceiveCallback.onReceive(batchOfFoodstuffs));
                }
            }
            if (finishCallback != null) {
                finishCallback.onFinish();
            }
        });


//            AppDatabase database = databaseHolder.getDatabase();
//            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
//            Observable<List<FoodstuffEntity>> entityFlowable = foodstuffsDao.loadListedFoodstuffsAAAAAAAAAA();
//
//            Disposable d = entityFlowable
////                    .buffer(1)
//                    .observeOn(mainThreadExecutor.asScheduler())
//                    .subscribe((foodstuffEntities) -> {
//                        List<Foodstuff> batch = new ArrayList<>();
////                        for (FoodstuffEntity entity : foodstuffEntities) {
////                            Foodstuff foodstuff = Foodstuff
////                                    .withId(entity.getId())
////                                    .withName(entity.getName())
////                                    .withNutrition(entity.getProtein(), entity.getFats(), entity.getCarbs(), entity.getCalories());
////                            batch.add(foodstuff);
////                        }
//                        foodstuffsBatchReceiveCallback.onReceive(batch);
//                    }, (Throwable e) -> {
//                        throw new RuntimeException(e);
//                    }, () -> {
//                        if (finishCallback != null) {
//                            finishCallback.onFinish();
//                        }
//                    });
//            return d;

//                    .subscribe(new Observer<List<FoodstuffEntity>>() {
//                        @Override
//                        public void onSubscribe(Disposable d) {
//
//                        }
//
//                        @Override
//                        public void onNext(List<FoodstuffEntity> foodstuffEntities) {
//                            Log.e("DANIL", "DANIL, Nope, too late!");
//                            List<Foodstuff> batch = new ArrayList<>();
//                            for (FoodstuffEntity entity : foodstuffEntities) {
//                                Foodstuff foodstuff = Foodstuff
//                                        .withId(entity.getId())
//                                        .withName(entity.getName())
//                                        .withNutrition(entity.getProtein(), entity.getFats(), entity.getCarbs(), entity.getCalories());
//                                batch.add(foodstuff);
//                            }
//                            foodstuffsBatchReceiveCallback.onReceive(batch);
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            throw new RuntimeException(e);
//                        }
//
//                        @Override
//                        public void onComplete() {
//                            if (finishCallback != null) {
//                                finishCallback.onFinish();
//                            }
//                        }
//                    });
//            if (finishCallback != null) {
//                mainThreadExecutor.execute(finishCallback::onFinish);
//            }
    }

    private List<Foodstuff> entitiesToFoodstuffs(List<FoodstuffEntity> foodstuffEntities) {
        List<Foodstuff> foodstuffs = new ArrayList<>();
        for (FoodstuffEntity entity : foodstuffEntities) {
            Foodstuff foodstuff = Foodstuff
                    .withId(entity.getId())
                    .withName(entity.getName())
                    .withNutrition(entity.getProtein(), entity.getFats(), entity.getCarbs(), entity.getCalories());
            foodstuffs.add(foodstuff);
        }
        return foodstuffs;
    }

    public void requestListedFoodstuffsFromDb(
            final int batchSize,
            @NonNull final FoodstuffsBatchReceiveCallback foodstuffsBatchReceiveCallback) {
        requestListedFoodstuffsFromDb(batchSize, foodstuffsBatchReceiveCallback, null);
    }

    /**
     * Метод получает фудстаффы по их id.
     * Порядок возвращенных из метода фудстаффов соответствует порядку переданных id. (Для этого
     * мы запрашиваем продукты по одному).
     * Если передать несуществующие id, то метод вернёт пустой List.
     * @param ids - id продуктов, которые мы хотим получить. TODO: инфа уже не 100%
     * @param callback - возвращает продукты.
     */
    public void requestFoodstuffsByIds(
            List<Long> ids,
            FoodstuffsBatchReceiveCallback callback) {
        databaseThreadExecutor.execute(() -> {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
//            List<Foodstuff> result = new ArrayList<>();
//            String[] columns = new String[] {
//                    COLUMN_NAME_FOODSTUFF_NAME,
//                    COLUMN_NAME_PROTEIN,
//                    COLUMN_NAME_FATS,
//                    COLUMN_NAME_CARBS,
//                    COLUMN_NAME_CALORIES };
//
//            for (Long id : ids) {
//                Cursor cursor = database.query(
//                        FOODSTUFFS_TABLE_NAME,
//                        columns,
//                        FoodstuffsContract.ID + "=?",
//                        new String[]{ String.valueOf(id) },
//                        null, null, null);
//                if (cursor.moveToNext()) {
//                    String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
//                    double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
//                    double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
//                    double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
//                    double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
//                    Foodstuff foodstuff =
//                            Foodstuff
//                                    .withId(id)
//                                    .withName(name)
//                                    .withNutrition(protein, fats, carbs, calories);
//                    result.add(foodstuff);
//                }
//                cursor.close();
//            }
//            if (callback != null) {
//                mainThreadExecutor.execute(() -> callback.onReceive(result));
//            }
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
            List<FoodstuffEntity> foodstuffEntities = foodstuffsDao.loadFoodstuffsByIds(ids);

            // Вызвыающий код ожидает, что полученые продукты будут расположен в том же порядке,
            // что имеют их переданные идентификаторы.
            // Поэтому после получения продуктов из БД мы меняем их порядок, используя Map.

            Map<Long, Foodstuff> foodstuffsMap = new HashMap<>();
            for (FoodstuffEntity entity : foodstuffEntities) {
                Foodstuff foodstuff = Foodstuff
                        .withId(entity.getId())
                        .withName(entity.getName())
                        .withNutrition(entity.getProtein(), entity.getFats(), entity.getCarbs(), entity.getCalories());
                foodstuffsMap.put(foodstuff.getId(), foodstuff);
            }

            List<Foodstuff> foodstuffs = new ArrayList<>();
            for (Long id : ids) {
                Foodstuff foodstuff = foodstuffsMap.get(id);
                if (foodstuff == null) {
                    throw new IllegalArgumentException("Foodstuff with passed ID wasn't present in DB: " + id);
                }
                foodstuffs.add(foodstuff);
            }

            mainThreadExecutor.execute(() -> callback.onReceive(foodstuffs));
        });
    }

    /**
     * Получает фудстаффы с названием, похожим на запрос.
     * @param nameQuery - поисковый запрос
     * @param limit - требуемое количество результатов поиска
     * @param callback
     */
    public void requestFoodstuffsLike(String nameQuery, int limit, FoodstuffsBatchReceiveCallback callback) {
        databaseThreadExecutor.execute(() -> {
//            DbHelper dbHelper = new DbHelper(context);
//            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
//            List<Foodstuff> result = new ArrayList<>();
//            String limitString = null;
//            if (limit != NO_LIMIT) {
//                limitString = String.valueOf(limit);
//            }
//            Cursor cursor = database.query(
//                    FOODSTUFFS_TABLE_NAME,
//                    null,
//                    COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " LIKE ?",
//                    new String[]{"%" + nameQuery.toLowerCase() + "%"},
//                    null,
//                    null,
//                    COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " ASC",
//                    limitString);
//            while (cursor.moveToNext()) {
//                long id = cursor.getLong(cursor.getColumnIndex(FoodstuffsContract.ID));
//                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
//                double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
//                double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
//                double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
//                double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
//                Foodstuff foodstuff =
//                        Foodstuff.withId(id).withName(name)
//                                .withNutrition(protein, fats, carbs, calories);
//                result.add(foodstuff);
//            }
//            cursor.close();
//            mainThreadExecutor.execute(() -> callback.onReceive(result));
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();

            String nameQueryRightFormat = '%' + nameQuery.toLowerCase() + '%';
            List<FoodstuffEntity> foodstuffEntities = foodstuffsDao.loadFoodstuffsLike(nameQueryRightFormat, limit);
            List<Foodstuff> foodstuffs = new ArrayList<>();
            for (FoodstuffEntity entity : foodstuffEntities) {
                Foodstuff foodstuff = Foodstuff
                        .withId(entity.getId())
                        .withName(entity.getName())
                        .withNutrition(entity.getProtein(), entity.getFats(), entity.getCarbs(), entity.getCalories());
                foodstuffs.add(foodstuff);
            }

            mainThreadExecutor.execute(() -> callback.onReceive(foodstuffs));
        });
    }
}
