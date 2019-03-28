package korablique.recipecalculator.ui.inputfilters;

import android.text.Spanned;

class FiltersUtils {
    private FiltersUtils() {
    }

    /**
     * Computes the result string produced by accepting user's input by
     * {@link android.text.InputFilter#filter(CharSequence, int, int, Spanned, int, int)}.
     */
    static String inputToString(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String input = source.subSequence(start, end).toString();

        String result = "";
        result += dest.subSequence(0, dstart);
        result += input;
        result += dest.subSequence(dend, dest.length());

        return result;
    }
}