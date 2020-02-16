package korablique.recipecalculator.ui.mainactivity.mainscreen;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.subjects.PublishSubject;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.base.SoftKeyboardStateWatcher;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.search.FoodstuffsSearchEngine;
import korablique.recipecalculator.search.FoodstuffsSearchEngine.SearchResults;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.mainactivity.MainActivityFragmentsController;

@FragmentScope
public class MainScreenSearchController
        implements ActivityCallbacks.Observer, FragmentCallbacks.Observer, MainActivityFragmentsController.Observer {
    private static final int SEARCH_SUGGESTIONS_NUMBER = 3;
    private final MainThreadExecutor mainThreadExecutor;
    private final BucketList bucketList;
    private final FoodstuffsList foodstuffsList;
    private final FoodstuffsSearchEngine foodstuffsSearchEngine;
    private final MainScreenFragment mainFragment;
    private final FragmentActivity context;
    private final ActivityCallbacks activityCallbacks;
    private final MainScreenCardController cardController;
    private final MainScreenReadinessDispatcher mainScreenReadinessDispatcher;
    private final RxFragmentSubscriptions subscriptions;
    private final SoftKeyboardStateWatcher softKeyboardStateWatcher;
    private final MainActivityFragmentsController mainActivityFragmentsController;
    private FloatingSearchView searchView;

    private BucketList.Observer bucketListObserver = new BucketList.Observer() {
        @Override
        public void onFoodstuffAdded(WeightedFoodstuff weightedFoodstuff) {
            searchView.clearQuery();
        }
    };

    private FoodstuffsList.Observer foodstuffsListObserver = new FoodstuffsList.Observer() {
        // подписываемся на FoodstuffsList, чтобы после добавления нового продукта
        // он отображался в результатах поиска
        @Override
        public void onFoodstuffSaved(Foodstuff savedFoodstuff, int index) {
            // Поиск заново
            if (!searchView.getQuery().isEmpty()) {
                performSearch(searchView.getQuery());
            }
        }
        @Override
        public void onFoodstuffDeleted(Foodstuff deleted) {
            // Поиск заново
            if (!searchView.getQuery().isEmpty()) {
                performSearch(searchView.getQuery());
            }
        }
    };

    // Disposable чтобы отменить последний начатый поиск при старте нового.
    // Такая отмена нужна на случай, если поиск 2 будет быстрее поиска 1 - чтобы между
    // поисками не было состояния гонки и именно последний поиск всегда был отображен.
    // По-умолчанию Disposables.empty() чтобы не нужно было делать проверки на null.
    private Disposable lastSearchDisposable = Disposables.empty();

    @Inject
    public MainScreenSearchController(
            MainThreadExecutor mainThreadExecutor,
            BucketList bucketList,
            FoodstuffsList foodstuffsList,
            FoodstuffsSearchEngine foodstuffsSearchEngine,
            MainScreenFragment mainFragment,
            ActivityCallbacks activityCallbacks,
            FragmentCallbacks fragmentCallbacks,
            MainScreenCardController cardController,
            MainScreenReadinessDispatcher mainScreenReadinessDispatcher,
            RxFragmentSubscriptions subscriptions,
            SoftKeyboardStateWatcher softKeyboardStateWatcher,
            MainActivityFragmentsController mainActivityFragmentsController) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.bucketList = bucketList;
        this.foodstuffsList = foodstuffsList;
        this.foodstuffsSearchEngine = foodstuffsSearchEngine;
        this.mainFragment = mainFragment;
        this.context = mainFragment.getActivity();
        this.activityCallbacks = activityCallbacks;
        this.cardController = cardController;
        this.mainScreenReadinessDispatcher = mainScreenReadinessDispatcher;
        this.subscriptions = subscriptions;
        this.softKeyboardStateWatcher = softKeyboardStateWatcher;
        this.mainActivityFragmentsController = mainActivityFragmentsController;
        fragmentCallbacks.addObserver(this);

        cardController.addObserver(new MainScreenCardController.Observer() {
            @Override
            public void onCardClosedByPerformedAction() {
                SearchResultsFragment fragment = SearchResultsFragment.findFragment(mainFragment);
                if (fragment != null) {
                    SearchResultsFragment.closeFragment(mainFragment, fragment);
                }
                clearSearchQuery();
            }
        });
    }

    @Override
    public  void onFragmentDestroy() {
        bucketList.removeObserver(bucketListObserver);
        foodstuffsList.removeObserver(foodstuffsListObserver);
        activityCallbacks.removeObserver(this);
        mainActivityFragmentsController.removeObserver(this);
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        searchView = fragmentView.findViewById(R.id.floating_search_view);
        mainScreenReadinessDispatcher.runWhenReady(() -> {
            configureSuggestionsDisplaying();
            configureSearch();
            bucketList.addObserver(bucketListObserver);
            foodstuffsList.addObserver(foodstuffsListObserver);
            activityCallbacks.addObserver(this);
            mainActivityFragmentsController.addObserver(this);

            // Отключаем затемнение экрана при появлении фрагмента с результатами поиска,
            // включаем затемнение обратно при его уходе.
            mainFragment.getChildFragmentManager().registerFragmentLifecycleCallbacks(new FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentAttached(
                        @NonNull FragmentManager fm, @NonNull Fragment fragment, @NonNull Context context) {
                    if (fragment instanceof SearchResultsFragment) {
                        searchView.setDimBackground(false);
                    }
                }
                @Override
                public void onFragmentDetached(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
                    if (fragment instanceof SearchResultsFragment) {
                        searchView.setDimBackground(true);
                    }
                }
            }, false /*recursive*/);
        });
    }

    private void configureSuggestionsDisplaying() {
        searchView.setOnQueryChangeListener((oldQuery, newQuery) -> {
            performSearch(newQuery);
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
                long keyboardWaitingTimeout = 500;
                softKeyboardStateWatcher.hideKeyboardWithoutClearingFocusAndCall(keyboardWaitingTimeout, () -> {
                    cardController.showCard(suggestion.getFoodstuff());
                });
            }
            // когда пользователь нажал на клавиатуре enter
            @Override
            public void onSearchAction(String currentQuery) {
                SearchResultsFragment.show(currentQuery, mainFragment);
                performSearch(currentQuery);
            }
        });

        // когда пользователь нажал кнопку лупы в searchView
        searchView.setOnMenuItemClickListener(item -> {
            SearchResultsFragment.show(searchView.getQuery(), mainFragment);
            performSearch(searchView.getQuery());
        });
    }

    private void performSearch(String query) {
        SearchResults searchResults =
                foodstuffsSearchEngine.requestFoodstuffsLike(query);

        lastSearchDisposable.dispose();
        lastSearchDisposable = searchResults.getTopFoodstuffs()
                // Сперва получаем результаты TopFoodstuffs, затем AllFoodstuffs
                .concatWith(searchResults.getAllFoodstuffs())
                // Сделаем так, чтобы вместо событий "List<Foodstuffs>", мы получали
                // список списков продуктов, который будет увеличиваться с каждым новым списком.
                //
                // Т.е. сперва в subscribe придёт List<List<Foodstuff>> с size == 1, который будет
                // содержать только найденные по запросу продукты в топе.
                // Затем, вторым событием, в subscribe придёт List<List<Foodstuff>> с size == 2,
                // который будет содержать как предыдущие продукты из топа, так и продукты,
                // найденные в списке всех продуктов.
                //
                // Это уродство нужно, т.к. SearchView не имеет операции addSuggestions, имеет только
                // swapSuggestions, а нам нужно сперва в него вставить найденые продукты в топе,
                // а затем добавить к ним продукты, найденные в полном списке продуктов.
                // Т.о. мы заменяем 2 операции addSuggestions(fromTop) и addSuggestions(fromAll) на
                // 2 операции swapSuggestions(fromTop) и swapSuggestions(fromTop + fromAll).
                .scan(new ArrayList<List<Foodstuff>>(), (allLists, list) -> {
                    allLists.add(list);
                    return allLists;
                })
                .subscribe((allLists) -> {
                    if (allLists.isEmpty()) {
                        // Поиск может быть отменён не начавшись, тогда мы получим
                        // пустой список, который создали для scan выше
                        return;
                    }
                    // Если показан фрагмент с результатами поисков - покажем результаты в нём,
                    // иначе в подсказках.
                    SearchResultsFragment fragment = SearchResultsFragment.findFragment(mainFragment);
                    if (fragment != null) {
                        fragment.setDisplayedQuery(searchResults.getQuery());
                        fragment.setFoundFromTop(allLists.get(0));
                        if (allLists.size() > 1) {
                            fragment.setFoundFromAll(allLists.get(1));
                        } else {
                            fragment.setFoundFromAll(Collections.emptyList());
                        }
                    } else {
                        List<FoodstuffSearchSuggestion> suggestions =
                                Observable
                                        // List<List<Foodstuff>> -> Observable<List<Foodstuff>>
                                        .fromIterable(allLists)
                                        // Observable<List<Foodstuff>> -> Observable<Foodstuff>
                                        .flatMapIterable(list -> list)
                                        // [1, 2, 2, 3, ..] -> [1, 2, 3, ..]
                                        .distinct()
                                        // [1, 2, 3, .. N] -> [1, 2, 3, .. SEARCH_SUGGESTIONS_NUMBER]
                                        .take(SEARCH_SUGGESTIONS_NUMBER)
                                        // Observable<Foodstuff> -> Observable<FoodstuffSearchSuggestion>
                                        .map(FoodstuffSearchSuggestion::new)
                                        // Observable<FoodstuffSearchSuggestion> -> Single<List<FoodstuffSearchSuggestion>>
                                        .toList()
                                        // Single<List<FoodstuffSearchSuggestion>> -> List<FoodstuffSearchSuggestion>
                                        .blockingGet();

                        Collections.reverse(suggestions);
                        searchView.swapSuggestions(suggestions, false/*animate*/);
                    }
                });
    }

    private void clearSearchQueryIfSearchResultsNotShown() {
        if (SearchResultsFragment.findFragment(mainFragment) != null) {
            return;
        }
        clearSearchQuery();
    }

    private void clearSearchQuery() {
        searchView.clearQuery();
        searchView.clearFocus();
    }

    @Override
    public boolean onActivityBackPressed() {
        SearchResultsFragment fragment = SearchResultsFragment.findFragment(mainFragment);
        if (fragment != null) {
            SearchResultsFragment.closeFragment(mainFragment, fragment);
            // В следующей итерации основного UI-цикла очистим строку поиска, если
            // фрагментов с результатами поиска больше нет
            mainThreadExecutor.execute(this::clearSearchQueryIfSearchResultsNotShown);
            // Мы поглотили событие - сами решили, что должно происходить.
            return true;
        }

        if (!TextUtils.isEmpty(searchView.getQuery())) {
            searchView.clearQuery();
            // Мы поглотили событие - сами решили, что должно происходить.
            return true;
        }

        return false;
    }

    @Override
    public void onMainActivityFragmentSwitch(Fragment oldShownFragment, Fragment newShownFragment) {
        // При смене фрагментов главного экрана, закроем фрагмент с результатами поиска, если он показан.
        if (SearchResultsFragment.findFragment(mainFragment) != null) {
            // Для закрытия фргмента сэмулируем нажатие на back - оно обрабатывается закрытием
            // результатов поиска.
            onActivityBackPressed();
        }
    }
}
