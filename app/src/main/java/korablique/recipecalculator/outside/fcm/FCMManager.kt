package korablique.recipecalculator.outside.fcm

import com.crashlytics.android.Crashlytics
import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.moshi.JsonClass
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.network.NetworkStateDispatcher
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val SERV_FIELD_MSG_TYPE = "msg_type"

@Singleton
class FCMManager @Inject constructor(
        private val networkStateDispatcher: NetworkStateDispatcher,
        private val httpContext: BroccalcHttpContext,
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
            httpContext.execute {
                // Won't handle response, because
                httpRequest(url, UpdateFCMTokenResponse::class)
            }
        }
    }

    internal fun onMessageReceived(data: Map<String, String>) {
        val msgType = data[SERV_FIELD_MSG_TYPE]
        if (msgType == null) {
            Crashlytics.logException(RuntimeException("Server FCM message without msg type: $data"))
            return
        }
        messageReceivers[msgType]?.onNewFcmMessage(data["msg"]!!) // TODO: remove !!
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
