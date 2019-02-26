package korablique.recipecalculator.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.BuildConfig;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class FuzzySearcherTest {
    @Test
    public void stringIsSimilarToItself() {
        List<String> input = new ArrayList<>();
        input.add("some_string");

        List<String> result =
                FuzzySearcher.search("some_string", input, (str) -> str, Integer.MAX_VALUE);

        Assert.assertEquals(input, result);
    }

    @Test
    public void resultIsOrderedFromMostSimilarToLeastSimilar() {
        List<String> input = new ArrayList<>();
        input.add("some_striAA");
        input.add("some_strinA");
        input.add("some_string");

        List<String> result =
                FuzzySearcher.search("some_string", input, (str) -> str, Integer.MAX_VALUE);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals(result.get(0), "some_string");
        Assert.assertEquals(result.get(1), "some_strinA");
        Assert.assertEquals(result.get(2), "some_striAA");
    }

    @Test
    public void sentencesWithSameWordsAreSimilar() {
        List<String> input = new ArrayList<>();
        input.add("elephants like humans");

        List<String> result =
                FuzzySearcher.search("humans like elephants", input, (str) -> str, Integer.MAX_VALUE);

        Assert.assertEquals(input, result);
    }
}
