package korablique.recipecalculator.model;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import korablique.recipecalculator.BuildConfig;
import korablique.recipecalculator.DishNutritionCalculator;
import korablique.recipecalculator.util.FloatUtils;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class DishNutritionCalculatorTest {
    @Test
    public void dishNutritionCalculatesRight() {
        ArrayList<WeightedFoodstuff> foodstuffs = new ArrayList<>();
        foodstuffs.add(Foodstuff.withName("морковь").withNutrition(1.3, 0.1, 6.9, 32).withWeight(310));
        foodstuffs.add(Foodstuff.withName("масло подсолнечное").withNutrition(0, 99.9, 0, 899).withWeight(13));
        Nutrition withoutChangingWeight = DishNutritionCalculator.calculate(foodstuffs, 323);
        Assert.assertTrue(FloatUtils.areFloatsEquals(withoutChangingWeight.getProtein(), 1.2476));
        Assert.assertTrue(FloatUtils.areFloatsEquals(withoutChangingWeight.getFats(), 4.1167));
        Assert.assertTrue(FloatUtils.areFloatsEquals(withoutChangingWeight.getCarbs(), 6.6222));
        Assert.assertTrue(FloatUtils.areFloatsEquals(withoutChangingWeight.getCalories(), 66.8947));

        Nutrition withChangingWeight = DishNutritionCalculator.calculate(foodstuffs, 269);
        Assert.assertTrue(FloatUtils.areFloatsEquals(withChangingWeight.getProtein(), 1.4981));
        Assert.assertTrue(FloatUtils.areFloatsEquals(withChangingWeight.getFats(), 4.9431));
        Assert.assertTrue(FloatUtils.areFloatsEquals(withChangingWeight.getCarbs(), 7.9516));
        Assert.assertTrue(FloatUtils.areFloatsEquals(withChangingWeight.getCalories(), 80.3234));
    }
}
