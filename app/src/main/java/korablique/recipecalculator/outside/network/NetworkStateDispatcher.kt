package korablique.recipecalculator.outside.network

interface NetworkStateDispatcher {
    interface Observer {
        fun onNetworkAvailabilityChange(available: Boolean)
    }
    fun addObserver(observer: Observer)
    fun removeObserver(observer: Observer)
    fun isNetworkAvailable(): Boolean
}