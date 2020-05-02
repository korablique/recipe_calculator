package korablique.recipecalculator.ui.bucketlist.states

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.bucketlist.BucketListAdapter

abstract class BucketListActivityState {
    private lateinit var delegate: Delegate
    private lateinit var adapter: BucketListAdapter

    enum class ID {
        DisplayState,
        EditingState,
    }

    interface Delegate {
        fun onRecipeUpdated(recipe: Recipe)
        fun <T : View?> findViewById(@IdRes id: Int): T
        fun switchState(newState: BucketListActivityState)
        fun finish(finishResult: FinishResult)
    }
    sealed class FinishResult {
        data class Ok(val recipe: Recipe) : FinishResult()
        object Canceled : FinishResult()
    }

    fun init(delegate: Delegate, adapter: BucketListAdapter) {
        this.delegate = delegate
        this.adapter = adapter
        initImpl()
    }

    fun destroy() {
        destroyImpl()
    }

    protected fun onRecipeUpdated(recipe: Recipe) {
        delegate.onRecipeUpdated(recipe)
    }

    protected fun <T : View?> findViewById(@IdRes id: Int): T {
        return delegate.findViewById<T>(id)
    }

    protected fun finish(finishResult: FinishResult) {
        delegate.finish(finishResult)
    }

    protected fun switchState(newState: BucketListActivityState) {
        delegate.switchState(newState)
    }

    abstract fun saveInstanceState(): Bundle
    abstract fun getStateID(): ID
    abstract fun getTitleStringID(): Int
    abstract fun getMainConstraintSetDescriptionLayout(): Int
    abstract fun getConstraintSetDescriptionLayout(): Int

    protected abstract fun initImpl()
    protected abstract fun destroyImpl()
    abstract fun getRecipe(): Recipe
    open fun onDisplayedIngredientClicked(ingredient: Ingredient, position: Int) {}
    open fun onDisplayedIngredientLongClicked(
            ingredient: Ingredient,
            position: Int,
            view: View): Boolean {
        return false
    }
    open fun onActivityBackPressed(): Boolean = false
    open fun supportsIngredientsAddition(): Boolean = false
    open fun onAddIngredientButtonClicked() {}
}