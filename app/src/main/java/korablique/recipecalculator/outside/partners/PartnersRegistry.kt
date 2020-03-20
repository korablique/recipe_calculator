package korablique.recipecalculator.outside.partners

import android.content.Context
import com.squareup.moshi.JsonClass
import korablique.recipecalculator.R
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.outside.fcm.FCMManager
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

private const val SERV_MSG_PAIRED_WITH_PARTNER = "paired_with_partner"

/**
 * "Хранилище" партнёров пользователя.
 * NOTE: на данный момент не хранит партнёров персистентно, на каждом старте приложения
 * заново их получает с сервера.
 * TODO: хранить партнёров персистентно
 */
@Singleton
class PartnersRegistry @Inject constructor(
        private val context: Context,
        private val mainThreadExecutor: MainThreadExecutor,
        private val networkStateDispatcher: NetworkStateDispatcher,
        private val userParamsRegistry: ServerUserParamsRegistry,
        private val broccalcHttpContext: BroccalcHttpContext,
        private val fcmManager: FCMManager
) : FCMManager.MessageReceiver, NetworkStateDispatcher.Observer, ServerUserParamsRegistry.Observer {
    private val cachedPartners = mutableListOf<Partner>()
    private var partnersCached = false

    private val observers = mutableListOf<Observer>()

    interface Observer {
        fun onPartnersChanged(partners: List<Partner>)
    }

    init {
        fcmManager.registerMessageReceiver(SERV_MSG_PAIRED_WITH_PARTNER, this)
        GlobalScope.launch(mainThreadExecutor) {
            if (networkStateDispatcher.isNetworkAvailable()) {
                updateAndGetPartners()
            }
        }
        networkStateDispatcher.addObserver(this)
        userParamsRegistry.addObserver(this)
    }

    fun getPartnersCache(): List<Partner> = cachedPartners

    suspend fun requestPartners(): BroccalcNetJobResult<List<Partner>> {
        if (partnersCached) {
            return BroccalcNetJobResult.Ok(cachedPartners)
        }
        return updateAndGetPartners()
    }

    private suspend fun updateAndGetPartners(): BroccalcNetJobResult<List<Partner>> {
        val userParams = userParamsRegistry.getUserParams()
        if (userParams == null) {
            return BroccalcNetJobResult.Error.ServerError.NotLoggedIn(null)
        }

        val url = ("${serverAddr(context)}/v1/user/list_partners?"
                + "client_token=${userParams.token}&user_id=${userParams.uid}")
        return broccalcHttpContext.run {
            val response = unwrap(httpRequest(url, ListPartnersResponse::class))

            cachedPartners.clear()
            cachedPartners.addAll(response.partners.map { it.into() })
            partnersCached = true
            observers.forEach { it.onPartnersChanged(cachedPartners) }
            BroccalcNetJobResult.Ok(cachedPartners as List<Partner>)
        }
    }

    fun addObserver(oberver: Observer) {
        observers += oberver
    }

    fun removeObserver(oberver: Observer) {
        observers -= oberver
    }

    override fun onNewFcmMessage(msg: String) {
        // We subscribed only to SERV_MSG_PAIRED_WITH_PARTNER
        GlobalScope.launch(mainThreadExecutor) {
            updateAndGetPartners()
        }
    }

    override fun onNetworkAvailabilityChange(available: Boolean) {
        if (partnersCached) {
            return
        }
        GlobalScope.launch(mainThreadExecutor) {
            updateAndGetPartners()
        }
    }

    override fun onUserParamsChange(userParams: ServerUserParams?) {
        cachedPartners.clear()
        partnersCached = false
        observers.forEach { it.onPartnersChanged(cachedPartners) }
    }
}

@JsonClass(generateAdapter = true)
private data class ReceivedPartner(
        val partner_user_id: String,
        val partner_name: String
)

@JsonClass(generateAdapter = true)
private data class ListPartnersResponse(
        val partners: List<ReceivedPartner>
)

private fun ReceivedPartner.into(): Partner {
    return Partner(partner_user_id, partner_name)
}
