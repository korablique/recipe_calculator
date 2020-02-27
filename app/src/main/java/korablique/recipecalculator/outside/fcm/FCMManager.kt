package korablique.recipecalculator.outside.fcm

import com.crashlytics.android.Crashlytics
import com.google.firebase.iid.FirebaseInstanceId
import korablique.recipecalculator.outside.http.HttpClient
import korablique.recipecalculator.outside.http.TypedRequestResult
import korablique.recipecalculator.outside.network.NetworkStateDispatcher
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import java.lang.IllegalArgumentException
import java.lang.RuntimeException

private const val SERV_FIELD_MSG_TYPE = "msg_type"

@Singleton
class FCMManager @Inject constructor(
        private val networkStateDispatcher: NetworkStateDispatcher,
        private val httpClient: HttpClient,
        private val userParamsRegistry: ServerUserParamsRegistry)
    : NetworkStateDispatcher.Observer, ServerUserParamsRegistry.Observer {

    private val messageReceivers = mutableMapOf<String, MessageReceiver>()

    interface MessageReceiver {
        fun onNewFcmMessage(msg: String)
    }

    init {
        networkStateDispatcher.addObserver(this)
        userParamsRegistry.addObserver(this)
        maybeAcquireTokenAndSendToServer()
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
        val userParams = userParamsRegistry.getUserParams()
        if (!networkStateDispatcher.isNetworkAvailable || userParams == null) {
            return
        }

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@addOnCompleteListener
                }

                val result = task.result
                if (result == null) {
                    throw NullPointerException("Not expecting successful task to have no result")
                }

                sendTokenToServer(result.token, userParams)
            }
    }

    private fun sendTokenToServer(fcmToken: String, userParams: ServerUserParams) {
        val url = ("https://blazern.me/broccalc/v1/user/update_fcm_token?"
                + "client_token=${userParams.token}&user_id=${userParams.uid}&"
                + "fcm_token=$fcmToken")
        GlobalScope.launch {
            val response = httpClient.requestWithTypedResponse(url, UpdateFCMTokenResponse::class)
            when (response) {
                is TypedRequestResult.Failure -> {
                    Crashlytics.logException(response.exception)
                }
                is TypedRequestResult.Success -> {
                    // Nice!
                }
            }
        }
    }

    internal fun onMessageReceived(data: Map<String, String>) {
        val msgType = data[SERV_FIELD_MSG_TYPE]
        if (msgType == null) {
            Crashlytics.logException(RuntimeException("Server FCM message without msg type: $data"))
            return
        }
        val dataAsJson = data.map {
            "\"${it.key}\": \"${it.value}\""
        }.joinToString(separator = ",", prefix = "{", postfix = "}")
        messageReceivers[msgType]?.onNewFcmMessage(dataAsJson)
    }

    fun registerMessageReceiver(msgType: String, messageReceiver: MessageReceiver) {
        val displacedReceiver = messageReceivers.put(msgType, messageReceiver)
        if (displacedReceiver != null) {
            throw IllegalArgumentException("Only 1 receiver for a msg type allowed, "
                    + "second received. Type: $msgType")
        }
    }
}