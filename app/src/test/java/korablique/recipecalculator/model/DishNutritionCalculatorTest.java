package korablique.recipecalculator.model;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import korablique.recipecalculator.BuildConfig;
import korablique.recipecalculator.DishNutritionCalculator;
import korablique.recipecalculator.FloatUtils;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class DishNutritionCalculatorTest {
    @Test
    public void dishNutritionCalculatesRight() {
        ArrayList<Foodstuff> foodstuffs = new ArrayList<>();
        foodstuffs.add(new Foodstuff("морковь", 310, 1.3, 0.1, 6.9, 32));
        foodstuffs.add(new Foodstuff("масло подсолнечное", 13, 0, 99.9, 0, 899));
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
