package korablique.recipecalculator.ui.netsnack

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.material.snackbar.Snackbar
import korablique.recipecalculator.R
import korablique.recipecalculator.outside.network.NetworkStateDispatcher


class NetworkSnackbarController(
        private val context: Context,
        private val parentView: View,
        private val lifecycle: Lifecycle,
        private val networkStateDispatcher: NetworkStateDispatcher) : LifecycleObserver, NetworkStateDispatcher.Observer {
    private var snackbar: Snackbar? = null

    init {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_RESUME)
    fun onResume() {
        networkStateDispatcher.addObserver(this)
        onNetworkAvailabilityChange(networkStateDispatcher.isNetworkAvailable())
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        networkStateDispatcher.removeObserver(this)
        snackbar?.dismiss()
        snackbar = null
    }

    override fun onNetworkAvailabilityChange(available: Boolean) {
        if (networkStateDispatcher.isNetworkAvailable()) {
            snackbar?.dismiss()
            snackbar = null
        } else {
            if (snackbar == null) {
                snackbar = Snackbar.make(
                        parentView,
                        context.getString(R.string.possibly_no_network_connection),
                        Snackbar.LENGTH_LONG)
                snackbar?.show()
            }
        }
    }
}