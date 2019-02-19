package korablique.recipecalculator.base.executors;

/**
 * Executor for CPU-intensive work. Uses many threads.
 */
public interface ComputationThreadsExecutor extends Executor {
    void execute(Runnable runnable);
}
