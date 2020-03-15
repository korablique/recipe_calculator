package korablique.recipecalculator.outside.partners

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.*
import korablique.recipecalculator.*
import korablique.recipecalculator.outside.fcm.FCMManager
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.http.RequestResult
import korablique.recipecalculator.outside.http.Response
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class PartnersRegistryTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val netStateDispatcher = FakeNetworkStateDispatcher()

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
        on { getUserParams() } doReturn ServerUserParams("uid", "token")
    }

    val httpClient = FakeHttpClient()
    val fcmManager = mock<FCMManager>()

    lateinit var partnersRegistry: PartnersRegistry

    @Before
    fun setUp() {
        httpClient.setResponse(".*list_partners.*") {
            val body = """
                {
                    "status": "ok",
                    "partners": [
                        {
                            "partner_user_id": "uid1",
                            "partner_name": "name1"
                        },
                        {
                            "partner_user_id": "uid2",
                            "partner_name": "name2"
                        }
                    ]
                }
            """.trimIndent()
            RequestResult.Success(Response(body))
        }
        netStateDispatcher.setNetworkAvailable(true)
    }

    private fun initRegistry() {
        partnersRegistry = PartnersRegistry(
                context,
                InstantMainThreadExecutor(),
                netStateDispatcher,
                userParamsRegistry,
                BroccalcHttpContext(httpClient),
                fcmManager)
    }

    @Test
    fun `BroccalcApplication initializes us`() {
        val app = BroccalcApplication()
        assertEquals(1, app.javaClass.declaredFields.filter { it.type == PartnersRegistry::class.java }.size)
    }


    @Test
    fun `partners acquired at init time`() {
        assertEquals(0, httpClient.getRequestsMatching(".*list_partners.*").size)
        initRegistry()
        assertEquals(1, httpClient.getRequestsMatching(".*list_partners.*").size)
    }

    @Test
    fun `partners not acquired at init time if network disabled`() {
        netStateDispatcher.setNetworkAvailable(false)
        initRegistry()
        assertEquals(0, httpClient.getRequestsMatching(".*list_partners.*").size)
    }

    @Test
    fun `partners acquired after init immediately on network switch`() {
        netStateDispatcher.setNetworkAvailable(false)
        initRegistry()

        assertEquals(0, httpClient.getRequestsMatching(".*list_partners.*").size)
        netStateDispatcher.setNetworkAvailable(true)
        assertEquals(1, httpClient.getRequestsMatching(".*list_partners.*").size)
    }

    @Test
    fun `partners request goes to server even when network disabled`() = runBlocking {
        netStateDispatcher.setNetworkAvailable(false)
        initRegistry()

        assertEquals(0, httpClient.getRequestsMatching(".*list_partners.*").size)
        partnersRegistry.getPartners()
        assertEquals(1, httpClient.getRequestsMatching(".*list_partners.*").size)
    }

    @Test
    fun `partners cache used`() = runBlocking {
        initRegistry()

        val initSize = httpClient.getRequestsMatching(".*list_partners.*").size
        partnersRegistry.getPartners()
        assertEquals(initSize, httpClient.getRequestsMatching(".*list_partners.*").size)
    }

    @Test
    fun `proper partners list is obtained`() = runBlocking {
        initRegistry()
        val response = partnersRegistry.getPartners() as BroccalcNetJobResult.Ok
        val partners = response.item
        assertEquals(2, partners.size)
        assertEquals(Partner("uid1", "name1"), partners[0])
        assertEquals(Partner("uid2", "name2"), partners[1])
    }

    @Test
    fun `immediate response NotLoggedIn when no user params`() = runBlocking {
        whenever(userParamsRegistry.getUserParams()).doReturn(null)
        initRegistry()

        assertTrue(partnersRegistry.getPartners() is BroccalcNetJobResult.Error.ServerError.NotLoggedIn)
        assertEquals(0, httpClient.getRequestsMatching(".*list_partners.*").size)
    }

    @Test
    fun `cached partners are erased when user logs out`() = runBlocking {
        initRegistry()

        assertTrue(partnersRegistry.getPartners() is BroccalcNetJobResult.Ok)
        whenever(userParamsRegistry.getUserParams()).doReturn(null)
        userParamsRegistryObservers.forEach { it.onUserParamsChange(null) }
        assertTrue(partnersRegistry.getPartners() is BroccalcNetJobResult.Error.ServerError.NotLoggedIn)
    }

    @Test
    fun `notifies observers when acquires partners`() {
        netStateDispatcher.setNetworkAvailable(false)
        initRegistry()

        val observer = mock<PartnersRegistry.Observer>()
        partnersRegistry.addObserver(observer)

        verify(observer, never()).onPartnersChanged(any())
        netStateDispatcher.setNetworkAvailable(true)
        verify(observer).onPartnersChanged(any())
    }

    @Test
    fun `notifies observers when partners lost because of logout`() {
        initRegistry()

        val observer = mock<PartnersRegistry.Observer>()
        partnersRegistry.addObserver(observer)

        verify(observer, never()).onPartnersChanged(any())
        whenever(userParamsRegistry.getUserParams()).doReturn(null)
        userParamsRegistryObservers.forEach { it.onUserParamsChange(null) }
        verify(observer).onPartnersChanged(any())
    }

    @Test
    fun `sends correct request`() {
        initRegistry()
        val request = httpClient.getRequestsMatching(".*list_partners.*")[0]

        assertTrue(request.body.isEmpty())
        val url = request.url
        val serverUrl = context.getString(R.string.server_address)
        val expectedUrlStart = "$serverUrl/v1/user/list_partners?"
        assertTrue("$serverUrl vs $expectedUrlStart", url.startsWith(expectedUrlStart))

        val userParams = userParamsRegistry.getUserParams()!!
        assertTrue(url.contains("client_token=${userParams.token}"))
        assertTrue(url.contains("user_id=${userParams.uid}"))
    }

    @Test
    fun `subscribes to paired_with_partner`() {
        verify(fcmManager, never()).registerMessageReceiver(any(), any())
        initRegistry()
        verify(fcmManager).registerMessageReceiver(eq("paired_with_partner"), any())
    }

    @Test
    fun `requests partners again when receives a FCM message`() = runBlocking {
        initRegistry()

        val initSize = httpClient.getRequestsMatching(".*list_partners.*").size
        partnersRegistry.onNewFcmMessage("")
        assertEquals(initSize + 1, httpClient.getRequestsMatching(".*list_partners.*").size)
    }
}
