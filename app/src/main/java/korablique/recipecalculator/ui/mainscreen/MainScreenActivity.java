package korablique.recipecalculator.ui.mainscreen;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.card.NewCard;

public class MainScreenActivity extends BaseActivity implements MainScreenView {
    public static final String CLICKED_FOODSTUFF = "CLICKED_FOODSTUFF";
    @Inject
    MainScreenPresenter presenter;

    private SelectedFoodstuffsSnackbar snackbar;
    private BottomNavigationView bottomNavigationView;
    private RecyclerView recyclerView;
    private FloatingSearchView searchView;
    private NewCard.OnAddFoodstuffButtonClickListener cardDialogListener;
    private OnActivityResultListener onActivityResultListener;

    // Этот флаг нужен, чтобы приложение не крешило при показе диалога, когда тот показывается в момент,
    // когда активити в фоне (запаузена).
    // fragment manager не позваляет выполнять никакие операции с фрагментами, пока активити запаузена -
    // ведь fragment manager уже сохранил состояние всех фрагментов,
    // и ещё раз это сделать до резьюма активити невозможно (больше не вызовается Activity.onSaveInstanseState).
    // Чтобы сохранение стейта случилось ещё раз, активити должна выйти на передний план.
    // А когда активити в фоне, неизвестно, выйдет ли она на передний план - fm от этой неизвестности страхуется исключением.
    // (Если не выйдет, то будет потеря состояния.)
    // (Тут иерархичное подчинение - ОС требует от Активити сохранение стейта,
    // Активти требует от всех своих компонентов, в т.ч. от fm,
    // а fm требует сохранение стейта от всех своих компонентов, и т.д.)
    private boolean activitySavedInstanceStateDone;
    // Действие, которое нужно выполнить с диалогом после savedInstanceState (показ или скрытие диалога)
    private Runnable dialogAction;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        snackbar = new SelectedFoodstuffsSnackbar(MainScreenActivity.this);
        bottomNavigationView = findViewById(R.id.navigation);
        searchView = findViewById(R.id.floating_search_view);

        recyclerView = findViewById(R.id.main_screen_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        presenter.onActivityCreate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        onActivityResultListener.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        snackbar.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
        activitySavedInstanceStateDone = true;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        snackbar.onRestoreInstanceState(savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        activitySavedInstanceStateDone = false;
        if (dialogAction != null) {
            dialogAction.run();
        }
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
    public void showCard(Foodstuff foodstuff) {
        dialogAction = () -> {
            CardDialog cardDialog = CardDialog.showCard(MainScreenActivity.this, foodstuff);
            cardDialog.setOnAddFoodstuffButtonClickListener(cardDialogListener);
            dialogAction = null;
        };
        if (!activitySavedInstanceStateDone) {
            dialogAction.run();
        }
    }

    @Override
    public void hideCard() {
        dialogAction = () -> {
            CardDialog.hideCard(MainScreenActivity.this);
            dialogAction = null;
        };
        if (!activitySavedInstanceStateDone) {
            dialogAction.run();
        }
    }

    @Override
    public void setOnSnackbarClickListener(OnSnackbarBasketClickListener listener) {
        snackbar.setOnBasketClickRunnable(() -> listener.onClick(snackbar.getSelectedFoodstuffs()));
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
        CardDialog cardDialog = CardDialog.findCard(this);
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
    public void addSnackbarFoodstuff(Foodstuff foodstuff) {
        snackbar.addFoodstuff(foodstuff);
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

    @Override
    public void setOnActivityResultListener(OnActivityResultListener listener) {
        onActivityResultListener = listener;
    }
}
