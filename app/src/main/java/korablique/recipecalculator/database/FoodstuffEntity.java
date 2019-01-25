package korablique.recipecalculator.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import static korablique.recipecalculator.database.FoodstuffsContract.*;

@Entity(tableName = FOODSTUFFS_TABLE_NAME)
public class FoodstuffEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FoodstuffsContract.ID)
    private long id;

    @ColumnInfo(name = COLUMN_NAME_FOODSTUFF_NAME)
    private String name;

    @ColumnInfo(name = COLUMN_NAME_FOODSTUFF_NAME_NOCASE)
    private String nameNoCase;

    @ColumnInfo(name = COLUMN_NAME_PROTEIN)
    private float protein;

    @ColumnInfo(name = COLUMN_NAME_FATS)
    private float fats;

    @ColumnInfo(name = COLUMN_NAME_CARBS)
    private float carbs;

    @ColumnInfo(name = COLUMN_NAME_CALORIES)
    private float calories;

    @ColumnInfo(name = COLUMN_NAME_IS_LISTED) // TODO: 10.01.19  DEFAULT 1 NOT NULL
    private int isListed;

    public FoodstuffEntity(
            String name,
            String nameNoCase,
            float protein,
            float fats,
            float carbs,
            float calories,
            int isListed) {
        this.name = name;
        this.nameNoCase = nameNoCase;
        this.protein = protein;
        this.fats = fats;
        this.carbs = carbs;
        this.calories = calories;
        this.isListed = isListed;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getNameNoCase() {
        return nameNoCase;
    }
    public void setNameNoCase(String nameNoCase) {
        this.nameNoCase = nameNoCase;
    }

    public float getProtein() {
        return protein;
    }

    public float getFats() {
        return fats;
    }

    public float getCarbs() {
        return carbs;
    }

    public float getCalories() {
        return calories;
    }

    public int getIsListed() {
        return isListed;
    }

//    FoodstuffsContract.ID + " INTEGER PRIMARY KEY, " +
//    COLUMN_NAME_FOODSTUFF_NAME + " TEXT, " +
//    COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " TEXT, " +
//    COLUMN_NAME_PROTEIN + " REAL, " +
//    COLUMN_NAME_FATS + " REAL, " +
//    COLUMN_NAME_CARBS + " REAL, " +
//    COLUMN_NAME_CALORIES + " REAL, " +
//    COLUMN_NAME_IS_LISTED + " INTEGER DEFAULT 1 NOT NULL)");
}
