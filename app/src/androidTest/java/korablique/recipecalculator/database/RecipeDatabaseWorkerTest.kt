package korablique.recipecalculator.database

import android.database.sqlite.SQLiteConstraintException
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import korablique.recipecalculator.InstantDatabaseThreadExecutor
import korablique.recipecalculator.InstantIOExecutor
import korablique.recipecalculator.InstantMainThreadExecutor
import korablique.recipecalculator.database.room.DatabaseHolder
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.RuntimeException

@RunWith(AndroidJUnit4::class)
@LargeTest
class RecipeDatabaseWorkerTest {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val dbHolder = DatabaseHolder(context, InstantDatabaseThreadExecutor())
    lateinit var databaseWorker: DatabaseWorker
    lateinit var recipeDatabaseWorker: RecipeDatabaseWorkerImpl

    @Before
    fun setUp() {
        dbHolder.database.clearAllTables()

        databaseWorker = DatabaseWorker(
            dbHolder,
            InstantMainThreadExecutor(),
            InstantDatabaseThreadExecutor())
        recipeDatabaseWorker =
            RecipeDatabaseWorkerImpl(
                    InstantIOExecutor(),
                    dbHolder,
                    databaseWorker)
    }

    @Test
    fun canInsertAndSelect() = runBlocking {
        val foodstuff = saveFoodstuffWith("recipe", 10f, 20f, 30f, 40f)
        val foodstuffsIngredients = mutableListOf<Foodstuff>()
        for (indx in 0 until 10) {
            foodstuffsIngredients.add(saveFoodstuffWith("ingredient$indx", 1f, 2f, 3f, 4f))
        }
        var ingredients = foodstuffsIngredients.map { Ingredient.create(it, 10f, "comment") }

        val insertedRecipe = recipeDatabaseWorker.createRecipe(foodstuff, ingredients, "comment", 123f)

        // Update IDs of manually created ingredients for easier equals assertion below
        ingredients = ingredients.zip(insertedRecipe.ingredients) { oldIngred, newIngred ->
            oldIngred.copy(id = newIngred.id)
        }

        assertEquals(insertedRecipe.comment, "comment")
        assertEquals(insertedRecipe.foodstuff, foodstuff)
        assertEquals(insertedRecipe.ingredients, ingredients)
        assertEquals(insertedRecipe.weight, 123f)
        assertTrue(insertedRecipe.id > 0)

        val selectedRecipe = recipeDatabaseWorker.getRecipeOfFoodstuff(foodstuff)
        assertEquals(insertedRecipe, selectedRecipe)

        Unit
    }

    @Test(expected = SQLiteConstraintException::class)
    fun cannotCreate2RecipesFor1Foodstuff() = runBlocking {
        val foodstuff = saveFoodstuffWith("recipe", 10f, 20f, 30f, 40f)
        recipeDatabaseWorker.createRecipe(foodstuff, emptyList(), "comment1", 123f)
        recipeDatabaseWorker.createRecipe(foodstuff, emptyList(), "comment2", 123f)

        Unit
    }

    @Test(expected = IllegalArgumentException::class)
    fun doesntAcceptNotSavedFoodstuff() = runBlocking {
        val foodstuff = Foodstuff.withName("recipe").withNutrition(10f, 20f, 30f, 40f)
        recipeDatabaseWorker.createRecipe(foodstuff, emptyList(), "comment1", 123f)

        Unit
    }

    @Test
    fun selectAllRecipes() = runBlocking {
        val foodstuff1 = saveFoodstuffWith("recipe1", 10f, 20f, 30f, 40f)
        val foodstuff2 = saveFoodstuffWith("recipe2", 10f, 20f, 30f, 40f)
        val foodstuff3 = saveFoodstuffWith("recipe3", 10f, 20f, 30f, 40f)

        val recipes = listOf(
                recipeDatabaseWorker.createRecipe(foodstuff1, emptyList(), "comment1", 123f),
                recipeDatabaseWorker.createRecipe(foodstuff2, emptyList(), "comment2", 123f),
                recipeDatabaseWorker.createRecipe(foodstuff3, emptyList(), "comment3", 123f))
        assertEquals(recipes, recipeDatabaseWorker.getAllRecipes())
    }

    @Test
    fun update() = runBlocking {
        // Initial state
        var foodstuff = saveFoodstuffWith("recipe", 10f, 20f, 30f, 40f)
        val foodstuffsIngredients = mutableListOf(
                saveFoodstuffWith("ingredient1", 1f, 2f, 3f, 4f),
                saveFoodstuffWith("ingredient2", 4f, 3f, 2f, 1f))
        val initialIngredients = foodstuffsIngredients.map { Ingredient.create(it, 10f, "comment") }
        val insertedRecipe = recipeDatabaseWorker.createRecipe(foodstuff, initialIngredients, "comment", 123f)

        // Verify initial state
        assertEquals("recipe", insertedRecipe.foodstuff.name)
        assertEquals(123f, insertedRecipe.weight, 0.001f)
        assertEquals("comment", insertedRecipe.comment)
        assertEquals(10.0, insertedRecipe.foodstuff.protein, 0.001)
        assertEquals(20.0, insertedRecipe.foodstuff.fats, 0.001)
        assertEquals(30.0, insertedRecipe.foodstuff.carbs, 0.001)
        assertEquals(40.0, insertedRecipe.foodstuff.calories, 0.001)
        assertEquals(foodstuffsIngredients[0], insertedRecipe.ingredients[0].foodstuff)
        assertEquals("comment", insertedRecipe.ingredients[0].comment)
        assertEquals(10f, insertedRecipe.ingredients[0].weight, 0.001f)
        assertEquals(foodstuffsIngredients[1], insertedRecipe.ingredients[1].foodstuff)
        assertEquals("comment", insertedRecipe.ingredients[1].comment)
        assertEquals(10f, insertedRecipe.ingredients[1].weight, 0.001f)

        // Update
        foodstuffsIngredients.removeAt(0)
        foodstuffsIngredients.add(saveFoodstuffWith("ingredient3", 1f, 1f, 1f, 1f))
        val keptIngredient = initialIngredients[1]
        val updatedIngredients = listOf(
                keptIngredient,
                Ingredient.create(foodstuffsIngredients[1], 777f, "wow")
        )
        val newFoodstuff = saveFoodstuffWith("recipe updated", 1f, 20f, 3f, 40f)
        var updatedRecipe = insertedRecipe.copy(
                foodstuff = newFoodstuff,
                ingredients = updatedIngredients,
                weight = 333f,
                comment = "such comment")
        updatedRecipe = recipeDatabaseWorker.updateRecipe(updatedRecipe)!!

        // Verify updated state
        assertEquals("recipe updated", updatedRecipe.foodstuff.name)
        assertEquals(333f, updatedRecipe.weight, 0.001f)
        assertEquals("such comment", updatedRecipe.comment)
        assertEquals(1.0, updatedRecipe.foodstuff.protein, 0.001)
        assertEquals(20.0, updatedRecipe.foodstuff.fats, 0.001)
        assertEquals(3.0, updatedRecipe.foodstuff.carbs, 0.001)
        assertEquals(40.0, updatedRecipe.foodstuff.calories, 0.001)
        assertEquals(foodstuffsIngredients[0], updatedRecipe.ingredients[0].foodstuff)
        assertEquals("comment", updatedRecipe.ingredients[0].comment)
        assertEquals(10f, updatedRecipe.ingredients[0].weight, 0.001f)
        assertEquals(foodstuffsIngredients[1], updatedRecipe.ingredients[1].foodstuff)
        assertEquals("wow", updatedRecipe.ingredients[1].comment)
        assertEquals(777f, updatedRecipe.ingredients[1].weight, 0.001f)

        // Verify selected from DB recipes are also valid
        val allRecipes = recipeDatabaseWorker.getAllRecipes()
        assertEquals(1, allRecipes.size)
        assertEquals(updatedRecipe, allRecipes[0])
    }

    @Test(expected = RuntimeException::class)
    fun updateWhenRowCannotBeFound() = runBlocking {
        val recipe = Recipe.create(saveFoodstuffWith("name", 1f, 2f, 3f, 4f), emptyList(), 1f, "")
        // NOTE: the recipe is not saved to DB
        val updatedRecipe = recipeDatabaseWorker.updateRecipe(recipe)
        assertNull(updatedRecipe)
    }

    private fun saveFoodstuffWith(
            name: String,
            protein: Float,
            fats: Float,
            carbs: Float,
            calories: Float): Foodstuff {
        var foodstuff = Foodstuff.withName(name).withNutrition(protein, fats, carbs, calories)
        databaseWorker.saveFoodstuff(foodstuff) {
            foodstuff = foodstuff.recreateWithId(it)
        }
        return foodstuff
    }
}