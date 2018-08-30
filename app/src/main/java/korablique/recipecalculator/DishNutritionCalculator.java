package korablique.recipecalculator;


import java.util.List;

import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.WeightedFoodstuff;

public class DishNutritionCalculator {

    public static Nutrition calculate(List<WeightedFoodstuff> foodstuffs, double resultWeight) {
        Nutrition result = Nutrition.zero();
        for (WeightedFoodstuff foodstuff : foodstuffs) {
            result = result.plus(Nutrition.of(foodstuff));
        }
        result = result.multiply(100 / resultWeight);
        return result;
    }
}
