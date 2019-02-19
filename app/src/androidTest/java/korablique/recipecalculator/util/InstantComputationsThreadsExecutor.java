package korablique.recipecalculator.util;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import korablique.recipecalculator.base.executors.ComputationThreadsExecutor;

/**
 * Использует {@link Schedulers#trampoline()} для синхронного выполнения задач в вызывающем потоке.
 */
public class InstantComputationsThreadsExecutor implements ComputationThreadsExecutor {
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
