package korablique.recipecalculator.util;

import android.app.Instrumentation;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import korablique.recipecalculator.base.MainThreadExecutor;

public class SyncMainThreadExecutor extends MainThreadExecutor {
    @Override
    public void execute(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
