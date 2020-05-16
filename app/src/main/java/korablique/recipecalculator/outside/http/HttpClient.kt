package korablique.recipecalculator.outside.http

interface HttpClient {
    suspend fun request(url: String, body: String): RequestResult
}