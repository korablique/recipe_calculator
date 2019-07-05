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

    @Test
    public void bestMatchIsUsedEvenWhenItIsAfterLimit() {
        List<String> input = new ArrayList<>();
        input.add("some_striAA");
        input.add("some_strinA");
        input.add("some_string");

        List<String> result =
                FuzzySearcher.search("some_string", input, (str) -> str, 1);

        // First 2 strings will be found before "some_string" and
        // would be returned by a flawed algorithm because the limit would be reached.
        // Correctly working algorithm should go through all the items to find the best
        // matching ones.
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(result.get(0), "some_string");
    }

    @Test
    public void substringOfBigStringIsConsideredToBeMatch() {
        List<String> input = new ArrayList<>();
        input.add("very long string with product in the middle and other words on the sides");

        List<String> result =
                FuzzySearcher.search("product", input, (str) -> str, 1);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(
                result.get(0),
                "very long string with product in the middle and other words on the sides");
    }
}
