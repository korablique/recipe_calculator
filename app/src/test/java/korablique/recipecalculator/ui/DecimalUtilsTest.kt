package korablique.recipecalculator.ui

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import korablique.recipecalculator.BuildConfig
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class DecimalUtilsTest {
    @Test
    fun `by default to decimal str with 1 digit after dot`() {
        val str = DecimalUtils.toDecimalString(123.123)
        assertEquals("123.1", str)
    }

    @Test
    fun `to decimal str with 0 digits after dot`() {
        val str = DecimalUtils.toDecimalString(123.123, 0)
        assertEquals("123", str)
    }

    @Test
    fun `to decimal str with 2 digits after dot`() {
        val str = DecimalUtils.toDecimalString(123.123, 2)
        assertEquals("123.12", str)
    }

    @Test
    fun `to decimal str with 1 digit after dot puts no digits when digits are 0`() {
        val str = DecimalUtils.toDecimalString(123.023, 1)
        assertEquals("123", str)
    }
}
