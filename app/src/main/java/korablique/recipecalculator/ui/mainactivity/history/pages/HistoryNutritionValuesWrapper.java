package korablique.recipecalculator.ui.mainactivity.history.pages;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.ui.NutritionValuesWrapper;

public class HistoryNutritionValuesWrapper extends NutritionValuesWrapper {
    private Context context;
    private ViewGroup layout;
    private Nutrition currentNutrition = Nutrition.zero();

    public HistoryNutritionValuesWrapper(Context context, ViewGroup layout) {
        super(context, layout);
        this.context = context;
        this.layout = layout;
    }

    public void setNutrition(Nutrition nutrition, Rates rates) {
        super.setNutrition(nutrition);
        currentNutrition = nutrition;

        TextView proteinRateView = layout.findViewById(R.id.protein_layout).findViewById(R.id.of_n_grams);
        TextView fatsRateView = layout.findViewById(R.id.fats_layout).findViewById(R.id.of_n_grams);
        TextView carbsRateView = layout.findViewById(R.id.carbs_layout).findViewById(R.id.of_n_grams);
        TextView caloriesRateView = layout.findViewById(R.id.calories_layout).findViewById(R.id.of_n_grams);

        proteinRateView.setText(context.getString(R.string.of_n_grams, Math.round(rates.getProtein())));
        fatsRateView.setText(context.getString(R.string.of_n_grams, Math.round(rates.getFats())));
        carbsRateView.setText(context.getString(R.string.of_n_grams, Math.round(rates.getCarbs())));
        caloriesRateView.setText(context.getString(R.string.of_n_calories, Math.round(rates.getCalories())));
    }

    public Nutrition getCurrentNutrition() {
        return currentNutrition;
    }
}
