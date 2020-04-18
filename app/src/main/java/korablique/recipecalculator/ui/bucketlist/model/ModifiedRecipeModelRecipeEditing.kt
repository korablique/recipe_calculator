package korablique.recipecalculator.ui.bucketlist.model

import korablique.recipecalculator.DishNutritionCalculator
import korablique.recipecalculator.base.Callback
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.database.UpdateRecipeResult
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.model.Recipe
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ModifiedRecipeModelRecipeEditing(
        recipe: Recipe,
        private val recipesRegistry: RecipesRepository,
        private val mainThreadExecutor: MainThreadExecutor
) : ModifiedRecipeModel {
    private var editedRecipe: Recipe
    private val originalRecipe: Recipe

    init {
        editedRecipe = recipe
        originalRecipe = recipe
    }

    override fun addIngredient(ingredient: Ingredient) {
        val updatedIngredients = editedRecipe.ingredients + ingredient
        updateRecipe(ingredients = updatedIngredients)
    }

    override fun removeIngredient(ingredient: Ingredient) {
        val updatedIngredients = editedRecipe.ingredients - ingredient
        updateRecipe(ingredients = updatedIngredients)
    }

    override fun getIngredients(): List<Ingredient> = editedRecipe.ingredients

    override fun setTotalWeight(totalWeight: Float) {
        updateRecipe(totalWeight = totalWeight)
    }

    override fun getTotalWeight(): Float = editedRecipe.weight

    override fun setName(name: String) {
        updateRecipe(name = name)
    }

    override fun getName(): String = editedRecipe.foodstuff.name

    override fun setComment(comment: String) {
        updateRecipe(comment = comment)
    }

    override fun getComment(): String = editedRecipe.comment

    override fun extractEditedRecipe(): Recipe = editedRecipe

    private fun updateRecipe(
            ingredients: List<Ingredient> = getIngredients(),
            totalWeight: Float = getTotalWeight(),
            name: String = getName(),
            comment: String = getComment()) {
        var nutrition = DishNutritionCalculator.calculateIngredients(
                ingredients, totalWeight.toDouble())
        nutrition = normalizeFoodstuffNutrition(nutrition)
        val updatedFoodstuff =
                Foodstuff.withId(editedRecipe.foodstuff.id)
                .withName(name)
                .withNutrition(nutrition)
        val updatedRecipe =
                Recipe(editedRecipe.id,
                        updatedFoodstuff,
                        ingredients,
                        totalWeight,
                        comment)
        editedRecipe = updatedRecipe
    }

    override fun flushChanges(callback: Callback<RecipeModelSaveChangesResult>) {
        GlobalScope.launch(mainThreadExecutor) {
            val result = recipesRegistry.updateRecipe(originalRecipe, editedRecipe)
            when (result) {
                is UpdateRecipeResult.Ok -> {
                    callback.onResult(RecipeModelSaveChangesResult.Ok(result.recipe))
                }
                is UpdateRecipeResult.UpdatedRecipeNotFound -> {
                    callback.onResult(RecipeModelSaveChangesResult.InternalError)
                }
                else -> throw Error("Not handled item: $result")
            }
        }
    }
}