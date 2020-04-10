package korablique.recipecalculator.database

import korablique.recipecalculator.base.executors.IOExecutor
import korablique.recipecalculator.database.room.DatabaseHolder
import korablique.recipecalculator.database.room.IngredientEntity
import korablique.recipecalculator.database.room.RecipeEntity
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeDatabaseWorker @Inject constructor(
        private val ioExecutor: IOExecutor,
        private val databaseHolder: DatabaseHolder,
        private val databaseWorker: DatabaseWorker) {
    suspend fun getRecipeOfFoodstuff(foodstuff: Foodstuff): Recipe? = withContext(ioExecutor) {
        val db = databaseHolder.database
        val recipeEntity = db.recipeDao().selectRecipe(foodstuff.id)
        if (recipeEntity == null) {
            return@withContext null
        }
        entityToRecipe(recipeEntity)
    }

    private suspend fun entityToRecipe(entity: RecipeEntity): Recipe? {
        val recipeFoodstuffs = databaseWorker.requestFoodstuffsByIds(listOf(entity.foodstuffId))
        if (recipeFoodstuffs.size != 1) {
            // TODO: write to log about the error
            return null
        }
        val recipeFoodstuff = recipeFoodstuffs[0]
        val db = databaseHolder.database

        val ingredientsEntities = db.ingredientDao().selectIngredientsByRecipe(entity.id)
        val ingredientsIds = ingredientsEntities.map { it.ingredientFoodstuffId }
        val ingredientsFoodstuffs = databaseWorker.requestFoodstuffsByIds(ingredientsIds)
        if (ingredientsEntities.size != ingredientsFoodstuffs.size) {
            // TODO: write to log about the error
            return null
        }
        val ingredients = ingredientsEntities.zip(ingredientsFoodstuffs) { entity, foodstuff ->
            Ingredient.from(entity, foodstuff)
        }

        return Recipe.from(entity, recipeFoodstuff, ingredients)
    }

    suspend fun getAllRecipes(): List<Recipe> = withContext(ioExecutor) {
        val db = databaseHolder.database
        val allEntities = db.recipeDao().getAllRecipes()
        val allRecipes = mutableListOf<Recipe>()
        allEntities.forEach {
            val recipe = entityToRecipe(it)
            if (recipe != null) {
                allRecipes.add(recipe)
            }
        }
        allRecipes
    }

    /**
     * @param foodstuff MUST already exist in db, otherwise an SQLite exception will be thrown.
     */
    suspend fun createRecipe(
            foodstuff: Foodstuff,
            ingredients: List<Ingredient>,
            comment: String,
            weight: Float): Recipe = withContext(ioExecutor) {
        if (foodstuff.id == -1L) {
            throw IllegalArgumentException("Not registered in DB foodstuff given: $foodstuff")
        }

        val db = databaseHolder.database
        var recipe: Recipe? = null
        db.runInTransaction {
            val newEntity = RecipeEntity(0, foodstuff.id, weight, comment)
            val id = db.recipeDao().insertRecipe(newEntity)
            val entity = newEntity.copy(id = id)

            val ingredientsEntities = ingredients.map {
                IngredientEntity(0, entity.id, it.weight, it.foodstuff.id, it.comment)
            }

            val ids = db.ingredientDao().insertIngredients(ingredientsEntities)
            val ingredientsWithIds = ingredients.zip(ids) { ingredient, id ->
                ingredient.copy(id = id)
            }

            recipe = Recipe.from(entity, foodstuff, ingredientsWithIds)
        }
        if (recipe == null) {
            throw IllegalStateException(
                    "Couldn't create a recipe with: $foodstuff, $ingredients, $comment, $weight")
        }
        recipe!!
    }
}