package korablique.recipecalculator.base.executors;

import kotlinx.coroutines.CoroutineDispatcher;

public abstract class MainThreadExecutor extends CoroutineDispatcher implements Executor {
    public abstract void executeDelayed(long delayMillis, Runnable runnable);
    public abstract boolean isCurrentThreadMain();
}
