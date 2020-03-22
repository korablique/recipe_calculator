package korablique.recipecalculator.ui.mainactivity.mainscreen

import android.view.View
import androidx.appcompat.widget.PopupMenu
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.dagger.ActivityScope
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import korablique.recipecalculator.ui.mainactivity.partners.PartnersListFragment
import javax.inject.Inject

/**
 * Temp, because long clicks are a terrible way of handling foodstuffs sending.
 */
@ActivityScope
class TempLongClickedFoodstuffsHandler @Inject constructor(
        private val context: BaseActivity,
        private val userParamsRegistry: ServerUserParamsRegistry) {
    fun onLongClick(foodstuff: Foodstuff, view: View): Boolean {
        if (userParamsRegistry.getUserParams() == null) {
            // Our user is not registered yet
            return false
        }
        val menu = PopupMenu(context, view)
        menu.inflate(R.menu.foodstuff_menu)
        menu.show()
        menu.setOnMenuItemClickListener {
            if (it.itemId == R.id.send_foodstuff_to_partner) {
                PartnersListFragment.startToSendFoodstuff(context, foodstuff)
                return@setOnMenuItemClickListener true
            }
            return@setOnMenuItemClickListener false
        }
        return true
    }
}