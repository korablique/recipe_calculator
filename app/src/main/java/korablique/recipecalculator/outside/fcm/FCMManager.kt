package korablique.recipecalculator.outside.fcm

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.squareup.moshi.JsonClass
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.base.prefs.PrefsOwner.FCM_MANAGER
import korablique.recipecalculator.base.prefs.SharedPrefsManager
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.network.NetworkStateDispatcher
import korablique.recipecalculator.outside.serverAddr
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@VisibleForTesting
const val SERV_FIELD_MSG_TYPE = "msg_type"
private const val PREF_TOKEN = "token"

@Singleton
class FCMManager @Inject constructor(
        private val context: Context,
        private val mainThreadExecutor: MainThreadExecutor,
        private val prefsManager: SharedPrefsManager,
        private val networkStateDispatcher: NetworkStateDispatcher,
        private val httpContext: BroccalcHttpContext,
        private val userParamsRegistry: ServerUserParamsRegistry,
        private val fcmTokenObtainer: FCMTokenObtainer)
    : NetworkStateDispatcher.Observer, ServerUserParamsRegistry.Observer {
    companion object {
        @VisibleForTesting
        fun createMsgForTests(type: String, otherData: Map<String, String> = emptyMap()): Map<String, String> {
            if (SERV_FIELD_MSG_TYPE in otherData) {
                throw IllegalArgumentException("$SERV_FIELD_MSG_TYPE expected not to be in $otherData")
            }
            val result = otherData.toMutableMap()
            result[SERV_FIELD_MSG_TYPE] = type
            return result
        }
    }

    private val messageReceivers = mutableMapOf<String, MessageReceiver>()

    interface MessageReceiver {
        fun onNewFcmMessage(msg: String)
    }

    init {
        networkStateDispatcher.addObserver(this)
        userParamsRegistry.addObserver(this)
        maybeAcquireTokenAndSendToServer()
    }

    fun destroy() {
        networkStateDispatcher.removeObserver(this)
        userParamsRegistry.removeObserver(this)
    }

    override fun onNetworkAvailabilityChange(available: Boolean) {
        maybeAcquireTokenAndSendToServer()
    }

    override fun onUserParamsChange(userParams: ServerUserParams?) {
        maybeAcquireTokenAndSendToServer()
    }

    internal fun onFCMTokenChanged(token: String) {
        maybeAcquireTokenAndSendToServer()
    }

    private fun maybeAcquireTokenAndSendToServer() {
        GlobalScope.launch(mainThreadExecutor) {
            val userParams = userParamsRegistry.getUserParams()
            if (userParams == null) {
                return@launch
            }

            val token = fcmTokenObtainer.requestToken()
            if (token == null) {
                return@launch
            }

            val lastToken = prefsManager.getString(FCM_MANAGER, PREF_TOKEN)
            if (!networkStateDispatcher.isNetworkAvailable() || token == lastToken) {
                return@launch
            }

            sendTokenToServer(token, userParams)
        }
    }

    private suspend fun sendTokenToServer(fcmToken: String, userParams: ServerUserParams) {
        val url = ("${serverAddr(context)}/v1/user/update_fcm_token?"
                + "client_token=${userParams.token}&user_id=${userParams.uid}&"
                + "fcm_token=$fcmToken")
        val response = httpContext.run {
            httpRequest(url, UpdateFCMTokenResponse::class)
        }
        if (response is BroccalcNetJobResult.Ok) {
            prefsManager.putString(FCM_MANAGER, PREF_TOKEN, fcmToken)
        }
    }

    internal fun onMessageReceived(data: Map<String, String>) {
        val msgType = data[SERV_FIELD_MSG_TYPE]
        if (msgType == null) {
//            Crashlytics.logException(RuntimeException("Server FCM message without msg type: $data"))
            return
        }
        val jsonKeyValues = data.map { """ "${it.key}":"${it.value}" """ }
        val jsonMsg = jsonKeyValues.joinToString(separator = ",\n", prefix = "{", postfix = "}")
        messageReceivers[msgType]?.onNewFcmMessage(jsonMsg)
    }

    fun registerMessageReceiver(msgType: String, messageReceiver: MessageReceiver) {
        val displacedReceiver = messageReceivers.put(msgType, messageReceiver)
        if (displacedReceiver != null) {
            throw IllegalArgumentException("Only 1 receiver for a msg type allowed, "
                    + "second received. Type: $msgType")
        }
    }
}

@JsonClass(generateAdapter = true)
private data class UpdateFCMTokenResponse(
        val status: String
)
