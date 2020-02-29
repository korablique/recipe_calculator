package korablique.recipecalculator.ui.mainactivity.partners.pairing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.BaseFragment
import javax.inject.Inject

private const val TAG = "PairingFragment"

class PairingFragment : BaseFragment() {
    @Inject
    lateinit var controller: PairingFragmentController

    companion object {
        fun start(activity: BaseActivity) {
            val fragment = PairingFragment()
            activity.supportFragmentManager
                    .beginTransaction()
                    .add(R.id.main_fullscreen_container, fragment, TAG)
                    .commit()
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
        val fragmentView = inflater.inflate(R.layout.pairing_fragment, container, false)
        return fragmentView
    }

    override fun shouldCloseOnBack() = true
}