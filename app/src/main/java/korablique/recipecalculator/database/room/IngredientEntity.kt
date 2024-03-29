package korablique.recipecalculator.database.room

import androidx.room.*
import korablique.recipecalculator.database.FoodstuffsContract
import korablique.recipecalculator.database.IngredientContract
import korablique.recipecalculator.database.IngredientContract.COLUMN_INDEX
import korablique.recipecalculator.database.IngredientContract.COLUMN_NAME_COMMENT
import korablique.recipecalculator.database.IngredientContract.COLUMN_NAME_INGREDIENT_FOODSTUFF_ID
import korablique.recipecalculator.database.IngredientContract.COLUMN_NAME_INGREDIENT_WEIGHT
import korablique.recipecalculator.database.IngredientContract.COLUMN_NAME_RECIPE_ID
import korablique.recipecalculator.database.IngredientContract.INGREDIENT_TABLE_NAME
import korablique.recipecalculator.database.RecipeContract

@Entity(tableName = INGREDIENT_TABLE_NAME,
        foreignKeys = [
            ForeignKey(
                    entity = RecipeEntity::class,
                    parentColumns = [RecipeContract.ID],
                    childColumns = [COLUMN_NAME_RECIPE_ID]),
            ForeignKey(
                    entity = FoodstuffEntity::class,
                    parentColumns = [FoodstuffsContract.ID],
                    childColumns = [COLUMN_NAME_INGREDIENT_FOODSTUFF_ID])],
        indices = [Index(COLUMN_NAME_RECIPE_ID), Index(COLUMN_NAME_INGREDIENT_FOODSTUFF_ID)]
        )
class IngredientEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = IngredientContract.ID)
    val id: Long

    @ColumnInfo(name = COLUMN_NAME_RECIPE_ID)
    val recipeId: Long

    @ColumnInfo(name = COLUMN_NAME_INGREDIENT_WEIGHT)
    val ingredientWeight: Float

    @ColumnInfo(name = COLUMN_NAME_INGREDIENT_FOODSTUFF_ID)
    val ingredientFoodstuffId: Long

    @ColumnInfo(name = COLUMN_NAME_COMMENT)
    val comment: String

    @ColumnInfo(name = COLUMN_INDEX)
    val index: Int

    constructor(id: Long, recipeId: Long, ingredientWeight: Float, ingredientFoodstuffId: Long,
                comment: String, index: Int) {
        this.id = id
        this.recipeId = recipeId
        this.ingredientWeight = ingredientWeight
        this.ingredientFoodstuffId = ingredientFoodstuffId
        this.comment = comment
        this.index = index
    }
}

