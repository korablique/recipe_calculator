package korablique.recipecalculator.ui.bucketlist.states

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.bucketlist.BucketList
import korablique.recipecalculator.ui.bucketlist.BucketListAdapter
import korablique.recipecalculator.ui.bucketlist.CommentLayoutController
import korablique.recipecalculator.ui.numbersediting.SimpleTextWatcher
import korablique.recipecalculator.util.FloatUtils

private const val EXTRA_INITIAL_RECIPE = "EXTRA_INITIAL_RECIPE"
private const val EXTRA_DISPLAYED_RECIPE = "EXTRA_DISPLAYED_RECIPE"

class BucketListActivityCookingState private constructor(
        private val initialRecipe: Recipe,
        private var displayedRecipe: Recipe,
        private val commentLayoutController: CommentLayoutController,
        private val activity: BaseActivity,
        private val bucketList: BucketList,
        private val recipesRepository: RecipesRepository,
        private val mainThreadExecutor: MainThreadExecutor
) : BucketListActivityState() {
    private lateinit var totalWeightTextWatcher: SimpleTextWatcher<EditText>
    private lateinit var totalWeightEditText: EditText

    constructor(
            savedState: Bundle,
            commentLayoutController: CommentLayoutController,
            activity: BaseActivity,
            bucketList: BucketList,
            recipesRepository: RecipesRepository,
            mainThreadExecutor: MainThreadExecutor)
            : this(
            savedState.getParcelable(EXTRA_INITIAL_RECIPE) as Recipe,
            savedState.getParcelable(EXTRA_DISPLAYED_RECIPE) as Recipe,
            commentLayoutController,
            activity,
            bucketList,
            recipesRepository,
            mainThreadExecutor
    )

    constructor(
            initialRecipe: Recipe,
            commentLayoutController: CommentLayoutController,
            activity: BaseActivity,
            bucketList: BucketList,
            recipesRepository: RecipesRepository,
            mainThreadExecutor: MainThreadExecutor)
            : this(
            initialRecipe,
            initialRecipe,
            commentLayoutController,
            activity,
            bucketList,
            recipesRepository,
            mainThreadExecutor
    )

    override fun saveInstanceState(): Bundle {
        val state = Bundle()
        state.putParcelable(EXTRA_INITIAL_RECIPE, initialRecipe)
        state.putParcelable(EXTRA_DISPLAYED_RECIPE, displayedRecipe)
        return state
    }

    override fun getStateID(): ID = ID.CookingState
    override fun getTitleStringID(): Int = R.string.bucket_list_title_cooking
    override fun getMainConstraintSetDescriptionLayout(): Int = R.layout.activity_bucket_list_main_state_cooking
    override fun getConstraintSetDescriptionLayout(): Int = R.layout.activity_bucket_list_state_displaying
    override fun getRecipe(): Recipe = displayedRecipe

    override fun initImpl() {
        findViewById<EditText>(R.id.recipe_name_edit_text).isEnabled = false
        commentLayoutController.setEditable(false)

        findViewById<View>(R.id.button_close).setOnClickListener {
            onActivityBackPressed()
        }

        totalWeightEditText = activity.findViewById(R.id.total_weight_edit_text);
        totalWeightTextWatcher = SimpleTextWatcher(totalWeightEditText) {
            val updatedText = totalWeightEditText.text.toString()
            var updatedWeight = 0
            if (!TextUtils.isEmpty(updatedText)) {
                val textWeight = updatedText.toBigDecimal()
                updatedWeight = if (textWeight > Int.MAX_VALUE.toBigDecimal()) {
                    Int.MAX_VALUE
                } else {
                    textWeight.toInt()
                }
            }
            if (!FloatUtils.areFloatsEquals(updatedWeight.toFloat(), displayedRecipe.weight)) {
                val factor = updatedWeight / initialRecipe.weight
                updateRecipeWithFactor(factor)
            }
        }
        totalWeightEditText.addTextChangedListener(totalWeightTextWatcher)
    }

    override fun destroyImpl() {
        findViewById<EditText>(R.id.recipe_name_edit_text).isEnabled = true
        findViewById<View>(R.id.button_close).setOnClickListener(null)
        totalWeightEditText.removeTextChangedListener(totalWeightTextWatcher)
    }

    override fun createIngredientWeightEditionObserver(): BucketListAdapter.ItemWeightEditionObserver? {
        return object : BucketListAdapter.ItemWeightEditionObserver {
            override fun onItemWeightEdited(ingredient: Ingredient, newWeight: Float, position: Int) {
                val initialWeight = initialRecipe.ingredients[position].weight
                val factor = if (newWeight == 0f || initialWeight == 0f) {
                    0f
                } else {
                    newWeight / initialRecipe.ingredients[position].weight
                }
                updateRecipeWithFactor(factor)
            }
        }
    }

    private fun updateRecipeWithFactor(factor: Float) {
        val updatedIngredients =
                initialRecipe.ingredients.map { it.copy(weight = it.weight * factor) }
        displayedRecipe = initialRecipe.copy(
                weight = initialRecipe.weight * factor,
                ingredients = updatedIngredients)
        onRecipeUpdated(displayedRecipe)
    }

    override fun onActivityBackPressed(): Boolean {
        switchState(
                BucketListActivityDisplayRecipeState(
                        initialRecipe, commentLayoutController, activity,
                        bucketList, recipesRepository, mainThreadExecutor))
        return true
    }
}