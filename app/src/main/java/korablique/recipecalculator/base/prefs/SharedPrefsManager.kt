package korablique.recipecalculator.base.prefs

import android.content.Context
import android.util.Base64
import korablique.recipecalculator.ui.DecimalUtils
import javax.inject.Inject
import javax.inject.Singleton

private const val VALS_DELIMETER = ";"
private const val VAL_EMPTY_LIST = "empty"

/**
 * Обёртка над SharedPreferences для более удобного использования их в тестах
 * и для добавления удобных методов (например, с возможностью сохранять массивы чисел).
 */
@Singleton
class SharedPrefsManager @Inject constructor(val context: Context) {
    fun putLongList(owner: PrefsOwner, key: String, list: List<Long>) {
        val strList = list.map { it.toString() }
        putStringList(owner, key, strList)
    }

    fun getLongList(owner: PrefsOwner, key: String): List<Long>? {
        return getStringList(owner, key)?.map { it.toLong() }
    }

    fun putFloatList(owner: PrefsOwner, key: String, list: List<Float>, digitsAfterDot: Short) {
        val strList = list.map { DecimalUtils.toDecimalString(it, digitsAfterDot.toInt()) }
        putStringList(owner, key, strList)
    }

    fun getFloatList(owner: PrefsOwner, key: String): List<Float>? {
        return getStringList(owner, key)?.map { it.toFloat() }
    }

    fun putFloat(owner: PrefsOwner, key: String, value: Float) {
        context.getSharedPreferences(owner.fileName, Context.MODE_PRIVATE)
                .edit()
                .putFloat(key, value)
                .apply()
    }

    fun getFloat(owner: PrefsOwner, key: String, default: Float): Float {
        return context.getSharedPreferences(owner.fileName, Context.MODE_PRIVATE)
                .getFloat(key, default)
    }

    fun putStringList(owner: PrefsOwner, key: String, strList: List<String>) {
        val convertedStrs = if (!strList.isEmpty()) {
            strList.map { String(Base64.encode(it.toByteArray(), 0)) }
                    .joinToString(
                            separator = VALS_DELIMETER,
                            prefix = VALS_DELIMETER,
                            postfix = VALS_DELIMETER)
        } else {
            VAL_EMPTY_LIST
        }
        putString(owner, key, convertedStrs)
    }

    fun getStringList(owner: PrefsOwner, key: String): List<String>? {
        val str = getString(owner, key)
        if (str == null || str.isEmpty()) {
            return null
        }
        if (str == VAL_EMPTY_LIST) {
            return emptyList()
        }
        return str
                .substring(1, str.length-1)
                .split(VALS_DELIMETER)
                .map { String(Base64.decode(it, 0)) }
    }

    fun putString(owner: PrefsOwner, key: String, value: String?) {
        val base64 = value?.let { String(Base64.encode(it.toByteArray(), 0)) }
        context.getSharedPreferences(owner.fileName, Context.MODE_PRIVATE)
                .edit()
                .putString(key, base64)
                .apply()
    }

    fun getString(owner: PrefsOwner, key: String, default: String? = null): String? {
        val result = context
                .getSharedPreferences(owner.fileName, Context.MODE_PRIVATE)
                .getString(key, default)
        return result?.let { String(Base64.decode(it, 0)) }
    }

    fun putBool(owner: PrefsOwner, key: String, value: Boolean) {
        context.getSharedPreferences(owner.fileName, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key, value)
                .apply()
    }

    fun getBool(owner: PrefsOwner, key: String, default: Boolean = false): Boolean {
        return context
                .getSharedPreferences(owner.fileName, Context.MODE_PRIVATE)
                .getBoolean(key, default)
    }
}