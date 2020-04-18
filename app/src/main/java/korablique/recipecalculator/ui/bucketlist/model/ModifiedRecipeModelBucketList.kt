package korablique.recipecalculator.ui.bucketlist.model

import korablique.recipecalculator.DishNutritionCalculator
import korablique.recipecalculator.base.Callback
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.database.CreateRecipeResult
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.bucketlist.BucketList
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class ModifiedRecipeModelBucketList(
        private val bucketList: BucketList,
        private val recipesRepository: RecipesRepository,
        private val mainThreadExecutor: MainThreadExecutor) : ModifiedRecipeModel {
    override fun addIngredient(ingredient: Ingredient) = bucketList.add(ingredient)
    override fun removeIngredient(ingredient: Ingredient) = bucketList.remove(ingredient)
    override fun getIngredients(): List<Ingredient> = bucketList.list
    override fun setTotalWeight(totalWeight: Float) = bucketList.setTotalWeight(totalWeight)
    override fun getTotalWeight(): Float = bucketList.totalWeight
    override fun setName(name: String) = bucketList.setName(name)
    override fun getName(): String = bucketList.name
    override fun setComment(comment: String) = bucketList.setComment(comment)
    override fun getComment(): String = bucketList.comment

    override fun extractEditedRecipe(): Recipe {
        var nutrition = DishNutritionCalculator.calculateIngredients(
                bucketList.list, bucketList.totalWeight.toDouble())
        nutrition = normalizeFoodstuffNutrition(nutrition)
        val name = bucketList.name
        return Recipe.create(
                Foodstuff.withName(name).withNutrition(nutrition),
                bucketList.list,
                bucketList.totalWeight,
                bucketList.comment)
    }

    override fun flushChanges(callback: Callback<RecipeModelSaveChangesResult>) {
        GlobalScope.launch(mainThreadExecutor) {
            val result = recipesRepository.saveRecipe(extractEditedRecipe())
            when (result) {
                is CreateRecipeResult.Ok -> {
                    bucketList.clear()
                    callback.onResult(RecipeModelSaveChangesResult.Ok(result.recipe))
                }
                is CreateRecipeResult.FoodstuffDuplicationError -> {
                    callback.onResult(RecipeModelSaveChangesResult.FoodstuffDuplicationError)
                }
            }
        }
    }
}