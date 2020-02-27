package korablique.recipecalculator.outside.partners

import korablique.recipecalculator.base.CurrentActivityProvider
import korablique.recipecalculator.base.executors.IOExecutor
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.outside.ServerErrorException
import korablique.recipecalculator.outside.fcm.FCMManager
import korablique.recipecalculator.outside.http.HttpClient
import korablique.recipecalculator.outside.http.TypedRequestResult
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val SERV_MSG_PAIRED_WITH_PARTNER = "paired_with_partner"

sealed class GetPartnersResult {
    data class Ok(val partners: List<Partner>) : GetPartnersResult()
    data class Failure(val exception: Exception) : GetPartnersResult()
    object NotLoggedIn : GetPartnersResult()
}

/**
 * "Хранилище" партнёров пользователя.
 */
@Singleton
class PartnersRegistry @Inject constructor(
        private val mainThreadExecutor: MainThreadExecutor,
        private val userParamsRegistry: ServerUserParamsRegistry,
        private val httpClient: HttpClient,
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
    }

    suspend fun getPartners(): GetPartnersResult {
        if (partnersCached) {
            return GetPartnersResult.Ok(cachedPartners)
        }
        return updateAndGetPartners()
    }

    private suspend fun updateAndGetPartners(): GetPartnersResult {
        val userParams = userParamsRegistry.getUserParams()
        if (userParams == null) {
            return GetPartnersResult.NotLoggedIn
        }

        val url = ("https://blazern.me/broccalc/v1/user/list_partners?"
                + "client_token=${userParams.token}&user_id=${userParams.uid}")
        val responseFull = httpClient.requestWithTypedResponse(url, ListPartnersResponseFull::class)
        val response = when (responseFull) {
            is TypedRequestResult.Failure -> {
                return GetPartnersResult.Failure(responseFull.exception)
            }
            is TypedRequestResult.Success -> {
                responseFull.result.simplify()
            }
        }

        when (response) {
            is ListPartnersResponse.ServerError -> {
                return GetPartnersResult.Failure(ServerErrorException(response.err))
            }
            is ListPartnersResponse.ParseError -> {
                return GetPartnersResult.Failure(response.exception)
            }
            is ListPartnersResponse.Ok -> {
                cachedPartners.clear()
                cachedPartners += response.partners
                partnersCached = true
                observers.forEach { it.onPartnersChanged(cachedPartners) }
                return GetPartnersResult.Ok(cachedPartners)
            }
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
            withContext(mainThreadExecutor) {
                updateAndGetPartners()
            }
        }
    }
}