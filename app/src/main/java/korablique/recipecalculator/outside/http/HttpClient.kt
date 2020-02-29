package korablique.recipecalculator.outside.http

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import java.io.IOException
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
open class HttpClient @Inject constructor() {
    private val impl: OkHttpClient = OkHttpClient()
    private val moshi: Moshi

    init {
        moshi = Moshi
                .Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
    }

    open suspend fun request(url: String, body: String): RequestResult = suspendCoroutine { continuation ->
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

    open suspend fun <T:Any> requestWithTypedResponse(
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
