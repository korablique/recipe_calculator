package korablique.recipecalculator.outside.thirdparty

import korablique.recipecalculator.base.BaseActivity

sealed class GPAuthResult {
    data class Success(val token: String) : GPAuthResult()
    data class Failure(val exception: Exception) : GPAuthResult()
    object CanceledByUser : GPAuthResult()
}

interface GPAuthorizer {
    suspend fun auth(context: BaseActivity): GPAuthResult
}
