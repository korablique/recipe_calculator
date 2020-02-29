package korablique.recipecalculator.ui.mainactivity.partners

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.BaseFragment
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.ui.mainactivity.mainscreen.SearchResultsFragment
import javax.inject.Inject

private const val TAG = "PartnersListFragment"

class PartnersListFragment : BaseFragment() {
    @Inject
    lateinit var controller: PartnersListFragmentController

    companion object {
        @JvmOverloads
        fun start(activity: BaseActivity, args: Bundle = Bundle()) {
            val fragment = PartnersListFragment()
            fragment.arguments = args
            activity.supportFragmentManager
                    .beginTransaction()
                    .add(R.id.main_fullscreen_container, fragment, TAG)
                    .commit()
        }

        fun startToSendFoodstuff(activity: BaseActivity, foodstuff: Foodstuff) {
            PartnersListFragmentController.startToSendFoodstuff(activity, foodstuff)
        }
    }

    fun close(): Boolean {
        if (!isVisible) {
            return false
        }
        val fragmentManager = fragmentManager ?: return false
        fragmentManager
                .beginTransaction()
                .remove(this)
                .commit()
        return true
    }

    override fun createView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_partners_list, container, false)
    }

    override fun shouldCloseOnBack() = true
}