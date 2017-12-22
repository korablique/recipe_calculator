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
@Config(constants = BuildConfig.class)
public class PopularProductsUtilsTest {
    @Test
    public void checkTop() {
        List<Long> ids = Arrays.asList(5L, 4L, 3L, 2L, 1L, 5L, 4L, 3L, 2L, 5L, 4L, 3L, 5L, 4L, 5L);
        List<PopularProductsUtils.FoodstuffFrequency> top = PopularProductsUtils.getTop(ids);
        Assert.assertEquals(5, top.get(0).getFrequency());
        Assert.assertEquals(5, top.get(0).getFoodstuffId());

        Assert.assertEquals(4, top.get(1).getFrequency());
        Assert.assertEquals(4, top.get(1).getFoodstuffId());

        Assert.assertEquals(3, top.get(2).getFrequency());
        Assert.assertEquals(3, top.get(2).getFoodstuffId());

        Assert.assertEquals(2, top.get(3).getFrequency());
        Assert.assertEquals(2, top.get(3).getFoodstuffId());

        Assert.assertEquals(1, top.get(4).getFrequency());
        Assert.assertEquals(1, top.get(4).getFoodstuffId());
    }
}
