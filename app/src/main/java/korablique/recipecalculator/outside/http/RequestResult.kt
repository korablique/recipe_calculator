package korablique.recipecalculator.outside.http

import java.io.IOException

sealed class RequestResult {
    data class Success(val response: Response) : RequestResult()
    data class Failure(val exception: IOException) : RequestResult()
}