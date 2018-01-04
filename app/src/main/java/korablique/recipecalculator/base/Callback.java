package korablique.recipecalculator.base;

@FunctionalInterface
public interface Callback<T> {
    void onResult(T t);
}
