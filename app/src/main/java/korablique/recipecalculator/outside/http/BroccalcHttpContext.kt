package korablique.recipecalculator.outside.http

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import korablique.recipecalculator.outside.STATUS_INVALID_CLIENT_TOKEN
import korablique.recipecalculator.outside.STATUS_OK
import korablique.recipecalculator.outside.STATUS_USER_NOT_FOUND
import korablique.recipecalculator.outside.ServerErrorResponse
import java.lang.RuntimeException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Exception
import kotlin.reflect.KClass

@Singleton
class BroccalcHttpContext @Inject constructor(
        private val httpClient: HttpClient
) {
    private val moshi = Moshi
            .Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    suspend fun <T:Any> run(
            fn: suspend BroccalcHttpContext.()->BroccalcNetJobResult<T>): BroccalcNetJobResult<T> {
        try {
            return fn.invoke(this)
        } catch (e: UnwrapException) {
            return e.result as BroccalcNetJobResult<T>
        }
    }

    fun <T:Any> unwrap(result: BroccalcNetJobResult<T>): T {
        when (result) {
            is BroccalcNetJobResult.Ok -> {
                return result.item
            }
            else -> throw UnwrapException(result)
        }
    }

    suspend fun <T:Any> httpRequestUnwrapped(
            url: String,
            resultType: KClass<T>,
            requestBody: String = ""): T {
        return unwrap(httpRequest(url, resultType, requestBody))
    }

    suspend fun <T:Any> httpRequest(
            url: String,
            resultType: KClass<T>,
            requestBody: String = ""): BroccalcNetJobResult<T> {
        val requestResult = try {
             httpClient.request(url, requestBody)
        } catch (e: Exception) {
            return BroccalcNetJobResult.Error.OtherError(e)
        }

        val responseStr = when (requestResult) {
            is RequestResult.Failure -> {
                return BroccalcNetJobResult.Error.NetError(requestResult.exception)
            }
            is RequestResult.Success -> {
                val responseStr = requestResult.response.body
                if (responseStr == null) {
                    return BroccalcNetJobResult.Error.ResponseFormatError(
                            NullPointerException("Response has no body. Request url: $url, body: $requestBody")
                    )
                }
                responseStr
            }
        }

        val generalizedResponse = try {
            responseStrToType(responseStr, GeneralServerResponse::class)
        } catch (e: Exception) {
            return BroccalcNetJobResult.Error.ResponseFormatError(
                    Exception("Response couldn't be casted to ${GeneralServerResponse::class}."
                            + "Response str: $responseStr", e))
        }

        if (generalizedResponse.status != STATUS_OK) {
            val servErr = try {
                responseStrToType(responseStr, ServerErrorResponse::class)
            } catch (e: java.lang.Exception) {
                return BroccalcNetJobResult.Error.ResponseFormatError(
                        Exception("Couldn't transform response error into ${ServerErrorResponse::class}."
                                + "Response str: $responseStr", e))
            }

            return when (servErr.status) {
                STATUS_INVALID_CLIENT_TOKEN -> {
                    BroccalcNetJobResult.Error.ServerError.NotLoggedIn(servErr)
                }
                STATUS_USER_NOT_FOUND -> {
                    BroccalcNetJobResult.Error.ServerError.NotLoggedIn(servErr)
                }
                else -> {
                    BroccalcNetJobResult.Error.ServerError.Other(servErr)
                }
            }
        }

        val result = try {
            responseStrToType(responseStr, resultType)
        } catch (e: Throwable) {
            return BroccalcNetJobResult.Error.ResponseFormatError(
                    Exception("Couldn't transform OK response into $resultType."
                            + "Response str: $responseStr", e))
        }
        return BroccalcNetJobResult.Ok(result)
    }

    /**
     * CATCH ALL EXCEPTIONS FROM FUNCTION
     */
    private fun <T:Any> responseStrToType(
            responseStr: String,
            resultType: KClass<T>): T {
        val typedResponse =
                moshi.adapter<T>(resultType.java).fromJson(responseStr)
        if (typedResponse == null) {
            throw NullPointerException("Response was parsed as null. "
                    + "Wanted type: $resultType, response: $responseStr")
        }
        return typedResponse
    }
}

private data class UnwrapException(
        val result: BroccalcNetJobResult<*>)
    : RuntimeException("Should never cause a crash")

@JsonClass(generateAdapter = true)
private data class GeneralServerResponse(val status: String)
