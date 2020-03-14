package korablique.recipecalculator

import korablique.recipecalculator.outside.http.HttpClient
import korablique.recipecalculator.outside.http.RequestResult
import korablique.recipecalculator.outside.http.Response
import java.util.regex.Pattern

typealias RegexStringPattern = String

class FakeHttpClient(
        responsesMap: Map<RegexStringPattern, ()->RequestResult> = mapOf()) : HttpClient {

    private val responsesMap = responsesMap.toMutableMap()
    private val requests = mutableListOf<ReceivedRequest>()

    data class ReceivedRequest(val url: String, val body: String)

    override suspend fun request(url: String, body: String): RequestResult {
        requests += ReceivedRequest(url, body)
        for (entry in responsesMap) {
            val pattern = Pattern.compile(entry.key)
            if (pattern.matcher(url).matches()) {
                return entry.value.invoke()
            }
        }
        return RequestResult.Success(Response(null))
    }

    fun setResponse(requestPattern: RegexStringPattern, resp: ()->RequestResult) {
        responsesMap[requestPattern] = resp
    }

    fun getRequestsMatching(urlRegex: RegexStringPattern): List<ReceivedRequest> {
        val pattern = Pattern.compile(urlRegex)
        return requests.filter { pattern.matcher(it.url).matches() }
    }
}