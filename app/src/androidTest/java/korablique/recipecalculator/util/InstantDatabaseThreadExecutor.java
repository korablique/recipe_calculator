package korablique.recipecalculator.util;

import korablique.recipecalculator.database.DatabaseThreadExecutor;

public class InstantDatabaseThreadExecutor extends DatabaseThreadExecutor {
    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }
}
