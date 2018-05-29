package korablique.recipecalculator.util;

import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import korablique.recipecalculator.base.MainThreadExecutor;

/**
 * Выполняет переданный Runnable на главном потоке, но синхронно
 * (не возвращает управление, пока Runnable не окажется выполненным).
 */
public class SyncMainThreadExecutor extends MainThreadExecutor {
    @Override
    public void execute(Runnable runnable) {
        if (Looper.getMainLooper().getThread().getId() == Thread.currentThread().getId()) {
            // Instrumentation.runOnMainSync выбрасывает исключение, если вызван на главном потоке
            runnable.run();
            return;
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }

    @Override
    public void executeDelayed(long delayMillis, Runnable runnable) {
        execute(runnable);
    }
}
