package korablique.recipecalculator.ui.mainscreen;

import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.PopularProductsUtils;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.KeyboardHandler;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.card.NewCard;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.ui.nestingadapters.AdapterParent;
import korablique.recipecalculator.ui.nestingadapters.FoodstuffsAdapterChild;
import korablique.recipecalculator.ui.nestingadapters.SingleItemAdapterChild;

import static android.app.Activity.RESULT_OK;
import static korablique.recipecalculator.IntentConstants.EDIT_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.EDIT_RESULT;
import static korablique.recipecalculator.IntentConstants.FIND_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;
import static korablique.recipecalculator.ui.mainscreen.SearchResultsFragment.REQUEST;
import static korablique.recipecalculator.ui.mainscreen.SearchResultsFragment.SEARCH_RESULTS;

public class MainScreenActivityController extends ActivityCallbacks.Observer {
    private static final int SEARCH_SUGGESTIONS_NUMBER = 3;
    private static final int TOP_LIMIT = 5;
    private final FragmentActivity context;
    private final DatabaseWorker databaseWorker;
    private final HistoryWorker historyWorker;
    private final Lifecycle lifecycle;
    private AdapterParent adapterParent;
    private FoodstuffsAdapterChild topAdapterChild;
    private FoodstuffsAdapterChild foodstuffAdapterChild;
    private List<Foodstuff> top;
    private List<Foodstuff> all;

    private SelectedFoodstuffsSnackbar snackbar;
    private BottomNavigationView bottomNavigationView;
    private RecyclerView recyclerView;
    private FloatingSearchView searchView;
    private NewCard.OnAddFoodstuffButtonClickListener cardDialogListener;
    private NewCard.OnEditButtonClickListener cardDialogOnEditButtonClickListener;
    // Действие, которое нужно выполнить с диалогом после savedInstanceState (показ или скрытие диалога)
    // Поле нужно, чтобы приложение не крешило при показе диалога, когда тот показывается в момент,
    // когда активити в фоне (запаузена).
    // fragment manager не позваляет выполнять никакие операции с фрагментами, пока активити запаузена -
    // ведь fragment manager уже сохранил состояние всех фрагментов,
    // и ещё раз это сделать до резьюма активити невозможно (больше не вызовается Activity.onSaveInstanceState).
    // Чтобы сохранение стейта случилось ещё раз, активити должна выйти на передний план.
    // А когда активити в фоне, неизвестно, выйдет ли она на передний план - fm от этой неизвестности страхуется исключением.
    // (Если не выйдет, то будет потеря состояния.)
    // (Тут иерархичное подчинение - ОС требует от Активити сохранение стейта,
    // Активти требует от всех своих компонентов, в т.ч. от fm,
    // а fm требует сохранение стейта от всех своих компонентов, и т.д.)
    private Runnable dialogAction;

    public MainScreenActivityController(
            MainScreenActivity context,
            DatabaseWorker databaseWorker,
            HistoryWorker historyWorker,
            ActivityCallbacks activityCallbacks,
            Lifecycle lifecycle) {
        this.context = context;
        this.databaseWorker = databaseWorker;
        this.historyWorker = historyWorker;
        this.lifecycle = lifecycle;
        activityCallbacks.addObserver(this);
    }

    @Override
    public void onActivityCreate(Bundle savedInstanceState) {
        initActivity();

        BucketList bucketList = BucketList.getInstance();
        bucketList.addObserver(weightedFoodstuff -> {
            snackbar.addFoodstuff(weightedFoodstuff);
            snackbar.show();
        });

        snackbar.setOnBasketClickRunnable(() -> {
            BucketListActivity.start(new ArrayList<>(snackbar.getSelectedFoodstuffs()), context);
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_item_history:
                    Intent historyIntent = new Intent(context, HistoryActivity.class);
                    context.startActivity(historyIntent);
                    break;
            }
            return false;
        });

        adapterParent = new AdapterParent();
        recyclerView.setAdapter(adapterParent);

        cardDialogListener = foodstuff -> {
            hideCard();
            bucketList.add(foodstuff);
            snackbar.show();
            new KeyboardHandler(context).hideKeyBoard();
            searchView.clearQuery();
        };
        cardDialogOnEditButtonClickListener = foodstuff -> {
            EditFoodstuffActivity.startForEditing(context, foodstuff);
        };

        CardDialog cardDialog = CardDialog.findCard(context);
        if (cardDialog != null) {
            cardDialog.setOnAddFoodstuffButtonClickListener(cardDialogListener);
            cardDialog.setOnEditButtonClickListener(cardDialogOnEditButtonClickListener);
        }



        requestTopFoodstuffs(context, TOP_LIMIT, (foodstuffs) -> {
            top = new ArrayList<>();
            top.addAll(foodstuffs);
            attemptToAddElementsToAdapters();
        });

        int batchSize = 100;
        databaseWorker.requestListedFoodstuffsFromDb(context, batchSize, (foodstuffs) -> {
            if (all == null) {
                all = new ArrayList<>();
            }
            all.addAll(foodstuffs);
            attemptToAddElementsToAdapters();
        }, () -> {
            searchView.setOnQueryChangeListener((oldQuery, newQuery) -> {
                //get suggestions based on newQuery
                databaseWorker.requestFoodstuffsLike(context, newQuery, SEARCH_SUGGESTIONS_NUMBER, foodstuffs -> {
                    //pass them on to the search view
                    List<FoodstuffSearchSuggestion> newSuggestions = new ArrayList<>();
                    for (Foodstuff foodstuff : foodstuffs) {
                        FoodstuffSearchSuggestion suggestion = new FoodstuffSearchSuggestion(foodstuff);
                        newSuggestions.add(suggestion);
                    }
                    searchView.swapSuggestions(newSuggestions);
                });
            });

            searchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
                @Override
                public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                    FoodstuffSearchSuggestion suggestion = (FoodstuffSearchSuggestion) searchSuggestion;
                    showCard(suggestion.getFoodstuff());
                }

                // когда пользователь нажал на клавиатуре enter
                @Override
                public void onSearchAction(String currentQuery) {
                    performSearch();
                }
            });

            // когда пользователь нажал кнопку лупы в searchView
            searchView.setOnMenuItemClickListener(item -> {
                performSearch();
            });
        });
    }

    private void initActivity() {
        context.setContentView(R.layout.activity_main_screen);

        snackbar = new SelectedFoodstuffsSnackbar(context);
        bottomNavigationView = context.findViewById(R.id.navigation);
        searchView = context.findViewById(R.id.floating_search_view);

        recyclerView = context.findViewById(R.id.main_screen_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void requestTopFoodstuffs(Context context, int limit, Callback<List<Foodstuff>> callback) {
        // Сначала делаем запросы в БД, в коллбеках сохраняем результаты,
        // а затем уже добавляем в адаптеры элементы.
        // Это нужно для того, чтобы элементы на экране загружались все сразу
        List<Long> foodstuffsIds = new ArrayList<>(); // это айдишники всех продуктов за период
        historyWorker.requestFoodstuffsIdsFromHistoryForPeriod(
                0,
                Long.MAX_VALUE,
                (ids) -> {
                    foodstuffsIds.addAll(ids);
                    List<PopularProductsUtils.FoodstuffFrequency> topList = PopularProductsUtils.getTop(foodstuffsIds); // это топ из них
                    List<Long> topFoodstuffIds = new ArrayList<>(); // это айдишники топа
                    for (int index = 0; index < topList.size() && index < limit; ++index) {
                        topFoodstuffIds.add(topList.get(index).getFoodstuffId());
                    }
                    databaseWorker.requestFoodstuffsByIds(context, topFoodstuffIds, (foodstuffs) -> {
                        callback.onResult(foodstuffs);
                    });
                });
    }

    private void attemptToAddElementsToAdapters() {
        if (top == null || all == null) {
            return;
        }
        if (!top.isEmpty()) {
            if (topAdapterChild == null) {
                topAdapterChild = new FoodstuffsAdapterChild(context, (foodstuff, pos) -> showCard(foodstuff));
                SingleItemAdapterChild topTitle = new SingleItemAdapterChild(R.layout.top_foodstuffs_header);
                adapterParent.addChild(topTitle);
                adapterParent.addChild(topAdapterChild);
                topAdapterChild.addItems(top);
            }
        }
        // если топ пустой, то топ-адаптер не нужно создавать, чтобы не было заголовка

        if (foodstuffAdapterChild == null) {
            SingleItemAdapterChild.Observer observer = v -> {
                View addNewFoodstuffButton = v.findViewById(R.id.add_new_foodstuff);
                addNewFoodstuffButton.setOnClickListener(v1 -> {
                    EditFoodstuffActivity.startForCreation(context);
                });
            };
            SingleItemAdapterChild foodstuffsTitle = new SingleItemAdapterChild(
                    R.layout.all_foodstuffs_header, observer);
            foodstuffAdapterChild = new FoodstuffsAdapterChild(context, (foodstuff, pos) -> showCard(foodstuff));
            adapterParent.addChild(foodstuffsTitle);
            adapterParent.addChild(foodstuffAdapterChild);
        }
        foodstuffAdapterChild.addItems(all);
    }

    @Override
    public void onActivityResume() {
        if (dialogAction != null) {
            dialogAction.run();
        }

        BucketList bucketList = BucketList.getInstance();
        snackbar.update(bucketList.getList());
    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState) {
        snackbar.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityRestoreInstanceState(Bundle savedInstanceState) {
        snackbar.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff foodstuff = data.getParcelableExtra(SEARCH_RESULT);
                showCard(foodstuff);
            }
        } else if (requestCode == EDIT_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff editedFoodstuff = data.getParcelableExtra(EDIT_RESULT);
                topAdapterChild.replaceItem(editedFoodstuff);
                foodstuffAdapterChild.replaceItem(editedFoodstuff);
                showCard(editedFoodstuff);
            }
        }
    }

    private void showCard(Foodstuff foodstuff) {
        dialogAction = () -> {
            CardDialog cardDialog = CardDialog.showCard(context, foodstuff);
            cardDialog.setOnAddFoodstuffButtonClickListener(cardDialogListener);
            cardDialog.setOnEditButtonClickListener(cardDialogOnEditButtonClickListener);
            dialogAction = null;
        };
        if (lifecycle.getCurrentState() == Lifecycle.State.RESUMED) {
            dialogAction.run();
        }
    }

    private void hideCard() {
        dialogAction = () -> {
            CardDialog.hideCard(context);
            dialogAction = null;
        };
        if (lifecycle.getCurrentState() == Lifecycle.State.RESUMED) {
            dialogAction.run();
        }
    }

    private void performSearch() {
        String wanted = searchView.getQuery().toLowerCase().trim();
        List<Foodstuff> searchResults = new ArrayList<>();
        for (Foodstuff f : all) {
            if (f.getName().toLowerCase().contains(wanted)) {
                searchResults.add(f);
            }
        }
        SearchResultsFragment.show(wanted, searchResults, context);
    }
}
