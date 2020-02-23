package korablique.recipecalculator.outside.partners

import korablique.recipecalculator.base.executors.IOExecutor
import korablique.recipecalculator.base.executors.MainThreadExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Хранилище" партнёров пользователя.
 * Пока представляет представляет собой очень плохо написанную непротестированную хрень.
 */
@Singleton
class PartnersRegistry @Inject constructor(
        private val mainThreadExecutor: MainThreadExecutor,
        private val ioExecutor: IOExecutor
) {
    suspend fun getPartners(): List<Partner> = withContext(ioExecutor) {
        delay(1000L)
        listOf(Partner("Boris"), Partner("Borya"), Partner("Matvey"),
                Partner("Boris"), Partner("Borya"), Partner("Matvey"),
                Partner("Boris"), Partner("Borya"), Partner("Matvey"),
                Partner("Boris"), Partner("Borya"), Partner("Matvey"))
    }
}