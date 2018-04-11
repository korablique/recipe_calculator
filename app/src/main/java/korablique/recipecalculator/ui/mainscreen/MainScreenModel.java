package korablique.recipecalculator.ui.mainscreen;


import android.content.Context;

import java.util.List;

import korablique.recipecalculator.base.Callback;
import korablique.recipecalculator.model.Foodstuff;

public interface MainScreenModel {
    void requestTopFoodstuffs(Context context, int limit, Callback<List<Foodstuff>> callback);
    void requestAllFoodstuffs(Context context, Callback<List<Foodstuff>> callback);
    void requestFoodstuffsLike(Context context, String query, int suggestionsNumber, Callback<List<Foodstuff>> callback);
}
