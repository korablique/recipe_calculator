package korablique.recipecalculator.outside.http

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.Exception

@Singleton
class HttpClientImpl @Inject constructor() : HttpClient {
    private val impl: OkHttpClient = OkHttpClient()
    private val moshi: Moshi

    init {
        moshi = Moshi
                .Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
    }

    override suspend fun request(url: String, body: String): RequestResult = suspendCoroutine { continuation ->
        val request = Request.Builder().url(url).post(body.toRequestBody()).build()

        impl.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                val convertedResponse = Response(response.body?.string())
                continuation.resume(RequestResult.Success(convertedResponse))
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(RequestResult.Failure(e))
            }
        })
    }

    /**
     * Returned TypedRequestResult.Failure will have types:
     * - IOException in case of a network error
     * - NoBodyException when response has no body
     * - ResponseParseException when response could not pe parsed
     */
    // TODO: write tests for it if it ever going to be used
    suspend fun <T:Any> requestWithTypedResponse(
            url: String,
            type: KClass<T>,
            body: String = ""): TypedRequestResult<T> {
        val requestResult = request(url, body)
        when (requestResult) {
            is RequestResult.Failure -> {
                return TypedRequestResult.Failure(requestResult.exception)
            }
            is RequestResult.Success -> {
                val responseStr = requestResult.response.body
                if (responseStr == null) {
                    return TypedRequestResult.Failure(NoBodyException(url, body))
                }

                val typedResponse = try {
                    moshi.adapter<T>(type.java).fromJson(responseStr)
                } catch (e: Exception) {
                    return TypedRequestResult.Failure(
                            ResponseParseException(url, body, responseStr, type, e))
                }
                if (typedResponse == null) {
                    return TypedRequestResult.Failure(
                            ResponseParseException(url, body, responseStr, type, null))
                }
                return TypedRequestResult.Success(typedResponse, responseStr)
            }
        }
    }

    data class NoBodyException(val url: String, val body: String)
        : Exception("Response has no body. Request URL: $url, body: '$body'")

    data class ResponseParseException(
            val url: String,
            val body: String,
            val response: String,
            val targetType: KClass<*>,
            val parentException: Throwable?)
        : Exception("Error parsing response ('$response') as $targetType."
            + "Request URL: $url, body: '$body'", parentException)
}
