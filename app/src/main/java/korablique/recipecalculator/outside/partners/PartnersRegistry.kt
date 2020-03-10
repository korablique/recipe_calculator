package korablique.recipecalculator.outside.partners

import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.outside.fcm.FCMManager
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import javax.inject.Inject
import javax.inject.Singleton

private const val SERV_MSG_PAIRED_WITH_PARTNER = "paired_with_partner"

/**
 * "Хранилище" партнёров пользователя.
 */
@Singleton
class PartnersRegistry @Inject constructor(
        private val mainThreadExecutor: MainThreadExecutor,
        private val userParamsRegistry: ServerUserParamsRegistry,
        private val broccalcHttpContext: BroccalcHttpContext,
        private val fcmManager: FCMManager
) : FCMManager.MessageReceiver {
    private val cachedPartners = mutableListOf<Partner>()
    private var partnersCached = false

    private val observers = mutableListOf<Observer>()

    interface Observer {
        fun onPartnersChanged(partners: List<Partner>)
    }

    init {
        fcmManager.registerMessageReceiver(SERV_MSG_PAIRED_WITH_PARTNER, this)
        suspend {
            updateAndGetPartners()
        }
    }

    suspend fun getPartners(): BroccalcNetJobResult<List<Partner>> {
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

        val url = ("https://blazern.me/broccalc/v1/user/list_partners?"
                + "client_token=${userParams.token}&user_id=${userParams.uid}")
        return broccalcHttpContext.execute {
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
        suspend {
            updateAndGetPartners()
        }
    }
}