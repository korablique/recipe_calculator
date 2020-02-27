package korablique.recipecalculator.ui.mainactivity.partners.pairing

import com.squareup.moshi.JsonClass
import korablique.recipecalculator.outside.STATUS_OK
import korablique.recipecalculator.outside.ServerErrorResponse
import java.lang.IllegalStateException
import java.lang.RuntimeException

@JsonClass(generateAdapter = true)
data class StartPairingResponseFull(
        val status: String,
        val error_description: String?,
        val pairing_code: Int?,
        val pairing_code_expiration_date: Long?
)

sealed class StartPairingResponse {
    data class Ok(
            val pairing_code: Int,
            val pairing_code_expiration_date: Long) : StartPairingResponse()
    data class ServerError(val err: ServerErrorResponse) : StartPairingResponse()
    data class ParseError(val exception: RuntimeException) : StartPairingResponse()
}

fun StartPairingResponseFull.simplify(): StartPairingResponse {
    when (status) {
        STATUS_OK -> {
            if (pairing_code == null || pairing_code_expiration_date == null) {
                return StartPairingResponse.ParseError(
                        IllegalStateException("Lacking some of the ok fields: $this"))
            }
            return StartPairingResponse.Ok(pairing_code, pairing_code_expiration_date)
        }
        else -> {
            val descr = error_description ?: ""
            return StartPairingResponse.ServerError(ServerErrorResponse(status, descr))
        }
    }
}