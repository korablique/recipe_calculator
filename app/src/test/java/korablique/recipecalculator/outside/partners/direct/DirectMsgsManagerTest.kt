package korablique.recipecalculator.outside.partners.direct

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import korablique.recipecalculator.FakeHttpClient
import korablique.recipecalculator.R
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.partners.Partner
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class DirectMsgsManagerTest {
    val context = ApplicationProvider.getApplicationContext<Context>()

    @Test(expected = IllegalArgumentException::class)
    fun `only 1 receiver for 1 type is accepted`() {
        val manager = DirectMsgsManager(mock(), mock(), mock(), mock())
        manager.registerReceiver("mytype", mock())
        manager.registerReceiver("mytype", mock())
    }

    @Test
    fun `network request format`() = runBlocking {
        val httpClient = FakeHttpClient()
        val httpContext = BroccalcHttpContext(httpClient)
        val userParamsRegistry = mock<ServerUserParamsRegistry>()
        whenever(userParamsRegistry.getUserParams()).thenReturn(ServerUserParams("uid", "token"))

        val manager = DirectMsgsManager(context, mock(), userParamsRegistry, httpContext)

        manager.sendDirectMSGToPartner(
                "msgtype",
                "hello there!",
                Partner("partneruid", "partnername"))

        val request = httpClient.getRequestsMatching(".*direct_partner_msg.*")[0]

        assertFalse(request.body.isEmpty())
        val url = request.url
        val serverUrl = context.getString(R.string.server_address)
        val expectedUrlStart = "$serverUrl/v1/user/direct_partner_msg?"
        assertTrue("$serverUrl vs $expectedUrlStart", url.startsWith(expectedUrlStart))

        val userParams = userParamsRegistry.getUserParams()!!
        assertTrue(url.contains("client_token=${userParams.token}"))
        assertTrue(url.contains("user_id=${userParams.uid}"))
        assertTrue(url.contains("partner_user_id=partneruid"))
    }

    @Test
    fun `2 direct msgs managers can communicate`() = runBlocking {
        val httpClient = FakeHttpClient()
        val httpContext = BroccalcHttpContext(httpClient)
        val userParamsRegistry = mock<ServerUserParamsRegistry>()
        whenever(userParamsRegistry.getUserParams()).thenReturn(ServerUserParams("uid", "token"))

        val manager1 = DirectMsgsManager(context, mock(), userParamsRegistry, httpContext)
        val manager2 = DirectMsgsManager(context, mock(), userParamsRegistry, httpContext)

        // Let's ask manager1 to send message to a partner and then intercept it
        manager1.sendDirectMSGToPartner(
                "msgtype",
                "hello there!",
                Partner("partneruid", "partnername"))
        val request = httpClient.getRequestsMatching(".*direct_partner_msg.*")[0]
        val msg = request.body

        // Now let's pass the intercepted message to manager2 and verify it delivers it
        // to correct receiver
        val manager2Observer = mock<DirectMsgsManager.DirectMessageReceiver>()
        manager2.registerReceiver("msgtype", manager2Observer)

        verifyZeroInteractions(manager2Observer)
        manager2.onNewFcmMessage("""{"msg":"$msg"}""")
        verify(manager2Observer).onNewDirectMessage("hello there!")
    }
}