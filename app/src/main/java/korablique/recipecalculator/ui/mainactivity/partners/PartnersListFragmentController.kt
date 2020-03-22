package korablique.recipecalculator.ui.mainactivity.partners

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.FragmentCallbacks
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.dagger.FragmentScope
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.http.unwrapException
import korablique.recipecalculator.outside.partners.Partner
import korablique.recipecalculator.outside.partners.PartnersRegistry
import korablique.recipecalculator.outside.partners.direct.FoodstuffsCorrespondenceManager
import korablique.recipecalculator.outside.userparams.InteractiveServerUserParamsObtainer
import korablique.recipecalculator.outside.userparams.ObtainResult
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import korablique.recipecalculator.ui.mainactivity.partners.pairing.PairingFragment
import korablique.recipecalculator.ui.netsnack.NetworkSnackbarController
import korablique.recipecalculator.ui.netsnack.NetworkSnackbarControllersFactory
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FOODSTUFF_PARCEL_TO_PARTNER = "FOODSTUFF_PARCEL_TO_PARTNER"

@FragmentScope
class PartnersListFragmentController @Inject constructor(
        private val mainThreadExecutor: MainThreadExecutor,
        private val activity: BaseActivity,
        private val fragment: PartnersListFragment,
        private val fragmentCallbacks: FragmentCallbacks,
        private val userParamsRegistry: ServerUserParamsRegistry,
        private val partnersRegistry: PartnersRegistry,
        private val interactiveServerUserParamsObtainer: InteractiveServerUserParamsObtainer,
        private val foodstuffsCorrespondenceManager: FoodstuffsCorrespondenceManager,
        private val networkSnackbarControllersFactory: NetworkSnackbarControllersFactory)
    : FragmentCallbacks.Observer, PartnersRegistry.Observer, ServerUserParamsRegistry.Observer {
    private val partnersAdapter: PartnersListAdapter = PartnersListAdapter(this::onPartnerClick)
    private lateinit var fragmentView: View
    private lateinit var netSnackbarController: NetworkSnackbarController

    companion object {
        internal fun startToSendFoodstuff(activity: BaseActivity, foodstuff: Foodstuff) {
            val args = Bundle()
            args.putParcelable(FOODSTUFF_PARCEL_TO_PARTNER, foodstuff)
            PartnersListFragment.start(activity, args)
        }
    }

    init {
        fragmentCallbacks.addObserver(this)
        partnersRegistry.addObserver(this)
        userParamsRegistry.addObserver(this)
    }

    override fun onFragmentDestroy() {
        partnersRegistry.removeObserver(this)
        userParamsRegistry.removeObserver(this)
    }

    override fun onFragmentViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        this.fragmentView = fragmentView
        netSnackbarController = networkSnackbarControllersFactory.createFor(fragmentView, fragment.lifecycle)
        onUserParamsChange(userParamsRegistry.getUserParams())

        val recyclerView: RecyclerView =
                fragmentView.findViewById(R.id.partners_list_recycler_view)
        val layoutManager = LinearLayoutManager(fragment.context)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = partnersAdapter

        fragmentView.findViewById<TextView>(R.id.title_text).setText(R.string.partners_list)
        fragmentView.findViewById<View>(R.id.button_close).setOnClickListener {
            fragment.close()
        }
        fragmentView.findViewById<View>(R.id.button_google_sign_in).setOnClickListener {
            fragment.lifecycleScope.launch(mainThreadExecutor) {
                interactiveServerUserParamsObtainer.obtainUserParams()
            }
        }
        fragmentView.findViewById<View>(R.id.add_fab).setOnClickListener {
            fragment.lifecycleScope.launch(mainThreadExecutor) {
                val paramsResult = interactiveServerUserParamsObtainer.obtainUserParams()
                when (paramsResult) {
                    is ObtainResult.Success -> {
                        PairingFragment.start(activity)
                    }
                    is ObtainResult.CanceledByUser -> {
                        // Nothing to do
                    }
                    is ObtainResult.Failure -> {
                        // Crashlytics.logException(paramsResult.exception)
                    }
                }
            }
        }

        updateDisplayedState(fragmentView)
    }

    private fun onPartnerClick(partner: Partner) {
        val foodstuff = fragment.arguments?.getParcelable<Foodstuff>(FOODSTUFF_PARCEL_TO_PARTNER)
        if (foodstuff != null) {
            fragment.lifecycleScope.launch(mainThreadExecutor) {
                foodstuffsCorrespondenceManager.sendFooodstuffToPartner(foodstuff, partner)
                fragment.close()
            }
        }
    }

    override fun onPartnersChanged(
            partners: List<Partner>, newPartners: List<Partner>, removedPartners: List<Partner>) {
        updateDisplayedState(fragmentView)
    }

    override fun onUserParamsChange(userParams: ServerUserParams?) {
        updateDisplayedState(fragmentView)
    }

    private fun updateDisplayedState(view: View) {
        view.findViewById<View>(R.id.not_logged_in_layout).visibility = View.GONE
        view.findViewById<View>(R.id.add_fab).visibility = View.GONE
        view.findViewById<View>(R.id.no_partners_layout).visibility = View.GONE

        // Let's refresh partners list
        fragment.lifecycleScope.launch(mainThreadExecutor) {
            partnersRegistry.requestPartnersFromServer()
        }

        if (userParamsRegistry.getUserParams() == null) {
            view.findViewById<View>(R.id.not_logged_in_layout).visibility = View.VISIBLE
        } else {
            fragmentView.findViewById<View>(R.id.add_fab).visibility = View.VISIBLE
            val partners = partnersRegistry.getPartnersCache()
            if (partners.isEmpty()) {
                view.findViewById<View>(R.id.no_partners_layout).visibility = View.VISIBLE
            } else {
                view.findViewById<View>(R.id.no_partners_layout).visibility = View.GONE
                partnersAdapter.setPartners(partners)
            }
        }
    }
}
