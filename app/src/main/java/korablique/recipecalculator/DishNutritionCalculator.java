package korablique.recipecalculator;


import java.util.List;

import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;

public class DishNutritionCalculator {

    public static Nutrition calculate(List<Foodstuff> foodstuffs, double resultWeight) {
        Nutrition result = Nutrition.zero();
        for (Foodstuff foodstuff : foodstuffs) {
            result = result.plus(Nutrition.of(foodstuff));
        }
        result = result.multiply(100 / resultWeight);
        return result;
    }
}
