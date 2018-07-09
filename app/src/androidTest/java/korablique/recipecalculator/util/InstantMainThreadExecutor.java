package korablique.recipecalculator.util;

import korablique.recipecalculator.base.MainThreadExecutor;

public class InstantMainThreadExecutor extends MainThreadExecutor {
    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void executeDelayed(long delayMillis, Runnable runnable) {
        runnable.run();
    }
}
