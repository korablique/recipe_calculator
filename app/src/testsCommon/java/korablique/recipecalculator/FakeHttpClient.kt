package korablique.recipecalculator

import korablique.recipecalculator.outside.http.HttpClient
import korablique.recipecalculator.outside.http.RequestResult
import korablique.recipecalculator.outside.http.Response
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias RegexStringPattern = String
typealias ResponseFun = ()->RequestResult

class FakeHttpClient(
        responsesMap: Map<RegexStringPattern, ResponseFun> = mapOf()) : HttpClient {

    private val responsesMap = responsesMap
            .mapValues { WrappedResponse.Normal(it.value) as WrappedResponse }
            .toMutableMap()
    private val requests = mutableListOf<ReceivedRequest>()

    data class ReceivedRequest(val url: String, val body: String)

    override suspend fun request(url: String, body: String)
            : RequestResult = suspendCoroutine { continuation ->
        requests += ReceivedRequest(url, body)
        for (entry in responsesMap) {
            val pattern = Pattern.compile(entry.key)
            if (pattern.matcher(url).matches()) {
                val wrappedResponse = entry.value
                return@suspendCoroutine when (wrappedResponse) {
                    is WrappedResponse.Normal -> continuation.resume(wrappedResponse.response.invoke())
                    is WrappedResponse.Delayed -> Thread {
                        Thread.sleep(wrappedResponse.delay)
                        continuation.resume(wrappedResponse.response.invoke())
                    }.start()
                }
            }
        }
        continuation.resume(RequestResult.Success(Response(null)))
    }

    fun setResponse(requestPattern: RegexStringPattern, resp: ResponseFun) {
        responsesMap[requestPattern] = WrappedResponse.Normal(resp)
    }

    fun setDelayedResponse(requestPattern: RegexStringPattern, delay: Long, resp: ResponseFun) {
        responsesMap[requestPattern] = WrappedResponse.Delayed(resp, delay)
    }

    fun getRequestsMatching(urlRegex: RegexStringPattern): List<ReceivedRequest> {
        val pattern = Pattern.compile(urlRegex)
        return requests.filter { pattern.matcher(it.url).matches() }
    }
}

private sealed class WrappedResponse {
    data class Normal(val response: ResponseFun) : WrappedResponse()
    data class Delayed(val response: ResponseFun, val delay: Long) : WrappedResponse()
}