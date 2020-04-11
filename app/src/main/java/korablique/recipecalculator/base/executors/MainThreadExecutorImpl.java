package korablique.recipecalculator.base.executors;

import android.os.Looper;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import kotlin.coroutines.CoroutineContext;

/**
 * Выполняет переданные в него задачи на главном потоке.
 */
public class MainThreadExecutorImpl extends MainThreadExecutor {
    private Scheduler scheduler = AndroidSchedulers.mainThread();

    @Override
    public void executeDelayed(long delayMillis, Runnable runnable) {
        scheduler.scheduleDirect(runnable, delayMillis, TimeUnit.MILLISECONDS);
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
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
