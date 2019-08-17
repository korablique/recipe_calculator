package korablique.recipecalculator.database;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.database.room.AppDatabase;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.database.room.FoodstuffEntity;
import korablique.recipecalculator.database.room.FoodstuffsDao;
import korablique.recipecalculator.model.Foodstuff;

import static korablique.recipecalculator.database.EntityConverter.toEntity;
import static korablique.recipecalculator.database.EntityConverter.toModel;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.FoodstuffsContract.ID;

public class DatabaseWorker {
    public static final int NO_LIMIT = -1;
    private static final int UNLISTED = 0;
    private static final int LISTED = 1;
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
        default void onDuplication() {}
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
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
            FoodstuffEntity entity = toEntity(foodstuff, LISTED);
            long id = foodstuffsDao.insertFoodstuff(entity);

            if (callback != null) {
                if (id > 0) {
                    mainThreadExecutor.execute(() -> callback.onResult(id));
                } else {
                    mainThreadExecutor.execute(() -> callback.onDuplication());
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
        databaseThreadExecutor.execute(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
            List<FoodstuffEntity> foodstuffsEntities = new ArrayList<>();
            for (Foodstuff foodstuff : foodstuffs) {
                FoodstuffEntity entity = toEntity(foodstuff, LISTED);
                foodstuffsEntities.add(entity);
            }

            List<Long> ids = foodstuffsDao.insertFoodstuffs(foodstuffsEntities);
            if (callback != null) {
                mainThreadExecutor.execute(() -> callback.onResult(ids));
            }
        });
    }

    public void saveUnlistedFoodstuff(
            final Foodstuff foodstuff,
            @NonNull final SaveUnlistedFoodstuffCallback callback) {
        databaseThreadExecutor.execute(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
            FoodstuffEntity entity = toEntity(foodstuff, UNLISTED);
            long id = foodstuffsDao.insertFoodstuff(entity);
            if (callback != null) {
                mainThreadExecutor.execute(() -> callback.onResult(id));
            }
        });
    }

    /**
     * @param foodstuffId new foodstuff id the same as old
     * @param newFoodstuff updated foodstuff
     */
    public void editFoodstuff(
            final long foodstuffId,
            final Foodstuff newFoodstuff,
            Runnable callback) {
        databaseThreadExecutor.execute(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
            foodstuffsDao.updateFoodstuff(
                    foodstuffId,
                    newFoodstuff.getName(),
                    newFoodstuff.getName().toLowerCase(),
                    (float) newFoodstuff.getProtein(),
                    (float) newFoodstuff.getFats(),
                    (float) newFoodstuff.getCarbs(),
                    (float) newFoodstuff.getCalories());
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }
        });
    }

    /**
     * @param foodstuff foodstuff containing id
     * @param callback
     */
    public void makeFoodstuffUnlisted(final Foodstuff foodstuff, final Runnable callback) {
        databaseThreadExecutor.execute(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();
            foodstuffsDao.updateFoodstuffVisibility(foodstuff.getId(), UNLISTED);
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }
        });
    }

    public void requestListedFoodstuffsFromDb(
            final int batchSize,
            final FoodstuffsBatchReceiveCallback foodstuffsBatchReceiveCallback,
            @Nullable FinishCallback finishCallback) {
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
                mainThreadExecutor.execute(() -> finishCallback.onFinish());
            }
        });
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
     * @param ids - id продуктов, которые мы хотим получить.
     * @param callback - возвращает продукты.
     */
    public void requestFoodstuffsByIds(
            List<Long> ids,
            FoodstuffsBatchReceiveCallback callback) {
        databaseThreadExecutor.execute(() -> {
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
     */
    public void requestFoodstuffsLike(String nameQuery, int limit, FoodstuffsBatchReceiveCallback callback) {
        databaseThreadExecutor.execute(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            FoodstuffsDao foodstuffsDao = database.foodstuffsDao();

            String nameQueryRightFormat = '%' + nameQuery.toLowerCase() + '%';
            List<FoodstuffEntity> foodstuffEntities = foodstuffsDao.loadFoodstuffsLike(nameQueryRightFormat, limit);
            List<Foodstuff> foodstuffs = new ArrayList<>();
            for (FoodstuffEntity entity : foodstuffEntities) {
                foodstuffs.add(toModel(entity));
            }

            mainThreadExecutor.execute(() -> callback.onReceive(foodstuffs));
        });
    }
}
