package korablique.recipecalculator.ui.mainscreen;


import android.content.Intent;
import android.support.v7.widget.RecyclerView;

import java.util.List;

import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.card.NewCard;

public interface MainScreenView {
    void showSnackbar();
    void hideSnackbar();
    void showCard(Foodstuff foodstuff);
    void hideCard();
    void setOnSnackbarClickListener(OnSnackbarBasketClickListener listener);
    void setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener listener);
    void setAdapter(RecyclerView.Adapter adapter);
    void setCardDialogAddButtonClickListener(NewCard.OnAddFoodstuffButtonClickListener listener);
    void setOnSearchQueryChangeListener(OnSearchQueryChangeListener listener);
    void setSearchSuggestions(List<FoodstuffSearchSuggestion> suggestions);
    void addSnackbarFoodstuff(Foodstuff foodstuff);
    void setOnSearchListener(OnSearchListener listener);
    void setOnActivityResultListener(OnActivityResultListener listener);

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

    interface OnActivityResultListener {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    enum NavigationItem {
        HISTORY, PROFILE, MAINSCREEN
    }
}
