package korablique.recipecalculator.outside

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServerErrorResponse(
        val status: String,
        @Json(name = "error_description")
        val description: String
)