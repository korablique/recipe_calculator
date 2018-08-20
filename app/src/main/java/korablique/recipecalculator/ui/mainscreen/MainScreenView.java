package korablique.recipecalculator.ui.mainscreen;

import android.support.v7.widget.RecyclerView;

import java.util.List;

import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.card.NewCard;

public interface MainScreenView {
    void initActivity();
    void showSnackbar();
    void addSnackbarFoodstuff(Foodstuff foodstuff);
    void setOnSnackbarClickListener(OnSnackbarBasketClickListener listener);
    void showCard(Foodstuff foodstuff);
    void hideCard();
    void setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener listener);
    void setAdapter(RecyclerView.Adapter adapter);
    void setCardDialogAddButtonClickListener(NewCard.OnAddFoodstuffButtonClickListener listener);
    void setOnSearchQueryChangeListener(OnSearchQueryChangeListener listener);
    void setSearchSuggestions(List<FoodstuffSearchSuggestion> suggestions);
    void setOnSearchListener(OnSearchListener listener);

    interface OnSnackbarBasketClickListener {
        void onClick(List<Foodstuff> selectedFoodstuffs);
    }

    interface OnNavigationItemSelectedListener {
        void onSelect(NavigationItem item);
    }

    interface OnSearchQueryChangeListener {
        void onSearchQueryChange(String newQuery);
    }

    interface OnSearchListener {
        void onSuggestionClicked(FoodstuffSearchSuggestion suggestion);
        void onSearchAction(String query);
    }

    enum NavigationItem {
        HISTORY, PROFILE, MAINSCREEN
    }
}
