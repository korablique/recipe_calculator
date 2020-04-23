package korablique.recipecalculator.ui.bucketlist.model

import korablique.recipecalculator.base.Callback
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.model.Recipe

sealed class RecipeModelSaveChangesResult {
    data class Ok(val recipe: Recipe) : RecipeModelSaveChangesResult()
    object FoodstuffDuplicationError : RecipeModelSaveChangesResult()
    object InternalError : RecipeModelSaveChangesResult()
}

interface ModifiedRecipeModel {
    fun addIngredient(ingredient: Ingredient)
    fun removeIngredient(ingredient: Ingredient)
    fun getIngredients(): List<Ingredient>
    fun setTotalWeight(totalWeight: Float)
    fun getTotalWeight(): Float
    fun setName(name: String)
    fun getName(): String
    fun getComment(): String
    fun setComment(comment: String)
    fun extractEditedRecipe(): Recipe
    fun flushChanges(callback: Callback<RecipeModelSaveChangesResult>)
    /**
     * When sum of protein, fats and carbs is greater than 100, then we should not create
     * a foodstuff with such nutrition, and must normalize the nutrition before foodstuff creation.
     */
    fun normalizeFoodstuffNutrition(nutrition: Nutrition): Nutrition {
        val gramsSum = nutrition.protein + nutrition.fats + nutrition.carbs
        if (gramsSum <= 100f) {
            return nutrition
        }
        val factor = 100f / gramsSum
        return Nutrition.withValues(
                nutrition.protein * factor,
                nutrition.fats * factor,
                nutrition.carbs * factor,
                nutrition.calories * factor)
    }
}
