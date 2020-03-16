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
import korablique.recipecalculator.dagger.FragmentScope
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.http.unwrapException
import korablique.recipecalculator.outside.partners.Partner
import korablique.recipecalculator.outside.partners.PartnersRegistry
import korablique.recipecalculator.outside.partners.direct.FoodstuffsCorrespondenceManager
import korablique.recipecalculator.outside.userparams.InteractiveServerUserParamsObtainer
import korablique.recipecalculator.outside.userparams.ObtainResult
import korablique.recipecalculator.ui.mainactivity.partners.pairing.PairingFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FOODSTUFF_PARCEL_TO_PARTNER = "FOODSTUFF_PARCEL_TO_PARTNER"

@FragmentScope
class PartnersListFragmentController @Inject constructor(
        private val activity: BaseActivity,
        private val fragment: PartnersListFragment,
        private val fragmentCallbacks: FragmentCallbacks,
        private val partnersRegistry: PartnersRegistry,
        private val serverUserParamsObtainer: InteractiveServerUserParamsObtainer,
        private val foodstuffsCorrespondenceManager: FoodstuffsCorrespondenceManager)
    : FragmentCallbacks.Observer, PartnersRegistry.Observer {

    private val partnersAdapter: PartnersListAdapter = PartnersListAdapter(this::onPartnerClick)

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
    }

    override fun onFragmentDestroy() {
        partnersRegistry.removeObserver(this)
    }

    override fun onFragmentViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        val recyclerView: RecyclerView =
                fragmentView.findViewById(R.id.partners_list_recycler_view)
        val layoutManager = LinearLayoutManager(fragment.context)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = partnersAdapter

        fragmentView.findViewById<TextView>(R.id.title_text).setText(R.string.partners_list)
        fragmentView.findViewById<View>(R.id.button_close).setOnClickListener {
            fragment.close()
        }
        fragmentView.findViewById<View>(R.id.add_fab).setOnClickListener {
            fragment.lifecycleScope.launch {
                val paramsResult = serverUserParamsObtainer.obtainUserParams()
                when (paramsResult) {
                    is ObtainResult.Success -> {
                        PairingFragment.start(activity)
                    }
                    is ObtainResult.CanceledByUser -> {
                        Toast.makeText(activity, "Login canceled", Toast.LENGTH_LONG).show()
                    }
                    is ObtainResult.Failure -> {
                        Toast.makeText(activity, "Unexpected failure: ${paramsResult.exception}", Toast.LENGTH_LONG).show()
                        // Crashlytics.logException(paramsResult.exception)
                    }
                }
            }
        }

        fragment.lifecycleScope.launch {
            val partnersResult = partnersRegistry.getPartners()
            when (partnersResult) {
                is BroccalcNetJobResult.Ok -> {
                    updateDisplayedPartners(partnersResult.item, fragmentView)
                }
                is BroccalcNetJobResult.Error.ServerError.NotLoggedIn -> {
                    // TODO: ask user if they want to login or to cancel
                    serverUserParamsObtainer.obtainUserParams()
                }
                else -> {
                    Toast.makeText(activity, "Unexpected failure: partnersResult", Toast.LENGTH_LONG).show()
                    // Crashlytics.logException(partnersResult.exception)
                }
            }
        }
    }

    private fun updateDisplayedPartners(partners: List<Partner>, view: View) {
        partnersAdapter.setPartners(partners)
        if (partners.isEmpty()) {
            view.findViewById<View>(R.id.no_partners_layout).visibility = View.VISIBLE
        } else {
            view.findViewById<View>(R.id.no_partners_layout).visibility = View.GONE
        }
    }

    override fun onPartnersChanged(partners: List<Partner>) {
        updateDisplayedPartners(partners, fragment.requireView())
    }

    private fun onPartnerClick(partner: Partner) {
        val foodstuff = fragment.arguments?.getParcelable<Foodstuff>(FOODSTUFF_PARCEL_TO_PARTNER)
        if (foodstuff != null) {
            fragment.lifecycleScope.launch {
                val sendResult = foodstuffsCorrespondenceManager.sendFooodstuffToPartner(foodstuff, partner)
                when (sendResult) {
                    is BroccalcNetJobResult.Ok -> {
                        Toast.makeText(fragment.context, "Sent!", Toast.LENGTH_LONG).show()
                        fragment.close()
                    }
                    is BroccalcNetJobResult.Error.ServerError.NotLoggedIn -> {
                        Toast.makeText(fragment.context, "Not logged in", Toast.LENGTH_LONG).show()
                        fragment.close()
                    }
                    is BroccalcNetJobResult.Error -> {
                        Toast.makeText(activity, "Unexpected failure: ${sendResult.unwrapException()}", Toast.LENGTH_LONG).show()
                        // Crashlytics.logException(sendResult.exception)
                        fragment.close()
                    }
                }
            }
        }
    }
}
