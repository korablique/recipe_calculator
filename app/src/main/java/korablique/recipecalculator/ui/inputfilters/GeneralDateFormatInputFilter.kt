package korablique.recipecalculator.ui.inputfilters

import android.text.InputFilter
import android.text.Spanned
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException

/**
 * General date format input filter.
 *
 * **NOTE that the filter partially LETS a lot of invalid dates in**, because an input filter
 * cannot verify a complex input format.
 * For example, if user types in "29.02", our input filter cannot know if the 29th day is ok
 * or not until the year is typed in. The filter of course could then allow only years with
 * 29 days in february, but the user might get confused because they cannot type the year they want.
 */
class GeneralDateFormatInputFilter(
        private val startYear: Int,
        private val endYear: Int) : InputFilter {
    override fun filter(source: CharSequence?, start: Int, end: Int,
                        dest: Spanned?, dstart: Int, dend: Int): CharSequence? {
        val potentialResult = FiltersUtils.inputToString(source, start, end, dest, dstart, dend)
        val initialStr = dest.toString().substring(dstart, dend)

        // Empty str
        if (potentialResult.isEmpty()) {
            return null
        }

        val divider = '.'

        // Day
        val firstDividerIndex = potentialResult.indexOf(divider)
        val dayEndIndex = if (firstDividerIndex != -1) {
            firstDividerIndex
        } else {
            potentialResult.length
        }
        if (!isSectionValid(potentialResult, 0, dayEndIndex, 1, 31, divider)) {
            return initialStr
        }

        // Month
        if (isLastSection(dayEndIndex, divider, potentialResult)) {
            // No month yet -
            return null
        }
        val secondDividerIndex = potentialResult.indexOf(divider, dayEndIndex + 1)
        val monthEndIndex = if (secondDividerIndex != -1) {
            secondDividerIndex
        } else {
            potentialResult.length
        }
        if (!isSectionValid(potentialResult, dayEndIndex + 1, monthEndIndex, 1, 12, divider)) {
            return initialStr
        }

        // Year
        if (isLastSection(monthEndIndex, divider, potentialResult)) {
            // No year yet
            return null
        }
        if (!isSectionValid(
                        potentialResult,
                        monthEndIndex + 1, potentialResult.length,
                        startYear, endYear,
                        divider)) {
            return initialStr
        }

        return null
    }

    private fun isLastSection(sectionEndIndex: Int, divider: Char, potentialResult: String): Boolean {
        // If we're at the end of
        return sectionEndIndex == potentialResult.length
                || (potentialResult[sectionEndIndex] == divider
                    && sectionEndIndex + 1 == potentialResult.length)
    }

    private fun isSectionValid(str: String, start: Int, end: Int, min: Int, max: Int, divider: Char): Boolean {
        // Empty
        if (end == start) {
            // Valid if there's no divider yet
            return end == str.length
        }

        val substr = str.substring(start, end)
        // Definitely too big, even if it has zeroes at the start (e.g. '001').
        if (substr.length > max.toString().length) {
            return false
        }

        // If divider is put already - let's check the value
        if (end != str.length && str[end] == divider) {
            val value = try {
                substr.toInt()
            } catch (e: NumberFormatException) {
                return false
            }
            return value in min..max
        }

        // Let's check if the value IS or CAN be greater than min
        val maxStr = max.toString()
        var possibleMaxValueStr = substr
        while (possibleMaxValueStr.length < maxStr.length) {
            possibleMaxValueStr += "9"
        }
        val possibleMaxValue = try {
            possibleMaxValueStr.toInt()
        } catch (e: NumberFormatException) {
            return false
        }
        if (possibleMaxValue < min) {
            return false
        }

        // Let's check that the value will be lesser than max if the user will finish typing it
        var possibleMinValue = try {
            substr.toInt()
        } catch (e: NumberFormatException) {
            return false
        }

        if (possibleMinValue != 0) {
            var possibleMinValueStr = possibleMinValue.toString()
            val minStr = min.toString()
            while (possibleMinValue < min && possibleMinValueStr.length < minStr.length) {
                possibleMinValue = (possibleMinValue.toString() + "0").toInt()
                possibleMinValueStr = possibleMinValue.toString()
            }
            if (max < possibleMinValue) {
                return false
            }
        }

        return true
    }
}