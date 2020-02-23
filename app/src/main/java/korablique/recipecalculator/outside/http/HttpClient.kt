package korablique.recipecalculator.outside.http

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

@Singleton
class HttpClient @Inject constructor() {
    private val impl: OkHttpClient = OkHttpClient()
    private val moshi: Moshi

    init {
        moshi = Moshi
                .Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
    }

    suspend fun request(url: String): RequestResult = suspendCoroutine { continuation ->
        val request = Request.Builder().url(url).build()

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

    suspend fun <T:Any> requestWithTypedResponse(url: String, type: KClass<T>): TypedRequestResult<T> {
        val requestResult = request(url)
        when (requestResult) {
            is RequestResult.Failure -> {
                return TypedRequestResult.Failure(requestResult.exception)
            }
            is RequestResult.Success -> {
                val responseStr = requestResult.response.body
                if (responseStr == null) {
                    return TypedRequestResult.Failure(
                            NullPointerException("RequestResult has no body: $requestResult"))
                }

                val typedResponse = moshi
                        .adapter<T>(type.java)
                        .fromJson(responseStr)
                if (typedResponse == null) {
                    return TypedRequestResult.Failure(
                            IllegalStateException("RequestResult was not parsed: $responseStr"))
                }
                return TypedRequestResult.Success(typedResponse, responseStr)
            }
        }
    }
}
