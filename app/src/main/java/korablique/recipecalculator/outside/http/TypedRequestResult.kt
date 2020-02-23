package korablique.recipecalculator.outside.http

import java.lang.Exception

sealed class TypedRequestResult<T:Any> {
    data class Success<T:Any>(val result: T, val bodyStr: String) : TypedRequestResult<T>()
    data class Failure<T:Any>(val exception: Exception) : TypedRequestResult<T>()
}