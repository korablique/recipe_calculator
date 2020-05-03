package korablique.recipecalculator.ui.bucketlist.states

import android.os.Bundle
import android.text.Editable
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.database.CreateRecipeResult
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.database.UpdateRecipeResult
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.TwoOptionsDialog
import korablique.recipecalculator.ui.bucketlist.BucketList
import korablique.recipecalculator.ui.bucketlist.CommentLayoutController
import korablique.recipecalculator.ui.bucketlist.IngredientCommentDialog
import korablique.recipecalculator.ui.card.Card
import korablique.recipecalculator.ui.card.CardDialog
import korablique.recipecalculator.util.FloatUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

private const val TAG_CANCELLATION_DIALOG = "TAG_CANCELLATION_DIALOG"
private const val EXTRA_INITIAL_RECIPE = "EXTRA_INITIAL_RECIPE"
private const val EXTRA_COMMENTED_INGREDIENT_POSITION = "EXTRA_COMMENTED_INGREDIENT_POSITION"

class BucketListActivityRecipeEditingState private constructor(
        private val initialDisplayedRecipe: Recipe,
        private var commentDialogIngredientPosition: Int?,
        private val commentLayoutController: CommentLayoutController,
        private val savedInstanceState: Bundle?,
        private val activity: BaseActivity,
        private val bucketList: BucketList,
        private val recipesRepository: RecipesRepository,
        private val mainThreadExecutor: MainThreadExecutor
) : BucketListActivityState() {
    private var displayedInCardFoodstuffPosition = 0

    private lateinit var buttonClose: View
    private lateinit var totalWeightEditText: EditText
    private lateinit var recipeNameEditText: EditText
    private lateinit var saveAsRecipeButton: Button

    private lateinit var totalWeightTextWatcher: TextWatcherAdapter
    private lateinit var recipeNameTextWatcher: TextWatcherAdapter
    private lateinit var commentEditsObserver: CommentLayoutController.CommentEditsObserver

    constructor(
            initialRecipe: Recipe,
            commentLayoutController: CommentLayoutController,
            activity: BaseActivity,
            bucketList: BucketList,
            recipesRepository: RecipesRepository,
            mainThreadExecutor: MainThreadExecutor) :
            this(initialRecipe, null, commentLayoutController, null,
                    activity, bucketList, recipesRepository, mainThreadExecutor)

    constructor(
            savedInstanceState: Bundle,
            commentLayoutController: CommentLayoutController,
            activity: BaseActivity,
            bucketList: BucketList,
            recipesRepository: RecipesRepository,
            mainThreadExecutor: MainThreadExecutor) :
            this(savedInstanceState.getParcelable(EXTRA_INITIAL_RECIPE) as Recipe,
                    extractCommentedIngredientPosition(savedInstanceState),
                    commentLayoutController,
                    savedInstanceState,
                    activity, bucketList, recipesRepository, mainThreadExecutor)

    override fun getStateID(): ID = ID.EditingState
    override fun getTitleStringID(): Int =
        when (initialDisplayedRecipe.isFromDB) {
            true -> R.string.bucket_list_title_recipe_editing
            else -> R.string.bucket_list_title_recipe_creation
        }
    override fun getMainConstraintSetDescriptionLayout(): Int = R.layout.activity_bucket_list_main_state_editing
    override fun getConstraintSetDescriptionLayout(): Int = R.layout.activity_bucket_list_state_editing

    override fun saveInstanceState(): Bundle {
        val result = Bundle()
        result.putParcelable(EXTRA_INITIAL_RECIPE, initialDisplayedRecipe)
        val commentedIngredientPosition = commentDialogIngredientPosition ?: -1
        result.putInt(EXTRA_COMMENTED_INGREDIENT_POSITION, commentedIngredientPosition)
        return result
    }

    override fun initImpl() {
        // Close the card if it's open
        CardDialog.findCard(activity)?.dismiss()

        // Reinit comment dialog - the user might've written a long comment
        // and it's bad to lose it
        val ingredientCommentDialog = IngredientCommentDialog.findDialog(activity.supportFragmentManager)
        if (ingredientCommentDialog != null) {
            val pos = commentDialogIngredientPosition
            if (pos != null) {
                initCommentDialog(ingredientCommentDialog, pos)
            } else {
                // We don't know for which position the dialog is shown
                ingredientCommentDialog.dismiss()
                // TODO: report an error
            }
        }

        buttonClose = findViewById(R.id.button_close)
        saveAsRecipeButton = findViewById(R.id.save_as_recipe_button)
        recipeNameEditText = findViewById(R.id.recipe_name_edit_text)
        totalWeightEditText = findViewById(R.id.total_weight_edit_text)

        recipeNameEditText.isEnabled = true
        totalWeightEditText.isEnabled = true

        // If we're not restored and BucketList doesn't contain the initial recipe yet for
        // some reason - let's put it in.
        // If we're restored - we have already put the recipe into BucketList in the past
        // and should do it again, because at this moment BucketList might contain a modified
        // version of our initial recipe.
        if (savedInstanceState == null
                && bucketList.getRecipe() != initialDisplayedRecipe) {
            bucketList.setRecipe(initialDisplayedRecipe)
        }

        buttonClose.setOnClickListener {
            GlobalScope.launch(mainThreadExecutor) {
                onUserExitAttempt()
            }
        }

        saveAsRecipeButton.setOnClickListener {
            bucketList.setComment(bucketList.getComment().trim())
            if (initialDisplayedRecipe.isFromDB) {
                saveAndDisplayRecipe()
            } else {
                createRecipeAndFinish()
            }
        }

        recipeNameTextWatcher = object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable) {
                updateSaveButtonsEnability()

                if (s.toString() == bucketList.getName()) {
                    return
                }
                bucketList.setName(s.toString())
                onRecipeUpdated(getRecipe())
            }
        }
        totalWeightTextWatcher = object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable) {
                updateSaveButtonsEnability()

                var totalWeight = 0f
                if (!s.toString().isEmpty()) {
                    totalWeight = s.toString().toFloat()
                }
                if (totalWeight == bucketList.getTotalWeight()) {
                    return;
                }
                bucketList.setTotalWeight(totalWeight)
                onRecipeUpdated(getRecipe())
            }
        }
        recipeNameEditText.addTextChangedListener(recipeNameTextWatcher)
        totalWeightEditText.addTextChangedListener(totalWeightTextWatcher)

        commentEditsObserver = object : CommentLayoutController.CommentEditsObserver {
            override fun onCommentViewTextEdited(comment: String) {
                val recipe = getRecipe().copy(comment = comment)
                bucketList.setRecipe(recipe)
                onRecipeUpdated(recipe)
            }
        }
        commentLayoutController.setEditable(true)
        commentLayoutController.addCommentEditsObserver(commentEditsObserver)

        updateSaveButtonsEnability()
    }

    private fun saveAndDisplayRecipe() {
        GlobalScope.launch(mainThreadExecutor) {
            val result = recipesRepository.updateRecipe(initialDisplayedRecipe, getRecipe())
            val recipe = when (result) {
                is UpdateRecipeResult.Ok -> {
                    result.recipe
                }
                is UpdateRecipeResult.UpdatedRecipeNotFound -> {
                    Toast.makeText(activity, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                    return@launch
                }
            }
            bucketList.clear()
            switchState(BucketListActivityDisplayRecipeState(
                    recipe, commentLayoutController, activity,
                    bucketList, recipesRepository, mainThreadExecutor))
        }
    }

    private fun createRecipeAndFinish() {
        GlobalScope.launch(mainThreadExecutor) {
            val result = recipesRepository.saveRecipe(getRecipe())
            val recipe = when (result) {
                is CreateRecipeResult.Ok -> {
                    result.recipe
                }
                is CreateRecipeResult.FoodstuffDuplicationError -> {
                    Toast.makeText(activity, R.string.foodstuff_already_exists, Toast.LENGTH_LONG).show()
                    return@launch
                }
            }
            bucketList.clear()
            finish(FinishResult.Ok(recipe))
        }
    }

    override fun destroyImpl() {
        buttonClose.setOnClickListener(null)
        saveAsRecipeButton.setOnClickListener(null)
        recipeNameEditText.removeTextChangedListener(recipeNameTextWatcher)
        totalWeightEditText.removeTextChangedListener(totalWeightTextWatcher)
        commentLayoutController.removeCommentEditsObserver(commentEditsObserver)
    }

    override fun getRecipe(): Recipe = bucketList.getRecipe()

    private fun updateSaveButtonsEnability() {
        val weightText = totalWeightEditText.text.toString()
        val name = recipeNameEditText.text.toString().trim()
        saveAsRecipeButton.isEnabled =
                !weightText.isEmpty()
                        && !name.isEmpty()
                        && !FloatUtils.areFloatsEquals(weightText.toDouble(), 0.0)
                        && !bucketList.getList().isEmpty()
    }

    override fun onDisplayedIngredientClicked(ingredient: Ingredient, position: Int) {
        displayedInCardFoodstuffPosition = position
        val cardDialog: CardDialog = CardDialog.showCard(
                activity, ingredient.toWeightedFoodstuff())

        val cardSaveButtonClickListener = Card.OnMainButtonSimpleClickListener { newFoodstuff ->
            CardDialog.hideCard(activity)

            var recipe = getRecipe()
            val oldIngredient = recipe.ingredients[displayedInCardFoodstuffPosition]
            val newIngredient = Ingredient.create(newFoodstuff, oldIngredient.comment)
            val newIngredients = recipe.ingredients.toMutableList()
            newIngredients[displayedInCardFoodstuffPosition] = newIngredient

            var newTotalWeight = 0f
            newIngredients.forEach { newTotalWeight += it.weight }

            recipe = recipe.copy(ingredients = newIngredients, weight = newTotalWeight)
            bucketList.setRecipe(recipe)
            onRecipeUpdated(recipe)
            updateSaveButtonsEnability()
        }

        cardDialog.setUpButton1(cardSaveButtonClickListener, R.string.save)
    }

    override fun onDisplayedIngredientLongClicked(
            ingredient: Ingredient,
            position: Int,
            view: View): Boolean {
        val menu = PopupMenu(activity, view)
        menu.inflate(R.menu.bucket_list_menu)
        menu.show()
        menu.setOnMenuItemClickListener { item: MenuItem ->
            if (item.itemId == R.id.delete_ingredient) {
                var recipe = getRecipe()
                val newIngredients = recipe.ingredients.toMutableList()
                newIngredients.removeAt(position)

                var newTotalWeight = 0f
                newIngredients.forEach { newTotalWeight += it.weight }

                recipe = recipe.copy(ingredients = newIngredients, weight = newTotalWeight)
                bucketList.setRecipe(recipe)
                onRecipeUpdated(bucketList.getRecipe())
                updateSaveButtonsEnability()
                true
            } else if (item.itemId == R.id.comment_ingredient) {
                val dialog = IngredientCommentDialog.showDialog(activity.supportFragmentManager, ingredient.comment)
                initCommentDialog(dialog, position)
                true
            } else {
                false
            }
        }
        return true
    }

    private fun initCommentDialog(dialog: IngredientCommentDialog, position: Int) {
        commentDialogIngredientPosition = position
        dialog.setOnDismissListener {
            commentDialogIngredientPosition = null
        }
        dialog.setOnSaveButtonClickListener { newComment ->
            var recipe = getRecipe()
            val newIngredients = recipe.ingredients.toMutableList()
            newIngredients[position] = newIngredients[position].copy(comment = newComment.trim())
            recipe = recipe.copy(ingredients = newIngredients)
            bucketList.setRecipe(recipe)
            onRecipeUpdated(bucketList.getRecipe())
            dialog.dismiss()
        }
    }

    override fun onActivityBackPressed(): Boolean {
        GlobalScope.launch(mainThreadExecutor) {
            onUserExitAttempt()
        }
        return true
    }

    private suspend fun onUserExitAttempt() {
        if (getRecipe().isFromDB) {
            val recipeBeforeEditing = recipesRepository.getRecipeOfFoodstuff(initialDisplayedRecipe.foodstuff)
            if (recipeBeforeEditing == getRecipe()) {
                // The recipe is unchanged and it's original state will be displayed anyway -
                // let's clean BucketList and display the recipe.
                bucketList.clear()
                switchState(BucketListActivityDisplayRecipeState(
                        recipeBeforeEditing, commentLayoutController, activity,
                        bucketList, recipesRepository, mainThreadExecutor))
                return
            }
        }

        val titleId = if (initialDisplayedRecipe.isFromDB) {
            R.string.cancel_recipe_editing_dialog_title
        } else {
            R.string.cancel_recipe_creation_dialog_title
        }
        val dialog = TwoOptionsDialog.showDialog(
                activity,
                TAG_CANCELLATION_DIALOG,
                titleId,
                R.string.cancel_recipe_dialog_confirmation,
                R.string.cancel_recipe_dialog_cancellation)
        dialog.setOnButtonsClickListener {
            dialog.dismiss()
            when (it) {
                TwoOptionsDialog.ButtonName.POSITIVE -> {
                    bucketList.clear()
                    if (initialDisplayedRecipe.isFromDB) {
                        GlobalScope.launch(mainThreadExecutor) {
                            val recipeBeforeEditing =
                                    recipesRepository.getRecipeOfFoodstuff(initialDisplayedRecipe.foodstuff)
                            if (recipeBeforeEditing == null) {
                                throw IllegalStateException("Initial displayed recipe is from "
                                        + "DB, but it wasn't found within recipes repository. "
                                        + "Recipe: $initialDisplayedRecipe")
                            }
                            switchState(BucketListActivityDisplayRecipeState(
                                    recipeBeforeEditing, commentLayoutController,
                                    activity, bucketList, recipesRepository, mainThreadExecutor))
                        }
                    } else {
                        finish(FinishResult.Canceled)
                    }
                }
                TwoOptionsDialog.ButtonName.NEGATIVE -> {
                    // The dialog is dismissed anyway
                }
            }
        }
    }

    override fun supportsIngredientsAddition(): Boolean = true
    override fun onAddIngredientButtonClicked() {
        // Finish the Activity without cleaning BucketList - filled bucket list
        // effectively keeps all changes and lets the user to add new ingredients into it.
        finish(FinishResult.Canceled)
    }
}

private fun extractCommentedIngredientPosition(savedState: Bundle): Int? {
    val position = savedState.getInt(EXTRA_COMMENTED_INGREDIENT_POSITION, -1)
    return if (position >= 0) {
        position
    } else {
        null
    }
}