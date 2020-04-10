package korablique.recipecalculator.database.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import korablique.recipecalculator.database.IngredientContract.COLUMN_NAME_RECIPE_ID
import korablique.recipecalculator.database.IngredientContract.INGREDIENT_TABLE_NAME

@Dao
interface IngredientDao {
    @Insert
    fun insertIngredients(ingredients: List<IngredientEntity>): List<Long>

    @Query("SELECT * FROM $INGREDIENT_TABLE_NAME WHERE $COLUMN_NAME_RECIPE_ID = :recipeId")
    fun selectIngredientsByRecipe(recipeId: Long): List<IngredientEntity>
}
