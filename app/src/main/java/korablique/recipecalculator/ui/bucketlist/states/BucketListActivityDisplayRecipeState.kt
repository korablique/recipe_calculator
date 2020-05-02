package korablique.recipecalculator.ui.bucketlist.states

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.bucketlist.BucketList
import korablique.recipecalculator.ui.bucketlist.CommentLayoutController

const val EXTRA_DISPLAYED_RECIPE = "EXTRA_DISPLAYED_RECIPE"

class BucketListActivityDisplayRecipeState(
        private val recipe: Recipe,
        private val commentLayoutController: CommentLayoutController,
        private val activity: BaseActivity,
        private val bucketList: BucketList,
        private val recipesRepository: RecipesRepository,
        private val mainThreadExecutor: MainThreadExecutor
) : BucketListActivityState() {

    constructor(savedInstanceState: Bundle,
                commentLayoutController: CommentLayoutController,
                activity: BaseActivity,
                bucketList: BucketList,
                recipesRepository: RecipesRepository,
                mainThreadExecutor: MainThreadExecutor):
            this(savedInstanceState.getParcelable(EXTRA_DISPLAYED_RECIPE) as Recipe,
                    commentLayoutController,
                    activity, bucketList, recipesRepository, mainThreadExecutor)

    override fun getStateID(): ID = ID.DisplayState
    override fun getTitleStringID(): Int = R.string.bucket_list_title_recipe
    override fun getConstraintSetDescriptionLayout(): Int = R.layout.activity_bucket_list_state_displaying

    override fun saveInstanceState(): Bundle {
        val result = Bundle()
        result.putParcelable(EXTRA_DISPLAYED_RECIPE, recipe)
        return result
    }

    override fun initImpl() {
        findViewById<View>(R.id.button_close).setOnClickListener { finish(FinishResult.Canceled) }
        findViewById<EditText>(R.id.recipe_name_edit_text).isEnabled = false
        findViewById<EditText>(R.id.total_weight_edit_text).isEnabled = false

        findViewById<View>(R.id.button_edit).setOnClickListener {
            switchState(BucketListActivityRecipeEditingState(
                    recipe, commentLayoutController, activity, bucketList,
                    recipesRepository, mainThreadExecutor))
        }

        commentLayoutController.setEditable(false)
    }

    override fun destroyImpl() {
        findViewById<View>(R.id.button_close).setOnClickListener(null)
        findViewById<EditText>(R.id.recipe_name_edit_text).isEnabled = true
        findViewById<EditText>(R.id.total_weight_edit_text).isEnabled = true
        findViewById<View>(R.id.button_edit).setOnClickListener(null)
    }

    override fun getRecipe(): Recipe = recipe
}