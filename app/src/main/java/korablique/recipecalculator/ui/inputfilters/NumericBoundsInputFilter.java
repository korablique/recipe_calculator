package korablique.recipecalculator.ui.inputfilters;

import android.text.InputFilter;
import android.text.Spanned;

import korablique.recipecalculator.util.FloatUtils;

/**
 * Makes sure that the text user inputs is a number within given bounds.
 * Note that because of nature of floats the bounds are not exact (see {@link FloatUtils}).
 */
public class NumericBoundsInputFilter implements InputFilter {
    private final float min;
    private final float max;

    private NumericBoundsInputFilter(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public static NumericBoundsInputFilter withBounds(float min, float max) {
        return new NumericBoundsInputFilter(min, max);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String result = FiltersUtils.inputToString(source, start, end, dest, dstart, dend);

        if (result.isEmpty()) {
            return null;
        }

        float number;
        try {
            number = Float.parseFloat(result);
        } catch (NumberFormatException e) {
            // Invalid number!
            return "";
        }

        // min > number || number > max
        if (FloatUtils.isLhsGreater(min, number)
                || FloatUtils.isLhsGreater(number, max)) {
            // Number is not in bounds!
            return "";
        }
        return null;
    }
}
