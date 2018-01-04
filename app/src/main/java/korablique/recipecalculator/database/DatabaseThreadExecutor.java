package korablique.recipecalculator.database;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseThreadExecutor {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void execute(Runnable runnable) {
        executorService.execute(runnable);
    }
}
