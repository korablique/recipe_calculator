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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalArgumentException

@RunWith(AndroidJUnit4::class)
@LargeTest
class RecipeDatabaseWorkerTest {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val dbHolder = DatabaseHolder(context, InstantDatabaseThreadExecutor())
    val databaseWorker = DatabaseWorker(
            dbHolder,
            InstantMainThreadExecutor(),
            InstantDatabaseThreadExecutor())

    val recipeDatabaseWorker =
            RecipeDatabaseWorkerImpl(
                    InstantIOExecutor(),
                    dbHolder,
                    databaseWorker)

    @Test
    fun canInsertAndSelect() = runBlocking {
        var foodstuff = Foodstuff.withName("recipe").withNutrition(10f, 20f, 30f, 40f)
        databaseWorker.saveFoodstuff(foodstuff) { foodstuff = foodstuff.recreateWithId(it) }
        val foodstuffsIngredients = mutableListOf<Foodstuff>()
        for (indx in 0 until 10) {
            var foodstuffIngredient = Foodstuff.withName("ingredient$indx").withNutrition(1f, 2f, 3f, 4f)
            databaseWorker.saveFoodstuff(foodstuffIngredient) {
                foodstuffIngredient = foodstuffIngredient.recreateWithId(it)
            }
            foodstuffsIngredients.add(foodstuffIngredient)
        }
        var ingredients = foodstuffsIngredients.map { Ingredient.new(it, 10f, "comment") }

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
        var foodstuff = Foodstuff.withName("recipe").withNutrition(10f, 20f, 30f, 40f)
        databaseWorker.saveFoodstuff(foodstuff) { foodstuff = foodstuff.recreateWithId(it) }

        recipeDatabaseWorker.createRecipe(foodstuff, emptyList(), "comment1", 123f)
        recipeDatabaseWorker.createRecipe(foodstuff, emptyList(), "comment2", 123f)

        Unit
    }

    @Test(expected = IllegalArgumentException::class)
    fun doesntAcceptNotSavedFoodstuff() = runBlocking {
        var foodstuff = Foodstuff.withName("recipe").withNutrition(10f, 20f, 30f, 40f)
        recipeDatabaseWorker.createRecipe(foodstuff, emptyList(), "comment1", 123f)

        Unit
    }

    @Test
    fun selectAllRecipes() = runBlocking {
        var foodstuff1 = Foodstuff.withName("recipe1").withNutrition(10f, 20f, 30f, 40f)
        databaseWorker.saveFoodstuff(foodstuff1) { foodstuff1 = foodstuff1.recreateWithId(it) }
        var foodstuff2 = Foodstuff.withName("recipe2").withNutrition(10f, 20f, 30f, 40f)
        databaseWorker.saveFoodstuff(foodstuff2) { foodstuff2 = foodstuff2.recreateWithId(it) }
        var foodstuff3 = Foodstuff.withName("recipe3").withNutrition(10f, 20f, 30f, 40f)
        databaseWorker.saveFoodstuff(foodstuff3) { foodstuff3 = foodstuff3.recreateWithId(it) }

        val recipes = listOf(
                recipeDatabaseWorker.createRecipe(foodstuff1, emptyList(), "comment1", 123f),
                recipeDatabaseWorker.createRecipe(foodstuff2, emptyList(), "comment2", 123f),
                recipeDatabaseWorker.createRecipe(foodstuff3, emptyList(), "comment3", 123f))
        assertEquals(recipes, recipeDatabaseWorker.getAllRecipes())
    }
}