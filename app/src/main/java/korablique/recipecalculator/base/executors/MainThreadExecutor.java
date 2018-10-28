package korablique.recipecalculator.base.executors;

public interface MainThreadExecutor extends Executor {
    void execute(Runnable runnable);
    void executeDelayed(long delayMillis, Runnable runnable);
}
