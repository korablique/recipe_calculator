package korablique.recipecalculator.outside.partners.direct

import android.content.Context
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import korablique.recipecalculator.outside.fcm.FCMManager
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.partners.Partner
import korablique.recipecalculator.outside.serverAddr
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import javax.inject.Inject
import javax.inject.Singleton

private const val SERV_MSG_DIRECT_MSG_FROM_PARTNER = "direct_msg_from_partner"

@Singleton
class DirectMsgsManager @Inject constructor(
        private val context: Context,
        private val fcmManager: FCMManager,
        private val userParamsRegistry: ServerUserParamsRegistry,
        private val httpContext: BroccalcHttpContext)
    : FCMManager.MessageReceiver {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val messageReceivers = mutableMapOf<String, DirectMessageReceiver>()

    interface DirectMessageReceiver {
        fun onNewDirectMessage(msg: String)
    }

    init {
        fcmManager.registerMessageReceiver(SERV_MSG_DIRECT_MSG_FROM_PARTNER, this)
    }

    fun registerReceiver(msgType: String, msgReceiver: DirectMessageReceiver) {
        val existingReceiver = messageReceivers.put(msgType, msgReceiver)
        if (existingReceiver != null) {
            throw IllegalArgumentException("Multiple receivers for single msg type not supported")
        }
    }

    override fun onNewFcmMessage(msg: String) {
        val directMsgWrapped = try {
            moshi.adapter<DirectMsgWrapped>(DirectMsgWrapped::class.java).fromJson(msg)
        } catch (e: Exception) {
            null
        }
        if (directMsgWrapped == null) {
            return
        }

        val directMsg = try {
            moshi.adapter<DirectMsg>(DirectMsg::class.java)
                    .fromJson(String(Base64.decode(directMsgWrapped.msg, Base64.DEFAULT)))
        } catch (e: Exception) {
            null
        }
        if (directMsg == null) {
            return
        }

        onNewDirectMsg(directMsg.msg_type, directMsg.msg)
    }

    @VisibleForTesting
    fun onNewDirectMsg(type: String, msg: String) {
        val receiver = messageReceivers[type] ?: return
        receiver.onNewDirectMessage(msg)
    }

    suspend fun sendDirectMSGToPartner(msgType: String, msg: String, partner: Partner): BroccalcNetJobResult<Unit> {
        val userParams = userParamsRegistry.getUserParams()
        if (userParams == null) {
            return BroccalcNetJobResult.Error.ServerError.NotLoggedIn(null)
        }

        val directMsg = DirectMsg(msgType, msg)
        val directMsgJson = moshi
                .adapter<DirectMsg>(DirectMsg::class.java)
                .toJson(directMsg)

        val url = ("${serverAddr(context)}/v1/user/direct_partner_msg?"
                + "client_token=${userParams.token}&user_id=${userParams.uid}&"
                + "partner_user_id=${partner.uid}")

        return httpContext.run {
            httpRequestUnwrapped(
                    url,
                    DirectPartnerMsgResponse::class,
                    Base64.encodeToString(directMsgJson.toByteArray(), Base64.DEFAULT))
            BroccalcNetJobResult.Ok(Unit)
        }
    }
}

@JsonClass(generateAdapter = true)
private data class DirectMsgWrapped(
        val msg: String
)

@JsonClass(generateAdapter = true)
private data class DirectMsg(
        val msg_type: String,
        val msg: String
)

@JsonClass(generateAdapter = true)
private data class DirectPartnerMsgResponse(
        val status: String
)
