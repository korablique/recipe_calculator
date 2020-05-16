package korablique.recipecalculator.outside.partners.direct

import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import korablique.recipecalculator.R
import korablique.recipecalculator.base.CurrentActivityProvider
import korablique.recipecalculator.base.RxGlobalSubscriptions
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.partners.Partner
import korablique.recipecalculator.ui.mainactivity.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

private const val DIRECT_MSG_TYPE_FOODSTUFF = "foodstuff"

@Singleton
class FoodstuffsCorrespondenceManager @Inject constructor(
        private val directMsgsManager: DirectMsgsManager,
        private val foodstuffsList: FoodstuffsList,
        private val currentActivityProvider: CurrentActivityProvider,
        private val globalSubscriptions: RxGlobalSubscriptions
) : DirectMsgsManager.DirectMessageReceiver {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    companion object {
        @VisibleForTesting
        fun createFoodstuffDirectMsg(
                foodstuff: Foodstuff,
                moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build())
                : Pair<String, String> {
            val foodstuffMsg = FoodstuffMsg(
                    foodstuff.name,
                    (foodstuff.protein * 1000).toInt(),
                    (foodstuff.fats * 1000).toInt(),
                    (foodstuff.carbs * 1000).toInt(),
                    (foodstuff.calories * 1000).toInt()
            )

            val foodstuffJson =
                    moshi.adapter<FoodstuffMsg>(FoodstuffMsg::class.java).toJson(foodstuffMsg)
            val foodstuffEncoded = Base64.encodeToString(foodstuffJson.toByteArray(), Base64.DEFAULT)
            return Pair(DIRECT_MSG_TYPE_FOODSTUFF, foodstuffEncoded)
        }
    }

    init {
        directMsgsManager.registerReceiver(DIRECT_MSG_TYPE_FOODSTUFF, this)
    }

    suspend fun sendFooodstuffToPartner(foodstuff: Foodstuff, partner: Partner): BroccalcNetJobResult<Unit> {
        val msg = createFoodstuffDirectMsg(foodstuff, moshi)
        val result = directMsgsManager.sendDirectMSGToPartner(msg.first, msg.second, partner)

        val view = currentActivityProvider.currentActivity?.contentView
        if (view == null) {
            return result
        }
        when (result) {
            is BroccalcNetJobResult.Ok -> {
                Snackbar.make(view, R.string.foodstuff_is_sent, Snackbar.LENGTH_SHORT).show()
            }
            is BroccalcNetJobResult.Error -> {
                Snackbar.make(view, R.string.something_went_wrong, Snackbar.LENGTH_SHORT).show()
            }
        }
        return result
    }

    override fun onNewDirectMessage(msg: String) {
        val foodstuffDecoded = String(Base64.decode(msg, Base64.DEFAULT))
        val foodstuffMsg = try {
            moshi.adapter<FoodstuffMsg>(FoodstuffMsg::class.java).fromJson(foodstuffDecoded)
        } catch (e: Exception) {
            null
        }
        if (foodstuffMsg == null) {
            return
        }

        val foodstuff = Foodstuff
                .withName(foodstuffMsg.name)
                .withNutrition(
                        foodstuffMsg.protein / 1000.toDouble(),
                        foodstuffMsg.fats / 1000.toDouble(),
                        foodstuffMsg.carbs / 1000.toDouble(),
                        foodstuffMsg.calories / 1000.toDouble())
        val d = foodstuffsList
                .saveFoodstuff(foodstuff)
                .subscribe(
                        {
                            tryShowReceivedFoodstuffSnackbar(it)
                        },
                        {
                            // Couldn't save foodstuff, nothing to do
                        })
        globalSubscriptions.add(d)
    }

    private fun tryShowReceivedFoodstuffSnackbar(foodstuff: Foodstuff) {
        val activity = currentActivityProvider.currentActivity ?: return
        val view = activity.contentView ?: return
        val msg = activity.getString(R.string.foodstuff_is_received, foodstuff.name)
        val snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
        if (activity is MainActivity) {
            snackbar.setAction(R.string.show_received_foodstuff) {
                activity.openFoodstuffCard(foodstuff)
            }
        }
        snackbar.show()
    }
}

@JsonClass(generateAdapter = true)
private data class FoodstuffMsg(
        val name: String,
        val protein: Int,
        val fats: Int,
        val carbs: Int,
        val calories: Int
)
