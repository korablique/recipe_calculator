package korablique.recipecalculator.outside.partners.direct

import com.squareup.moshi.JsonClass
import korablique.recipecalculator.outside.STATUS_OK
import korablique.recipecalculator.outside.ServerErrorResponse
import java.lang.RuntimeException

@JsonClass(generateAdapter = true)
data class DirectPartnerMsgResponseFull(
        val status: String,
        val error_description: String?
)

sealed class DirectPartnerMsgResponse {
    object Ok : DirectPartnerMsgResponse()
    data class ServerError(val err: ServerErrorResponse) : DirectPartnerMsgResponse()
    data class ParseError(val exception: RuntimeException) : DirectPartnerMsgResponse()
}

fun DirectPartnerMsgResponseFull.simplify(): DirectPartnerMsgResponse {
    when (status) {
        STATUS_OK -> {
            return DirectPartnerMsgResponse.Ok
        }
        else -> {
            val descr = error_description ?: ""
            return DirectPartnerMsgResponse.ServerError(ServerErrorResponse(status, descr))
        }
    }
}