package korablique.recipecalculator.util;

import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import korablique.recipecalculator.base.executors.MainThreadExecutor;

/**
 * Выполняет переданный Runnable на главном потоке, но синхронно
 * (не возвращает управление, пока Runnable не окажется выполненным).
 */
public class SyncMainThreadExecutor implements MainThreadExecutor {
    private final Scheduler scheduler = new SyncMainThreadScheduler();

    @Override
    public void execute(Runnable runnable) {
        scheduler.scheduleDirect(runnable);
    }

    @Override
    public void executeDelayed(long delayMillis, Runnable runnable) {
        scheduler.scheduleDirect(runnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public Scheduler asScheduler() {
        return scheduler;
    }

    private static class SyncMainThreadScheduler extends Scheduler {
        @Override
        public Worker createWorker() {
            return new SyncMainThreadRxWorker();
        }

    }
}
