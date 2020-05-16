package korablique.recipecalculator.model

import org.joda.time.DateTime

import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.Observable
import io.reactivex.Single
import korablique.recipecalculator.base.TimeProvider
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.database.HistoryWorker

@Singleton
open class FoodstuffsTopList @Inject
constructor(
        private val historyWorker: HistoryWorker,
        foodstuffsList: FoodstuffsList,
        private val timeProvider: TimeProvider) {
    private val observers = mutableListOf<Observer>()
    private var monthTop: Observable<Foodstuff> = Observable.empty()
    private var weekTop: Observable<Foodstuff> = Observable.empty()

    interface Observer {
        fun onFoodstuffsTopPossiblyChanged()
    }

    init {
        updateCache()
        historyWorker.addHistoryChangeObserver {
            updateCache()
            observers.forEach { it.onFoodstuffsTopPossiblyChanged() }
        }
        foodstuffsList.addObserver(object : FoodstuffsList.Observer {
            override fun onFoodstuffEdited(edited: Foodstuff) {
                // Заменяем в закешированном списке отредактированный
                // продукт на его отредактированную версию
                monthTop = monthTop
                        .map { foodstuff ->
                            if (foodstuff.id == edited.id) {
                                edited
                            } else {
                                foodstuff
                            }
                        }
                weekTop = weekTop
                        .map { foodstuff ->
                            if (foodstuff.id == edited.id) {
                                edited
                            } else {
                                foodstuff
                            }
                        }
                observers.forEach { it.onFoodstuffsTopPossiblyChanged() }
            }

            override fun onFoodstuffDeleted(deleted: Foodstuff) {
                monthTop = monthTop.filter { foodstuff -> foodstuff.id != deleted.id }
                weekTop = weekTop.filter { foodstuff -> foodstuff.id != deleted.id }
                observers.forEach { it.onFoodstuffsTopPossiblyChanged() }
            }
        })
    }

    private fun updateCache() {
        val now = timeProvider.now()
        val monthAgo = now.minusMonths(1).withTimeAtStartOfDay()
        val weekAgo = now.minusWeeks(1).withTimeAtStartOfDay()
        val tomorrowMidnight = now.plusDays(1).withTimeAtStartOfDay()
        this.monthTop = requestTopFoodstuffs(monthAgo, tomorrowMidnight).cache()
        this.monthTop.subscribe()
        this.weekTop = requestTopFoodstuffs(weekAgo, tomorrowMidnight).cache()
        this.weekTop.subscribe()
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    /**
     * Функция возвращает рассчитанный топ продуктов за месяц.
     * Топ продуктов меняется при каждом обновлении таблицы с историей в БД, поэтому
     * если таблица недавно менялась, то возможно топ ещё не вычислен и
     * операция будет не моментальна.
     * Если история менялась давно, то топ уже вычислен и подписка на него отдаст результат
     * моментально.
     */
    open fun getMonthTop(): Single<List<Foodstuff>> {
        return monthTop.toList()
    }

    /**
     * @see getMonthTop
     */
    open fun getWeekTop(): Single<List<Foodstuff>> {
        return weekTop.toList()
    }

    private fun requestTopFoodstuffs(from: DateTime, to: DateTime): Observable<Foodstuff> {
        val from = from.toDate().time
        val to = to.toDate().time
        return historyWorker.requestListedFoodstuffsFromHistoryForPeriod(from, to)
                .toList()
                .map { foodstuffs ->
                    // Преобразуем набор продуктов из истории в топ, а затем обратно
                    // в список продуктов.
                    val topList = PopularProductsUtils.getTop(foodstuffs)
                    topList.map { it.foodstuff }
                }
                .toObservable()
                .flatMapIterable { foodstuffs -> foodstuffs }
    }
}
