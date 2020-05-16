package korablique.recipecalculator.util

import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.database.HistoryWorker
import korablique.recipecalculator.database.room.DatabaseHolder
import java.util.*

class DBTestingUtils {
    companion object {
        @JvmStatic
        fun clearAllData(
                foodstuffsList: FoodstuffsList,
                historyWorker: HistoryWorker,
                databaseHolder: DatabaseHolder) {
            historyWorker.requestAllHistoryFromDb { historyEntries ->
                val historyEntries = ArrayList(historyEntries)
                historyEntries.forEach { historyWorker.deleteEntryFromHistory(it) }
            }
            foodstuffsList.getAllFoodstuffs({}, { foodstuffs ->
                val foodstuffs = ArrayList(foodstuffs)
                foodstuffs.forEach { foodstuffsList.deleteFoodstuff(it) }
            })
            databaseHolder.database.clearAllTables()
        }
    }
}