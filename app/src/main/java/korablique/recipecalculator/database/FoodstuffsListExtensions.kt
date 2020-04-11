package korablique.recipecalculator.database

import korablique.recipecalculator.model.Foodstuff
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class SaveFoodstuffResult {
    data class Ok(val foodstuff: Foodstuff) : SaveFoodstuffResult()
    object DuplicationFailure : SaveFoodstuffResult()
}

suspend fun FoodstuffsList.saveFoodstuffKx(foodstuff: Foodstuff): SaveFoodstuffResult
        = suspendCoroutine { continuation ->
    saveFoodstuff(foodstuff, object : FoodstuffsList.SaveFoodstuffCallback {
        override fun onResult(addedFoodstuff: Foodstuff) {
            continuation.resume(SaveFoodstuffResult.Ok(addedFoodstuff))
        }
        override fun onDuplication() {
            continuation.resume(SaveFoodstuffResult.DuplicationFailure)
        }
    })
}