package korablique.recipecalculator.ui.calckeyboard

import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import korablique.recipecalculator.R
import korablique.recipecalculator.base.CurrentActivityProvider
import korablique.recipecalculator.database.DatabaseWorker
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.database.room.DatabaseHolder
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity
import korablique.recipecalculator.util.InjectableActivityTestRule
import korablique.recipecalculator.util.InstantComputationsThreadsExecutor
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor
import korablique.recipecalculator.util.SyncMainThreadExecutor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
@LargeTest
class CalcKeyboardTest {
    private val calcEditText = R.id.protein_value
    private val normalEditText = R.id.foodstuff_name
    private lateinit var context: Context

    // Тест использует EditFoodstuffActivity в качестве "подопотного" экрана для тестирования клавиатуры
    @get:Rule
    val activityRule: ActivityTestRule<EditFoodstuffActivity> =
            InjectableActivityTestRule.forActivity(EditFoodstuffActivity::class.java)
            .withSingletones {
                context = InstrumentationRegistry.getTargetContext()
                val mainThreadExecutor = SyncMainThreadExecutor()
                val databaseThreadExecutor = InstantDatabaseThreadExecutor()
                val databaseHolder = DatabaseHolder(context, databaseThreadExecutor)
                val databaseWorker = DatabaseWorker(
                        databaseHolder, mainThreadExecutor, databaseThreadExecutor)
                val foodstuffsList = FoodstuffsList(databaseWorker, mainThreadExecutor,
                        InstantComputationsThreadsExecutor())
                val currentActivityProvider = CurrentActivityProvider()
                val calcKeyboardController = CalcKeyboardController()
                Arrays.asList<Any>(mainThreadExecutor, databaseThreadExecutor, databaseWorker,
                        foodstuffsList, currentActivityProvider, calcKeyboardController)
            }
            .build()

    @Test
    fun calcKeyboardAppearsWhenCalcEditTextIsFocused() {
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
        onView(withId(calcEditText)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(matches(isDisplayed()))
    }

    @Test
    fun calcKeyboardDoesNotAppearsWhenNormalEditTextIsFocused() {
        onView(withId(normalEditText)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
    }

    @Test
    fun calcKeyboardShownWhenFocusMovesFromNormalToCalcEditText() {
        onView(withId(normalEditText)).perform(click())

        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
        onView(withId(calcEditText)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(matches(isDisplayed()))
    }

    @Test
    fun calcKeyboardHiddenWhenFocusMovesFromCalcToNormalEditText() {
        onView(withId(calcEditText)).perform(click())

        onView(withId(R.id.calc_keyboard)).check(matches(isDisplayed()))
        onView(withId(normalEditText)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
    }

    @Test
    fun canWriteAndEraseTextWithCalcKeyboard() {
        // Открываем клавиатуру
        onView(withId(calcEditText)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_point)).perform(click())
        onView(withId(R.id.button_9)).perform(click())

        // Проверяем, что текст введен
        onView(withId(calcEditText)).check(matches(withText("12.9")))

        // Стираем символы
        onView(withId(R.id.button_backspace)).perform(click())
        onView(withId(R.id.button_backspace)).perform(click())

        // Проверяем, что символы стерлись
        onView(withId(calcEditText)).check(matches(withText("12")))
    }
}