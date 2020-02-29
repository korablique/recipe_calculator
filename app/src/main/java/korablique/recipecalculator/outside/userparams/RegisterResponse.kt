package korablique.recipecalculator.outside.userparams

import com.squareup.moshi.JsonClass
import korablique.recipecalculator.outside.STATUS_OK
import korablique.recipecalculator.outside.ServerErrorResponse
import java.lang.IllegalStateException
import java.lang.RuntimeException

@JsonClass(generateAdapter = true)
data class RegisterResponseFull(
        val status: String,
        val error_description: String?,
        val user_id: String?,
        val client_token: String?
)

sealed class RegisterResponse {
    data class Ok(
            val user_id: String,
            val client_token: String) : RegisterResponse()
    data class ServerError(val err: ServerErrorResponse) : RegisterResponse()
    data class ParseError(val exception: RuntimeException) : RegisterResponse()
}

fun RegisterResponseFull.simplify(): RegisterResponse {
    when (status) {
        STATUS_OK -> {
            if (user_id == null || client_token == null) {
                return RegisterResponse.ParseError(
                        IllegalStateException("Lacking some of the ok fields: $this"))
            }
            return RegisterResponse.Ok(user_id, client_token)
        }
        else -> {
            val descr = error_description ?: ""
            return RegisterResponse.ServerError(ServerErrorResponse(status, descr))
        }
    }
}