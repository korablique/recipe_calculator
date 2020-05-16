package korablique.recipecalculator.database

import korablique.recipecalculator.model.Foodstuff
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun DatabaseWorker.requestFoodstuffsByIds(ids: List<Long>): List<Foodstuff>
        = suspendCoroutine { continuation ->
    requestFoodstuffsByIds(ids) { continuation.resume(it) }
}
