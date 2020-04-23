package korablique.recipecalculator.ui.bucketlist.model

import korablique.recipecalculator.base.Callback
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.database.CreateRecipeResult
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.ui.bucketlist.BucketList
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ModifiedRecipeModelBucketList(
        private val bucketList: BucketList,
        private val recipesRepository: RecipesRepository,
        private val mainThreadExecutor: MainThreadExecutor) : ModifiedRecipeModel {
    override fun addIngredient(ingredient: Ingredient) = bucketList.add(ingredient)
    override fun removeIngredient(ingredient: Ingredient) = bucketList.remove(ingredient)
    override fun getIngredients(): List<Ingredient> = bucketList.getList()
    override fun setTotalWeight(totalWeight: Float) = bucketList.setTotalWeight(totalWeight)
    override fun getTotalWeight(): Float = bucketList.getTotalWeight()
    override fun setName(name: String) = bucketList.setName(name)
    override fun getName(): String = bucketList.getName()
    override fun setComment(comment: String) = bucketList.setComment(comment)
    override fun getComment(): String = bucketList.getComment()
    override fun extractEditedRecipe() = bucketList.getRecipe()

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