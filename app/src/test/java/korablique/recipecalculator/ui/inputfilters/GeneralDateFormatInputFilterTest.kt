package korablique.recipecalculator.ui.inputfilters

import android.text.InputFilter
import android.text.SpannedString
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class GeneralDateFormatInputFilterTest {
    @Test
    fun `date in the middle`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals(null, filterPastedText("01.10.2000", filter))
    }

    @Test
    fun `start date`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals(null, filterPastedText("01.01.1800", filter))
    }

    @Test
    fun `end date`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals(null, filterPastedText("31.12.2020", filter))
    }

    @Test
    fun `date before start`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals("", filterPastedText("31.12.1799", filter))
    }

    @Test
    fun `date after end`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals("", filterPastedText("01.01.2021", filter))
    }

    @Test
    fun `not digit day`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals("", filterPastedText("1o.10.2000", filter))
    }

    @Test
    fun `not digit month`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals("", filterPastedText("10.1o.2000", filter))
    }

    @Test
    fun `not digit year`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals("", filterPastedText("10.10.2ooo", filter))
    }

    @Test
    fun `1th wrong digit of year is not allowed`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals("", filterPastedText("31.12.3", filter))
    }

    @Test
    fun `2nd wrong digit of year is not allowed`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals("", filterPastedText("31.12.23", filter))
    }

    @Test
    fun `3rd wrong digit of year is not allowed`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals("", filterPastedText("31.12.203", filter))
    }

    @Test
    fun `4rd wrong digit of year is not allowed`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals("", filterPastedText("31.12.2023", filter))
    }

    @Test
    fun `not finished dates allowed`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals(null, filterPastedText("0", filter))
        assertEquals(null, filterPastedText("01", filter))
        assertEquals(null, filterPastedText("01.", filter))
        assertEquals(null, filterPastedText("01.0", filter))
        assertEquals(null, filterPastedText("01.01", filter))
        assertEquals(null, filterPastedText("01.01.", filter))
        assertEquals(null, filterPastedText("01.01.2", filter))
        assertEquals(null, filterPastedText("01.01.20", filter))
        assertEquals(null, filterPastedText("01.01.202", filter))
        assertEquals(null, filterPastedText("01.01.2020", filter))
    }

    @Test
    fun `section cannot be empty`() {
        val filter = GeneralDateFormatInputFilter(1800, 2020)
        assertEquals("", filterPastedText("31..2010", filter))
    }

    @Test
    fun `most popular years range`() {
        val filter = GeneralDateFormatInputFilter(1900, 2020)
        assertEquals("", filterPastedText("31.11.1899", filter))
        assertEquals(null, filterPastedText("1.1.1900", filter))
        assertEquals(null, filterPastedText("1.1.1950", filter))
        assertEquals(null, filterPastedText("31.12.1999", filter))
        assertEquals(null, filterPastedText("01.01.2000", filter))
        assertEquals(null, filterPastedText("31.12.2020", filter))
        assertEquals("", filterPastedText("1.1.2021", filter))
    }

    private fun filterPastedText(text: String, filter: InputFilter): String? {
        return filter.filter(text, 0, text.length, SpannedString(""), 0, 0)?.toString()
    }
}