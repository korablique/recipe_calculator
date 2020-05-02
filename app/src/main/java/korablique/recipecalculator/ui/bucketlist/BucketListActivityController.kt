package korablique.recipecalculator.ui.bucketlist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import korablique.recipecalculator.R
import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.dagger.ActivityScope
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.DecimalUtils
import korablique.recipecalculator.ui.NutritionValuesWrapper
import korablique.recipecalculator.ui.bucketlist.states.BucketListActivityDisplayRecipeState
import korablique.recipecalculator.ui.bucketlist.states.BucketListActivityRecipeEditingState
import korablique.recipecalculator.ui.bucketlist.states.BucketListActivityState
import korablique.recipecalculator.ui.bucketlist.states.BucketListActivityState.FinishResult
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar
import korablique.recipecalculator.util.FloatUtils
import javax.inject.Inject

const val EXTRA_CURRENT_STATE = "EXTRA_CURRENT_STATE"
const val EXTRA_CURRENT_STATE_TYPE = "EXTRA_CURRENT_STATE_TYPE"

@ActivityScope
class BucketListActivityController @Inject constructor(
        private val activity: BucketListActivity,
        private val recipesRepository: RecipesRepository,
        private val bucketList: BucketList,
        private val mainThreadExecutor: MainThreadExecutor)
    : ActivityCallbacks.Observer, BucketListActivityState.Delegate {
    private lateinit var pluralProgressBar: PluralProgressBar
    private lateinit var nutritionValuesWrapper: NutritionValuesWrapper
    private lateinit var adapter: BucketListAdapter
    private lateinit var commentLayoutController: CommentLayoutController
    private lateinit var currentState: BucketListActivityState

    companion object {
        fun createRecipeResultIntent(recipe: Recipe?): Intent {
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_PRODUCED_RECIPE, recipe)
            return resultIntent
        }

        fun start(
                context: Activity,
                requestCode: Int) {
            context.startActivityForResult(createIntent(context), requestCode)
        }

        fun createIntent(context: Context): Intent {
            return Intent(context, BucketListActivity::class.java)
        }

        fun startForRecipe(
                fragment: Fragment,
                requestCode: Int,
                recipe: Recipe) {
            fragment.startActivityForResult(
                    createIntent(fragment.requireContext(), recipe), requestCode)
        }

        fun createIntent(context: Context, recipe: Recipe): Intent {
            val intent = Intent(context, BucketListActivity::class.java)
            intent.action = ACTION_EDIT_RECIPE
            intent.putExtra(EXTRA_RECIPE, recipe)
            return intent
        }
    }

    init {
        activity.activityCallbacks.addObserver(this)
    }

    override fun onActivityDestroy() {
        activity.activityCallbacks.removeObserver(this)
    }

    override fun onActivitySaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_CURRENT_STATE_TYPE, currentState.getStateID().ordinal)
        outState.putBundle(EXTRA_CURRENT_STATE, currentState.saveInstanceState())
    }

    override fun onActivityCreate(savedInstanceState: Bundle?) {
        pluralProgressBar = findViewById(R.id.new_nutrition_progress_bar)
        nutritionValuesWrapper = NutritionValuesWrapper(
                activity,
                findViewById<ViewGroup>(R.id.nutrition_progress_with_values))

        adapter = BucketListAdapter(activity)
        findViewById<RecyclerView>(R.id.ingredients_list).adapter = adapter

        adapter.setOnItemClickedObserver(object : BucketListAdapter.OnItemClickedObserver {
            override fun onItemClicked(ingredient: Ingredient, position: Int) {
                currentState.onDisplayedIngredientClicked(ingredient, position)
            }
        })
        adapter.setOnItemLongClickedObserver(object : BucketListAdapter.OnItemLongClickedObserver {
            override fun onItemLongClicked(ingredient: Ingredient, position: Int, view: View): Boolean {
                return currentState.onDisplayedIngredientLongClicked(ingredient, position, view)
            }
        })
        commentLayoutController = CommentLayoutController(findViewById(R.id.comment_layout))

        switchStateImpl(createFirstState(savedInstanceState), first = true)
        onRecipeUpdated(currentState.getRecipe())
    }

    private fun createFirstState(savedInstanceState: Bundle?): BucketListActivityState {
        if (savedInstanceState != null) {
            val stateIdOrdinal = savedInstanceState.getInt(EXTRA_CURRENT_STATE_TYPE)
            val stateId = BucketListActivityState.ID.values()[stateIdOrdinal]
            val stateOfState = savedInstanceState.getBundle(EXTRA_CURRENT_STATE)!!
            return when (stateId) {
                BucketListActivityState.ID.DisplayState -> {
                    BucketListActivityDisplayRecipeState(
                            stateOfState, commentLayoutController, activity, bucketList,
                            recipesRepository, mainThreadExecutor)
                }
                BucketListActivityState.ID.EditingState -> {
                    BucketListActivityRecipeEditingState(
                            stateOfState, commentLayoutController, activity, bucketList,
                            recipesRepository, mainThreadExecutor)
                }
            }
        }

        return if (ACTION_EDIT_RECIPE == activity.intent.action) {
            val recipe: Recipe = activity.intent.getParcelableExtra(EXTRA_RECIPE)
            BucketListActivityDisplayRecipeState(
                    recipe, commentLayoutController, activity, bucketList,
                    recipesRepository, mainThreadExecutor)
        } else {
            BucketListActivityRecipeEditingState(
                    bucketList.getRecipe(), commentLayoutController, activity, bucketList,
                    recipesRepository, mainThreadExecutor)
        }
    }

    override fun <T : View?> findViewById(@IdRes id: Int): T {
        return activity.findViewById<T>(id)
    }

    private fun updateNutritionWrappers() {
        val recipe = currentState.getRecipe()
        var nutrition = Nutrition.zero()
        if (!FloatUtils.areFloatsEquals(0f, recipe.weight, 0.0001f)) {
            nutrition = Nutrition.of100gramsOf(recipe.foodstuff)
        }
        nutritionValuesWrapper.setNutrition(nutrition)
        pluralProgressBar.setProgress(
                nutrition.protein.toFloat(),
                nutrition.fats.toFloat(),
                nutrition.carbs.toFloat())
    }

    override fun onRecipeUpdated(recipe: Recipe) {
        updateNutritionWrappers()
        val weightEditText = findViewById<TextView>(R.id.total_weight_edit_text)
        val nameEditText = findViewById<TextView>(R.id.recipe_name_edit_text)

        val weightText = DecimalUtils.toDecimalString(recipe.weight)
        if (weightText != weightEditText.text.toString()) {
            weightEditText.text = weightText
        }
        if (recipe.name != nameEditText.text.toString()) {
            nameEditText.text = recipe.name
        }
        adapter.setItems(recipe.ingredients)

        commentLayoutController.setComment(recipe.comment)
    }

    override fun switchState(newState: BucketListActivityState) {
        switchStateImpl(newState, first = false)
    }

    private fun switchStateImpl(state: BucketListActivityState, first: Boolean) {
        if (!first) {
            currentState.destroy()
        }
        currentState = state
        currentState.init(this, adapter)
        onRecipeUpdated(currentState.getRecipe())

        if (currentState.supportsIngredientsAddition()) {
            adapter.setUpAddIngredientButton(Runnable {
                currentState.onAddIngredientButtonClicked()
            })
        } else {
            adapter.deinitAddIngredientButton()
        }

        findViewById<TextView>(R.id.title_text).setText(currentState.getTitleStringID())

        TransitionManager.beginDelayedTransition(findViewById(R.id.bucket_list_activity_layout))
        switchConstraints(R.id.bucket_list_activity_main_content_layout, currentState.getMainConstraintSetDescriptionLayout())
        switchConstraints(R.id.bucket_list_activity_layout, currentState.getConstraintSetDescriptionLayout())
    }

    private fun switchConstraints(rootId: Int, newConstraintsId: Int) {
        val root = activity.findViewById<ConstraintLayout>(rootId)
        val newConstraintSet = ConstraintSet()
        newConstraintSet.clone(activity, newConstraintsId)
        newConstraintSet.applyTo(root)
    }

    override fun finish(finishResult: FinishResult) {
        when (finishResult) {
            is FinishResult.Ok -> {
                activity.setResult(
                        Activity.RESULT_OK,
                        createRecipeResultIntent(finishResult.recipe))
            }
            is FinishResult.Canceled -> {
                activity.setResult(Activity.RESULT_CANCELED)
            }
        }
        activity.finish()
    }

    override fun onActivityBackPressed(): Boolean {
        return currentState.onActivityBackPressed()
    }
}