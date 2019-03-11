package korablique.recipecalculator.ui;

import java.text.DecimalFormat;

public class DecimalUtils {
    public static String toDecimalString(float decimal) {
        return toDecimalString((double) decimal);
    }

    public static String toDecimalString(double decimal) {
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(decimal).replace(',', '.');
    }
}
