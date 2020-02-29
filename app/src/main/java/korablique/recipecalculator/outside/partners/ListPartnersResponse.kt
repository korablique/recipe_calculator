package korablique.recipecalculator.outside.partners

import com.squareup.moshi.JsonClass
import korablique.recipecalculator.outside.STATUS_OK
import korablique.recipecalculator.outside.ServerErrorResponse
import java.lang.IllegalStateException
import java.lang.RuntimeException

@JsonClass(generateAdapter = true)
data class ReceivedPartner(
        val partner_user_id: String,
        val partner_name: String
)

@JsonClass(generateAdapter = true)
data class ListPartnersResponseFull(
        val status: String,
        val error_description: String?,
        val partners: List<ReceivedPartner>?
)

sealed class ListPartnersResponse {
    data class Ok(
            val partners: List<Partner>) : ListPartnersResponse()
    data class ServerError(val err: ServerErrorResponse) : ListPartnersResponse()
    data class ParseError(val exception: RuntimeException) : ListPartnersResponse()
}

fun ListPartnersResponseFull.simplify(): ListPartnersResponse {
    when (status) {
        STATUS_OK -> {
            if (partners == null) {
                return ListPartnersResponse.ParseError(
                        IllegalStateException("Lacking some of the ok fields: $this"))
            }
            return ListPartnersResponse.Ok(partners.map { Partner(it.partner_user_id, it.partner_name) })
        }
        else -> {
            val descr = error_description ?: ""
            return ListPartnersResponse.ServerError(ServerErrorResponse(status, descr))
        }
    }
}