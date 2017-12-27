package korablique.recipecalculator.util;

import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import korablique.recipecalculator.base.MainThreadExecutor;

public class SyncMainThreadExecutor extends MainThreadExecutor {
    @Override
    public void execute(Runnable runnable) {
        if (Looper.getMainLooper().isCurrentThread()) {
            // Instrumentation.runOnMainSync выбрасывает исключение, если вызван на главном потоке
            runnable.run();
            return;
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
