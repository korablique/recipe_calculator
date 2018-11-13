package korablique.recipecalculator.database;

import korablique.recipecalculator.base.executors.Executor;

public interface DatabaseThreadExecutor extends Executor {
    void execute(Runnable runnable);
}
