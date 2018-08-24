package korablique.recipecalculator.model;

import javax.annotation.Nullable;

public class FoodstuffBuilderNameStep {
    @Nullable
    FoodstuffBuilderIDStep idStep;
    String name;

    FoodstuffBuilderNameStep(String name) {
        this(null, name);
    }

    FoodstuffBuilderNameStep(@Nullable FoodstuffBuilderIDStep idStep, String name) {
        this.idStep = idStep;
        this.name = name;
    }

    public Foodstuff withNutrition(Nutrition nutrition) {
        return withNutrition(nutrition.getProtein(), nutrition.getFats(),
                nutrition.getCarbs(), nutrition.getCalories());
    }

    public Foodstuff withNutrition(double protein, double fats, double carbs, double calories) {
        if (idStep != null) {
            return new Foodstuff(idStep.id, name, protein, fats, carbs, calories);
        } else {
            return new Foodstuff(name, protein, fats, carbs, calories);
        }
    }
}