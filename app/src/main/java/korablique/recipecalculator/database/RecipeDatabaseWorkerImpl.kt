package korablique.recipecalculator.database

import korablique.recipecalculator.base.executors.IOExecutor
import korablique.recipecalculator.database.room.AppDatabase
import korablique.recipecalculator.database.room.DatabaseHolder
import korablique.recipecalculator.database.room.IngredientEntity
import korablique.recipecalculator.database.room.RecipeEntity
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.RuntimeException
import javax.inject.Inject
import javax.inject.Singleton

class RecipeDatabaseWorkerImpl constructor(
        private val ioExecutor: IOExecutor,
        private val databaseHolder: DatabaseHolder,
        private val databaseWorker: DatabaseWorker) : RecipeDatabaseWorker {
    override suspend fun getRecipeOfFoodstuff(foodstuff: Foodstuff): Recipe? = withContext(ioExecutor) {
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

    override suspend fun getAllRecipes(): List<Recipe> = withContext(ioExecutor) {
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
    override suspend fun createRecipe(
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
            recipe = createOrUpdateRecipeWithIngredients(db, newEntity, foodstuff, ingredients)
        }
        if (recipe == null) {
            throw IllegalStateException(
                    "Couldn't create a recipe with: $foodstuff, $ingredients, $comment, $weight")
        }
        recipe!!
    }

    private fun createOrUpdateRecipeWithIngredients(
            db: AppDatabase,
            inputEntity: RecipeEntity,
            foodstuff: Foodstuff,
            ingredients: List<Ingredient>): Recipe {
        if (!db.inTransaction()) {
            throw Error("Must be called within transaction because of multiple DB updates")
        }
        val entity = when (inputEntity.id) {
            0L -> {
                val id = db.recipeDao().insertRecipe(inputEntity)
                inputEntity.copy(id = id)
            }
            else -> {
                // Update the recipe
                val updatedRowsCount = db.recipeDao().updateRecipe(inputEntity)
                if (updatedRowsCount <= 0) {
                    throw RuntimeException("Couldn't update given entity: $inputEntity")
                }
                // Delete its old ingredients
                db.ingredientDao().deleteIngredientsByRecipe(inputEntity.id)
                inputEntity
            }
        }

        val ingredientsEntities = ingredients.map {
            IngredientEntity(0, entity.id, it.weight, it.foodstuff.id, it.comment)
        }
        val ids = db.ingredientDao().insertIngredients(ingredientsEntities)
        val ingredientsWithIds = ingredients.zip(ids) { ingredient, id ->
            ingredient.copy(id = id)
        }
        return Recipe.from(entity, foodstuff, ingredientsWithIds)
    }

    override suspend fun updateRecipe(updatedRecipe: Recipe): Recipe = withContext(ioExecutor) {
        val db = databaseHolder.database
        var result: Recipe? = null
        db.runInTransaction {
            val entity = RecipeEntity(
                    updatedRecipe.id,
                    updatedRecipe.foodstuff.id,
                    updatedRecipe.weight,
                    updatedRecipe.comment)
            result = createOrUpdateRecipeWithIngredients(
                    db, entity, updatedRecipe.foodstuff, updatedRecipe.ingredients)
        }
        if (result == null) {
            throw IllegalStateException(
                    "Couldn't update recipe: $updatedRecipe")
        }
        result!!
    }
}