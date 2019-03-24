package korablique.recipecalculator.base.executors;

public interface MainThreadExecutor extends Executor {
    void executeDelayed(long delayMillis, Runnable runnable);
}
