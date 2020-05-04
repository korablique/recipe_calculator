package korablique.recipecalculator.ui.bucketlist

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import korablique.recipecalculator.R
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.ui.MyViewHolder
import java.lang.ref.WeakReference
import java.util.*

private const val VIEW_TYPE_INGREDIENT = 0
private const val VIEW_TYPE_ADD_INGREDIENT_BUTTON = 1

class BucketListAdapter(private val context: Context)
    : RecyclerView.Adapter<MyViewHolder>(), AdapterDragHelperCallback.Delegate {

    interface OnItemClickedObserver {
        fun onItemClicked(ingredient: Ingredient, position: Int)
    }

    interface OnItemLongClickedObserver {
        fun onItemLongClicked(ingredient: Ingredient, position: Int, view: View): Boolean
    }

    interface ItemDragAndDropObserver {
        fun onItemDraggedAndDropped(oldPosition: Int, newPosition: Int)
    }

    private val dragHelperCallback = AdapterDragHelperCallback(this)
    private var itemTouchHelper: ItemTouchHelper? = null

    private val ingredients: MutableList<Ingredient> = ArrayList()
    private var onItemClickedObserver: OnItemClickedObserver? = null
    private var onItemLongClickedObserver: OnItemLongClickedObserver? = null
    private var onItemDragAndDropObserver: ItemDragAndDropObserver? = null
    private var onAddIngredientButtonObserver: Runnable? = null

    private var recyclerView = WeakReference<RecyclerView>(null)

    private val ingredientViewHolders: List<RecyclerView.ViewHolder?>
        get() {
            val recyclerView = recyclerView.get() ?: return emptyList()
            val result = mutableListOf<RecyclerView.ViewHolder?>()
            for (index in 0 until ingredients.size) {
                result.add(recyclerView.findViewHolderForAdapterPosition(index))
            }
            return result
        }
    private val ingredientViews: List<ViewGroup?>
        get() = ingredientViewHolders.map { it?.itemView as ViewGroup? }

    private var draggableMode = false

    fun setOnItemClickedObserver(observer: OnItemClickedObserver?) {
        onItemClickedObserver = observer
        ingredientViewHolders.forEachIndexed { index, viewHolder ->
            if (viewHolder != null) {
                initViewClicksProperties(viewHolder)
            }
        }
    }

    fun setOnItemLongClickedObserver(observer: OnItemLongClickedObserver?) {
        onItemLongClickedObserver = observer
        ingredientViewHolders.forEachIndexed { index, viewHolder ->
            if (viewHolder != null) {
                initViewClicksProperties(viewHolder)
            }
        }
    }

    fun initDragAndDrop(observer: ItemDragAndDropObserver?) {
        onItemDragAndDropObserver = observer
        draggableMode = onItemDragAndDropObserver != null
        dragHelperCallback.draggableMode = draggableMode
        ingredientViewHolders.forEachIndexed { index, viewHolder ->
            if (viewHolder != null) {
                initViewClicksProperties(viewHolder)
            }
        }
    }

    fun setUpAddIngredientButton(clickObserver: Runnable?) {
        val wasSet = onAddIngredientButtonObserver != null
        onAddIngredientButtonObserver = clickObserver
        val isSet = onAddIngredientButtonObserver != null
        if (!wasSet && isSet) {
            notifyItemInserted(ingredients.size)
        } else if (wasSet && !isSet) {
            notifyItemRemoved(ingredients.size)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (onAddIngredientButtonObserver != null && position == ingredients.size) {
            VIEW_TYPE_ADD_INGREDIENT_BUTTON
        } else {
            VIEW_TYPE_INGREDIENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return when (viewType) {
            VIEW_TYPE_INGREDIENT -> {
                MyViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.bucket_list_igredient_layout, parent, false))
            }
            VIEW_TYPE_ADD_INGREDIENT_BUTTON -> {
                MyViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.bucket_list_add_ingredient_button, parent, false))
            }
            else -> {
                throw Error("Not supported view type")
            }
        }
    }

    override fun onBindViewHolder(holder: MyViewHolder, displayedPosition: Int) {
        if (onAddIngredientButtonObserver != null && displayedPosition == ingredients.size) {
            holder.item.findViewById<View>(R.id.bucket_list_add_ingredient_button).setOnClickListener {
                onAddIngredientButtonObserver?.run()
            }
            return
        }

        val item = holder.item
        val ingredient = ingredients[displayedPosition]
        setTextViewText(item, R.id.name, ingredient.foodstuff.name)
        setTextViewText(item, R.id.extra_info_block, context.getString(R.string.n_gramms, Math.round(ingredient.weight)))
        if (!ingredient.comment.isEmpty()) {
            item.findViewById<View>(R.id.ingredient_comment).visibility = View.VISIBLE
            setTextViewText(item, R.id.ingredient_comment, ingredient.comment)
        } else {
            item.findViewById<View>(R.id.ingredient_comment).visibility = View.GONE
        }

        initViewClicksProperties(holder)
    }

    private fun initViewClicksProperties(viewHolder: RecyclerView.ViewHolder) {
        val ingredient = ingredients[viewHolder.adapterPosition]
        val view = viewHolder.itemView

        val onItemClickedObserver = onItemClickedObserver
        if (onItemClickedObserver != null) {
            view.setOnClickListener {
                onItemClickedObserver.onItemClicked(ingredient, viewHolder.adapterPosition)
            }
        } else {
            view.setOnClickListener(null)
        }

        val onItemLongClickedObserver = onItemLongClickedObserver
        if (onItemLongClickedObserver != null) {
            view.setOnLongClickListener {
                onItemLongClickedObserver.onItemLongClicked(
                        ingredient, viewHolder.adapterPosition, view)
            }
        } else {
            view.setOnLongClickListener(null)
        }

        makeViewDraggable(view, draggableMode)

        view.findViewById<View>(R.id.drag_handle).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                itemTouchHelper?.startDrag(viewHolder)
            }
            false
        }

        // Always consume long tap so that the long tap listener of the parent item
        // won't get triggered on drag_handle long tap
        view.findViewById<View>(R.id.drag_handle).setOnLongClickListener { true }

        val background = if (onItemClickedObserver != null || onItemLongClickedObserver != null) {
            view.context.getDrawable(R.drawable.bucket_list_ingredient_background_with_ripple)
        } else {
            view.context.getDrawable(R.drawable.bucket_list_ingredient_background_without_ripple)
        }
        view.findViewById<View>(R.id.bucket_list_ingredient_inner_layout).background = background
    }

    private fun makeViewDraggable(view: View, draggable: Boolean) {
        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.bucket_list_ingredient_inner_layout)
        val newConstraintSet = ConstraintSet()
        newConstraintSet.clone(constraintLayout)
        if (draggable) {
            newConstraintSet.setVisibility(R.id.drag_handle, View.VISIBLE)
            newConstraintSet.connect(
                    R.id.extra_info_wrapper_layout, ConstraintSet.RIGHT,
                    R.id.drag_handle, ConstraintSet.LEFT)
        } else {
            newConstraintSet.setVisibility(R.id.drag_handle, View.GONE)
            newConstraintSet.connect(
                    R.id.extra_info_wrapper_layout, ConstraintSet.RIGHT,
                    ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        }
        newConstraintSet.applyTo(constraintLayout)
    }

    override fun getItemCount(): Int {
        return if (onAddIngredientButtonObserver == null) {
            ingredients.size
        } else {
            ingredients.size + 1
        }
    }

    fun setItems(newIngredients: List<Ingredient>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return ingredients.size
            }

            override fun getNewListSize(): Int {
                return newIngredients.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // If 2 items have same foodstuff, they are most likely same items,
                // but with different weight. So, if 2 items have same foodstuffs, we consider
                // them to be same items.
                // Also, we ignore IDs because ingredients and their foodstuffs can be resaved,
                // so we recreate Foodstuffs.
                val lhs = ingredients[oldItemPosition].foodstuff.recreateWithId(0)
                val rhs = newIngredients[newItemPosition].foodstuff.recreateWithId(0)
                return lhs == rhs
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // We ignore IDs because ingredients and their foodstuffs can be resaved,
                // so we recreate Foodstuffs and Ingredients.
                val lhs = ingredients[oldItemPosition].copy(
                        id = 0,
                        foodstuff = ingredients[oldItemPosition].foodstuff.recreateWithId(0))
                val rhs = newIngredients[newItemPosition].copy(
                        id = 0,
                        foodstuff = newIngredients[newItemPosition].foodstuff.recreateWithId(0))
                return lhs == rhs
            }
        })
        ingredients.clear()
        ingredients.addAll(newIngredients)
        diff.dispatchUpdatesTo(this)
    }

    private fun <T> setTextViewText(parent: View, viewId: Int, text: T) {
        (parent.findViewById<View>(viewId) as TextView).text = text.toString()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = WeakReference(recyclerView)

        itemTouchHelper = ItemTouchHelper(dragHelperCallback)
        itemTouchHelper!!.attachToRecyclerView(recyclerView);
    }

    override fun onItemMove(oldPosition: Int, newPosition: Int) {
        onItemDragAndDropObserver?.onItemDraggedAndDropped(oldPosition, newPosition)
    }
}