package korablique.recipecalculator.model;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import korablique.recipecalculator.BuildConfig;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class GoalCalculatorTest {
    @Test
    public void percentDoneCalculatesCorrectly() {
        // похудение
        int currentWeight = 50, firstWeight = 50, targetWeight = 40;
        int percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(0, percentDone);

        currentWeight = 53;
        percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(0, percentDone);

        currentWeight = 45;
        percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(50, percentDone);

        currentWeight = 40;
        percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(100, percentDone);

        currentWeight = 39;
        percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(100, percentDone);

        // поддержка
        currentWeight = 50;
        firstWeight = 50;
        targetWeight = 50;
        percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(100, percentDone);

        // набор веса
        currentWeight = 40;
        firstWeight = 40;
        targetWeight = 60;
        percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(0, percentDone);

        currentWeight = 39;
        percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(0, percentDone);

        currentWeight = 45;
        percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(25, percentDone);

        currentWeight = 60;
        percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(100, percentDone);

        currentWeight = 62;
        percentDone = GoalCalculator.calculateProgressPercentage(currentWeight, firstWeight, targetWeight);
        Assert.assertEquals(100, percentDone);
    }
}
