package korablique.recipecalculator.database.room

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import korablique.recipecalculator.database.FoodstuffsContract.*
import korablique.recipecalculator.database.HistoryContract.*
import korablique.recipecalculator.database.HistoryContract.ID
import korablique.recipecalculator.database.FoodstuffsContract.ID as FOODSTUFF_ID

@Dao
interface HistoryDao {
    @Insert
    fun insertHistoryEntities(historyEntities: List<HistoryEntity>): List<Long>

    @Query("DELETE FROM $HISTORY_TABLE_NAME WHERE $ID = :historyEntityId")
    fun deleteHistoryEntity(historyEntityId: Long)

    @Query("UPDATE $HISTORY_TABLE_NAME SET $COLUMN_NAME_WEIGHT = :weight WHERE $ID = :historyId")
    fun updateWeight(historyId: Long, weight: Double)

    @Query("UPDATE $HISTORY_TABLE_NAME SET $COLUMN_NAME_FOODSTUFF_ID" +
            " = :foodstuffId WHERE $ID = :historyId")
    fun updateFoodstuff(historyId: Long, foodstuffId: Long)

    @Query("SELECT $COLUMN_NAME_FOODSTUFF_ID FROM $HISTORY_TABLE_NAME" +
            " WHERE $COLUMN_NAME_DATE >= :from AND $COLUMN_NAME_DATE <= :to" +
            " ORDER BY $COLUMN_NAME_DATE DESC")
    fun loadFoodstuffsIdsForPeriod(from: Long, to: Long): List<Long>

    @Query("SELECT $FOODSTUFFS_TABLE_NAME.$FOODSTUFF_ID, $COLUMN_NAME_FOODSTUFF_NAME," +
            " $COLUMN_NAME_PROTEIN, $COLUMN_NAME_FATS, $COLUMN_NAME_CARBS, $COLUMN_NAME_CALORIES" +
            " FROM $HISTORY_TABLE_NAME LEFT OUTER JOIN $FOODSTUFFS_TABLE_NAME" +
            " ON $HISTORY_TABLE_NAME.$COLUMN_NAME_FOODSTUFF_ID" +
            "=$FOODSTUFFS_TABLE_NAME.$FOODSTUFF_ID" +
            " WHERE $COLUMN_NAME_DATE >= :from AND $COLUMN_NAME_DATE <= :to" +
            " AND $COLUMN_NAME_IS_LISTED = 1" +
            " ORDER BY $COLUMN_NAME_DATE DESC")
    fun loadListedFoodstuffsFromHistoryForPeriod(from: Long, to: Long): Cursor

    @Query("SELECT * FROM $HISTORY_TABLE_NAME LEFT OUTER JOIN $FOODSTUFFS_TABLE_NAME" +
            " ON $HISTORY_TABLE_NAME.$COLUMN_NAME_FOODSTUFF_ID" +
            "=$FOODSTUFFS_TABLE_NAME.$FOODSTUFF_ID" +
            " WHERE $COLUMN_NAME_DATE >= :from AND $COLUMN_NAME_DATE <= :to" +
            " ORDER BY $COLUMN_NAME_DATE DESC")
    fun loadHistoryForPeriod(from: Long, to: Long): Cursor

    @Query("SELECT * FROM $HISTORY_TABLE_NAME LEFT OUTER JOIN $FOODSTUFFS_TABLE_NAME" +
            " ON $HISTORY_TABLE_NAME.$COLUMN_NAME_FOODSTUFF_ID" +
            "=$FOODSTUFFS_TABLE_NAME.$FOODSTUFF_ID ORDER BY $COLUMN_NAME_DATE DESC")
    fun loadHistory(): Cursor

    @Query("SELECT * FROM $HISTORY_TABLE_NAME LEFT OUTER JOIN " + FOODSTUFFS_TABLE_NAME +
            " ON $HISTORY_TABLE_NAME.$COLUMN_NAME_FOODSTUFF_ID" +
            "=$FOODSTUFFS_TABLE_NAME.$FOODSTUFF_ID ORDER BY $COLUMN_NAME_DATE DESC LIMIT :limit")
    fun loadHistoryWithLimit(limit: Int): Cursor
}

