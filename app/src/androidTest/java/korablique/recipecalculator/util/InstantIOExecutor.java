package korablique.recipecalculator.util;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import korablique.recipecalculator.base.executors.IOExecutor;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import kotlin.coroutines.CoroutineContext;

/**
 * Использует {@link Schedulers#trampoline()} для синхронного выполнения задач в вызывающем потоке.
 */
public class InstantIOExecutor extends IOExecutor {
    private final Scheduler scheduler = Schedulers.trampoline();

    @Override
    public void execute(Runnable runnable) {
        scheduler.scheduleDirect(runnable);
    }

    @Override
    public Scheduler asScheduler() {
        return scheduler;
    }

    @Override
    public void dispatch(@NotNull CoroutineContext coroutineContext, @NotNull Runnable runnable) {
        execute(runnable);
    }
}
