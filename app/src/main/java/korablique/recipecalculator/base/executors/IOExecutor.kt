package korablique.recipecalculator.base.executors

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Executor для IO-операций (работа с файловой системой, сетью, ...).
 */
abstract class IOExecutor : CoroutineDispatcher(), Executor
