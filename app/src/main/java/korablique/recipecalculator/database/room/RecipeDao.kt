package korablique.recipecalculator.database.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import korablique.recipecalculator.database.RecipeContract.COLUMN_NAME_FOODSTUFF_ID
import korablique.recipecalculator.database.RecipeContract.RECIPE_TABLE_NAME
import korablique.recipecalculator.model.Recipe


@Dao
interface RecipeDao {
    @Insert
    fun insertRecipe(recipe: RecipeEntity): Long

    @Query("SELECT * FROM $RECIPE_TABLE_NAME WHERE $COLUMN_NAME_FOODSTUFF_ID = :id LIMIT 1")
    fun selectRecipe(id: Long): RecipeEntity?

    @Query("SELECT * FROM $RECIPE_TABLE_NAME")
    fun getAllRecipes(): List<RecipeEntity>

    @Update
    fun updateRecipe(recipe: RecipeEntity): Int
}

