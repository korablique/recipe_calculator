package korablique.recipecalculator.ui.bucketlist

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class AdapterDragHelperCallback(private val delegate: Delegate) : ItemTouchHelper.Callback() {
    interface Delegate {
        fun onItemMove(oldPosition: Int, newPosition: Int)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (source.itemViewType != target.itemViewType) {
            return false
        }
        delegate.onItemMove(source.adapterPosition, target.adapterPosition)
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean = false
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) = Unit
}