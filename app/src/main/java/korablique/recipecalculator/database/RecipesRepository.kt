package korablique.recipecalculator.database

import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import korablique.recipecalculator.TestEnvironmentDetector
import korablique.recipecalculator.base.Optional
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
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

sealed class UpdateRecipeResult {
    data class Ok(val recipe: Recipe) : UpdateRecipeResult()
    object UpdatedRecipeNotFound : UpdateRecipeResult()
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
    private val recipesIdsCache = mutableMapOf<Long, Recipe>()

    private val cacheReadyCallbacks = mutableListOf<()->Unit>()
    private var cacheReady = false

    init {
        GlobalScope.launch(mainThreadExecutor) {
            val recipes = recipeDatabaseWorker.getAllRecipes()
            recipesCache.addAll(recipes)
            recipesCache.forEach { recipesFoodstuffsCache[it.foodstuff] = it }
            recipesCache.forEach { recipesIdsCache[it.id] = it }

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

    fun getRecipeOfFoodstuffRx(foodstuff: Foodstuff): Single<Optional<Recipe>> {
        val subject: SingleSubject<Optional<Recipe>> = SingleSubject.create()
        GlobalScope.launch(mainThreadExecutor) {
            val result = getRecipeOfFoodstuff(foodstuff)
            subject.onSuccess(Optional.ofNullable(result))
        }
        return subject.observeOn(mainThreadExecutor.asScheduler())
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

    fun getAllRecipesRx(): Single<Set<Recipe>> {
        val subject: SingleSubject<Set<Recipe>> = SingleSubject.create()
        GlobalScope.launch(mainThreadExecutor) {
            val result = getAllRecipes()
            subject.onSuccess(result)
        }
        return subject.observeOn(mainThreadExecutor.asScheduler())
    }

    private fun ensureMainThread() {
        if (TestEnvironmentDetector.isInTests()) {
            return;
        }
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
        recipesIdsCache[newRecipe.id] = newRecipe
        return CreateRecipeResult.Ok(newRecipe)
    }

    suspend fun saveRecipe(recipe: Recipe): CreateRecipeResult {
        return createRecipe(recipe.foodstuff, recipe.ingredients, recipe.comment, recipe.weight)
    }

    fun saveRecipeRx(recipe: Recipe): Single<CreateRecipeResult> {
        val subject: SingleSubject<CreateRecipeResult> = SingleSubject.create()
        GlobalScope.launch(mainThreadExecutor) {
            val result = saveRecipe(recipe)
            subject.onSuccess(result)
        }
        return subject.observeOn(mainThreadExecutor.asScheduler())
    }

    /**
     * Provided original recipe must be obtained from RecipesRepository.
     */
    suspend fun updateRecipe(originalRecipe: Recipe, updatedRecipe: Recipe): UpdateRecipeResult {
        if (originalRecipe.id != updatedRecipe.id) {
            throw IllegalArgumentException(
                    "Cannot updated recipe when ID is changed. "
                            + "Original: $originalRecipe, updated: $updatedRecipe")
        }
        if (recipesFoodstuffsCache[originalRecipe.foodstuff] == null) {
            throw IllegalArgumentException("Foodstuff of original recipe is not known. "
                    + "Was recipe taken from RecipesRepository? "
                    + "Original: $originalRecipe, updated: $updatedRecipe")
        }
        if (recipesIdsCache[originalRecipe.id] == null) {
            throw IllegalArgumentException("Original recipe is not known. "
                    + "Was recipe taken from RecipesRepository? "
                    + "Original: $originalRecipe, updated: $updatedRecipe")
        }

        foodstuffsList.editFoodstuffKx(originalRecipe.foodstuff.id, updatedRecipe.foodstuff)
        val updatedRecipe = recipeDatabaseWorker.updateRecipe(updatedRecipe)
        if (updatedRecipe == null) {
            return UpdateRecipeResult.UpdatedRecipeNotFound
        }
        if (updatedRecipe.id != originalRecipe.id) {
            throw IllegalStateException("Unexpected ID change: $updatedRecipe, $originalRecipe")
        }

        recipesCache.remove(originalRecipe)
        recipesCache.add(updatedRecipe)
        recipesFoodstuffsCache.remove(originalRecipe.foodstuff)
        recipesFoodstuffsCache[updatedRecipe.foodstuff] = updatedRecipe
        recipesIdsCache[updatedRecipe.id] = updatedRecipe
        return UpdateRecipeResult.Ok(updatedRecipe)
    }
}
