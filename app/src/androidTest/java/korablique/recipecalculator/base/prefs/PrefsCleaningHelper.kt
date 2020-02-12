package korablique.recipecalculator.base.prefs

import android.app.Activity
import android.content.Context

object PrefsCleaningHelper {
    fun cleanAllPrefs(context: Context) {
        PrefsOwner.values().forEach {
            context.getSharedPreferences(it.fileName, Activity.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
        }
    }
}