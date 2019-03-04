package korablique.recipecalculator.util;

import java.text.DecimalFormat;

public class TextUtils {
    public static String getDecimalString(float decimal) {
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(decimal).replace(',', '.');
    }
}
