package korablique.recipecalculator.ui.inputfilters

import android.text.InputFilter
import android.text.Spanned

/**
 * Принимает функцию фильтрации и фильтрует в зависимости от результата её вызова,
 * поэтому "Functional"
 */
class FunctionalInputFilter(private val isAcceptableStr: (String)->Boolean) : InputFilter {
    companion object {
        fun ofFunction(isAcceptableStr: (String)->Boolean): FunctionalInputFilter {
            return FunctionalInputFilter(isAcceptableStr)
        }
    }

    override fun filter(source: CharSequence, start: Int, end: Int,
                        dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        val potentialResult = FiltersUtils.inputToString(source, start, end, dest, dstart, dend)
        // Либо целиком соглашаемся с вставляемым значением (возвращаем null), либо целиком
        // его запрещаем (возвращаем уже имеющуюсю в [dstart..dend) строку).
        if (isAcceptableStr.invoke(potentialResult)) {
            return null
        } else {
            return dest.toString().substring(dstart, dend)
        }
    }
}
