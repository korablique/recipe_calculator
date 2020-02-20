package korablique.recipecalculator.ui.mainactivity.partners

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import korablique.recipecalculator.R
import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.FragmentCallbacks
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.dagger.FragmentScope
import korablique.recipecalculator.partners.PartnersRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@FragmentScope
class PartnersListFragmentController @Inject constructor(
        private val fragment: PartnersListFragment,
        private val fragmentCallbacks: FragmentCallbacks,
        private val activityCallbacks: ActivityCallbacks,
        private val partnersRegistry: PartnersRegistry)
    : FragmentCallbacks.Observer, ActivityCallbacks.Observer {

    init {
        fragmentCallbacks.addObserver(this)
        activityCallbacks.addObserver(this)
    }

    override fun onFragmentViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        val recyclerView: RecyclerView =
                fragmentView.findViewById(R.id.partners_list_recycler_view)
        val adapter = PartnersListAdapter()
        val layoutManager = LinearLayoutManager(fragment.context)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        fragment.lifecycleScope.launch {
            val partners = partnersRegistry.getPartners()
            adapter.setPartners(partners)
        }
        
        fragmentView.findViewById<TextView>(R.id.title_text).setText(R.string.partners_list)
        fragmentView.findViewById<View>(R.id.button_close).setOnClickListener {
            fragment.close()
        }
    }

    override fun onFragmentDestroy() {
        activityCallbacks.removeObserver(this)
    }

    override fun onActivityBackPressed(): Boolean {
        return fragment.close()
    }
}