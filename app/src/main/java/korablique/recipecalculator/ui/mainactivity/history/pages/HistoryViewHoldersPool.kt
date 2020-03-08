package korablique.recipecalculator.ui.mainactivity.history.pages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.executors.ComputationThreadsExecutor
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.dagger.ActivityScope
import korablique.recipecalculator.ui.MyViewHolder
import javax.inject.Inject

private const val INITIAL_CAPACITY = 150

/**
 * Helper class to recycle View Holders between several RecyclerViews.
 *
 * Inflating R.layout.history_item_layout is expensive, and history view pager
 * wants to inflate a lot of them when you switch between pages.
 *
 * HistoryViewHoldersPool tries to make the inflating much cheaper by inflating a lot of views
 * beforehand and then by serving as an inflated views pool.
 */
@ActivityScope
class HistoryViewHoldersPool @Inject constructor(
        private val computationThreadsExecutor: ComputationThreadsExecutor,
        private val mainThreadExecutor: MainThreadExecutor,
        private val activity: BaseActivity
) {
    /**
     * RecyclerView used as a source of layout params for inflated R.layout.history_item_layout.
     */
    private val zygoteRecyclerView: RecyclerView
    private val holders = mutableListOf<MyViewHolder>()

    init {
        // Let's create the zygoteRecyclerView by first inflating the history page layout
        // and then extracting the recycler view from it.
        val throwawayHistoryPage =
                LayoutInflater.from(activity).inflate(
                        R.layout.fragment_history_page_real,
                        activity.findViewById(R.id.fragment_history),
                        false) as ViewGroup
        zygoteRecyclerView = throwawayHistoryPage.findViewById(R.id.history_list)
        zygoteRecyclerView.layoutManager = LinearLayoutManager(activity)

        computationThreadsExecutor.execute {
            val initialPool = mutableListOf<MyViewHolder>()
            for (indx in 0 until INITIAL_CAPACITY) {
                val view = inflate()
                initialPool += MyViewHolder(view)
            }
            mainThreadExecutor.execute {
                holders.addAll(initialPool)
            }
        }
    }

    private fun inflate(): ViewGroup {
        return LayoutInflater.from(activity).inflate(
                R.layout.history_item_layout,
                zygoteRecyclerView,
                false) as ViewGroup
    }

    fun put(holder: MyViewHolder) {
        if (holder.item.parent != null) {
            (holder.item.parent as ViewGroup).removeView(holder.item)
        }
        holders += holder
    }

    fun put(holders: Collection<MyViewHolder>) {
        holders.forEach { put(it) }
    }

    fun take(): MyViewHolder {
        if (holders.isEmpty()) {
            return MyViewHolder(inflate())
        }
        return holders.removeAt(holders.size-1)
    }
}