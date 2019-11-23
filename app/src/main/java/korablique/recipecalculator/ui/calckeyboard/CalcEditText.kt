package korablique.recipecalculator.ui.calckeyboard

import android.content.Context
import android.text.InputFilter
import android.text.SpannableString
import android.util.AttributeSet
import android.widget.EditText
import com.udojava.evalex.Expression
import korablique.recipecalculator.R
import korablique.recipecalculator.ui.inputfilters.DecimalNumberInputFilter
import korablique.recipecalculator.ui.inputfilters.FunctionalInputFilter
import java.math.BigDecimal
import java.util.regex.Pattern

open class CalcEditText : EditText {
    private val extraOperatorsRegex = Pattern.compile(""".*[\-+*/][\-+*/].*""")
    private val operatorsRegex = Pattern.compile("""[\-+×÷]""")
    private val numbersFilters = mutableListOf<InputFilter>()
    private var minValue: BigDecimal
    private var maxValue: BigDecimal

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, android.R.attr.editTextStyle)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // Вытащим значения из XML-разметки
        val typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.CalcEditText,
                0, 0)
        val digitsAfterDecimalDot: Int
        try {
            minValue = BigDecimal(typedArray.getFloat(R.styleable.CalcEditText_min_value, Float.MIN_VALUE).toDouble())
            maxValue = BigDecimal(typedArray.getFloat(R.styleable.CalcEditText_max_value, Float.MAX_VALUE).toDouble())
            digitsAfterDecimalDot = typedArray.getInt(R.styleable.CalcEditText_digits_after_decimal_dot, Int.MAX_VALUE)
        } finally {
            typedArray.recycle()
        }

        // Зададим фильтр, не допускающий строки, которые невозможно вычислить
        val inputFilter = FunctionalInputFilter.ofFunction(this::isAcceptableUserInput)
        setFilters(getFilters() + arrayOf(inputFilter))

        if (digitsAfterDecimalDot != Int.MAX_VALUE) {
            numbersFilters += DecimalNumberInputFilter.ofNDigitsAfterPoint(digitsAfterDecimalDot)
        }
    }

    private fun isAcceptableUserInput(str: String): Boolean {
        val value = calcValueAsBigDecimal(str) ?: return false
        if (!isAllowedByNumbersFilters(str)) {
            return false
        }
        return isAcceptableBigDecimal(value)
    }

    private fun calcValueAsBigDecimal(inputStr: String): BigDecimal? {
        // Будем считать строку вида "-" нулём (если отрицательные числе приемлимы),
        // т.к. либа "-" не распарсит, но пользователь может находиться в процессе
        // ввода отрицательного числа.
        if (inputStr == "-" && minValue < BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }

        // Заменим операторы на те, о которых знают либы
        val str = inputStr.replace('×', '*').replace('÷', '/')

        // Строки вроде "2++3", "4**" не допускаются (но с единичными операторами
        // допускаются, потому что единичный оператор может обозначать, что пользователь
        // только-только его ввёл и сейчас введет число).
        if (extraOperatorsRegex.matcher(str).matches()) {
            return null
        }

        val lastDigit = str.indexOfLast { it in '0'..'9' }
        if (lastDigit == -1) {
            return null
        }
        // Обрежем символы справа, если они не цифры (на случай, если они операторы)
        val croppedStr = str.substring(0..lastDigit)

        try {
            return Expression(croppedStr).eval()
        } catch (e: Expression.ExpressionException) {
            return null
        } catch (e: ArithmeticException) {
            return null
        }
    }

    private fun isAllowedByNumbersFilters(inputStr: String): Boolean {
        // Раздробим входную строку на подстроки, содержащие только числа,
        // прогоним каждое из этих строк-чисел через numbersFilters.
        // Мотивация - InputFilter'ы в EditText'ах фильтруют всю вводимую пользователем строку целиком,
        // не пытаясь её дробить на части, но нам нужно уметь фильтровать не всю строку целиком,
        // а числа в ней.
        numbersFilters.forEach { numberFilter ->
            // Разделим строку вида "2+11-4" на строки ["2", "11", "4"]
            val numbersStrs = inputStr.split(operatorsRegex)
            numbersStrs.forEach { numberStr ->
                // mockDest - фальшивое destination для вставки отфильтрованной строки в него,
                // нужно для того, чтобы filter думал, что numberStr куда-то вставляется и
                // отфильтровал бы его хорошенько.
                val mockDest = SpannableString("")
                val filterResult = numberFilter.filter(
                        numberStr, 0, numberStr.length,
                        mockDest, 0, 0)
                // Если numberFilter не вернул null, то ему чем-то не нравится входящая строка.
                // Если входящая строка фильтру чем-то не нравится,
                // то будем считать inputStr неприемлимым.
                if (filterResult != null) {
                    return false
                }
            }
        }
        return true
    }

    private fun isAcceptableBigDecimal(value: BigDecimal): Boolean {
        return value in minValue..maxValue
    }

    /**
     * Вычисляет текущий текст.
     */
    fun calcCurrentValue(): Float? {
        val value = calcValueAsBigDecimal(getText().toString()) ?: return null
        if (isAcceptableBigDecimal(value)) {
            return value.toFloat()
        } else {
            return null
        }
    }

    /**
     * Устанавливает числовые границы возможного вычисляемого значения.
     */
    fun setBounds(min: Float, max: Float) {
        minValue = BigDecimal.valueOf(min.toDouble())
        maxValue = BigDecimal.valueOf(max.toDouble())
    }
}