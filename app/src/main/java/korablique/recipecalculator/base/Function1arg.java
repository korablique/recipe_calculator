package korablique.recipecalculator.base;

/**
 * Function that accepts 1 argument and produces a result
 * @param <R> type of returning object
 * @param <P> type of the parameter
 */
public interface Function1arg<R, P> {
    R call(P param);
}
