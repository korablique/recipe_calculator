package korablique.recipecalculator.database.room

import androidx.room.*
import korablique.recipecalculator.database.FoodstuffsContract
import korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME
import korablique.recipecalculator.database.RecipeContract
import korablique.recipecalculator.database.RecipeContract.COLUMN_NAME_FOODSTUFF_ID
import korablique.recipecalculator.database.RecipeContract.COLUMN_NAME_INGREDIENTS_TOTAL_WEIGHT
import korablique.recipecalculator.database.RecipeContract.RECIPE_TABLE_NAME

@Entity(tableName = RECIPE_TABLE_NAME,
        foreignKeys = [ForeignKey(
                entity = FoodstuffEntity::class,
                parentColumns = [FoodstuffsContract.ID],
                childColumns = [COLUMN_NAME_FOODSTUFF_ID])])
class RecipeEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = RecipeContract.ID)
    val id: Long

    @ColumnInfo(name = COLUMN_NAME_FOODSTUFF_ID)
    val foodstuffId: Long

    @ColumnInfo(name = COLUMN_NAME_INGREDIENTS_TOTAL_WEIGHT)
    val ingredientsTotalWeight: Float

    constructor(id: Long, foodstuffId: Long, ingredientsTotalWeight: Float) {
        this.id = id
        this.foodstuffId = foodstuffId
        this.ingredientsTotalWeight = ingredientsTotalWeight
    }
}

