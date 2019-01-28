package korablique.recipecalculator.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

/**
 * Реализует Rx-класс Scheduler.Worker т.о., что все задачи выполняются синхронно на главном потоке.
 * Данное поведение реализуется либо моментальным выполнением переданной задачи (если передача
 * была осуществлена уже на главном потоке), либо отправкой задачи в Handler с блокировкой текущего
 * потока до окончания выполнения задачи.
 */
class SyncMainThreadRxWorker extends Scheduler.Worker {
    private volatile boolean isDisposed;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * @return т.к. задача выполняется синхронно, метод всегда возвращает Disposables.disposed().
     */
    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        if (isDisposed) {
            return Disposables.disposed();
        }

        if (delay > 0) {
            try {
                Thread.sleep(TimeUnit.MILLISECONDS.convert(delay, unit));
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interruptions not supported", e);
            }
        }

        if (Looper.getMainLooper().isCurrentThread()) {
            run.run();
            return Disposables.disposed();
        }

        CountDownLatch latch = new CountDownLatch(1);
        Runnable runnableWrapper = () -> {
            run.run();
            latch.countDown();
        };
        handler.post(runnableWrapper);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interruption not supported", e);
        }
        return Disposables.disposed();
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
}
