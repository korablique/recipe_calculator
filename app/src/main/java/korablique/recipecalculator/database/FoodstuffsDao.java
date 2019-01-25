package korablique.recipecalculator.database;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME_NOCASE;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.ID;

@Dao
interface FoodstuffsDao {
    /**
     * @param foodstuff saving foodstuff
     * @return saved foodstuff's id. If the same foodstuff is already exists,
     * method aborts with returning -1
     */
    @Insert
    long insertFoodstuff(FoodstuffEntity foodstuff);

    @Insert
    List<Long> insertFoodstuffs(List<FoodstuffEntity> foodstuffs);

    @Update
    void updateFoodstuffs(FoodstuffEntity... foodstuffs);

    @Delete
    void deleteFoodstuff(FoodstuffEntity foodstuff);

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
