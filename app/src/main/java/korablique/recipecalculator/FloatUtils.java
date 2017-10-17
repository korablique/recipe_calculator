package korablique.recipecalculator;

public class FloatUtils {
    private FloatUtils() {}

    public static boolean areFloatsEquals(float lhs, float rhs) {
        return Math.abs(lhs - rhs) < 0.0001;
    }

    public static boolean areFloatsEquals(double lhs, double rhs) {
        return Math.abs(lhs - rhs) < 0.0001;
    }
}
