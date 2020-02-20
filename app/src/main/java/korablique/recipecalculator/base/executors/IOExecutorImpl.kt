package korablique.recipecalculator.base.executors

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import kotlin.coroutines.CoroutineContext

class IOExecutorImpl : IOExecutor() {
    private val scheduler = Schedulers.io()

    override fun asScheduler(): Scheduler {
        return scheduler
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        execute(block)
    }
}
