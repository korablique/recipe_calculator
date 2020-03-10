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
data class ListPartnersResponse(
        val partners: List<ReceivedPartner>
)

fun ReceivedPartner.into(): Partner {
    return Partner(partner_user_id, partner_name)
}