package korablique.recipecalculator.ui;

import java.text.DecimalFormat;

public class DecimalUtils {
    public static String toDecimalString(float decimal) {
        return toDecimalString((double) decimal);
    }

    public static String toDecimalString(double decimal) {
        return toDecimalString(decimal, 1);
    }

    public static String toDecimalString(float decimal, int digitsAfterDot) {
        return toDecimalString((double)decimal, digitsAfterDot);
    }

    public static String toDecimalString(double decimal, int digitsAfterDot) {
        if (digitsAfterDot == 0) {
            return String.valueOf((long)decimal);
        }

        StringBuilder formatStr = new StringBuilder();
        formatStr.append("#.");
        for (int index = 0; index < digitsAfterDot; ++index) {
            formatStr.append('#');
        }
        DecimalFormat df = new DecimalFormat(formatStr.toString());
        String result = df.format(decimal).replace(',', '.');
        if ("-0".equals(result)) {
            return "0";
        }
        return result;
    }
}
