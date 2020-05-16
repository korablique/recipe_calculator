package korablique.recipecalculator.ui;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class DecimalUtils {
    private static ConcurrentHashMap<String, DecimalFormat> decimalFormats = new ConcurrentHashMap<>();

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

        StringBuilder formatStrBuilder = new StringBuilder();
        formatStrBuilder.append("#.");
        for (int index = 0; index < digitsAfterDot; ++index) {
            formatStrBuilder.append('#');
        }
        String formatStr = formatStrBuilder.toString();
        DecimalFormat df = decimalFormats.get(formatStr);
        if (df == null) {
            df = new DecimalFormat(formatStrBuilder.toString());
            decimalFormats.put(formatStr, df);
        }
        String result = df.format(decimal).replace(',', '.');
        if ("-0".equals(result)) {
            return "0";
        }
        return result;
    }
}
