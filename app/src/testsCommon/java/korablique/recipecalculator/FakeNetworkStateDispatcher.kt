package korablique.recipecalculator

import korablique.recipecalculator.outside.network.NetworkStateDispatcher

class FakeNetworkStateDispatcher : NetworkStateDispatcher {
    private val observers = mutableListOf<NetworkStateDispatcher.Observer>()
    private var networkAvailable = false

    override fun addObserver(observer: NetworkStateDispatcher.Observer) {
        observers += observer
    }

    override fun removeObserver(observer: NetworkStateDispatcher.Observer) {
        observers -= observer
    }

    override fun isNetworkAvailable(): Boolean = networkAvailable

    fun setNetworkAvailable(available: Boolean) {
        networkAvailable = available
        observers.forEach { it.onNetworkAvailabilityChange(available) }
    }
}