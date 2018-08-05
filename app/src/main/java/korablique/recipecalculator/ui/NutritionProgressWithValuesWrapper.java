package korablique.recipecalculator.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import korablique.recipecalculator.model.Nutrition;

public class NutritionProgressWithValuesWrapper {
    private NutritionProgressWrapper nutritionProgressWrapper;
    private NutritionValuesWrapper nutritionValuesWrapper;

    private static class NutritionWithProgress {
        TextView nutritionTextView;
        View progressView;

        public NutritionWithProgress(TextView nutritionTextView, View progressView) {
            this.nutritionTextView = nutritionTextView;
            this.progressView = progressView;
        }
    }

    public NutritionProgressWithValuesWrapper(Context context, ViewGroup layout) {
        nutritionProgressWrapper = new NutritionProgressWrapper(context, layout);
        nutritionValuesWrapper = new NutritionValuesWrapper(context, layout);
    }

    public void setNutrition(Nutrition nutrition) {
        nutritionValuesWrapper.setNutrition(nutrition);
        nutritionProgressWrapper.setProgressInProgressBar(nutrition.getProtein(), nutrition.getFats(), nutrition.getCarbs());
    }

    public double getProteinValue() {
        return nutritionValuesWrapper.getProteinValue();
    }

    public double getFatsValue() {
        return nutritionValuesWrapper.getFatsValue();
    }

    public double getCarbsValue() {
        return nutritionValuesWrapper.getCarbsValue();
    }

    public double getCaloriesValue() {
        return nutritionValuesWrapper.getCaloriesValue();
    }
}
