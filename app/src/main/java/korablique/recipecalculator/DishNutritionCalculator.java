package korablique.recipecalculator;


import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.WeightedFoodstuff;

public class DishNutritionCalculator {

    public static Nutrition calculateIngredients(List<Ingredient> ingredients, double resultWeight) {
        List<WeightedFoodstuff> foodstuffs = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            foodstuffs.add(ingredient.toWeightedFoodstuff());
        }
        return calculate(foodstuffs, resultWeight);
    }

    public static Nutrition calculate(List<WeightedFoodstuff> foodstuffs, double resultWeight) {
        Nutrition result = Nutrition.zero();
        for (WeightedFoodstuff foodstuff : foodstuffs) {
            result = result.plus(Nutrition.of(foodstuff));
        }
        result = result.multiply(100 / resultWeight);
        return result;
    }
}
