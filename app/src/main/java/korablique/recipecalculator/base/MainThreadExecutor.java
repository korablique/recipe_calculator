package korablique.recipecalculator.base;

import android.os.Handler;
import android.os.Looper;


public class MainThreadExecutor {
    private Handler handler = new Handler(Looper.getMainLooper());

    public void execute(Runnable runnable) {
        handler.post(runnable);
    }
}
