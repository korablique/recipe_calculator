package korablique.recipecalculator.outside.partners

import android.content.Context
import androidx.annotation.VisibleForTesting
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

@VisibleForTesting
const val SERV_MSG_PAIRED_WITH_PARTNER = "paired_with_partner"

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
        fun onPartnersChanged(
                partners: List<Partner>,
                newPartners: List<Partner>,
                removedPartners: List<Partner>)
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

    suspend fun requestPartnersFromServer(): BroccalcNetJobResult<List<Partner>> {
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

            val updatedPartnersList = response.partners.map { it.into() }
            val newPartners =
                    updatedPartnersList
                            .toMutableList()
                            .apply { removeAll(cachedPartners) }
            val deletedPartners =
                    cachedPartners
                            .toMutableList()
                            .apply { removeAll(updatedPartnersList) }

            val partnersChanged = updatedPartnersList != cachedPartners
            cachedPartners.clear()
            cachedPartners.addAll(updatedPartnersList)
            partnersCached = true
            if (partnersChanged) {
                observers.forEach {
                    it.onPartnersChanged(cachedPartners, newPartners, deletedPartners)
                }
            }
            BroccalcNetJobResult.Ok(cachedPartners as List<Partner>)
        }
    }

    suspend fun deletePartner(partnerUserId: String): BroccalcNetJobResult<Unit> {
        val userParams = userParamsRegistry.getUserParams()
        if (userParams == null) {
            return BroccalcNetJobResult.Error.ServerError.NotLoggedIn(null)
        }

        val url = ("${serverAddr(context)}/v1/user/unpair?"
                + "client_token=${userParams.token}&"
                + "user_id=${userParams.uid}&"
                + "partner_user_id=$partnerUserId")
        return broccalcHttpContext.run {
            httpRequestUnwrapped(url, EmptyResponse::class)
            unwrap(updateAndGetPartners())
            BroccalcNetJobResult.Ok(Unit)
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
        val deletedPartners = cachedPartners.toMutableList()
        cachedPartners.clear()
        partnersCached = false
        observers.forEach { it.onPartnersChanged(cachedPartners, emptyList(), deletedPartners) }
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

@JsonClass(generateAdapter = true)
private class EmptyResponse()

private fun ReceivedPartner.into(): Partner {
    return Partner(partner_user_id, partner_name)
}
