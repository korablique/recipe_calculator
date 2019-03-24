package korablique.recipecalculator.base.executors;

import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Выполняет переданные в него задачи на главном потоке.
 */
public class MainThreadExecutorImpl implements MainThreadExecutor {
    private Scheduler scheduler = AndroidSchedulers.mainThread();

    @Override
    public void executeDelayed(long delayMillis, Runnable runnable) {
        scheduler.scheduleDirect(runnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public Scheduler asScheduler() {
        return scheduler;
    }
}
