package korablique.recipecalculator.ui.inputfilters;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes sure that the text user inputs is a decimal number with a certain max number of digits
 * after the decimal point.
 */
public class DecimalNumberInputFilter implements InputFilter {
    private final Pattern pattern;
    private final int maxDigitsAfterPoint;

    private DecimalNumberInputFilter(int digitsAfterPointCount) {
        this.pattern = Pattern.compile("[0-9]+((\\.|\\.[0-9]+))?");
        this.maxDigitsAfterPoint = digitsAfterPointCount;
    }

    public static DecimalNumberInputFilter of1DigitAfterPoint() {
        return new DecimalNumberInputFilter(1);
    }

    public static DecimalNumberInputFilter ofNDigitsAfterPoint(int n) {
        return new DecimalNumberInputFilter(n);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String result = FiltersUtils.inputToString(source, start, end, dest, dstart, dend);

        if (result.isEmpty()) {
            // Empty result is ok
            return null;
        }

        Matcher matcher = pattern.matcher(result);
        if(!matcher.matches()) {
            // Result not matching regex is not ok
            return "";
        }

        if (maxDigitsAfterPoint == 0 && result.contains(".")) {
            // No point allowed when max digits is 0
            return "";
        }

        int pointIndex = result.indexOf(".");
        if (pointIndex < 0) {
            // No point - that's ok
            return null;
        }

        int digitsAfterPoint = result.substring(pointIndex).length() - 1;
        if (maxDigitsAfterPoint < digitsAfterPoint) {
            return "";
        } else {
            return null;
        }
    }
}
