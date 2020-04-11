package korablique.recipecalculator.util;

import android.os.Looper;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import kotlin.coroutines.CoroutineContext;

/**
 * Выполняет переданный Runnable на главном потоке.
 * <br>
 * - Если объект был использован из НЕ-главного потока,
 *   то выполнение переданного Runnable выполняется синхронно (объект не возвращает управление,
 *   пока Runnable не окажется выполненным).
 * <br>
 * - Если объект был использован ИЗ ГЛАВНОГО потока, то переданный Runnable НЕ БУДЕТ выполнен синхронно -
 *   он будет передан в Handler, который выполнит его только _на_следующей_итерации_главного_цикла_.
 * <br>
 * Это различие сделанно намеренно - все андроидские шедулеры всех видов, когда обещают выполнить задачу
 * на главном потоке, выполняют её не синхронно, а именно так.
 * <br>
 * <br>
 * Ещё более подробное описание проблемы, которую решает описанное выше поведение:
 * <br>
 * Раньше тестовые экзекьюторы не следовали общему андроидскому контракту - выполняли задачи
 * как только те в них приходили, если задачу нужно было выполнить на главном потоке,
 * и экзекьютор был вызван на главном потоке.
 * <br>
 * Это неправильно, т.к. выполняемая задача может сама добавить задачу в очередь, и при этом скорее
 * всего она не будет ожидать, что добавляемая ей задача выполнится тут же, сразу, во время её выполнения.
 * <br>
 * В Rx, например, эту проблему частично решает Schedulers.trampoline.
 */
public class SyncMainThreadExecutor extends MainThreadExecutor {
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

    @Override
    public void dispatch(@NotNull CoroutineContext coroutineContext, @NotNull Runnable runnable) {
        execute(runnable);
    }

    private static class SyncMainThreadScheduler extends Scheduler {
        @Override
        public Worker createWorker() {
            return new SyncMainThreadRxWorker();
        }
    }

    @Override
    public boolean isCurrentThreadMain() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
