package korablique.recipecalculator.ui.mainactivity.mainscreen;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketList;

import static korablique.recipecalculator.ui.mainactivity.mainscreen.SearchResultsFragment.SEARCH_RESULTS_FRAGMENT_TAG;

@FragmentScope
public class MainScreenSearchController
        implements ActivityCallbacks.Observer, FragmentCallbacks.Observer {
    private static final int SEARCH_SUGGESTIONS_NUMBER = 3;
    private final MainThreadExecutor mainThreadExecutor;
    private final BucketList bucketList;
    private final FoodstuffsList foodstuffsList;
    private final FragmentActivity context;
    private final BaseFragment fragment;
    private final ActivityCallbacks activityCallbacks;
    private final MainScreenCardController cardController;
    private final MainScreenReadinessDispatcher mainScreenReadinessDispatcher;
    private FloatingSearchView searchView;

    private BucketList.Observer bucketListObserver = new BucketList.Observer() {
        @Override
        public void onFoodstuffAdded(WeightedFoodstuff weightedFoodstuff) {
            searchView.clearQuery();
        }
    };

    // Disposable чтобы отменить последний начатый поиск при старте нового.
    // Такая отмена нужна на случай, если поиск 2 будет быстрее поиска 1 - чтобы между
    // поисками не было состояния гонки и именно последний поиск всегда был отображен.
    // По-умолчанию Disposables.empty() чтобы не нужно было делать проверки на null.
    private Disposable lastSearchDisposable = Disposables.empty();
    private Stack<String> searchQueries = new Stack<>();

    @Inject
    public MainScreenSearchController(
            MainThreadExecutor mainThreadExecutor,
            FoodstuffsList foodstuffsList,
            BaseFragment fragment,
            ActivityCallbacks activityCallbacks,
            FragmentCallbacks fragmentCallbacks,
            MainScreenCardController cardController,
            MainScreenReadinessDispatcher mainScreenReadinessDispatcher) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.bucketList = BucketList.getInstance();
        this.foodstuffsList = foodstuffsList;
        this.context = fragment.getActivity();
        this.fragment = fragment;
        this.activityCallbacks = activityCallbacks;
        this.cardController = cardController;
        this.mainScreenReadinessDispatcher = mainScreenReadinessDispatcher;
        fragmentCallbacks.addObserver(this);
    }

    @Override
    public  void onFragmentDestroy() {
        bucketList.removeObserver(bucketListObserver);
        activityCallbacks.removeObserver(this);
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        searchView = fragmentView.findViewById(R.id.floating_search_view);
        mainScreenReadinessDispatcher.runWhenReady(() -> {
            configureSearch();
            configureSuggestionsDisplaying();
            bucketList.addObserver(bucketListObserver);
            activityCallbacks.addObserver(this);
        });
    }

    private void configureSearch() {
        searchView.setOnFocusChangeListener(new SearchViewFocusListener() {
            @Override
            public void onFocusCleared() {
                // Когда со строки поиска ушёл фокус - очищаем текст поиска.
                // Но только в том случае, если на экране нет результатов поиска.
                // Делаем это в следующей итерации главного UI-цикла, т.к. фокус мог пропасть
                // из-за того, что вот-вот покажутся результаты поиска (в следующей итерации
                // главного UI-цикла они уже будут показаны).
                mainThreadExecutor.execute(() -> clearSearchQueryIfSearchResultsNotShown());
            }
        });

        searchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            // действие при нажатии на подсказку
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                FoodstuffSearchSuggestion suggestion = (FoodstuffSearchSuggestion) searchSuggestion;
                cardController.showCard(suggestion.getFoodstuff());
            }

            // когда пользователь нажал на клавиатуре enter
            @Override
            public void onSearchAction(String currentQuery) {
                if (!currentQuery.isEmpty()) {
                    searchQueries.push(currentQuery);
                }
                SearchResultsFragment.show(currentQuery, context);
            }
        });

        // когда пользователь нажал кнопку лупы в searchView
        searchView.setOnMenuItemClickListener(item -> {
            String currentQuery = searchView.getQuery();
            if (!currentQuery.isEmpty()) {
                searchQueries.push(currentQuery);
            }
            SearchResultsFragment.show(currentQuery, context);
        });
    }

    private void configureSuggestionsDisplaying() {
        searchView.setOnQueryChangeListener((oldQuery, newQuery) -> {
            lastSearchDisposable.dispose();
            //get suggestions based on newQuery
            Single<List<Foodstuff>> searchResult = foodstuffsList.requestFoodstuffsLike(newQuery, SEARCH_SUGGESTIONS_NUMBER);
            lastSearchDisposable = searchResult.subscribe((result) -> {
                //pass them on to the search view
                List<FoodstuffSearchSuggestion> newSuggestions = new ArrayList<>();
                for (Foodstuff foodstuff : result) {
                    FoodstuffSearchSuggestion suggestion = new FoodstuffSearchSuggestion(foodstuff);
                    newSuggestions.add(suggestion);
                }
                searchView.swapSuggestions(newSuggestions);
            });
        });
    }

    private void clearSearchQueryIfSearchResultsNotShown() {
        FragmentManager fragmentManager = fragment.getFragmentManager();
        if (fragmentManager != null) {
            for (Fragment f : fragmentManager.getFragments()) {
                if (f instanceof SearchResultsFragment) {
                    return;
                }
            }
        }
        searchView.clearQuery();
        searchQueries.clear();
    }

    @Override
    public boolean onActivityBackPressed() {
        // когда показан SearchResultFragment - возвращать в строку прошлый запрос
        // иначе - очистить историю запросов
        Fragment searchResultsFragment = context.getSupportFragmentManager().findFragmentByTag(SEARCH_RESULTS_FRAGMENT_TAG);
        if (searchResultsFragment != null && searchResultsFragment.isVisible()) {
            if (!searchQueries.empty()) {
                searchQueries.pop();
            }
            if (!searchQueries.empty()) {
                searchView.setSearchText(searchQueries.peek());
            } else {
                searchView.clearQuery();
            }
            context.getSupportFragmentManager().beginTransaction().remove(searchResultsFragment).commit();
            // В следующей итерации основного UI-цикла очистим строку поиска, если
            // фрагментов с результатами поиска больше нет
            mainThreadExecutor.execute(this::clearSearchQueryIfSearchResultsNotShown);
            // Мы поглотили событие - сами решили, что должно происходить.
            return true;
        } else {
            searchQueries.clear();
            return false;
        }
    }
}
