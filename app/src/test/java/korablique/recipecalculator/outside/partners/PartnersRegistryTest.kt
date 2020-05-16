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
        partnersRegistry.requestPartnersFromServer()
        assertEquals(1, httpClient.getRequestsMatching(".*list_partners.*").size)
    }

    @Test
    fun `proper partners list is obtained`() = runBlocking {
        initRegistry()
        val response = partnersRegistry.requestPartnersFromServer() as BroccalcNetJobResult.Ok
        val partners = response.item
        assertEquals(2, partners.size)
        assertEquals(Partner("uid1", "name1"), partners[0])
        assertEquals(Partner("uid2", "name2"), partners[1])
    }

    @Test
    fun `immediate response NotLoggedIn when no user params`() = runBlocking {
        whenever(userParamsRegistry.getUserParams()).doReturn(null)
        initRegistry()

        assertTrue(partnersRegistry.requestPartnersFromServer() is BroccalcNetJobResult.Error.ServerError.NotLoggedIn)
        assertEquals(0, httpClient.getRequestsMatching(".*list_partners.*").size)
    }

    @Test
    fun `cached partners are erased when user logs out`() = runBlocking {
        initRegistry()

        assertTrue(partnersRegistry.requestPartnersFromServer() is BroccalcNetJobResult.Ok)
        whenever(userParamsRegistry.getUserParams()).doReturn(null)
        userParamsRegistryObservers.forEach { it.onUserParamsChange(null) }
        assertTrue(partnersRegistry.requestPartnersFromServer() is BroccalcNetJobResult.Error.ServerError.NotLoggedIn)
    }

    @Test
    fun `notifies observers when acquires partners`() {
        netStateDispatcher.setNetworkAvailable(false)
        initRegistry()

        val observer = mock<PartnersRegistry.Observer>()
        partnersRegistry.addObserver(observer)

        verify(observer, never()).onPartnersChanged(any(), any(), any())
        netStateDispatcher.setNetworkAvailable(true)

        val newPartners = listOf(Partner("uid1", "name1"), Partner("uid2", "name2"))
        verify(observer).onPartnersChanged(eq(newPartners), eq(newPartners), eq(emptyList()))
    }

    @Test
    fun `notifies observers when partners lost because of logout`() {
        initRegistry()

        val observer = mock<PartnersRegistry.Observer>()
        partnersRegistry.addObserver(observer)

        verify(observer, never()).onPartnersChanged(any(), any(), any())
        whenever(userParamsRegistry.getUserParams()).doReturn(null)
        userParamsRegistryObservers.forEach { it.onUserParamsChange(null) }

        val lostPartners = listOf(Partner("uid1", "name1"), Partner("uid2", "name2"))
        verify(observer).onPartnersChanged(emptyList(), emptyList(), lostPartners)
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

    @Test
    fun `correct added and removed partners are given on partners change`() = runBlocking {
        // Initial partners
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
        initRegistry()

        // Updated partners
        httpClient.setResponse(".*list_partners.*") {
            val body = """
                {
                    "status": "ok",
                    "partners": [
                        {
                            "partner_user_id": "uid2",
                            "partner_name": "name2"
                        },
                        {
                            "partner_user_id": "uid3",
                            "partner_name": "name3"
                        }
                    ]
                }
            """.trimIndent()
            RequestResult.Success(Response(body))
        }

        val observer = mock<PartnersRegistry.Observer>()
        partnersRegistry.addObserver(observer)
        verify(observer, never()).onPartnersChanged(any(), any(), any())

        partnersRegistry.requestPartnersFromServer()
        verify(observer).onPartnersChanged(
                eq(listOf(Partner("uid2", "name2"), Partner("uid3", "name3"))),
                eq(listOf(Partner("uid3", "name3"))),
                eq(listOf(Partner("uid1", "name1"))))
    }

    @Test
    fun `does not notify when partners are not changed`() = runBlocking {
        initRegistry()

        val observer = mock<PartnersRegistry.Observer>()
        partnersRegistry.addObserver(observer)

        partnersRegistry.requestPartnersFromServer()
        verify(observer, never()).onPartnersChanged(any(), any(), any())
    }

    @Test
    fun `delete partner`() = runBlocking {
        initRegistry()

        // Prepare deletion response
        httpClient.setResponse(".*unpair.*") {
            RequestResult.Success(Response("""{"status":"ok"}"""))
        }
        // Prepare a response without the deleted user
        httpClient.setResponse(".*list_partners.*") {
            val body = """
                {
                    "status": "ok",
                    "partners": [
                        {
                            "partner_user_id": "uid2",
                            "partner_name": "name2"
                        }
                    ]
                }
            """
            RequestResult.Success(Response(body))
        }

        val observer = mock<PartnersRegistry.Observer>()
        partnersRegistry.addObserver(observer)

        // Ask to delete the user
        partnersRegistry.deletePartner("uid1")

        // Verify the user was deleted
        verify(observer).onPartnersChanged(
                eq(listOf(Partner("uid2", "name2"))),
                eq(emptyList()),
                eq(listOf(Partner("uid1", "name1"))))
        assertEquals(listOf(Partner("uid2", "name2")), partnersRegistry.getPartnersCache())

        // Verify a correct request was sent
        assertEquals(1, httpClient.getRequestsMatching(".*unpair.*").size)

        val request = httpClient.getRequestsMatching(".*unpair.*").first()
        assertTrue(request.body.isEmpty())
        val url = request.url
        val serverUrl = context.getString(R.string.server_address)
        val expectedUrlStart = "$serverUrl/v1/user/unpair?"
        assertTrue("$serverUrl vs $expectedUrlStart", url.startsWith(expectedUrlStart))

        val userParams = userParamsRegistry.getUserParams()!!
        assertTrue(url.contains("client_token=${userParams.token}"))
        assertTrue(url.contains("user_id=${userParams.uid}"))
        assertTrue(url.contains("partner_user_id=uid1"))
    }
}
