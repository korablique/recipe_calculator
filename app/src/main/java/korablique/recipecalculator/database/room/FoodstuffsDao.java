package korablique.recipecalculator.database.room;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import korablique.recipecalculator.database.FoodstuffsContract;

import java.util.List;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME_NOCASE;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.ID;

@Dao
public interface FoodstuffsDao {
    /**
     * @param foodstuff saving foodstuff
     * @return saved foodstuff's id. If the same foodstuff is already exists,
     * method aborts with returning -1
     */
    @Insert
    long insertFoodstuff(FoodstuffEntity foodstuff);

    @Insert
    List<Long> insertFoodstuffs(List<FoodstuffEntity> foodstuffs);

    @Query("UPDATE " + FOODSTUFFS_TABLE_NAME + " SET " +
            COLUMN_NAME_FOODSTUFF_NAME + "=:name, " +
            COLUMN_NAME_FOODSTUFF_NAME_NOCASE + "=:nameNoCase, " +
            COLUMN_NAME_PROTEIN + "=:protein, " +
            COLUMN_NAME_FATS + "=:fats, " +
            COLUMN_NAME_CARBS + "=:carbs, " +
            COLUMN_NAME_CALORIES + "=:calories " +
            "WHERE " + ID + "=:foodstuffId")
    void updateFoodstuff(long foodstuffId, String name, String nameNoCase,
                         float protein, float fats, float carbs, float calories);

    @Query("UPDATE " + FOODSTUFFS_TABLE_NAME + " SET " +
            COLUMN_NAME_IS_LISTED + "=:isListed " +
            "WHERE " + ID + "=:foodstuffId")
    void updateFoodstuffVisibility(long foodstuffId, int isListed);

    @Query("DELETE FROM " + FOODSTUFFS_TABLE_NAME + " WHERE " + ID + "=:foodstuffId")
    void deleteFoodstuffById(long foodstuffId);

    @Query("SELECT * FROM " + FOODSTUFFS_TABLE_NAME +
            " WHERE " + COLUMN_NAME_IS_LISTED + "=1" +
            " ORDER BY " + COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " ASC")
    Cursor loadListedFoodstuffs();

    @Query("SELECT * FROM " + FOODSTUFFS_TABLE_NAME +
            " WHERE " + ID + " IN (:ids)")
    List<FoodstuffEntity> loadFoodstuffsByIds(List<Long> ids);

    @Query("SELECT * FROM " + FOODSTUFFS_TABLE_NAME +
            " WHERE " + COLUMN_NAME_FOODSTUFF_NAME_NOCASE +
            " LIKE :like " +
            " ORDER BY " + COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " ASC" +
            " LIMIT :limit")
    List<FoodstuffEntity> loadFoodstuffsLike(String like, int limit);
}
