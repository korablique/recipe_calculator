package korablique.recipecalculator.outside.network

import android.content.Context
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import korablique.recipecalculator.base.RxGlobalSubscriptions
import korablique.recipecalculator.base.executors.MainThreadExecutor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStateDispatcherImpl @Inject constructor(
        private val context: Context,
        private val subscriptions: RxGlobalSubscriptions,
        private val mainThreadExecutor: MainThreadExecutor) : NetworkStateDispatcher {
    private val observers = mutableListOf<NetworkStateDispatcher.Observer>()

    private var networkAvailable = false

    init {
        val d = ReactiveNetwork
                .observeNetworkConnectivity(context)
                .observeOn(mainThreadExecutor.asScheduler())
                .subscribe { connectivity ->
                    val wasAvailable = networkAvailable
                    networkAvailable = connectivity.available()
                    if (wasAvailable != networkAvailable) {
                        observers.forEach { it.onNetworkAvailabilityChange(networkAvailable) }
                    }
                }
        subscriptions.add(d)
    }

    override fun addObserver(observer: NetworkStateDispatcher.Observer) {
        observers += observer
    }

    override fun removeObserver(observer: NetworkStateDispatcher.Observer) {
        observers -= observer
    }

    override fun isNetworkAvailable(): Boolean = networkAvailable
}