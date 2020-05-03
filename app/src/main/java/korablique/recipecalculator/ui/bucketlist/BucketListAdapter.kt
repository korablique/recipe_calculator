package korablique.recipecalculator.ui.bucketlist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import korablique.recipecalculator.R
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.ui.MyViewHolder
import java.util.*

private const val VIEW_TYPE_INGREDIENT = 0
private const val VIEW_TYPE_ADD_INGREDIENT_BUTTON = 1

class BucketListAdapter(private val context: Context) : RecyclerView.Adapter<MyViewHolder>() {
    interface OnItemClickedObserver {
        fun onItemClicked(ingredient: Ingredient, position: Int) = Unit
    }

    interface OnItemLongClickedObserver {
        fun onItemLongClicked(ingredient: Ingredient, position: Int, view: View): Boolean = false
    }

    private val ingredients: MutableList<Ingredient> = ArrayList()
    private var onItemClickedObserver: OnItemClickedObserver? = null
    private var onItemLongClickedObserver: OnItemLongClickedObserver? = null
    private var onAddIngredientButtonObserver: Runnable? = null

    fun setOnItemClickedObserver(observer: OnItemClickedObserver?) {
        onItemClickedObserver = observer
    }

    fun setOnItemLongClickedObserver(observer: OnItemLongClickedObserver?) {
        onItemLongClickedObserver = observer
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

    fun deinitAddIngredientButton() {
        setUpAddIngredientButton(null)
    }

    override fun getItemViewType(position: Int): Int {
        return if (onAddIngredientButtonObserver != null && position == ingredients.size) {
            VIEW_TYPE_ADD_INGREDIENT_BUTTON
        } else {
            VIEW_TYPE_INGREDIENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return if (viewType == VIEW_TYPE_INGREDIENT) {
            MyViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.bucket_list_igredient_layout, parent, false))
        } else if (viewType == VIEW_TYPE_ADD_INGREDIENT_BUTTON) {
            MyViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.bucket_list_add_ingredient_button, parent, false))
        } else {
            throw Error("Not supported view type")
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
        item.setOnClickListener { onItemClickedObserver?.onItemClicked(ingredient, holder.adapterPosition) }
        item.setOnLongClickListener {
            onItemLongClickedObserver?.onItemLongClicked(
                    ingredient, holder.adapterPosition, holder.itemView)
                    ?: false
        }
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

}