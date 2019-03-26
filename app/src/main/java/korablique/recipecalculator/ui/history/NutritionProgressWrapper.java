package korablique.recipecalculator.ui.history;

import android.content.res.ColorStateList;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.Rates;

public class NutritionProgressWrapper {
    private ProgressBar proteinProgress;
    private ProgressBar fatsProgress;
    private ProgressBar carbsProgress;
    private ProgressBar caloriesProgress;

    public NutritionProgressWrapper(ViewGroup layout) {
        proteinProgress = layout.findViewById(R.id.protein_layout).findViewById(R.id.nutrition_progress);
        proteinProgress.setProgressTintList(ColorStateList.valueOf(layout.getResources().getColor(R.color.colorRed)));

        fatsProgress = layout.findViewById(R.id.fats_layout).findViewById(R.id.nutrition_progress);
        fatsProgress.setProgressTintList(ColorStateList.valueOf(layout.getResources().getColor(R.color.colorYellow)));

        carbsProgress = layout.findViewById(R.id.carbs_layout).findViewById(R.id.nutrition_progress);
        carbsProgress.setProgressTintList(ColorStateList.valueOf(layout.getResources().getColor(R.color.colorPrimary)));

        caloriesProgress = layout.findViewById(R.id.calories_layout).findViewById(R.id.nutrition_progress);
        caloriesProgress.setProgressTintList(ColorStateList.valueOf(layout.getResources().getColor(R.color.colorCalories)));
    }

    public void setProgresses(Nutrition nutrition, Rates rates) {
        setProgress(proteinProgress, Math.round(rates.getProtein()), Math.round((float) nutrition.getProtein()));
        setProgress(fatsProgress, Math.round(rates.getFats()), Math.round((float) nutrition.getFats()));
        setProgress(carbsProgress, Math.round(rates.getCarbs()), Math.round((float) nutrition.getCarbs()));
        setProgress(caloriesProgress, Math.round(rates.getCalories()), Math.round((float) nutrition.getCalories()));
    }

    private void setProgress(ProgressBar progressBar, int max, int progress) {
        progressBar.setMax(max);
        progressBar.setProgress(progress);
    }
}
