package korablique.recipecalculator.database;

import java.util.concurrent.Executors;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

/**
 * Выполняет переданные в него задачи на своём единственном фоновом потоке.
 */
public class DatabaseThreadExecutorImpl implements DatabaseThreadExecutor {
    private final Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());

    @Override
    public Scheduler asScheduler() {
        return scheduler;
    }
}
