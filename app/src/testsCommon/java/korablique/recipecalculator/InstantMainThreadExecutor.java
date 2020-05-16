package korablique.recipecalculator;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import kotlin.coroutines.CoroutineContext;

/**
 * Использует {@link Schedulers#trampoline()} для синхронного выполнения задач в вызывающем потоке.
 */
public class InstantMainThreadExecutor extends MainThreadExecutor {
    private final Scheduler scheduler = Schedulers.trampoline();

    @Override
    public void execute(Runnable runnable) {
        scheduler.scheduleDirect(runnable);
    }

    @Override
    public void executeDelayed(long delayMillis, Runnable runnable) {
        throw new IllegalStateException("Instant executor doesn't support delayed execution");
    }

    @Override
    public Scheduler asScheduler() {
        return scheduler;
    }

    @Override
    public void dispatch(@NotNull CoroutineContext coroutineContext, @NotNull Runnable runnable) {
        execute(runnable);
    }

    @Override
    public boolean isCurrentThreadMain() {
        return true;
    }
}
