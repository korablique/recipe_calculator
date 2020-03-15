package korablique.recipecalculator.outside.network

import android.content.Context
import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.nhaarman.mockitokotlin2.mock
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE, shadows=[NetworkStateDispatcherTest.ShadowReactiveNetwork::class])
class NetworkStateDispatcherTest {
    @Test
    fun `notifies when network state changes`() {
        val publishSubject = PublishSubject.create<Connectivity>()
        ShadowReactiveNetwork.currentObservable = publishSubject

        var networkAvailable = false
        val dispatcherObserver = object : NetworkStateDispatcher.Observer {
            override fun onNetworkAvailabilityChange(available: Boolean) {
                networkAvailable = available
            }
        }

        val dispatcher = NetworkStateDispatcherImpl(mock(), mock())
        dispatcher.addObserver(dispatcherObserver)

        assertFalse(networkAvailable)
        publishSubject.onNext(Connectivity.Builder().available(true).build())
        assertTrue(networkAvailable)
        publishSubject.onNext(Connectivity.Builder().available(false).build())
        assertFalse(networkAvailable)
    }

    @Test
    fun `notifies ONLY when network changes`() {
        val publishSubject = PublishSubject.create<Connectivity>()
        ShadowReactiveNetwork.currentObservable = publishSubject

        var notificationsCount = 0
        val dispatcherObserver = object : NetworkStateDispatcher.Observer {
            override fun onNetworkAvailabilityChange(available: Boolean) {
                notificationsCount += 1
            }
        }

        val dispatcher = NetworkStateDispatcherImpl(mock(), mock())
        dispatcher.addObserver(dispatcherObserver)

        publishSubject.onNext(Connectivity.Builder().available(true).build())
        assertEquals(1, notificationsCount)
        publishSubject.onNext(Connectivity.Builder().available(true).build())
        assertEquals(1, notificationsCount)

        publishSubject.onNext(Connectivity.Builder().available(false).build())
        assertEquals(2, notificationsCount)
        publishSubject.onNext(Connectivity.Builder().available(false).build())
        assertEquals(2, notificationsCount)
    }

    @Implements(ReactiveNetwork::class)
    class ShadowReactiveNetwork {
        companion object {
            lateinit var currentObservable: Observable<Connectivity>
            @Implementation
            @JvmStatic
            fun observeNetworkConnectivity(contex: Context) = currentObservable
        }
    }
}