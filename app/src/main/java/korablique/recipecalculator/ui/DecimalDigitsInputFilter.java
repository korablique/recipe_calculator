package korablique.recipecalculator.ui;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Make user to input one digit after decimal point
 */
public class DecimalDigitsInputFilter implements InputFilter {
    private Pattern pattern;

    public DecimalDigitsInputFilter() {
        pattern = Pattern.compile("[0-9]+((\\.|\\.[0-9]))?");
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String destS = dest.toString();
        String resultText;
        if (dstart == destS.length()) {
            resultText = destS + source;
        } else {
            resultText = dest.toString().replaceFirst(
                    dest.toString().substring(dstart, dend),
                    source.toString().substring(start, end));
        }

        Matcher matcher = pattern.matcher(resultText);
        if(!matcher.matches())
            return "";
        return null;
    }
}