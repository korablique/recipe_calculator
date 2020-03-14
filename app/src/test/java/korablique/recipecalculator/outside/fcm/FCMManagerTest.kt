package korablique.recipecalculator.outside.fcm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.*
import korablique.recipecalculator.BroccalcApplication
import korablique.recipecalculator.FakeHttpClient
import korablique.recipecalculator.InstantMainThreadExecutor
import korablique.recipecalculator.R
import korablique.recipecalculator.base.prefs.SharedPrefsManager
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.http.RequestResult
import korablique.recipecalculator.outside.http.Response
import korablique.recipecalculator.outside.network.NetworkStateDispatcher
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.IllegalArgumentException

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class FCMManagerTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsManager = spy(SharedPrefsManager(context))

    val netObservers = mutableListOf<NetworkStateDispatcher.Observer>()
    val netStateDispatcher = mock<NetworkStateDispatcher> {
        on { addObserver(any()) } doAnswer {
            netObservers.add(it.arguments[0] as NetworkStateDispatcher.Observer)
            Unit
        }
        on { removeObserver(any()) } doAnswer {
            netObservers.remove(it.arguments[0] as NetworkStateDispatcher.Observer)
            Unit
        }
    }

    val httpClient = FakeHttpClient()
    val httpContext = BroccalcHttpContext(httpClient)

    val userParamsRegistryObservers = mutableListOf<ServerUserParamsRegistry.Observer>()
    val userParamsRegistry = mock<ServerUserParamsRegistry> {
        on { addObserver(any()) } doAnswer {
            userParamsRegistryObservers.add(it.arguments[0] as ServerUserParamsRegistry.Observer)
            Unit
        }
        on { removeObserver(any()) } doAnswer {
            userParamsRegistryObservers.remove(it.arguments[0] as ServerUserParamsRegistry.Observer)
            Unit
        }
    }

    var token: String? = null

    val fcmManagerCreator = {
        FCMManager(
                context, InstantMainThreadExecutor(), prefsManager,
                netStateDispatcher, httpContext, userParamsRegistry,
                FakeFCMTokenProvider { token })
    }
    var fcmManager: FCMManager? = fcmManagerCreator.invoke()

    @Before
    fun setUp() {
        enableNetwork(true)
        setServerFcmResponseStatus("ok")
        setUserParams(ServerUserParams("uid", "token"))
    }

    private fun setUserParams(userParams: ServerUserParams?) {
        whenever(userParamsRegistry.getUserParams()).thenReturn(userParams)
        userParamsRegistryObservers.forEach { it.onUserParamsChange(userParams) }
    }

    @Test
    fun `stores token to prefs`() {
        verify(prefsManager, never()).putString(any(), any(), any())
        updateFCMToken("mynewtoken")
        verify(prefsManager).putString(any(), any(), eq("mynewtoken"))
    }

    @Test
    fun `sends token to server`() {
        assertEquals(0, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
        updateFCMToken("mynewtoken")
        assertEquals(1, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
    }

    @Test
    fun `sends token to server only once when token unchanged`() {
        assertEquals(0, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
        updateFCMToken("mynewtoken")
        updateFCMToken("mynewtoken")
        assertEquals(1, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
    }

    @Test
    fun `sends token to server every time it's updated`() {
        assertEquals(0, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
        updateFCMToken("mynewtoken1")
        assertEquals(1, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
        updateFCMToken("mynewtoken2")
        assertEquals(2, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
    }

    @Test
    fun `sends token to server when network becomes available`() {
        enableNetwork(false)
        updateFCMToken("mynewtoken")

        assertEquals(0, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
        enableNetwork(true)
        assertEquals(1, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
    }

    @Test
    fun `only first network switch causes same token sending`() {
        enableNetwork(false)
        updateFCMToken("mynewtoken")

        assertEquals(0, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
        enableNetwork(true)
        enableNetwork(false)
        enableNetwork(true)
        assertEquals(1, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
    }

    @Test
    fun `BroccalcApplication initializes us`() {
        val app = BroccalcApplication()
        assertEquals(1, app.javaClass.declaredFields.filter { it.type == FCMManager::class.java }.size)
    }

    @Test
    fun `tries to send token on creation`() {
        fcmManager?.destroy()
        fcmManager = null

        updateFCMToken("mynewtoken")
        val initSize = httpClient.getRequestsMatching(".*update_fcm_token.*").size
        fcmManager = fcmManagerCreator.invoke()
        assertEquals(initSize+1, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
    }

    @Test
    fun `sends same token to server again if server doesn't accept it at first`() {
        setServerFcmResponseStatus("internal_error")
        updateFCMToken("mynewtoken")
        setServerFcmResponseStatus("ok")

        val initSize = httpClient.getRequestsMatching(".*update_fcm_token.*").size
        updateFCMToken("mynewtoken")
        assertEquals(initSize+1, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
    }

    @Test
    fun `sends token when user params become available`() {
        setUserParams(null)
        updateFCMToken("mynewtoken")

        assertEquals(0, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
        setUserParams(ServerUserParams("uid", "token"))
        assertEquals(1, httpClient.getRequestsMatching(".*update_fcm_token.*").size)
    }

    @Test
    fun `sends correct request`() {
        updateFCMToken("mynewtoken")
        val request = httpClient.getRequestsMatching(".*update_fcm_token.*")[0]

        assertTrue(request.body.isEmpty())
        val url = request.url
        val serverUrl = context.getString(R.string.server_address)
        val expectedUrlStart = "$serverUrl/v1/user/update_fcm_token?"
        assertTrue("$serverUrl vs $expectedUrlStart", url.startsWith(expectedUrlStart))

        val userParams = userParamsRegistry.getUserParams()!!
        assertTrue(url.contains("client_token=${userParams.token}"))
        assertTrue(url.contains("user_id=${userParams.uid}"))
        assertTrue(url.contains("fcm_token=mynewtoken"))
    }

    @Test
    fun `passes received FCM messages`() {
        updateFCMToken("mynewtoken")

        val msgsReceiver = mock<FCMManager.MessageReceiver>()
        fcmManager!!.registerMessageReceiver("mymsgtype", msgsReceiver)

        verify(msgsReceiver, never()).onNewFcmMessage(any())
        fcmManager!!.onMessageReceived(mapOf(
                SERV_FIELD_MSG_TYPE to "mymsgtype",
                SERV_FIELD_MSG to "hello there"))
        verify(msgsReceiver).onNewFcmMessage("hello there")
    }

    @Test
    fun `received FCM message ignored if there's no type`() {
        updateFCMToken("mynewtoken")

        val msgsReceiver = mock<FCMManager.MessageReceiver>()
        fcmManager!!.registerMessageReceiver("mymsgtype", msgsReceiver)

        fcmManager!!.onMessageReceived(mapOf(SERV_FIELD_MSG to "hello there"))
        verify(msgsReceiver, never()).onNewFcmMessage(any())
    }

    @Test
    fun `received FCM message ignored if there's no msg`() {
        updateFCMToken("mynewtoken")

        val msgsReceiver = mock<FCMManager.MessageReceiver>()
        fcmManager!!.registerMessageReceiver("mymsgtype", msgsReceiver)

        fcmManager!!.onMessageReceived(mapOf(SERV_FIELD_MSG_TYPE to "mymsgtype"))
        verify(msgsReceiver, never()).onNewFcmMessage(any())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `can register only 1 msg receiver for single msg type`() {
        fcmManager!!.registerMessageReceiver("mymsgtype", mock())
        fcmManager!!.registerMessageReceiver("mymsgtype", mock())
    }

    private fun enableNetwork(enabled: Boolean) {
        whenever(netStateDispatcher.isNetworkAvailable).doReturn(enabled)
        netObservers.forEach { it.onNetworkAvailabilityChange(enabled) }
    }

    private fun setServerFcmResponseStatus(status: String) {
        httpClient.setResponse(".*update_fcm_token.*") {
            RequestResult.Success(Response("""{"status": "$status"}"""))
        }
    }

    private fun updateFCMToken(newVal: String) {
        token = newVal
        fcmManager?.onFCMTokenChanged(newVal)
    }

    private class FakeFCMTokenProvider(val token: ()->String?) : FCMTokenObtainer() {
        override suspend fun requestToken() = token()
    }
}
