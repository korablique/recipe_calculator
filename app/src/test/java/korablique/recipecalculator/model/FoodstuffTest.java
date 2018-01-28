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
public class FoodstuffTest {
    // проверяет, что конструктор копирует фудстафф в новый объект фудстаффа
    @Test
    public void foodstuffConstructorCopiesFoodstuff() {
        Foodstuff foodstuff = new Foodstuff("Apple", -1, 2, 0.5, 10, 40);
        long id = 1;
        Foodstuff foodstuffWithId = new Foodstuff(id, foodstuff);
        Assert.assertEquals(foodstuff.getName(), foodstuffWithId.getName());
        Assert.assertTrue(FloatUtils.areFloatsEquals(foodstuff.getProtein(), foodstuffWithId.getProtein()));
        Assert.assertTrue(FloatUtils.areFloatsEquals(foodstuff.getFats(), foodstuffWithId.getFats()));
        Assert.assertTrue(FloatUtils.areFloatsEquals(foodstuff.getCarbs(), foodstuffWithId.getCarbs()));
        Assert.assertTrue(FloatUtils.areFloatsEquals(foodstuff.getCalories(), foodstuffWithId.getCalories()));
        Assert.assertEquals(id, foodstuffWithId.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void foodstuffConstructorThrowsExceptionOnAttemptToChangeId() {
        Foodstuff foodstuff = new Foodstuff(1, "Apple", -1, 2, 0.5, 10, 40);
        long id = 2;
        Foodstuff foodstuffWithId = new Foodstuff(id, foodstuff);
    }
}
