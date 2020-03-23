package korablique.recipecalculator.outside.userparams

import android.content.Context
import com.squareup.moshi.JsonClass
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.executors.IOExecutor
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.base.prefs.PrefsOwner
import korablique.recipecalculator.base.prefs.SharedPrefsManager
import korablique.recipecalculator.model.UserNameProvider
import korablique.recipecalculator.outside.STATUS_ALREADY_REGISTERED
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.http.tryGetServerErrorStatus
import korablique.recipecalculator.outside.http.extractException
import korablique.recipecalculator.outside.serverAddr
import korablique.recipecalculator.outside.thirdparty.GPAuthResult
import korablique.recipecalculator.outside.thirdparty.GPAuthorizer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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
        private val context: Context,
        private val mainThreadExecutor: MainThreadExecutor,
        private val ioExecutor: IOExecutor,
        private val gpAuthorizer: GPAuthorizer,
        private val userNameProvider: UserNameProvider,
        private val httpContext: BroccalcHttpContext,
        private val prefsManager: SharedPrefsManager
): BroccalcHttpContext.ServerErrorsObserver {
    private val observers = mutableListOf<Observer>()
    @Volatile
    private var cachedUserParams: ServerUserParams? = null
        set(value) {
            field = value
            prefsManager.putString(PrefsOwner.USER_PARAMS_REGISTRY, "uid", value?.uid)
            prefsManager.putString(PrefsOwner.USER_PARAMS_REGISTRY, "token", value?.token)
            mainThreadExecutor.execute {
                observers.forEach { it.onUserParamsChange(cachedUserParams) }
            }
        }

    interface Observer {
        fun onUserParamsChange(userParams: ServerUserParams?)
    }

    init {
        val uid = prefsManager.getString(PrefsOwner.USER_PARAMS_REGISTRY, "uid")
        val token = prefsManager.getString(PrefsOwner.USER_PARAMS_REGISTRY, "token")
        if (token != null && uid != null) {
            cachedUserParams = ServerUserParams(uid, token)
        }
        httpContext.addServerErrorsObserver(this)
    }

    suspend fun getUserParamsMaybeRegister(context: BaseActivity) = withContext(ioExecutor) {
        val userParams = cachedUserParams
        if (userParams != null) {
            return@withContext GetWithRegistrationRequestResult.Success(userParams)
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
        val url = ("${serverAddr(context)}/v1/user/register?"
                + "name=$name&social_network_type=gp&social_network_token=$token")

        val response = httpContext.run {
            val response = httpRequestUnwrapped(url, RegisterResponse::class)
            val userParams = ServerUserParams(response.user_id, response.client_token)
            cachedUserParams = userParams
            BroccalcNetJobResult.Ok(userParams)
        }

        return when (response) {
            is BroccalcNetJobResult.Ok -> {
                GetWithRegistrationRequestResult.Success(response.item)
            }
            is BroccalcNetJobResult.Error -> {
                if (STATUS_ALREADY_REGISTERED == response.tryGetServerErrorStatus()) {
                    GetWithRegistrationRequestResult.RegistrationParamsTaken
                } else {
                    GetWithRegistrationRequestResult.Failure(response.extractException())
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
        val url = ("${serverAddr(context)}/v1/user/move_device_account?"
                + "social_network_type=gp&social_network_token=$token")
        val response = httpContext.run {
            val response = httpRequestUnwrapped(url, MoveDeviceAccountResponse::class)
            val userParams = ServerUserParams(response.user_id, response.client_token)
            cachedUserParams = userParams
            BroccalcNetJobResult.Ok(userParams)
        }

        return when (response) {
            is BroccalcNetJobResult.Ok -> {
                GetWithAccountMoveRequestResult.Success(response.item)
            }
            is BroccalcNetJobResult.Error -> {
                GetWithAccountMoveRequestResult.Failure(response.extractException())
            }
        }
    }

    fun getUserParams() = cachedUserParams

    fun addObserver(observer: Observer) {
        observers += observer
    }

    fun removeObserver(observer: Observer) {
        observers -= observer
    }

    // TODO: store the updated name persistently, resend it to server
    //       when previous sending failed and network/cold start happend again.
    suspend fun updateUserName(newName: String): BroccalcNetJobResult<Unit> {
        val userParams = cachedUserParams
        if (userParams == null) {
            return BroccalcNetJobResult.Error.ServerError.NotLoggedIn(null)
        }

        val url = ("${serverAddr(context)}/v1/user/update_user_name?"
                + "client_token=${userParams.token}&"
                + "user_id=${userParams.uid}&"
                + "name=$newName")
        return httpContext.run {
            httpRequest(url, EmptyResponse::class)
            BroccalcNetJobResult.Ok(Unit)
        }
    }

    /**
     * For Java
     */
    fun updateUserNameIgnoreResult(newName: String) {
        GlobalScope.launch(mainThreadExecutor) {
            updateUserName(newName)
        }
    }

    override fun onBroccalcServerError(error: BroccalcNetJobResult.Error.ServerError<*>) {
        if (error is BroccalcNetJobResult.Error.ServerError.NotLoggedIn) {
            cachedUserParams = null
        }
    }
}

@JsonClass(generateAdapter = true)
private data class MoveDeviceAccountResponse(
        val user_id: String,
        val client_token: String,
        val user_name: String
)


@JsonClass(generateAdapter = true)
private data class RegisterResponse(
        val user_id: String,
        val client_token: String
)

@JsonClass(generateAdapter = true)
private class EmptyResponse()
