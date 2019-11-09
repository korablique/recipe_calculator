package korablique.recipecalculator.ui.calckeyboard

import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import korablique.recipecalculator.R
import korablique.recipecalculator.base.CurrentActivityProvider
import korablique.recipecalculator.test.CalcKeyboardTestActivity
import korablique.recipecalculator.util.FloatUtils
import korablique.recipecalculator.util.InjectableActivityTestRule
import korablique.recipecalculator.util.SyncMainThreadExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CalcKeyboardTest {
    private lateinit var context: Context
    private val mainThreadExecutor = SyncMainThreadExecutor()

    @get:Rule
    val activityRule: ActivityTestRule<CalcKeyboardTestActivity> =
            InjectableActivityTestRule.forActivity(CalcKeyboardTestActivity::class.java)
            .withSingletones {
                context = InstrumentationRegistry.getTargetContext()
                val currentActivityProvider = CurrentActivityProvider()
                val calcKeyboardController = CalcKeyboardController()
                listOf(calcKeyboardController, currentActivityProvider)
            }
            .build()

    @Test
    fun calcKeyboardAppearsWhenCalcEditTextIsFocused() {
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
        onView(withId(R.id.calc_edit_text)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(matches(isDisplayed()))
    }

    @Test
    fun calcKeyboardDoesNotAppearsWhenNormalEditTextIsFocused() {
        onView(withId(R.id.normal_edit_text)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
    }

    @Test
    fun calcKeyboardShownWhenFocusMovesFromNormalToCalcEditText() {
        onView(withId(R.id.normal_edit_text)).perform(click())

        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
        onView(withId(R.id.calc_edit_text)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(matches(isDisplayed()))
    }

    @Test
    fun calcKeyboardHiddenWhenFocusMovesFromCalcToNormalEditText() {
        onView(withId(R.id.calc_edit_text)).perform(click())

        onView(withId(R.id.calc_keyboard)).check(matches(isDisplayed()))
        onView(withId(R.id.normal_edit_text)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
    }

    @Test
    fun canWriteAndEraseTextWithCalcKeyboard() {
        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_point)).perform(click())
        onView(withId(R.id.button_9)).perform(click())

        // Проверяем, что текст введен
        onView(withId(R.id.calc_edit_text)).check(matches(withText("12.9")))

        // Стираем символы
        onView(withId(R.id.button_backspace)).perform(click())
        onView(withId(R.id.button_backspace)).perform(click())

        // Проверяем, что символы стерлись
        onView(withId(R.id.calc_edit_text)).check(matches(withText("12")))
    }

    @Test
    fun canWriteAndCalculateMathExpressions() {
        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_plus)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_multiply)).perform(click())
        onView(withId(R.id.button_2)).perform(click())

        // Проверяем текст
        onView(withId(R.id.calc_edit_text)).check(matches(withText("2+2×2")))

        // Проверим вычисляемое значение
        val value = getValueOf(R.id.calc_edit_text)
        assertTrue(FloatUtils.areFloatsEquals(6f, value))
    }

    @Test
    fun backspaceLongClickWorks() {
        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_point)).perform(click())
        onView(withId(R.id.button_9)).perform(click())

        val initialText = getTextOf(R.id.calc_edit_text)
        onView(withId(R.id.button_backspace)).perform(longClick())
        val afterBackspaceText = getTextOf(R.id.calc_edit_text)

        // Убеждаемся, что что-то стёрлось (мы не можем контролировать
        // длительность нажатия бекспейса, поэтому не знаем, как много текста удалилось).
        assertTrue(afterBackspaceText.length < initialText.length)

        // Убедимся, что после долгого нажатия на бэкспейс новый текст можно ввести
        onView(withId(R.id.button_5)).perform(click())
        onView(withId(R.id.button_6)).perform(click())
        onView(withId(R.id.calc_edit_text)).check(matches(withText(afterBackspaceText + "56")))
    }

    @Test
    fun cannotWriteTextOutOfPositiveBound() {
        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(R.id.calc_edit_text)
            calcEditText.setBounds(0f, 10f)
        }

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_0)).perform(click())

        // 10 в пределах заданных границ
        onView(withId(R.id.calc_edit_text)).check(matches(withText("10")))

        onView(withId(R.id.button_backspace)).perform(click())
        onView(withId(R.id.button_1)).perform(click())
        // 11 за пределами заданных границ
        onView(withId(R.id.calc_edit_text)).check(matches(withText("1")))
    }

    @Test
    fun cannotWriteTextOutOfNegativeBound() {
        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(R.id.calc_edit_text)
            calcEditText.setBounds(-10f, 10f)
        }

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_minus)).perform(click())
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_0)).perform(click())

        // -10 в пределах заданных границ
        onView(withId(R.id.calc_edit_text)).check(matches(withText("-10")))

        onView(withId(R.id.button_backspace)).perform(click())
        onView(withId(R.id.button_1)).perform(click())
        // -11 за пределами заданных границ
        onView(withId(R.id.calc_edit_text)).check(matches(withText("-1")))
    }

    @Test
    fun cannotWriteMinusSign_ifTextMustBePositive() {
        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(R.id.calc_edit_text)
            calcEditText.setBounds(0f, 10f)
        }

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Пытаемся ввести "-1"
        onView(withId(R.id.button_minus)).perform(click())
        onView(withId(R.id.button_1)).perform(click())

        // "-" не должен быть введён - допустимы только положительные значения и ноль (0..10)
        onView(withId(R.id.calc_edit_text)).check(matches(withText("1")))
    }

    @Test
    fun editProgressText_worksSameAsCalcEditText() {
        // Открываем клавиатуру
        onView(withId(R.id.edit_progress_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_0)).perform(click())
        onView(withId(R.id.button_divide)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_multiply)).perform(click())
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_0)).perform(click())

        // Проверяем текст
        onView(withId(R.id.edit_progress_text)).check(matches(withText("10÷2×10")))

        // Проверим вычисляемое значение
        val value = getValueOf(R.id.edit_progress_text)
        assertTrue(FloatUtils.areFloatsEquals(50f, value))
    }

    private fun getTextOf(viewId: Int): String {
        var text: String? = null
        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(viewId)
            text = calcEditText.text.toString()
        }
        return text!!
    }

    private fun getValueOf(viewId: Int): Float {
        var value: Float? = null
        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(viewId)
            value = calcEditText.calcCurrentValue()
        }
        return value!!
    }
}
