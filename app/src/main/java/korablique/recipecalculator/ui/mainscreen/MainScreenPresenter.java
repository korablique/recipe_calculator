package korablique.recipecalculator.ui.mainscreen;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.PopularProductsUtils;
import korablique.recipecalculator.ui.card.NewCard;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.ui.history.HistoryActivity;

import static android.app.Activity.RESULT_OK;
import static korablique.recipecalculator.IntentConstants.FIND_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;

public class MainScreenPresenter {
    private static final int SEARCH_SUGGESTIONS_NUMBER = 3;
    private final MainScreenView view;
    private final Context context;
    private final DatabaseWorker databaseWorker;
    private final HistoryWorker historyWorker;

    private AdapterParent adapterParent;
    private FoodstuffsAdapterChild foodstuffAdapterChild;
    private List<Foodstuff> top;
    private List<Foodstuff> all;

    private FoodstuffsAdapterChild.ClickObserver clickObserver;

    public MainScreenPresenter(MainScreenView view, Activity context, DatabaseWorker databaseWorker, HistoryWorker historyWorker) {
        this.view = view;
        this.context = context;
        this.databaseWorker = databaseWorker;
        this.historyWorker = historyWorker;

        clickObserver = (foodstuff, displayedPosition) -> {
            view.showCard(foodstuff);
        };

        view.setOnSnackbarClickListener(new MainScreenView.OnSnackbarBasketClickListener() {
            @Override
            public void onClick(List<Foodstuff> selectedFoodstuffs) {
                StringBuilder selectedFoodstuffsStringBuilder = new StringBuilder();
                for (Foodstuff selectedFoodstuff : selectedFoodstuffs) {
                    selectedFoodstuffsStringBuilder.append(selectedFoodstuff.getName());
                    selectedFoodstuffsStringBuilder.append(", ");
                }
                Toast.makeText(context, "selected products: " + selectedFoodstuffsStringBuilder, Toast.LENGTH_LONG).show();
            }
        });

        view.setOnNavigationItemSelectedListener(new MainScreenView.OnNavigationItemSelectedListener() {
            @Override
            public void onSelect(MainScreenView.NavigationItem item) {
                if (item == MainScreenView.NavigationItem.HISTORY) {
                    Intent historyIntent = new Intent(context, HistoryActivity.class);
                    context.startActivity(historyIntent);
                }
            }
        });

        adapterParent = new AdapterParent();
        view.setAdapter(adapterParent);

        view.setCardDialogAddButtonClickListener(new NewCard.OnAddFoodstuffButtonClickListener() {
            @Override
            public void onClick(Foodstuff foodstuff) {
                view.hideCard();
                view.addSnackbarFoodstuff(foodstuff);
                view.showSnackbar();
            }
        });

        view.setOnSearchQueryChangeListener(new MainScreenView.OnSearchQueryChangeListener() {
            @Override
            public void onSearchQueryChange(String newQuery) {
                // model - БД, presenter - взаимодействия с view (searchView.swapSuggestions(newSuggestions);)
                //get suggestions based on newQuery

                databaseWorker.requestFoodstuffsLike(
                        context, newQuery, SEARCH_SUGGESTIONS_NUMBER, foodstuffs -> {
                            //pass them on to the search view
                            List<FoodstuffSearchSuggestion> newSuggestions = new ArrayList<>();
                            for (Foodstuff foodstuff : foodstuffs) {
                                FoodstuffSearchSuggestion suggestion = new FoodstuffSearchSuggestion(foodstuff);
                                newSuggestions.add(suggestion);
                            }
                            view.setSearchSuggestions(newSuggestions);
                        });
            }
        });

        view.setOnSearchListener(new MainScreenView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(FoodstuffSearchSuggestion suggestion) {
                view.showCard(suggestion.getFoodstuff());
            }

            @Override
            public void onSearchAction(String query) {
                ListOfFoodstuffsActivity.performSearch(context, query);
            }
        });

        view.setOnActivityResultListener((requestCode, resultCode, data) -> {
            if (requestCode == FIND_FOODSTUFF_REQUEST) {
                if (resultCode == RESULT_OK) {
                    Foodstuff foodstuff = data.getParcelableExtra(SEARCH_RESULT);
                    view.showCard(foodstuff);
                }
            }
        });

        // Сначала делаем запросы в БД, в коллбеках сохраняем результаты,
        // а затем уже добавляем в адаптеры элементы.
        // Это нужно для того, чтобы элементы на экране загружались все сразу

        // model (взаимодействие с адаптерами - presenter)
        // получаем топ продуктов
        List<Long> foodstuffsIds = new ArrayList<>(); // это айдишники всех продуктов за период
        historyWorker.requestFoodstuffsIdsFromHistoryForPeriod(
                0,
                Long.MAX_VALUE,
                (ids) -> {
                    foodstuffsIds.addAll(ids);
                    List<PopularProductsUtils.FoodstuffFrequency> topList = PopularProductsUtils.getTop(foodstuffsIds); // это топ из них
                    List<Long> topFoodstuffIds = new ArrayList<>(); // это айдишники топа
                    for (int index = 0; index < topList.size() && index < 5; ++index) {
                        topFoodstuffIds.add(topList.get(index).getFoodstuffId());
                    }
                    databaseWorker.requestFoodstuffsByIds(context, topFoodstuffIds, (foodstuffs) -> {
                        top = new ArrayList<>();
                        top.addAll(foodstuffs);
                        attemptToAddElementsToAdapters();
                    });
                });

        // получаем все продукты
        int batchSize = 100;
        databaseWorker.requestListedFoodstuffsFromDb(context, batchSize, (foodstuffs) -> {
            if (all == null) {
                all = new ArrayList<>();
            }
            all.addAll(foodstuffs);
            attemptToAddElementsToAdapters();
        });
    }

    private void attemptToAddElementsToAdapters() {
        if (top == null || all == null) {
            return;
        }
        if (!top.isEmpty()) {
            FoodstuffsAdapterChild topAdapterChild = new FoodstuffsAdapterChild(
                    context, clickObserver, R.layout.top_foodstuffs_header);
            adapterParent.addChild(topAdapterChild);
            topAdapterChild.addItems(top);
        }
        // если топ пустой, то топ-адаптер не нужно создавать, чтобы не было заголовка

        if (foodstuffAdapterChild == null) {
            foodstuffAdapterChild = new FoodstuffsAdapterChild(
                    context, clickObserver, R.layout.all_foodstuffs_header);
            adapterParent.addChild(foodstuffAdapterChild);
        }
        foodstuffAdapterChild.addItems(all);
    }

}
