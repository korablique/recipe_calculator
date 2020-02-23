package korablique.recipecalculator.outside.userparams

import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.executors.IOExecutor
import korablique.recipecalculator.outside.thirdparty.GPAuthResult
import korablique.recipecalculator.outside.thirdparty.GPAuthorizer
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import korablique.recipecalculator.model.UserNameProvider
import korablique.recipecalculator.outside.STATUS_ALREADY_REGISTERED
import korablique.recipecalculator.outside.STATUS_OK
import korablique.recipecalculator.outside.ServerErrorException
import korablique.recipecalculator.outside.ServerErrorResponse
import korablique.recipecalculator.outside.http.HttpClient
import korablique.recipecalculator.outside.http.TypedRequestResult
import java.lang.Exception

sealed class GetWithRegistrationRequestResult {
    data class Success(val user: ServerUserParams) : GetWithRegistrationRequestResult()
    data class Failure(val exception: Exception) : GetWithRegistrationRequestResult()
    object RegistrationParamsTaken : GetWithRegistrationRequestResult()
    object CanceledByUser : GetWithRegistrationRequestResult()
}

sealed class GetWithAccountMoveRequestResult {
    data class Success(val user: ServerUserParams) : GetWithAccountMoveRequestResult()
    data class Failure(val exception: Exception) : GetWithAccountMoveRequestResult()
    object CanceledByUser : GetWithAccountMoveRequestResult()
}

@Singleton
class ServerUserParamsRegistry @Inject constructor(
        private val ioExecutor: IOExecutor,
        private val gpAuthorizer: GPAuthorizer,
        private val userNameProvider: UserNameProvider,
        private val httpClient: HttpClient
) {
    @Volatile
    private var cachedUserParams: ServerUserParams? = null

    suspend fun getUserParamsMaybeRegister(context: BaseActivity) = withContext(ioExecutor) {
        val cachedUserParams = cachedUserParams
        if (cachedUserParams != null) {
            return@withContext GetWithRegistrationRequestResult.Success(cachedUserParams)
        }

        val gpAuthResult = gpAuthorizer.auth(context)
        when (gpAuthResult) {
            is GPAuthResult.Success -> {
                return@withContext registerWithGpToken(gpAuthResult.token)
            }
            is GPAuthResult.Failure -> {
                return@withContext GetWithRegistrationRequestResult.Failure(gpAuthResult.exception)
            }
            is GPAuthResult.CanceledByUser -> {
                return@withContext GetWithRegistrationRequestResult.CanceledByUser
            }
        }
    }

    private suspend fun registerWithGpToken(token: String): GetWithRegistrationRequestResult {
        val name = userNameProvider.userName.toString()
        val url = ("https://blazern.me/broccalc/v1/user/register?"
                + "name=$name&social_network_type=gp&social_network_token=$token")
        val responseResult = httpClient.requestWithTypedResponse(url, RegisterResponseFull::class)

        val response = when (responseResult) {
            is TypedRequestResult.Failure -> {
                return GetWithRegistrationRequestResult.Failure(responseResult.exception)
            }
            is TypedRequestResult.Success -> {
                responseResult.result.simplify()
            }
        }

        when (response) {
            is RegisterResponse.Ok -> {
                val userParams = ServerUserParams(response.user_id, response.client_token)
                cachedUserParams = userParams
                return GetWithRegistrationRequestResult.Success(userParams)
            }
            is RegisterResponse.ParseError -> {
                return GetWithRegistrationRequestResult.Failure(response.exception)
            }
            is RegisterResponse.ServerError -> {
                if (response.err.status == STATUS_ALREADY_REGISTERED) {
                    return GetWithRegistrationRequestResult.RegistrationParamsTaken
                } else {
                    return GetWithRegistrationRequestResult.Failure(
                            ServerErrorException(response.err))
                }
            }
        }
    }

    suspend fun getUserParamsMaybeMoveAccount(context: BaseActivity) = withContext(ioExecutor) {
        val cachedUserParams = cachedUserParams
        if (cachedUserParams != null) {
            return@withContext GetWithAccountMoveRequestResult.Success(cachedUserParams)
        }

        val gpAuthResult = gpAuthorizer.auth(context)
        when (gpAuthResult) {
            is GPAuthResult.Success -> {
                return@withContext moveAccountWithGpToken(gpAuthResult.token)
            }
            is GPAuthResult.Failure -> {
                return@withContext GetWithAccountMoveRequestResult.Failure(gpAuthResult.exception)
            }
            is GPAuthResult.CanceledByUser -> {
                return@withContext GetWithAccountMoveRequestResult.CanceledByUser
            }
        }
    }

    private suspend fun moveAccountWithGpToken(token: String): GetWithAccountMoveRequestResult {
        val url = ("https://blazern.me/broccalc/v1/user/move_device_account?"
                + "social_network_type=gp&social_network_token=$token")
        val responseResult = httpClient.requestWithTypedResponse(url, MoveDeviceAccountResponseFull::class)

        val response = when (responseResult) {
            is TypedRequestResult.Failure -> {
                return GetWithAccountMoveRequestResult.Failure(responseResult.exception)
            }
            is TypedRequestResult.Success -> {
                responseResult.result.simplify()
            }
        }

        when (response) {
            is MoveDeviceAccountResponse.Ok -> {
                val userParams = ServerUserParams(response.user_id, response.client_token)
                cachedUserParams = userParams
                return GetWithAccountMoveRequestResult.Success(userParams)
            }
            is MoveDeviceAccountResponse.ParseError -> {
                return GetWithAccountMoveRequestResult.Failure(response.exception)
            }
            is MoveDeviceAccountResponse.ServerError -> {
                return GetWithAccountMoveRequestResult.Failure(
                        ServerErrorException(response.err))
            }
        }
    }

}