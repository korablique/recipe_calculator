package korablique.recipecalculator.ui;


import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar;

public class NutritionProgressWrapper {
    private PluralProgressBar layout;

    public NutritionProgressWrapper(PluralProgressBar layout) {
        this.layout = layout;
    }

    public void setNutrition(double protein, double fats, double carbs) {
        layout.setProgress(0, (float) protein);
        layout.setProgress(1, (float) fats);
        layout.setProgress(2, (float) carbs);
    }

    public void setNutrition(Nutrition nutrition) {
        setNutrition(nutrition.getProtein(), nutrition.getFats(), nutrition.getCarbs());
    }
}
