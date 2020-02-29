package korablique.recipecalculator.ui.mainactivity.partners.pairing

import com.squareup.moshi.JsonClass
import korablique.recipecalculator.outside.STATUS_OK
import korablique.recipecalculator.outside.ServerErrorResponse
import java.lang.IllegalStateException
import java.lang.RuntimeException

@JsonClass(generateAdapter = true)
data class PairingRequestResponseFull(
        val status: String,
        val error_description: String?
)

sealed class PairingRequestResponse {
    object Ok : PairingRequestResponse()
    data class ServerError(val err: ServerErrorResponse) : PairingRequestResponse()
    data class ParseError(val exception: RuntimeException) : PairingRequestResponse()
}

fun PairingRequestResponseFull.simplify(): PairingRequestResponse {
    when (status) {
        STATUS_OK -> {
            return PairingRequestResponse.Ok
        }
        else -> {
            val descr = error_description ?: ""
            return PairingRequestResponse.ServerError(ServerErrorResponse(status, descr))
        }
    }
}