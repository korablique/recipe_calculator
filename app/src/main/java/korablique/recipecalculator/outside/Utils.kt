package korablique.recipecalculator.outside

import android.content.Context
import korablique.recipecalculator.R

fun serverAddr(context: Context): String {
    return context.getString(R.string.server_address)
}