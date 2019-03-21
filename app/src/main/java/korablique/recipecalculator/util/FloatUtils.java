package korablique.recipecalculator.util;

public class FloatUtils {
    private static final float FLOAT_THRESHOLD = 0.000000001f;
    private FloatUtils() {}

    public static boolean areFloatsEquals(float lhs, float rhs) {
        return areFloatsEquals(lhs, rhs, FLOAT_THRESHOLD);
    }

    public static boolean areFloatsEquals(float lhs, float rhs, float threshold) {
        return Math.abs(lhs - rhs) < threshold;
    }


    public static boolean areFloatsEquals(double lhs, double rhs) {
        return areFloatsEquals(lhs, rhs, FLOAT_THRESHOLD);
    }

    public static boolean areFloatsEquals(double lhs, double rhs, float threshold) {
        return Math.abs(lhs - rhs) < threshold;
    }

    public static boolean isLhsGreater(float lhs, float rhs) {
        if (areFloatsEquals(lhs, rhs)) {
            return false;
        }
        return lhs > rhs;
    }
}
