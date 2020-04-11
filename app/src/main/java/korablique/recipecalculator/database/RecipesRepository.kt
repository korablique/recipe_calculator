package korablique.recipecalculator.database

import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Comparator
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class CreateRecipeResult {
    data class Ok(val recipe: Recipe) : CreateRecipeResult()
    object FoodstuffDuplicationError : CreateRecipeResult()
}

/**
 * A convenience class to do all work with recipes.
 * All work with recipes _must_ be done through this class.
 */
@Singleton
class RecipesRepository @Inject constructor(
        private val recipeDatabaseWorker: RecipeDatabaseWorker,
        private val foodstuffsList: FoodstuffsList,
        private val mainThreadExecutor: MainThreadExecutor) {
    private val recipesCache = TreeSet<Recipe>(Comparator { lhs, rhs ->
        lhs.foodstuff.name.compareTo(rhs.foodstuff.name)
    })
    private val recipesFoodstuffsCache = mutableMapOf<Foodstuff, Recipe>()

    private val cacheReadyCallbacks = mutableListOf<()->Unit>()
    private var cacheReady = false

    init {
        GlobalScope.launch(mainThreadExecutor) {
            val recipes = recipeDatabaseWorker.getAllRecipes()
            recipesCache.addAll(recipes)
            recipesCache.forEach { recipesFoodstuffsCache[it.foodstuff] = it }

            cacheReady = true
            cacheReadyCallbacks.forEach { it.invoke() }
            cacheReadyCallbacks.clear()
        }
    }

    suspend fun getRecipeOfFoodstuff(foodstuff: Foodstuff): Recipe? {
        ensureMainThread()
        if (cacheReady) {
            return recipesFoodstuffsCache[foodstuff]
        } else {
            return recipeDatabaseWorker.getRecipeOfFoodstuff(foodstuff)
        }
    }

    /**
     * @return all recipes sorted lexicographically by names
     */
    suspend fun getAllRecipes(): Set<Recipe> = suspendCoroutine { continuation ->
        ensureMainThread()

        val callback = {
            continuation.resume(recipesCache!!)
        }
        if (cacheReady) {
            callback.invoke()
        } else {
            cacheReadyCallbacks.add(callback)
        }
    }

    private fun ensureMainThread() {
        if (!mainThreadExecutor.isCurrentThreadMain()) {
            throw IllegalStateException("Must be called on main thread only")
        }
    }

    /**
     * If given foodstuff is not saved to the DB yet, the repository will save it.
     */
    suspend fun createRecipe(
            foodstuff: Foodstuff,
            ingredients: List<Ingredient>,
            comment: String,
            weight: Float): CreateRecipeResult {
        ensureMainThread()

        val savedFoodstuff = when (foodstuff.hasValidID()) {
            true -> foodstuff
            false -> {
                val saveResult = foodstuffsList.saveFoodstuffKx(foodstuff)
                when (saveResult) {
                    is SaveFoodstuffResult.DuplicationFailure -> {
                        return CreateRecipeResult.FoodstuffDuplicationError
                    }
                    is SaveFoodstuffResult.Ok -> saveResult.foodstuff
                }
            }
        }

        val newRecipe = recipeDatabaseWorker.createRecipe(savedFoodstuff, ingredients, comment, weight)
        recipesCache.add(newRecipe)
        recipesFoodstuffsCache[newRecipe.foodstuff] = newRecipe
        return CreateRecipeResult.Ok(newRecipe)
    }
}
