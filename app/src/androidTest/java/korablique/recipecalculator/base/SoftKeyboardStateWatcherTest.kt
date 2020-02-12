package korablique.recipecalculator.base

import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import junit.framework.Assert.assertFalse
import korablique.recipecalculator.R
import korablique.recipecalculator.test.SoftKeyboardStateWatcherTestActivity
import korablique.recipecalculator.ui.KeyboardHandler
import korablique.recipecalculator.util.InjectableActivityTestRule
import korablique.recipecalculator.util.SyncMainThreadExecutor
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SoftKeyboardStateWatcherTest {
    private lateinit var context: Context
    private val mainThreadExecutor = SyncMainThreadExecutor()
    private lateinit var softKeyboardWatcher: SoftKeyboardStateWatcher

    @get:Rule
    val activityRule: ActivityTestRule<SoftKeyboardStateWatcherTestActivity> =
            InjectableActivityTestRule.forActivity(SoftKeyboardStateWatcherTestActivity::class.java)
                    .withSingletones {
                        context = InstrumentationRegistry.getInstrumentation().getTargetContext()
                        val currentActivityProvider = CurrentActivityProvider()
                        listOf(currentActivityProvider)
                    }
                    .withActivityScoped { target ->
                        softKeyboardWatcher =
                                SoftKeyboardStateWatcher(target as BaseActivity, mainThreadExecutor)
                        listOf(softKeyboardWatcher)
                    }
                    .build()


    @Test
    fun showingKeyboardStateWatchingTest() {
        var notified = false
        softKeyboardWatcher.addObserver(object : SoftKeyboardStateWatcher.Observer {
            override fun onKeyboardProbablyShown() {
                notified = true
            }
        })

        assertFalse(softKeyboardWatcher.isSoftKeyboardShown)
        assertFalse(notified)

        onView(withId(R.id.normal_edit_text)).perform(click())

        assertTrue(softKeyboardWatcher.isSoftKeyboardShown)
        assertTrue(notified)
    }

    @Test
    fun hidingKeyboardStateWatchingTest() {
        // Showing keyboard
        onView(withId(R.id.normal_edit_text)).perform(click())

        var notified = false
        softKeyboardWatcher.addObserver(object : SoftKeyboardStateWatcher.Observer {
            override fun onKeyboardProbablyHidden() {
                notified = true
            }
        })

        assertTrue(softKeyboardWatcher.isSoftKeyboardShown)
        assertFalse(notified)

        mainThreadExecutor.execute {
            KeyboardHandler(activityRule.activity).hideKeyBoard()
        }

        Thread.sleep(500) // Giving time to system keyboard
        assertFalse(softKeyboardWatcher.isSoftKeyboardShown)
        assertTrue(notified)
    }

    @Test
    fun closesKeyboardAndCallsCallback() {
        // Showing keyboard
        onView(withId(R.id.normal_edit_text)).perform(click())

        assertTrue(softKeyboardWatcher.isSoftKeyboardShown)

        var notified = false
        mainThreadExecutor.execute {
            softKeyboardWatcher.hideKeyboardAndCall(timeoutMillis = 500) {
                notified = true
            }
        }

        Thread.sleep(500) // Giving time to system keyboard
        assertFalse(softKeyboardWatcher.isSoftKeyboardShown)
        assertTrue(notified)
    }
}