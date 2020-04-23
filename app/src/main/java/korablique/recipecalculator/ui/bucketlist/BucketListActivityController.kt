package korablique.recipecalculator.ui.bucketlist

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter
import korablique.recipecalculator.R
import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.Callback
import korablique.recipecalculator.base.TimeProvider
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.dagger.ActivityScope
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.DecimalUtils
import korablique.recipecalculator.ui.NutritionValuesWrapper
import korablique.recipecalculator.ui.bucketlist.BucketListAdapter.OnItemLongClickedObserver
import korablique.recipecalculator.ui.bucketlist.BucketListAdapter.OnItemsCountChangeListener
import korablique.recipecalculator.ui.bucketlist.model.ModifiedRecipeModel
import korablique.recipecalculator.ui.bucketlist.model.ModifiedRecipeModelBucketList
import korablique.recipecalculator.ui.bucketlist.model.ModifiedRecipeModelRecipeEditing
import korablique.recipecalculator.ui.bucketlist.model.RecipeModelSaveChangesResult
import korablique.recipecalculator.ui.card.Card
import korablique.recipecalculator.ui.card.CardDialog
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar
import korablique.recipecalculator.util.FloatUtils
import javax.inject.Inject

private const val DISPLAYED_IN_CARD_FOODSTUFF_POSITION = "DISPLAYED_IN_CARD_FOODSTUFF_POSITION"


@ActivityScope
class BucketListActivityController @Inject constructor(
        val activity: BucketListActivity,
        val recipesRepository: RecipesRepository,
        val bucketList: BucketList,
        val mainThreadExecutor: MainThreadExecutor,
        val timeProvider: TimeProvider) : ActivityCallbacks.Observer {

    private lateinit var pluralProgressBar: PluralProgressBar
    private lateinit var nutritionValuesWrapper: NutritionValuesWrapper

    private lateinit var adapter: BucketListAdapter
    private lateinit var totalWeightEditText: EditText
    private lateinit var recipeNameEditText: EditText
    private lateinit var saveAsRecipeButton: Button

    private var displayedInCardFoodstuffPosition = 0
    private lateinit var onSaveFoodstuffButtonClickListener: Card.OnMainButtonSimpleClickListener

    private lateinit var recipeModel: ModifiedRecipeModel

    init {
        activity.activityCallbacks.addObserver(this)
    }

    override fun onActivityCreate(savedInstanceState: Bundle?) {
        val title = findViewById<TextView>(R.id.title_text)
        val intent = activity.intent
        if ((ACTION_EDIT_RECIPE == intent.action)) {
            val recipe: Recipe = intent.getParcelableExtra(EXTRA_RECIPE)
            recipeModel = ModifiedRecipeModelRecipeEditing(
                    recipe, recipesRepository, mainThreadExecutor)
            title.setText(R.string.bucket_list_title_recipe)
        } else {
            recipeModel = ModifiedRecipeModelBucketList(
                    bucketList, recipesRepository, mainThreadExecutor)
            title.setText(R.string.bucket_list_title_recipe_creation)
        }
        val nutritionLayout = findViewById<ViewGroup>(R.id.nutrition_progress_with_values)
        pluralProgressBar = findViewById(R.id.new_nutrition_progress_bar)
        nutritionValuesWrapper = NutritionValuesWrapper(activity, nutritionLayout)
        saveAsRecipeButton = findViewById(R.id.save_as_recipe_button)
        recipeNameEditText = findViewById(R.id.recipe_name_edit_text)
        totalWeightEditText = findViewById(R.id.total_weight_edit_text)
        onSaveFoodstuffButtonClickListener = Card.OnMainButtonSimpleClickListener { newFoodstuff ->
            val oldIngredient = adapter.getItem(displayedInCardFoodstuffPosition)
            val newIngredient = Ingredient.create(newFoodstuff, oldIngredient.comment)
            adapter.replaceItem(newIngredient, displayedInCardFoodstuffPosition)
            val newTotalWeight = countTotalWeight(adapter.items)
            totalWeightEditText.setText(DecimalUtils.toDecimalString(newTotalWeight))
            CardDialog.hideCard(activity)
            recipeModel.removeIngredient(oldIngredient)
            recipeModel.addIngredient(newIngredient)
            recipeModel.setTotalWeight(newTotalWeight)
            updateNutritionWrappers()
        }
        val existingCardDialog = CardDialog.findCard(activity)
        existingCardDialog?.setUpButton1(onSaveFoodstuffButtonClickListener, R.string.save)
        val onItemsCountChangeListener = OnItemsCountChangeListener { updateSaveButtonsEnability() }
        val onItemClickedObserver = BucketListAdapter.OnItemClickedObserver { ingredient: Ingredient, position: Int ->
            displayedInCardFoodstuffPosition = position
            val cardDialog: CardDialog = CardDialog.showCard(
                    activity, ingredient.toWeightedFoodstuff())
            cardDialog.setUpButton1(onSaveFoodstuffButtonClickListener, R.string.save)
        }
        val onItemLongClickedObserver = OnItemLongClickedObserver { ingredient: Ingredient?, position: Int, view: View ->
            val menu = PopupMenu(activity, view)
            menu.inflate(R.menu.bucket_list_menu)
            menu.show()
            menu.setOnMenuItemClickListener { item: MenuItem ->
                if (item.itemId == R.id.delete_ingredient) {
                    adapter.deleteItem(position)
                    recipeModel.removeIngredient((ingredient)!!)
                    val newWeight: Float = countTotalWeight(adapter.items)
                    totalWeightEditText.setText(DecimalUtils.toDecimalString(newWeight))
                    recipeModel.setTotalWeight(newWeight)
                    updateNutritionWrappers()
                    true
                } else {
                    false
                }
            }
            true
        }
        adapter = BucketListAdapter(
                activity,
                R.layout.new_foodstuff_layout,
                onItemsCountChangeListener,
                onItemClickedObserver,
                onItemLongClickedObserver)
        adapter.addItems(recipeModel.getIngredients())
        val ingredientsListRecyclerView = findViewById<RecyclerView>(R.id.ingredients_list)
        ingredientsListRecyclerView.adapter = adapter
        saveAsRecipeButton.setOnClickListener {
            recipeModel.flushChanges(Callback { result: RecipeModelSaveChangesResult? ->
                when (result) {
                    is RecipeModelSaveChangesResult.Ok -> {
                        Toast.makeText(activity, R.string.saved, Toast.LENGTH_SHORT).show()
                        activity.setResult(Activity.RESULT_OK, BucketListActivity.createRecipeResultIntent(result.recipe))
                        activity.finish()
                    }
                    is RecipeModelSaveChangesResult.FoodstuffDuplicationError -> {
                        Toast.makeText(activity, R.string.foodstuff_already_exists, Toast.LENGTH_LONG).show()
                    }
                    is RecipeModelSaveChangesResult.InternalError -> {
                        Toast.makeText(activity, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        throw Error("Unhandled sealed class")
                    }
                }
            })
        }
        recipeNameEditText.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable) {
                recipeModel.setName(s.toString())
                updateSaveButtonsEnability()
            }
        })
        totalWeightEditText.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable) {
                var totalWeight = 0f
                if (!s.toString().isEmpty()) {
                    totalWeight = s.toString().toFloat()
                }
                recipeModel.setTotalWeight(totalWeight)
                updateSaveButtonsEnability()
                updateNutritionWrappers()
            }
        })
        recipeNameEditText.setText(recipeModel.getName())
        totalWeightEditText.setText(DecimalUtils.toDecimalString(recipeModel.getTotalWeight()))
        val cancelView = findViewById<View>(R.id.button_close)
        cancelView.setOnClickListener { activity.finish() }
        updateNutritionWrappers()
    }

    private fun <T : View?> findViewById(@IdRes id: Int): T {
        return activity.findViewById<T>(id)
    }

    private fun updateNutritionWrappers() {
        var nutrition = Nutrition.zero()
        if (!FloatUtils.areFloatsEquals(0f, recipeModel.getTotalWeight(), 0.0001f)) {
            val recipe = recipeModel.extractEditedRecipe()
            nutrition = Nutrition.of100gramsOf(recipe.foodstuff)
        }
        nutritionValuesWrapper.setNutrition(nutrition)
        pluralProgressBar.setProgress(
                nutrition.protein.toFloat(),
                nutrition.fats.toFloat(),
                nutrition.carbs.toFloat())
    }

    private fun countTotalWeight(ingredients: List<Ingredient>): Float {
        var result = 0f
        for (foodstuff: Ingredient in ingredients) {
            result += foodstuff.weight
        }
        return result
    }

    private fun updateSaveButtonsEnability() {
        val text = totalWeightEditText.text.toString()
        val name = recipeNameEditText.text.toString().trim()
        saveAsRecipeButton.isEnabled =
                !text.isEmpty()
                        && !name.isEmpty()
                        && !FloatUtils.areFloatsEquals(text.toDouble(), 0.0)
                        && adapter.itemCount != 0
    }

    override fun onActivitySaveInstanceState(outState: Bundle) {
        outState.putInt(DISPLAYED_IN_CARD_FOODSTUFF_POSITION, displayedInCardFoodstuffPosition)
    }
    
    override fun onActivityRestoreInstanceState(savedInstanceState: Bundle) {
        displayedInCardFoodstuffPosition = savedInstanceState.getInt(DISPLAYED_IN_CARD_FOODSTUFF_POSITION)
        val cardDialog = CardDialog.findCard(activity)
        cardDialog?.setUpButton1(onSaveFoodstuffButtonClickListener, R.string.save)
    }
}