package korablique.recipecalculator.ui.bucketlist

import android.os.Looper
import korablique.recipecalculator.DishNutritionCalculator
import korablique.recipecalculator.DishNutritionCalculator.calculateIngredients
import korablique.recipecalculator.TestEnvironmentDetector
import korablique.recipecalculator.WrongThreadException
import korablique.recipecalculator.base.prefs.PrefsOwner
import korablique.recipecalculator.base.prefs.SharedPrefsManager
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Ingredient.Companion.create
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.model.proto.RecipeProtos
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_RECIPE = "PREFS_RECIPE"

@Singleton
class BucketList @Inject constructor(
        private val prefsManager: SharedPrefsManager) {
    interface Observer {
        fun onIngredientAdded(ingredient: Ingredient) {}
        fun onIngredientRemoved(ingredient: Ingredient) {}
    }

    private var editedRecipe: Recipe
    private val observers: MutableList<Observer> = ArrayList()

    init {
        editedRecipe = Recipe.create(
                Foodstuff.withName("").withNutrition(Nutrition.zero()),
                emptyList(),
                0f,
                "")
        val recipeBytes: ByteArray? = prefsManager.getBytes(PrefsOwner.BUCKET_LIST, PREFS_RECIPE)
        if (recipeBytes != null) {
            try {
                editedRecipe = Recipe.fromProto(RecipeProtos.Recipe.parseFrom(recipeBytes))
            } catch (e: IOException) {
                // TODO: report error
            }
        }
    }

    fun add(ingredients: List<Ingredient>) {
        checkCurrentThread()

        editedRecipe = editedRecipe.copy(ingredients = editedRecipe.ingredients + ingredients)
        recalculateNutrition()
        updatePersistentState()
        for (ingredient in ingredients) {
            for (observer in observers) {
                observer.onIngredientAdded(ingredient)
            }
        }
    }

    fun add(ingredient: Ingredient) {
        checkCurrentThread()

        editedRecipe = editedRecipe.copy(ingredients = editedRecipe.ingredients + ingredient)
        recalculateNutrition()
        updatePersistentState()
        for (observer in observers) {
            observer.onIngredientAdded(ingredient)
        }
    }

    fun remove(ingredient: Ingredient) {
        checkCurrentThread()

        editedRecipe = editedRecipe.copy(ingredients = editedRecipe.ingredients - ingredient)
        recalculateNutrition()
        updatePersistentState()
        for (observer in observers) {
            observer.onIngredientRemoved(ingredient)
        }
    }

    fun setRecipe(recipe: Recipe) {
        checkCurrentThread()

        val addedIngredients = recipe.ingredients - editedRecipe.ingredients
        val removedIngredients = editedRecipe.ingredients - recipe.ingredients

        editedRecipe = recipe
        updatePersistentState()

        observers.forEach { obs -> addedIngredients.forEach { obs.onIngredientAdded(it) } }
        observers.forEach { obs -> removedIngredients.forEach { obs.onIngredientRemoved(it) } }
    }

    fun getName(): String = editedRecipe.foodstuff.name

    fun getComment(): String = editedRecipe.comment

    fun getTotalWeight(): Float = editedRecipe.weight

    fun getList(): List<Ingredient> = editedRecipe.ingredients

    fun getRecipe(): Recipe = editedRecipe

    private fun recalculateNutrition() {
        var nutrition = calculateIngredients(editedRecipe.ingredients, editedRecipe.weight.toDouble())
        nutrition = normalizeFoodstuffNutrition(nutrition);
        editedRecipe = editedRecipe.copy(foodstuff = editedRecipe.foodstuff.recreateWithNutrition(nutrition))
    }

    /**
     * When sum of protein, fats and carbs is greater than 100, then we should not create
     * a foodstuff with such nutrition, and must normalize the nutrition before foodstuff creation.
     */
    private fun normalizeFoodstuffNutrition(nutrition: Nutrition): Nutrition {
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

    fun setName(name: String) {
        editedRecipe = editedRecipe.copy(foodstuff = editedRecipe.foodstuff.recreateWithName(name))
        updatePersistentState()
    }

    fun setComment(comment: String) {
        editedRecipe = editedRecipe.copy(comment = comment)
        updatePersistentState()
    }

    fun setTotalWeight(weight: Float) {
        editedRecipe = editedRecipe.copy(weight = weight)
        recalculateNutrition()
        updatePersistentState()
    }

    fun clear() {
        checkCurrentThread()
        val oldIngredients = editedRecipe.ingredients
        editedRecipe = Recipe.create(
                Foodstuff.withName("").withNutrition(Nutrition.zero()),
                emptyList(),
                0f,
                "")
        updatePersistentState()
        for (ingredient in oldIngredients) {
            for (observer in observers) {
                observer.onIngredientRemoved(ingredient)
            }
        }
    }

    private fun updatePersistentState() {
        prefsManager.putBytes(PrefsOwner.BUCKET_LIST, PREFS_RECIPE, editedRecipe.toProto().toByteArray())
    }

    fun addObserver(o: Observer) {
        checkCurrentThread()
        observers.add(o)
    }

    fun removeObserver(o: Observer) {
        checkCurrentThread()
        observers.remove(o)
    }

    private fun checkCurrentThread() {
        if (TestEnvironmentDetector.isInTests()) {
            return
        }
        if (Thread.currentThread().id != Looper.getMainLooper().thread.id) {
            throw WrongThreadException("Can't invoke BucketList's methods from not UI thread")
        }
    }
}