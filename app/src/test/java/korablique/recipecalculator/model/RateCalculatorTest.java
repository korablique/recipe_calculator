package korablique.recipecalculator.model;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import korablique.recipecalculator.BuildConfig;
import korablique.recipecalculator.FloatUtils;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class RateCalculatorTest {
    @Test
    public void ratesCalculatedCorrectly() {
        Rates rates1 = RateCalculator.calculate(
                40,
                Gender.FEMALE,
                24,
                158,
                45,
                Lifestyle.PASSIVE_LIFESTYLE,
                Formula.HARRIS_BENEDICT);
        Assert.assertTrue("Calories", FloatUtils.areFloatsEquals(1349.21376f, rates1.getCalories()));
        Assert.assertTrue("Protein", FloatUtils.areFloatsEquals(90, rates1.getProtein()));
        Assert.assertTrue("Fats", FloatUtils.areFloatsEquals(45, rates1.getFats()));
        Assert.assertTrue("Carbs", FloatUtils.areFloatsEquals(146.05344f, rates1.getCarbs()));

        Rates rates2 = RateCalculator.calculate(
                70,
                Gender.MALE,
                24,
                165,
                64,
                Lifestyle.INSIGNIFICANT_ACTIVITY,
                Formula.MIFFLIN_JEOR);
        Assert.assertTrue("Calories", FloatUtils.areFloatsEquals(2353.828125f, rates2.getCalories()));
        Assert.assertTrue("Protein", FloatUtils.areFloatsEquals(128, rates2.getProtein()));
        Assert.assertTrue("Fats", FloatUtils.areFloatsEquals(64, rates2.getFats()));
        Assert.assertTrue("Carbs", FloatUtils.areFloatsEquals(316.4570312f, rates2.getCarbs()));
    }
}
