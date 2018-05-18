package korablique.recipecalculator.ui.mainscreen;


import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.card.NewCard;

public class MainScreenViewImpl implements MainScreenView {
    private FragmentActivity activity;
    private SelectedFoodstuffsSnackbar snackbar;
    private BottomNavigationView bottomNavigationView;
    private RecyclerView recyclerView;
    private FloatingSearchView searchView;
    private NewCard.OnAddFoodstuffButtonClickListener cardDialogListener;
    // Этот флаг нужен, чтобы приложение не крешило при показе диалога, когда тот показывается в момент,
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
    private boolean isUiHidden;
    // Действие, которое нужно выполнить с диалогом после savedInstanceState (показ или скрытие диалога)
    private Runnable dialogAction;

    public MainScreenViewImpl(FragmentActivity activity) {
        this.activity = activity;
    }

    @Override
    public void initActivity() {
        activity.setContentView(R.layout.activity_main_screen);

        snackbar = new SelectedFoodstuffsSnackbar(activity);
        bottomNavigationView = activity.findViewById(R.id.navigation);
        searchView = activity.findViewById(R.id.floating_search_view);

        recyclerView = activity.findViewById(R.id.main_screen_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void saveState(Bundle outState) {
        snackbar.onSaveInstanceState(outState);
    }

    @Override
    public void restoreState(Bundle savedState) {
        snackbar.onRestoreInstanceState(savedState);
    }

    @Override
    public void onUIShown() {
        isUiHidden = false;
        if (dialogAction != null) {
            dialogAction.run();
        }
    }

    @Override
    public void onUiHidden() {
        isUiHidden = true;
    }

    @Override
    public void showSnackbar() {
        snackbar.show();
    }

    @Override
    public void hideSnackbar() {
        snackbar.hide();
    }

    @Override
    public void addSnackbarFoodstuff(Foodstuff foodstuff) {
        snackbar.addFoodstuff(foodstuff);
    }

    @Override
    public void setOnSnackbarClickListener(OnSnackbarBasketClickListener listener) {
        snackbar.setOnBasketClickRunnable(() -> listener.onClick(snackbar.getSelectedFoodstuffs()));
    }

    @Override
    public void showCard(Foodstuff foodstuff) {
        dialogAction = () -> {
            CardDialog cardDialog = CardDialog.showCard(activity, foodstuff);
            cardDialog.setOnAddFoodstuffButtonClickListener(cardDialogListener);
            dialogAction = null;
        };
        if (!isUiHidden) {
            dialogAction.run();
        }
    }

    @Override
    public void hideCard() {
        dialogAction = () -> {
            CardDialog.hideCard(activity);
            dialogAction = null;
        };
        if (!isUiHidden) {
            dialogAction.run();
        }
    }

    @Override
    public void setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener listener) {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_item_history:
                    listener.onSelect(NavigationItem.HISTORY);
                    break;
            }
            return false;
        });
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void setCardDialogAddButtonClickListener(NewCard.OnAddFoodstuffButtonClickListener listener) {
        cardDialogListener = listener;
        CardDialog cardDialog = CardDialog.findCard(activity);
        if (cardDialog != null) {
            cardDialog.setOnAddFoodstuffButtonClickListener(cardDialogListener);
        }
    }

    @Override
    public void setOnSearchQueryChangeListener(OnSearchQueryChangeListener listener) {
        searchView.setOnQueryChangeListener((oldQuery, newQuery) -> {
            listener.onSearchQueryChange(newQuery);
        });
    }

    @Override
    public void setSearchSuggestions(List<FoodstuffSearchSuggestion> suggestions) {
        searchView.swapSuggestions(suggestions);
    }

    @Override
    public void setOnSearchListener(OnSearchListener listener) {
        searchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                listener.onSuggestionClicked((FoodstuffSearchSuggestion) searchSuggestion);
            }

            // когда пользователь нажал на клавиатуре enter
            @Override
            public void onSearchAction(String currentQuery) {
                currentQuery = currentQuery.trim();
                listener.onSearchAction(currentQuery);
            }
        });

        // когда пользователь нажал кнопку лупы в searchView
        searchView.setOnMenuItemClickListener(item -> {
            String query = searchView.getQuery().trim();
            listener.onSearchAction(query);
        });
    }
}
