package korablique.recipecalculator.database

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import korablique.recipecalculator.InstantMainThreadExecutor
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.IllegalStateException
import kotlin.collections.HashSet
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RecipeRepositoryTest {
    val fakeDatabaseWorker = FakeRecipeDatabaseWorker()
    val instantExecutor = InstantMainThreadExecutor()

    val foodstuffsSavedToDB = mutableListOf<Foodstuff>()
    val foodstuffsList = mock<FoodstuffsList> {
        on { saveFoodstuff(any(), any()) } doAnswer {
            val foodstuff = it.arguments[0] as Foodstuff
            val callback = it.arguments[1] as FoodstuffsList.SaveFoodstuffCallback

            foodstuffsSavedToDB.add(foodstuff.recreateWithId(randID()))
            callback.onResult(foodstuffsSavedToDB.last())
            Unit
        }
    }
    val recipesRepository = RecipesRepository(fakeDatabaseWorker, foodstuffsList, instantExecutor)

    @Test
    fun `getting all recipes when cache is ready`() {
        val initialRecipes = createRecipes("1", "2", "3")
        fakeDatabaseWorker.setInitialRecipes(initialRecipes)

        var recipes: Set<Recipe>? = null
        GlobalScope.async(instantExecutor) {
            recipes = recipesRepository.getAllRecipes()
        }
        assertEquals(HashSet(initialRecipes), recipes)
    }

    @Test
    fun `getting all recipes when cache is not ready`() {
        var recipes: Set<Recipe>? = null
        GlobalScope.async(instantExecutor) {
            recipes = recipesRepository.getAllRecipes()
        }
        assertNull(recipes)

        val initialRecipes = createRecipes("1", "2", "3")
        fakeDatabaseWorker.setInitialRecipes(initialRecipes)

        assertEquals(HashSet(initialRecipes), recipes)
    }

    @Test
    fun `getting all recipes when new recipe was added after cache became ready`() {
        val initialRecipes = createRecipes("1", "2", "3")
        fakeDatabaseWorker.setInitialRecipes(initialRecipes)

        runBlocking {
            recipesRepository.createRecipe(
                    Foodstuff.withId(randID()).withName("new").withNutrition(1f, 2f, 3f, 4f),
                    emptyList(),
                    "comment",
                    123f)
        }

        val recipes = runBlocking { recipesRepository.getAllRecipes() }
        assertNotNull(recipes.find { it.foodstuff.name == "new" })
    }

    @Test
    fun `getting recipe of foodstuff when cache is ready`() {
        val foodstuff = Foodstuff.withId(randID()).withName("name").withNutrition(1f, 2f, 3f, 4f)
        fakeDatabaseWorker.setInitialRecipes(createRecipes(foodstuff))

        var recipe: Recipe? = null
        GlobalScope.async(instantExecutor) {
            recipe = recipesRepository.getRecipeOfFoodstuff(foodstuff)
        }

        assertEquals(foodstuff, recipe!!.foodstuff)
    }

    @Test
    fun `getting recipe of foodstuff when cache is not ready`() {
        val foodstuff = Foodstuff.withId(randID()).withName("name").withNutrition(1f, 2f, 3f, 4f)

        var recipe: Recipe? = null
        GlobalScope.async(instantExecutor) {
            recipe = recipesRepository.getRecipeOfFoodstuff(foodstuff)
        }

        assertNull(recipe)

        fakeDatabaseWorker.setInitialRecipes(createRecipes(foodstuff))
        assertEquals(foodstuff, recipe!!.foodstuff)
    }

    @Test
    fun `getting recipe of foodstuff which has no recipe`() {
        fakeDatabaseWorker.setInitialRecipes(emptyList())

        val foodstuff = Foodstuff.withId(randID()).withName("name").withNutrition(1f, 2f, 3f, 4f)
        val recipe = runBlocking { recipesRepository.getRecipeOfFoodstuff(foodstuff) }
        assertNull(recipe)
    }

    @Test
    fun `getting recipe of foodstuff when new recipe was added after cache became ready`() {
        fakeDatabaseWorker.setInitialRecipes(emptyList())

        val foodstuff = Foodstuff.withId(randID()).withName("name").withNutrition(1f, 2f, 3f, 4f)
        var recipe = runBlocking { recipesRepository.getRecipeOfFoodstuff(foodstuff) }
        assertNull(recipe)

        runBlocking { recipesRepository.createRecipe(foodstuff, emptyList(), "", 123f) }
        recipe = runBlocking { recipesRepository.getRecipeOfFoodstuff(foodstuff) }
        assertEquals(foodstuff, recipe!!.foodstuff)
    }

    @Test
    fun `recipes are in lexicographical order 1`() {
        fakeDatabaseWorker.setInitialRecipes(createRecipes("a name", "the name"))

        val recipes = runBlocking { recipesRepository.getAllRecipes() }.toList()
        assertEquals("a name", recipes[0].foodstuff.name)
        assertEquals("the name", recipes[1].foodstuff.name)
    }

    @Test
    fun `recipes are in lexicographical order 2`() {
        fakeDatabaseWorker.setInitialRecipes(createRecipes("the name", "a name"))

        val recipes = runBlocking { recipesRepository.getAllRecipes() }.toList()
        assertEquals("a name", recipes[0].foodstuff.name)
        assertEquals("the name", recipes[1].foodstuff.name)
    }

    @Test
    fun `create recipe when cache is ready`() {
        fakeDatabaseWorker.setInitialRecipes(createRecipes("1", "2", "3"))

        assertEquals(0, fakeDatabaseWorker.createRecipesCallsCount)

        var createdRecipe: Recipe? = null
        GlobalScope.async(instantExecutor) {
            val createResult = recipesRepository.createRecipe(
                    Foodstuff.withId(randID()).withName("name").withNutrition(1f, 2f, 3f, 4f),
                    emptyList(),
                    "comment",
                    123f) as CreateRecipeResult.Ok
            createdRecipe = createResult.recipe
        }
        assertEquals(1, fakeDatabaseWorker.createRecipesCallsCount)

        assertEquals("name", createdRecipe!!.foodstuff.name)
        val foundRecipe = runBlocking {
            recipesRepository.getAllRecipes().find { it.foodstuff.name == "name" }
        }
        assertNotNull(foundRecipe)
        assertEquals(createdRecipe, foundRecipe)
    }

    @Test
    fun `create recipe when cache is not ready`() {
        assertEquals(0, fakeDatabaseWorker.createRecipesCallsCount)
        var createdRecipe: Recipe? = null
        GlobalScope.async(instantExecutor) {
            val createResult = recipesRepository.createRecipe(
                    Foodstuff.withId(randID()).withName("name").withNutrition(1f, 2f, 3f, 4f),
                    emptyList(),
                    "comment",
                    123f) as CreateRecipeResult.Ok
            createdRecipe = createResult.recipe
        }
        assertEquals(1, fakeDatabaseWorker.createRecipesCallsCount)
        assertNotNull(createdRecipe)

        fakeDatabaseWorker.setInitialRecipes(createRecipes("1", "2", "3"))
        val foundRecipe = runBlocking {
            recipesRepository.getAllRecipes().find { it.foodstuff.name == "name" }
        }
        assertNotNull(foundRecipe)
        assertEquals(createdRecipe, foundRecipe)
    }

    @Test
    fun `does not save foodstuff to DB when it is already saved`() {
        fakeDatabaseWorker.setInitialRecipes(createRecipes("1", "2", "3"))

        val foodstuff = Foodstuff.withId(randID()).withName("name").withNutrition(1f, 2f, 3f, 4f)
        runBlocking {
            recipesRepository.createRecipe(foodstuff, emptyList(), "comment", 123f)
        }
        assertEquals(0, foodstuffsSavedToDB.size)
    }

    @Test
    fun `saves foodstuff to DB when it is not saved yet`() {
        fakeDatabaseWorker.setInitialRecipes(createRecipes("1", "2", "3"))

        val foodstuff = Foodstuff.withName("name").withNutrition(1f, 2f, 3f, 4f)
        val result = runBlocking {
            recipesRepository.createRecipe(foodstuff, emptyList(), "comment", 123f) as CreateRecipeResult.Ok
        }
        val recipe = result.recipe

        assertEquals(1, foodstuffsSavedToDB.size)
        assertTrue(recipe.foodstuff.hasValidID())
        assertFalse(foodstuff.hasValidID())
        assertEquals(foodstuff.recreateWithId(recipe.foodstuff.id), recipe.foodstuff)
    }

    @Test
    fun `does not return until foodstuff is saved to DB`() {
        fakeDatabaseWorker.setInitialRecipes(createRecipes("1", "2", "3"))

        val requests = mutableListOf<FoodstuffsList.SaveFoodstuffCallback>()
        whenever(foodstuffsList.saveFoodstuff(any(), any())).doAnswer {
            requests.add(it.arguments[1] as FoodstuffsList.SaveFoodstuffCallback)
            Unit
        }

        var createdRecipe: Recipe? = null
        val foodstuff = Foodstuff.withName("name").withNutrition(1f, 2f, 3f, 4f)
        GlobalScope.async(instantExecutor) {
            val result = recipesRepository.createRecipe(foodstuff, emptyList(), "comment", 123f)
            createdRecipe = (result as CreateRecipeResult.Ok).recipe
        }

        assertNull(createdRecipe)
        requests.forEach { it.onResult(foodstuff.recreateWithId(randID())) }
        assertNotNull(createdRecipe)
    }

    @Test
    fun `update recipe when cache is ready`() {
        val initialRecipes = createRecipes("1", "2", "3")
        fakeDatabaseWorker.setInitialRecipes(initialRecipes)

        val updatedRecipe = initialRecipes[0].copy(comment = "new comment")
        var returnedUpdatedRecipe: Recipe? = null
        GlobalScope.async(instantExecutor) {
            val result = recipesRepository.updateRecipe(initialRecipes[0], updatedRecipe)
            assertTrue(result is UpdateRecipeResult.Ok)
            returnedUpdatedRecipe = (result as UpdateRecipeResult.Ok).recipe
        }

        GlobalScope.launch(instantExecutor) {
            assertTrue(updatedRecipe in recipesRepository.getAllRecipes())
            assertTrue(returnedUpdatedRecipe in recipesRepository.getAllRecipes())
            assertEquals(updatedRecipe, recipesRepository.getRecipeOfFoodstuff(updatedRecipe.foodstuff))
            assertEquals(updatedRecipe, returnedUpdatedRecipe)
        }
    }

    @Test
    fun `update recipe when cache is not ready`() {
        val initialRecipes = createRecipes("1", "2", "3")
        val updatedRecipe = initialRecipes[0].copy(comment = "new comment")
        var returnedUpdatedRecipe: Recipe? = null
        GlobalScope.async(instantExecutor) {
            val result = recipesRepository.updateRecipe(initialRecipes[0], updatedRecipe)
            assertTrue(result is UpdateRecipeResult.Ok)
            returnedUpdatedRecipe = (result as UpdateRecipeResult.Ok).recipe
        }

        fakeDatabaseWorker.setInitialRecipes(initialRecipes)
        GlobalScope.launch(instantExecutor) {
            assertTrue(updatedRecipe in recipesRepository.getAllRecipes())
            assertTrue(returnedUpdatedRecipe in recipesRepository.getAllRecipes())
            assertEquals(updatedRecipe, recipesRepository.getRecipeOfFoodstuff(updatedRecipe.foodstuff))
            assertEquals(updatedRecipe, returnedUpdatedRecipe)
        }
    }

    @Test
    fun `update not existing recipe when cache is ready`() {
        val initialRecipes = createRecipes("1", "2", "3")
        fakeDatabaseWorker.setInitialRecipes(initialRecipes)
        val updatedRecipe = createRecipes("4")[0]
        GlobalScope.async(instantExecutor) {
            val result = recipesRepository.updateRecipe(initialRecipes[0], updatedRecipe)
            assertTrue(result is UpdateRecipeResult.UpdatedRecipeNotFound)
        }

        GlobalScope.launch(instantExecutor) {
            assertFalse(updatedRecipe in recipesRepository.getAllRecipes())
            assertNull(recipesRepository.getRecipeOfFoodstuff(updatedRecipe.foodstuff))
        }
    }

    @Test
    fun `update not existing recipe when cache is not ready`() {
        val initialRecipes = createRecipes("1", "2", "3")
        fakeDatabaseWorker.shouldUpdateRecipesBeforeInitialRecipesSet = false
        val updatedRecipe = createRecipes("4")[0]
        GlobalScope.async(instantExecutor) {
            val result = recipesRepository.updateRecipe(initialRecipes[0], updatedRecipe)
            assertTrue(result is UpdateRecipeResult.UpdatedRecipeNotFound)
        }

        fakeDatabaseWorker.setInitialRecipes(initialRecipes)

        GlobalScope.launch(instantExecutor) {
            assertFalse(updatedRecipe in recipesRepository.getAllRecipes())
            assertNull(recipesRepository.getRecipeOfFoodstuff(updatedRecipe.foodstuff))
        }
    }

    @Test
    fun `updated recipe passes foodstuff FoodstuffsList`() {
        val initialRecipes = createRecipes("1", "2", "3")
        fakeDatabaseWorker.setInitialRecipes(initialRecipes)
        val updatedRecipe = initialRecipes[0].copy(comment = "new comment")

        verify(foodstuffsList, never()).editFoodstuff(any(), any(), any())
        GlobalScope.launch(instantExecutor) {
            recipesRepository.updateRecipe(initialRecipes[0], updatedRecipe)
        }
        verify(foodstuffsList).editFoodstuff(any(), any(), any())
    }

    @Test
    fun `can update recipe even when provided initial recipe not fully same`() {
        val initialRecipes = createRecipes("1", "2", "3")
        fakeDatabaseWorker.setInitialRecipes(initialRecipes)

        val updatedRecipe = initialRecipes[0].copy(comment = "new comment")
        val initialRecipeChanged = initialRecipes[0].copy(weight = initialRecipes[0].weight + 1)
        var returnedUpdatedRecipe: Recipe? = null
        GlobalScope.async(instantExecutor) {
            val result = recipesRepository.updateRecipe(initialRecipeChanged, updatedRecipe)
            assertTrue(result is UpdateRecipeResult.Ok)
            returnedUpdatedRecipe = (result as UpdateRecipeResult.Ok).recipe
        }

        GlobalScope.launch(instantExecutor) {
            assertTrue(updatedRecipe in recipesRepository.getAllRecipes())
            assertTrue(returnedUpdatedRecipe in recipesRepository.getAllRecipes())
            assertEquals(updatedRecipe, recipesRepository.getRecipeOfFoodstuff(updatedRecipe.foodstuff))
            assertEquals(updatedRecipe, returnedUpdatedRecipe)
        }
    }

    private fun createRecipes(vararg names: String): List<Recipe> {
        val foodstuffs = names.map { Foodstuff.withId(randID()).withName(it).withNutrition(1f, 2f, 3f, 4f) }
        return createRecipes(foodstuffs)
    }

    private fun createRecipes(vararg recipeFoodstuffs: Foodstuff): List<Recipe> {
        return createRecipes(recipeFoodstuffs.asList())
    }

    private fun createRecipes(recipeFoodstuffs: List<Foodstuff>): List<Recipe> {
        return recipeFoodstuffs.map {
            Recipe(
                (Math.random()*Long.MAX_VALUE).toLong(),
                it,
                listOf(Ingredient(
                        (Math.random()*Long.MAX_VALUE).toLong(),
                        Foodstuff.withId(randID()).withName("ingredient").withNutrition(1f, 2f, 3f, 4f),
                        321f,
                        "comment of ingredient")),
                123f,
                "comment")
        }
    }

    class FakeRecipeDatabaseWorker : RecipeDatabaseWorker {
        private var recipes: MutableList<Recipe>? = null
        private val delayedActions = mutableListOf<()->Unit>()
        var createRecipesCallsCount = 0
        var shouldUpdateRecipesBeforeInitialRecipesSet = true

        fun setInitialRecipes(recipes: List<Recipe>) {
            if (this.recipes != null) {
                throw IllegalStateException("Must be initialized only once")
            }
            this.recipes = mutableListOf()
            this.recipes!!.addAll(recipes)
            runDelayedActions()
        }

        private fun runDelayedActions() {
            delayedActions.forEach { it.invoke() }
            delayedActions.clear()
        }

        override suspend fun getAllRecipes(): List<Recipe> = suspendCoroutine { continuation ->
            runWhenRecipesAcquired {
                continuation.resume(recipes!!)
            }
        }

        override suspend fun getRecipeOfFoodstuff(foodstuff: Foodstuff): Recipe? = suspendCoroutine { continuation ->
            runWhenRecipesAcquired {
                val result = recipes!!.find { it.foodstuff == foodstuff }
                continuation.resume(result)
            }
        }

        override suspend fun createRecipe(
                foodstuff: Foodstuff,
                ingredients: List<Ingredient>,
                comment: String,
                weight: Float): Recipe {
            createRecipesCallsCount += 1
            val recipe = Recipe(
                    randID(),
                    foodstuff,
                    ingredients,
                    weight,
                    comment)
            runWhenRecipesAcquired {
                recipes!!.add(recipe)
                Unit
            }
            return recipe
        }

        override suspend fun updateRecipe(updatedRecipe: Recipe): Recipe? {
            if (recipes == null) {
                if (shouldUpdateRecipesBeforeInitialRecipesSet) {
                    runWhenRecipesAcquired {
                        val removed = recipes!!.removeIf { it.id == updatedRecipe.id }
                        if (removed) {
                            recipes!!.add(updatedRecipe)
                        }
                    }
                    return updatedRecipe
                } else {
                    return null
                }
            }
            val removed = recipes!!.removeIf { it.id == updatedRecipe.id }
            if (removed) {
                recipes!!.add(updatedRecipe)
                return updatedRecipe
            } else {
                return null
            }
        }

        private fun runWhenRecipesAcquired(action: ()->Unit) {
            if (recipes != null) {
                action.invoke()
            } else {
                delayedActions.add(action)
            }
        }
    }
}

fun randID() = (Math.random() * (Long.MAX_VALUE-1)).toLong() + 1
