package korablique.recipecalculator.outside.partners.direct

import android.util.Base64
import android.widget.Toast
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import korablique.recipecalculator.base.CurrentActivityProvider
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.partners.Partner
import javax.inject.Inject
import javax.inject.Singleton

private const val DIRECT_MSG_TYPE_FOODSTUFF = "foodstuff"

@Singleton
class FoodstuffsCorrespondenceManager @Inject constructor(
        private val directMsgsManager: DirectMsgsManager,
        private val foodstuffsList: FoodstuffsList,
        private val currentActivityProvider: CurrentActivityProvider
) : DirectMsgsManager.DirrectMessageReceiver {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    init {
        directMsgsManager.registerReceiver(DIRECT_MSG_TYPE_FOODSTUFF, this)
    }

    suspend fun sendFooodstuffToPartner(foodstuff: Foodstuff, partner: Partner): BroccalcNetJobResult<Unit> {
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
        return directMsgsManager
                .sendDirectMSGToPartner(DIRECT_MSG_TYPE_FOODSTUFF, foodstuffEncoded, partner)
    }

    override fun onNewDirectMessage(msg: String) {
        val foodstuffDecoded = String(Base64.decode(msg, Base64.DEFAULT))
        val foodstuffMsg = moshi.adapter<FoodstuffMsg>(FoodstuffMsg::class.java).fromJson(foodstuffDecoded)
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
                            val currentActivity = currentActivityProvider.currentActivity
                            if (currentActivity != null) {
                                Toast.makeText(currentActivity, "Received ${foodstuff.name}", Toast.LENGTH_LONG).show()
                            }
                        },
                        {
                            // Couldn't save foodstuff, nothing to do
                        })
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