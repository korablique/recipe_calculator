package korablique.recipecalculator.ui.mainactivity.history.pages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.executors.ComputationThreadsExecutor
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.dagger.ActivityScope
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
    private val views = mutableListOf<View>()

    init {
        // Let's create the zygoteRecyclerView by first inflating the history page layout
        // and then extracting the recycler view from it.
        val throwawayHistoryPage =
                LayoutInflater.from(activity).inflate(
                        R.layout.fragment_history_page_real,
                        activity.findViewById(R.id.fragment_history),
                        false)
        zygoteRecyclerView = throwawayHistoryPage.findViewById(R.id.history_list)
        zygoteRecyclerView.layoutManager = LinearLayoutManager(activity)

        computationThreadsExecutor.execute {
            val initialPool = mutableListOf<View>()
            for (indx in 0 until INITIAL_CAPACITY) {
                initialPool += inflate()
            }
            mainThreadExecutor.execute {
                views.addAll(initialPool)
            }
        }
    }

    private fun inflate(): View {
        return LayoutInflater.from(activity).inflate(
                R.layout.history_item_layout,
                zygoteRecyclerView,
                false) as View
    }

    fun put(view: View) {
        if (view.parent != null) {
            (view.parent as ViewGroup).removeView(view)
        }
        views += view
    }

    fun put(holders: Collection<View>) {
        holders.forEach { put(it) }
    }

    fun take(): View {
        if (views.isEmpty()) {
            return inflate()
        }
        return views.removeAt(views.size-1)
    }
}