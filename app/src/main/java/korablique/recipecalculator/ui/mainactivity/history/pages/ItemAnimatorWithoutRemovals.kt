package korablique.recipecalculator.ui.mainactivity.history.pages

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class ItemAnimatorWithoutRemovals : DefaultItemAnimator() {
    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        dispatchRemoveFinished(holder)
        return false
    }
}