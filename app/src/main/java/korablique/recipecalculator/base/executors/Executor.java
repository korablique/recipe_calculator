package korablique.recipecalculator.base.executors;

import io.reactivex.Scheduler;

public interface Executor {
    Scheduler asScheduler();
}
