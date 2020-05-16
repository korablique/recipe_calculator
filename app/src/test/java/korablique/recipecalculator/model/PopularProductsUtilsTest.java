package korablique.recipecalculator.model;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import korablique.recipecalculator.BuildConfig;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class PopularProductsUtilsTest {
    @Test
    public void checkTop() {
        Foodstuff f1 = Foodstuff.withId(1).withName("1").withNutrition(1, 1, 1, 1);
        Foodstuff f2 = Foodstuff.withId(2).withName("2").withNutrition(2, 2, 2, 2);
        Foodstuff f3 = Foodstuff.withId(3).withName("3").withNutrition(3, 3, 3, 3);
        Foodstuff f4 = Foodstuff.withId(4).withName("4").withNutrition(4, 4, 4, 4);
        Foodstuff f5 = Foodstuff.withId(5).withName("5").withNutrition(5, 5, 5, 5);

        List<Foodstuff> foodstuffs = Arrays.asList(f5, f4, f3, f2 , f1, f5, f4, f3, f2, f5, f4, f3, f5, f4, f5);
        List<PopularProductsUtils.FoodstuffFrequency> top = PopularProductsUtils.getTop(foodstuffs);
        Assert.assertEquals(5, top.get(0).getFrequency());
        Assert.assertEquals(f5, top.get(0).getFoodstuff());

        Assert.assertEquals(4, top.get(1).getFrequency());
        Assert.assertEquals(f4, top.get(1).getFoodstuff());

        Assert.assertEquals(3, top.get(2).getFrequency());
        Assert.assertEquals(f3, top.get(2).getFoodstuff());

        Assert.assertEquals(2, top.get(3).getFrequency());
        Assert.assertEquals(f2, top.get(3).getFoodstuff());

        Assert.assertEquals(1, top.get(4).getFrequency());
        Assert.assertEquals(f1, top.get(4).getFoodstuff());
    }

    @Test
    public void foodstuffsWithSameFrequencies_haveOriginalOrder() {
        Foodstuff f1 = Foodstuff.withId(1).withName("1").withNutrition(1, 1, 1, 1);
        Foodstuff f2 = Foodstuff.withId(2).withName("2").withNutrition(2, 2, 2, 2);
        Foodstuff f3 = Foodstuff.withId(3).withName("3").withNutrition(3, 3, 3, 3);
        Foodstuff f4 = Foodstuff.withId(4).withName("4").withNutrition(4, 4, 4, 4);
        Foodstuff f5 = Foodstuff.withId(5).withName("5").withNutrition(5, 5, 5, 5);

        List<Foodstuff> foodstuffs = Arrays.asList(f5, f5, f3, f3 , f1, f1, f4, f4, f2, f2);
        List<PopularProductsUtils.FoodstuffFrequency> top = PopularProductsUtils.getTop(foodstuffs);
        Assert.assertEquals(2, top.get(0).getFrequency());
        Assert.assertEquals(f5, top.get(0).getFoodstuff());

        Assert.assertEquals(2, top.get(1).getFrequency());
        Assert.assertEquals(f3, top.get(1).getFoodstuff());

        Assert.assertEquals(2, top.get(2).getFrequency());
        Assert.assertEquals(f1, top.get(2).getFoodstuff());

        Assert.assertEquals(2, top.get(3).getFrequency());
        Assert.assertEquals(f4, top.get(3).getFoodstuff());

        Assert.assertEquals(2, top.get(4).getFrequency());
        Assert.assertEquals(f2, top.get(4).getFoodstuff());
    }
}
