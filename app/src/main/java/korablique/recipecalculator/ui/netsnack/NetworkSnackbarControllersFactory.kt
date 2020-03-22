package korablique.recipecalculator.ui.netsnack

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import korablique.recipecalculator.outside.network.NetworkStateDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkSnackbarControllersFactory @Inject constructor(
        private val context: Context,
        private val networkStateDispatcher: NetworkStateDispatcher) {
    fun createFor(view: View, lifecycle: Lifecycle): NetworkSnackbarController {
        return NetworkSnackbarController(context, view, lifecycle, networkStateDispatcher)
    }
}