package korablique.recipecalculator.ui.inputfilters

import android.text.SpannedString
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class FunctionalInputFilterTest {
    @Test
    fun `does not accept appended str when functions says so`() {
        val filter = FunctionalInputFilter { it.contains("apple")}
        // Disagree with the paste
        assertEquals("", filter.filter("maple", 0, 5, SpannedString(""), 0, 0))
        // Agree with the paste
        assertEquals(null, filter.filter("pineapple", 0, 9, SpannedString(""), 0, 0))
    }

    @Test
    fun `does not accept substr removal when functions says so`() {
        val filter = FunctionalInputFilter { it.contains("apple")}
        // "apple" -> "pple"
        // Disagree with the deletion
        assertEquals("a", filter.filter("", 0, 0, SpannedString("apple"), 0, 1))
    }
}