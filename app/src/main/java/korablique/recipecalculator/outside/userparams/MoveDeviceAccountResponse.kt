package korablique.recipecalculator.outside.userparams

import com.squareup.moshi.JsonClass
import korablique.recipecalculator.outside.STATUS_OK
import korablique.recipecalculator.outside.ServerErrorResponse
import java.lang.RuntimeException

@JsonClass(generateAdapter = true)
data class MoveDeviceAccountResponseFull(
        val status: String,
        val error_description: String?,
        val user_id: String?,
        val client_token: String?,
        val user_name: String?
)

sealed class MoveDeviceAccountResponse {
    data class Ok(
            val user_id: String,
            val client_token: String,
            val name: String) : MoveDeviceAccountResponse()
    data class ServerError(val err: ServerErrorResponse) : MoveDeviceAccountResponse()
    data class ParseError(val exception: RuntimeException) : MoveDeviceAccountResponse()
}

fun MoveDeviceAccountResponseFull.simplify(): MoveDeviceAccountResponse {
    when (status) {
        STATUS_OK -> {
            if (user_id == null || client_token == null || user_name == null) {
                return MoveDeviceAccountResponse.ParseError(
                        IllegalStateException("Lacking some of the ok fields: $this"))
            }
            return MoveDeviceAccountResponse.Ok(user_id, client_token, user_name)
        }
        else -> {
            val descr = error_description ?: ""
            return MoveDeviceAccountResponse.ServerError(ServerErrorResponse(status, descr))
        }
    }
}