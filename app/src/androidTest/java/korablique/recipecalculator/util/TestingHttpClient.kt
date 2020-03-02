package korablique.recipecalculator.util

import korablique.recipecalculator.outside.http.HttpClient
import korablique.recipecalculator.outside.http.RequestResult
import korablique.recipecalculator.outside.http.TypedRequestResult
import java.io.IOException
import kotlin.reflect.KClass

class TestingHttpClient : HttpClient() {
    override suspend fun request(url: String, body: String): RequestResult {
        return RequestResult.Failure(IOException("Not supported yet"))
    }


    override suspend fun <T:Any> requestWithTypedResponse(
            url: String,
            type: KClass<T>,
            body: String): TypedRequestResult<T> {
        return TypedRequestResult.Failure(IOException("Not supported yet"))
    }
}