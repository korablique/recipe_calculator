package korablique.recipecalculator.outside.http

import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@LargeTest
class HttpClientTest {
    val httpClient = HttpClientImpl()

    @Test
    fun testSuccessfulRequest() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("hello, world!"))
        server.start()

        val baseUrl = server.url("/my/address")

        val response = httpClient.request(baseUrl.toString(), "mybody")
        Assert.assertTrue(response is RequestResult.Success)

        val successResponse = response as RequestResult.Success
        Assert.assertEquals("hello, world!", successResponse.response.body)
        Assert.assertEquals("mybody", server.takeRequest().body.readUtf8())
    }

    @Test
    fun testSuccessfulRequestWithNoBody() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse())
        server.start()

        val baseUrl = server.url("/my/address")

        val response = httpClient.request(baseUrl.toString(), "mybody")
        Assert.assertTrue(response is RequestResult.Success)

        val successResponse = response as RequestResult.Success
        Assert.assertTrue(
                "" == successResponse.response.body
                    || null === successResponse.response.body)
    }
}
