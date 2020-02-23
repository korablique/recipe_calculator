package korablique.recipecalculator.ui.mainactivity.partners

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crashlytics.android.Crashlytics
import korablique.recipecalculator.R
import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.FragmentCallbacks
import korablique.recipecalculator.dagger.FragmentScope
import korablique.recipecalculator.outside.partners.PartnersRegistry
import korablique.recipecalculator.outside.userparams.GetWithRegistrationRequestResult
import korablique.recipecalculator.outside.userparams.InteractiveServerUserParamsObtainer
import korablique.recipecalculator.outside.userparams.ObtainResult
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import korablique.recipecalculator.ui.TwoOptionsDialog
import kotlinx.coroutines.launch
import javax.inject.Inject

@FragmentScope
class PartnersListFragmentController @Inject constructor(
        private val activity: BaseActivity,
        private val fragment: PartnersListFragment,
        private val fragmentCallbacks: FragmentCallbacks,
        private val activityCallbacks: ActivityCallbacks,
        private val partnersRegistry: PartnersRegistry,
        private val serverUserParamsObtainer: InteractiveServerUserParamsObtainer)
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

        fragmentView.findViewById<View>(R.id.add_fab).setOnClickListener {
            fragment.lifecycleScope.launch {
                val paramsResult = serverUserParamsObtainer.obtainUserParams()
                when (paramsResult) {
                    is ObtainResult.Success -> {
                        Toast.makeText(activity, "User: ${paramsResult.params}", Toast.LENGTH_LONG).show()
                    }
                    is ObtainResult.CanceledByUser -> {
                        Toast.makeText(activity, "Login canceled", Toast.LENGTH_LONG).show()
                    }
                    is ObtainResult.Failure -> {
                        Toast.makeText(activity, "Unexpected failure: ${paramsResult.exception}", Toast.LENGTH_LONG).show()
                        Crashlytics.logException(paramsResult.exception)
                    }
                }
            }
        }
    }

    override fun onFragmentDestroy() {
        activityCallbacks.removeObserver(this)
    }

    override fun onActivityBackPressed(): Boolean {
        return fragment.close()
    }
}