package korablique.recipecalculator.outside.network

import android.content.Context
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStateDispatcher @Inject constructor(
        private val context: Context) {
    private val observers = mutableListOf<Observer>()

    var isNetworkAvailable = false

    interface Observer {
        fun onNetworkAvailabilityChange(available: Boolean)
    }

    init {
        val d = ReactiveNetwork
                .observeNetworkConnectivity(context)
                .subscribe { connectivity ->
                    val wasAvailable = isNetworkAvailable
                    isNetworkAvailable = connectivity.available()
                    if (wasAvailable != isNetworkAvailable) {
                        observers.forEach { it.onNetworkAvailabilityChange(isNetworkAvailable) }
                    }
                }
        // Some usage of the Disposable - we don't want to cancel the job ever
        d.isDisposed
    }

    fun addObserver(observer: Observer) {
        observers += observer
    }

    fun removeObserver(observer: Observer) {
        observers -= observer
    }
}