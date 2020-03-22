package korablique.recipecalculator.outside.http

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.squareup.moshi.JsonClass
import korablique.recipecalculator.outside.STATUS_INVALID_CLIENT_TOKEN
import korablique.recipecalculator.outside.STATUS_USER_NOT_FOUND
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.lang.RuntimeException

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class BroccalcHttpContextTest {
    val httpClient = mock<HttpClient>()
    val broccalcHttpContext = BroccalcHttpContext(httpClient)

    @Test
    fun normalScenario() = runBlocking {
        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response("""
                {
                    "some_field": "some value",
                    "status": "ok"
                }""".trimIndent()))
        }
        val response = broccalcHttpContext.run {
            httpRequest("broccalc.com", SomeJsonClass::class)
        }

        assertTrue(response is BroccalcNetJobResult.Ok)
        val okResp = response as BroccalcNetJobResult.Ok
        assertEquals("some value", okResp.item.some_field)
    }

    @Test
    fun errorWhenHttpClientThrows() = runBlocking {
        whenever(httpClient.request(any(), any())).doAnswer {
            throw RuntimeException()
        }
        val response = broccalcHttpContext.run {
            httpRequest("broccalc.com", SomeJsonClass::class)
        }
        assertTrue(response is BroccalcNetJobResult.Error.OtherError)
    }

    @Test
    fun networkError() = runBlocking {
        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Failure(IOException())
        }
        val response = broccalcHttpContext.run {
            httpRequest("broccalc.com", SomeJsonClass::class)
        }
        assertTrue(response is BroccalcNetJobResult.Error.NetError)
    }

    @Test
    fun responseWithoutBody() = runBlocking {
        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response(null))
        }
        val response = broccalcHttpContext.run {
            httpRequest("broccalc.com", SomeJsonClass::class)
        }
        assertTrue(response is BroccalcNetJobResult.Error.ResponseFormatError)
    }

    @Test
    fun responseWithoutStatus() = runBlocking {
        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response("""
                {
                    "some_field": "some value"
                }""".trimIndent()))
        }
        val response = broccalcHttpContext.run {
            httpRequest("broccalc.com", SomeJsonWithoutStatus::class)
        }
        assertTrue(response is BroccalcNetJobResult.Error.ResponseFormatError)
    }

    @Test
    fun serverErrorInvalidClientToken() = runBlocking {
        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response("""
                {
                    "some_field": "some value",
                    "status": "$STATUS_INVALID_CLIENT_TOKEN",
                    "error_description": "wow such error"
                }""".trimIndent()))
        }
        val response = broccalcHttpContext.run {
            httpRequest("broccalc.com", SomeJsonClass::class)
        }
        assertTrue(response is BroccalcNetJobResult.Error.ServerError.NotLoggedIn)
    }

    @Test
    fun serverErrorUserNotFound() = runBlocking {
        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response("""
                {
                    "some_field": "some value",
                    "status": "$STATUS_USER_NOT_FOUND",
                    "error_description": "wow such error"
                }""".trimIndent()))
        }
        val response = broccalcHttpContext.run {
            httpRequest("broccalc.com", SomeJsonClass::class)
        }
        assertTrue(response is BroccalcNetJobResult.Error.ServerError.NotLoggedIn)
    }

    @Test
    fun otherServerError() = runBlocking {
        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response("""
                {
                    "some_field": "some value",
                    "status": "some_new_error",
                    "error_description": "wow such error"
                }""".trimIndent()))
        }
        val response = broccalcHttpContext.run {
            httpRequest("broccalc.com", SomeJsonClass::class)
        }
        assertTrue(response is BroccalcNetJobResult.Error.ServerError.Other)
    }

    @Test
    fun invalidJsonInResponse() = runBlocking {
        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response("""
                {{{{{{{{{{{{{{{{{{
                    "some_field": "some value",
                    "status": "ok"
                }""".trimIndent()))
        }
        val response = broccalcHttpContext.run {
            httpRequest("broccalc.com", SomeJsonClass::class)
        }
        assertTrue(response is BroccalcNetJobResult.Error.ResponseFormatError)
    }

    @Test
    fun responseWithUnexpectedJson() = runBlocking {
        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response("""
                {
                    "some other field": "some value",
                    "status": "ok"
                }""".trimIndent()))
        }
        val response = broccalcHttpContext.run {
            httpRequest("broccalc.com", SomeJsonClass::class)
        }
        assertTrue(response is BroccalcNetJobResult.Error.ResponseFormatError)
    }

    @Test
    fun serverErrorsAreDeliveredToObservers() = runBlocking {
        val receivedErrors = mutableListOf<BroccalcNetJobResult.Error.ServerError<*>>()
        val observer = object : BroccalcHttpContext.ServerErrorsObserver {
            override fun onBroccalcServerError(error: BroccalcNetJobResult.Error.ServerError<*>) {
                receivedErrors += error
            }
        }
        broccalcHttpContext.addServerErrorsObserver(observer)

        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response("""
                {
                    "status": "internal_error",
                    "error_description": "wow such error"
                }""".trimIndent()))
        }
        broccalcHttpContext.run { httpRequest("broccalc.com", SomeJsonClass::class) }
        assertEquals(1, receivedErrors.size)
        assertTrue(receivedErrors.removeAt(0) is BroccalcNetJobResult.Error.ServerError.Other)
        receivedErrors.clear()

        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response("""
                {
                    "status": "$STATUS_INVALID_CLIENT_TOKEN",
                    "error_description": "wow such error"
                }""".trimIndent()))
        }
        broccalcHttpContext.run { httpRequest("broccalc.com", SomeJsonClass::class) }
        assertEquals(1, receivedErrors.size)
        assertTrue(receivedErrors.removeAt(0) is BroccalcNetJobResult.Error.ServerError.NotLoggedIn)
        receivedErrors.clear()

        whenever(httpClient.request(any(), any())).doAnswer {
            RequestResult.Success(Response("""
                {
                    "status": "$STATUS_USER_NOT_FOUND",
                    "error_description": "wow such error"
                }""".trimIndent()))
        }
        broccalcHttpContext.run { httpRequest("broccalc.com", SomeJsonClass::class) }
        assertEquals(1, receivedErrors.size)
        assertTrue(receivedErrors.removeAt(0) is BroccalcNetJobResult.Error.ServerError.NotLoggedIn)
        receivedErrors.clear()
    }
}

@JsonClass(generateAdapter = true)
data class SomeJsonClass(
        val status: String,
        val some_field: String)

@JsonClass(generateAdapter = true)
data class SomeJsonWithoutStatus(
        val some_field: String)
