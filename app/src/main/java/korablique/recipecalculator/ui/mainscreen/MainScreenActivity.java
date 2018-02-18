package korablique.recipecalculator.ui.mainscreen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.PopularProductsUtils;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.ui.history.HistoryActivity;

import static korablique.recipecalculator.IntentConstants.FIND_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;

public class MainScreenActivity extends BaseActivity {
    public static final String CLICKED_FOODSTUFF = "CLICKED_FOODSTUFF";
    public static final String FOODSTUFF_CARD = "FOODSTUFF_CARD";
    private static final int SEARCH_SUGGESTIONS_NUMBER = 3;
    @Inject
    DatabaseWorker databaseWorker;
    @Inject
    HistoryWorker historyWorker;
    AdapterParent adapterParent;
    private FoodstuffsAdapterChild foodstuffAdapterChild;
    private List<Foodstuff> top;
    private List<Foodstuff> all;
    private SelectedFoodstuffsSnackbar snackbar;
    private boolean isSnackbarShown;
    private FoodstuffsAdapterChild.ClickObserver clickObserver = (foodstuff, displayedPosition) -> {
        CardDialog.showCard(MainScreenActivity.this, foodstuff);
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        snackbar = new SelectedFoodstuffsSnackbar(MainScreenActivity.this);
        snackbar.setOnBasketClickRunnable(() -> {
            StringBuilder selectedFoodstuffs = new StringBuilder();
            for (Foodstuff selectedFoodstuff : snackbar.getSelectedFoodstuffs()) {
                selectedFoodstuffs.append(selectedFoodstuff.getName());
                selectedFoodstuffs.append(", ");
            }
            Toast.makeText(MainScreenActivity.this, "selected products: " + selectedFoodstuffs, Toast.LENGTH_LONG).show();
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener((item) -> {
            switch (item.getItemId()) {
                case R.id.menu_item_history:
                    Intent historyIntent = new Intent(MainScreenActivity.this, HistoryActivity.class);
                    startActivity(historyIntent);
                    break;
            }
            return false;
        });

        RecyclerView recyclerView = findViewById(R.id.main_screen_recycler_view);
        adapterParent = new AdapterParent();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapterParent);

        // Сначала делаем запросы в БД, в коллбеках сохраняем результаты,
        // а затем уже добавляем в адаптеры элементы.
        // Это нужно для того, чтобы элементы на экране загружались все сразу

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
                    databaseWorker.requestFoodstuffsByIds(MainScreenActivity.this, topFoodstuffIds, (foodstuffs) -> {
                        top = new ArrayList<>();
                        top.addAll(foodstuffs);
                        attemptToAddElementsToAdapters();
                    });
                });

        // получаем все продукты
        int batchSize = 100;
        databaseWorker.requestListedFoodstuffsFromDb(MainScreenActivity.this, batchSize, (foodstuffs) -> {
            if (all == null) {
                all = new ArrayList<>();
            }
            all.addAll(foodstuffs);
            attemptToAddElementsToAdapters();
        });

        FloatingSearchView searchView = findViewById(R.id.floating_search_view);
        searchView.setOnQueryChangeListener((oldQuery, newQuery) -> {
            //get suggestions based on newQuery
            databaseWorker.requestFoodstuffsLike(
                    MainScreenActivity.this, newQuery, SEARCH_SUGGESTIONS_NUMBER, foodstuffs -> {
                //pass them on to the search view
                List<SearchSuggestion> newSuggestions = new ArrayList<>();
                for (Foodstuff foodstuff : foodstuffs) {
                    SearchSuggestion suggestion = new FoodstuffSearchSuggestion(foodstuff);
                    newSuggestions.add(suggestion);
                }
                searchView.swapSuggestions(newSuggestions);
            });
        });

        searchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                CardDialog.showCard(MainScreenActivity.this, ((FoodstuffSearchSuggestion)searchSuggestion).getFoodstuff());
            }

            @Override
            public void onSearchAction(String currentQuery) {
                ListOfFoodstuffsActivity.performSearch(MainScreenActivity.this, currentQuery);
            }
        });

        searchView.setOnMenuItemClickListener(item -> {
            String query = searchView.getQuery().trim();
            ListOfFoodstuffsActivity.performSearch(this, query);
        });

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentAttached(FragmentManager fm, Fragment f, Context context) {
                super.onFragmentAttached(fm, f, context);
                if (f instanceof CardDialog) {
                    CardDialog cardDialog = (CardDialog) f;
                    cardDialog.setOnAddFoodstuffButtonClickListener(foodstuff -> {
                        cardDialog.dismiss();
                        snackbar.addFoodstuff(foodstuff);
                        if (!isSnackbarShown) {
                            snackbar.show();
                        }
                        isSnackbarShown = true;
                    });
                } else {
                    throw new IllegalStateException("Unexpected type of fragment");
                }
            }
        }, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff foodstuff = data.getParcelableExtra(SEARCH_RESULT);
                CardDialog.showCard(MainScreenActivity.this, foodstuff);
            }
        }
    }

    private void attemptToAddElementsToAdapters() {
        if (top == null || all == null) {
            return;
        }
        if (!top.isEmpty()) {
            FoodstuffsAdapterChild topAdapterChild = new FoodstuffsAdapterChild(
                    MainScreenActivity.this, clickObserver, R.layout.top_foodstuffs_header);
            adapterParent.addChild(topAdapterChild);
            topAdapterChild.addItems(top);
        }
        // если топ пустой, то топ-адаптер не нужно создавать, чтобы не было заголовка

        if (foodstuffAdapterChild == null) {
            foodstuffAdapterChild = new FoodstuffsAdapterChild(
                    MainScreenActivity.this, clickObserver, R.layout.all_foodstuffs_header);
            adapterParent.addChild(foodstuffAdapterChild);
        }
        foodstuffAdapterChild.addItems(all);
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        snackbar.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        snackbar.onRestoreInstanceState(savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
    }
}
