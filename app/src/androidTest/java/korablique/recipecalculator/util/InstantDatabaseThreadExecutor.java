package korablique.recipecalculator.util;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import korablique.recipecalculator.database.DatabaseThreadExecutor;

/**
 * Использует {@link Schedulers#trampoline()} для синхронного выполнения задач в вызывающем потоке.
 */
public class InstantDatabaseThreadExecutor implements DatabaseThreadExecutor {
    private final Scheduler scheduler = Schedulers.trampoline();

    @Override
    public void execute(Runnable runnable) {
        scheduler.scheduleDirect(runnable);
    }

    @Override
    public Scheduler asScheduler() {
        return scheduler;
    }
}
