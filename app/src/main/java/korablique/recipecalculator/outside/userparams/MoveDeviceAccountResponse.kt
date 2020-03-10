package korablique.recipecalculator.outside.userparams

import com.squareup.moshi.JsonClass
import korablique.recipecalculator.outside.STATUS_OK
import korablique.recipecalculator.outside.ServerErrorResponse
import java.lang.RuntimeException

@JsonClass(generateAdapter = true)
data class MoveDeviceAccountResponse(
        val user_id: String,
        val client_token: String,
        val user_name: String
)
