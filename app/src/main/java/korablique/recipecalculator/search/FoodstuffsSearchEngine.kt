package korablique.recipecalculator.search

import io.reactivex.Single
import korablique.recipecalculator.base.executors.ComputationThreadsExecutor
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.FoodstuffsTopList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodstuffsSearchEngine @Inject constructor(
        private val foodstuffsList: FoodstuffsList,
        private val foodstuffsTopList: FoodstuffsTopList,
        private val computationThreadsExecutor: ComputationThreadsExecutor,
        private val mainThreadExecutor: MainThreadExecutor) {

    data class SearchResults(
            val query: String,
            val topFoodstuffs: Single<List<Foodstuff>>,
            val allFoodstuffs: Single<List<Foodstuff>>
    )

    fun requestFoodstuffsLike(nameQuery: String): SearchResults {
        // Perform search on a computation thread and then pass the result to the main thread.
        val allFoodstuffsResult = foodstuffsList
                .allFoodstuffs
                .toList()
                .observeOn(computationThreadsExecutor.asScheduler())
                .map { allFoodstuffs -> requestFoodstuffsLikeImpl(nameQuery, allFoodstuffs) }
                .observeOn(mainThreadExecutor.asScheduler())

        val topFoodstuffsResult = foodstuffsTopList
                .getMonthTop()
                .observeOn(computationThreadsExecutor.asScheduler())
                .map { foodstuffs -> requestFoodstuffsLikeImpl(nameQuery, foodstuffs) }
                .observeOn(mainThreadExecutor.asScheduler())

        return SearchResults(
                nameQuery,
                topFoodstuffsResult,
                allFoodstuffsResult)
    }

    private fun requestFoodstuffsLikeImpl(nameQuery: String, foodstuffs: List<Foodstuff>): List<Foodstuff> {
        return FuzzySearcher.search(
                foodstuffs,
                { foodstuff -> foodstuff.name.toLowerCase() },
                nameQuery.toLowerCase())
    }

}