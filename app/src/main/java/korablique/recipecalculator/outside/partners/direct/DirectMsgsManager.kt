package korablique.recipecalculator.outside.partners.direct

import android.util.Base64
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import korablique.recipecalculator.outside.ServerErrorException
import korablique.recipecalculator.outside.fcm.FCMManager
import korablique.recipecalculator.outside.http.HttpClient
import korablique.recipecalculator.outside.http.TypedRequestResult
import korablique.recipecalculator.outside.partners.Partner
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import java.lang.Exception
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton

private const val SERV_MSG_DIRECT_MSG_FROM_PARTNER = "direct_msg_from_partner"

sealed class DirectMsgSendResult {
    object Ok : DirectMsgSendResult()
    object NotLoggedIn : DirectMsgSendResult()
    data class Failure(val exception: Exception) : DirectMsgSendResult()
}

@Singleton
class DirectMsgsManager @Inject constructor(
        private val fcmManager: FCMManager,
        private val userParamsRegistry: ServerUserParamsRegistry,
        private val httpClient: HttpClient)
    : FCMManager.MessageReceiver {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val messageReceivers = mutableMapOf<String, DirrectMessageReceiver>()

    interface DirrectMessageReceiver {
        fun onNewDirectMessage(msg: String)
    }

    init {
        fcmManager.registerMessageReceiver(SERV_MSG_DIRECT_MSG_FROM_PARTNER, this)
    }

    fun registerReceiver(msgType: String, msgReceiver: DirrectMessageReceiver) {
        val existingReceiver = messageReceivers.put(msgType, msgReceiver)
        if (existingReceiver != null) {
            throw IllegalArgumentException("Multiple receivers for single msg type not supported")
        }
    }

    override fun onNewFcmMessage(msg: String) {
        val directMsg = moshi
                .adapter<DirectMsg>(DirectMsg::class.java)
                .fromJson(String(Base64.decode(msg, Base64.DEFAULT)))
        if (directMsg == null) {
            return
        }
        val receiver = messageReceivers[directMsg.msg_type] ?: return
        receiver.onNewDirectMessage(directMsg.msg)
    }

    suspend fun sendDirectMSGToPartner(msgType: String, msg: String, partner: Partner): DirectMsgSendResult {
        val userParams = userParamsRegistry.getUserParams()
        if (userParams == null) {
            return DirectMsgSendResult.NotLoggedIn
        }

        val directMsg = DirectMsg(msgType, msg)
        val directMsgJson = moshi
                .adapter<DirectMsg>(DirectMsg::class.java)
                .toJson(directMsg)

        val url = ("https://blazern.me/broccalc/v1/user/direct_partner_msg?"
                + "client_token=${userParams.token}&user_id=${userParams.uid}&"
                + "partner_user_id=${partner.uid}")
        val responseFull = httpClient.requestWithTypedResponse(
                url,
                DirectPartnerMsgResponseFull::class,
                Base64.encodeToString(directMsgJson.toByteArray(), Base64.DEFAULT))
        val response = when (responseFull) {
            is TypedRequestResult.Failure -> {
                return DirectMsgSendResult.Failure(responseFull.exception)
            }
            is TypedRequestResult.Success -> {
                responseFull.result.simplify()
            }
        }

        when (response) {
            is DirectPartnerMsgResponse.ServerError -> {
                return DirectMsgSendResult.Failure(ServerErrorException(response.err))
            }
            is DirectPartnerMsgResponse.ParseError -> {
                return DirectMsgSendResult.Failure(response.exception)
            }
            is DirectPartnerMsgResponse.Ok -> {
                return DirectMsgSendResult.Ok
            }
        }
    }
}

@JsonClass(generateAdapter = true)
private data class DirectMsg(
        val msg_type: String,
        val msg: String
)