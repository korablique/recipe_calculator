package korablique.recipecalculator.outside.fcm

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateFCMTokenResponse(
        val status: String,
        val error_description: String?
)
