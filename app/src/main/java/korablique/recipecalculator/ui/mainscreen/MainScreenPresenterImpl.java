package korablique.recipecalculator.ui.mainscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.ui.nestingadapters.AdapterParent;
import korablique.recipecalculator.ui.nestingadapters.FoodstuffsAdapterChild;
import korablique.recipecalculator.ui.nestingadapters.SingleItemAdapterChild;

import static android.app.Activity.RESULT_OK;
import static korablique.recipecalculator.IntentConstants.EDIT_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.EDIT_RESULT;
import static korablique.recipecalculator.IntentConstants.FIND_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;
import static korablique.recipecalculator.ui.card.NewCard.EDITED_FOODSTUFF;
import static korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity.EDIT_FOODSTUFF_ACTION;

public class MainScreenPresenterImpl implements MainScreenPresenter {
    private static final int SEARCH_SUGGESTIONS_NUMBER = 3;
    private static final int TOP_LIMIT = 5;
    private final MainScreenView view;
    private final Activity context;
    private final MainScreenModel model;
    private AdapterParent adapterParent;
    private FoodstuffsAdapterChild foodstuffAdapterChild;
    private List<Foodstuff> top;
    private List<Foodstuff> all;

    private FoodstuffsAdapterChild.ClickObserver clickObserver;

    public MainScreenPresenterImpl(MainScreenView view, MainScreenModel model, Activity context) {
        this.view = view;
        this.model = model;
        this.context = context;
    }

    @Override
    public void onActivityCreate() {
        view.initActivity();

        clickObserver = (foodstuff, displayedPosition) -> {
            view.showCard(foodstuff);
        };

        view.setOnSnackbarClickListener(new MainScreenView.OnSnackbarBasketClickListener() {
            @Override
            public void onClick(List<Foodstuff> selectedFoodstuffs) {
                BucketListActivity.start(new ArrayList<>(selectedFoodstuffs), context);
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

        view.setCardDialogAddButtonClickListener(foodstuff -> {
            view.hideCard();
            view.addSnackbarFoodstuff(foodstuff);
            view.showSnackbar();
        });

        view.setCardDialogOnEditButtonClickListener(foodstuff -> {
            Intent intent = new Intent(context, EditFoodstuffActivity.class);
            intent.setAction(EDIT_FOODSTUFF_ACTION);
            intent.putExtra(EDITED_FOODSTUFF, foodstuff);
            context.startActivityForResult(intent, EDIT_FOODSTUFF_REQUEST);
        });

        view.setOnSearchQueryChangeListener(new MainScreenView.OnSearchQueryChangeListener() {
            @Override
            public void onSearchQueryChange(String newQuery) {
                //get suggestions based on newQuery
                model.requestFoodstuffsLike(
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

        model.requestTopFoodstuffs(context, TOP_LIMIT, (foodstuffs) -> {
            top = new ArrayList<>();
            top.addAll(foodstuffs);
            attemptToAddElementsToAdapters();
        });

        model.requestAllFoodstuffs(context, (foodstuffs) -> {
            if (all == null) {
                all = new ArrayList<>();
            }
            all.addAll(foodstuffs);
            attemptToAddElementsToAdapters();
        });
    }

    @Override
    public void onActivitySaveState(Bundle outState) {
        view.saveState(outState);
    }

    @Override
    public void onActivityRestoreState(Bundle savedInstanceState) {
        view.restoreState(savedInstanceState);
    }

    @Override
    public void onActivityResume() {
        view.onUIShown();
    }

    @Override
    public void onActivityPause() {
        view.onUiHidden();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff foodstuff = data.getParcelableExtra(SEARCH_RESULT);
                view.showCard(foodstuff);
            }
        } else if (requestCode == EDIT_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff editedFoodstuff = data.getParcelableExtra(EDIT_RESULT);
                view.showCard(editedFoodstuff);
            }
        }
    }

    private void attemptToAddElementsToAdapters() {
        if (top == null || all == null) {
            return;
        }
        if (!top.isEmpty()) {
            FoodstuffsAdapterChild topAdapterChild = new FoodstuffsAdapterChild(context, clickObserver);
            SingleItemAdapterChild topTitle = new SingleItemAdapterChild(R.layout.top_foodstuffs_header);
            adapterParent.addChild(topTitle);
            adapterParent.addChild(topAdapterChild);
            topAdapterChild.addItems(top);
        }
        // если топ пустой, то топ-адаптер не нужно создавать, чтобы не было заголовка

        if (foodstuffAdapterChild == null) {
            SingleItemAdapterChild.Observer observer = v -> {
                View addNewFoodstuffButton = v.findViewById(R.id.add_new_foodstuff);
                addNewFoodstuffButton.setOnClickListener(v1 -> {
                    Intent intent = new Intent(context, EditFoodstuffActivity.class);
                    context.startActivity(intent);
                });
            };
            SingleItemAdapterChild foodstuffsTitle = new SingleItemAdapterChild(
                    R.layout.all_foodstuffs_header, observer);
            foodstuffAdapterChild = new FoodstuffsAdapterChild(context, clickObserver);
            adapterParent.addChild(foodstuffsTitle);
            adapterParent.addChild(foodstuffAdapterChild);
        }
        foodstuffAdapterChild.addItems(all);
    }
}
