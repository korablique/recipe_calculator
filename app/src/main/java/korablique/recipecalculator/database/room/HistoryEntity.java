package korablique.recipecalculator.database.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import korablique.recipecalculator.database.FoodstuffsContract;
import korablique.recipecalculator.database.HistoryContract;

import static korablique.recipecalculator.database.HistoryContract.*;

@Entity(tableName = HISTORY_TABLE_NAME,
        foreignKeys = @ForeignKey(entity = FoodstuffEntity.class,
        parentColumns = FoodstuffsContract.ID,
        childColumns = COLUMN_NAME_FOODSTUFF_ID),
        indices = {@Index(COLUMN_NAME_FOODSTUFF_ID)})
public class HistoryEntity {
    @ColumnInfo(name = HistoryContract.ID)
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = COLUMN_NAME_DATE)
    private long date;

    @ColumnInfo(name = COLUMN_NAME_FOODSTUFF_ID)
    private long foodstuffId;

    @ColumnInfo(name = COLUMN_NAME_WEIGHT)
    private float weight;

    /**
     * @param date date in milliseconds
     * @param foodstuffId foodstuff id
     * @param weight foodstuff weight in grams
     */
    public HistoryEntity(long date, long foodstuffId, float weight) {
        this.date = date;
        this.foodstuffId = foodstuffId;
        this.weight = weight;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getFoodstuffId() {
        return foodstuffId;
    }

    public void setFoodstuffId(long foodstuffId) {
        this.foodstuffId = foodstuffId;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
