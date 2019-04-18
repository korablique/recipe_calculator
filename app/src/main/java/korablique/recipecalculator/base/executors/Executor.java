package korablique.recipecalculator.base.executors;

import io.reactivex.Completable;
import io.reactivex.Scheduler;

public interface Executor {
    Scheduler asScheduler();

    default void execute(Runnable runnable) {
        // NOTE: вместо вызова "scheduler.scheduleDirect(runnable)" мы создаём Completable
        // и выполняем его на нашем шедулере.
        // Цель тут - заставить переданный Runnable исполняться внутри фреймворка
        // RxJava (потому что Rx крешит приложение при непойманных исключениях).
        // Чтобы заставить какую-то работу выполняться внутри Rx, нам нужно
        // сделать Observable/Single/Completable/... и подписаться на него - тут это и делается.
        Completable.create((subscriber) -> runnable.run())
                .subscribeOn(asScheduler())
                .subscribe();
    }
}
