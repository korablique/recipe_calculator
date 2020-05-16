package korablique.recipecalculator.ui.inputfilters;

import android.text.SpannedString;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import korablique.recipecalculator.BuildConfig;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class FiltersUtilsTest {
    @Test
    public void insertionWorks() {
        // EditText had: "123.1", user puts "0" to the index 2.
        Assert.assertEquals("1203.1", FiltersUtils.inputToString("0", 0, 1, new SpannedString("123.1"), 2, 2));
        // EditText had: "123.1", user puts "0" to the index 5.
        Assert.assertEquals("123.10", FiltersUtils.inputToString("0", 0, 1, new SpannedString("123.1"), 5, 5));
    }

    @Test
    public void replacementWorks() {
        // EditText had: "123.1", user replaces char at the index 3 with "00".
        Assert.assertEquals("123001", FiltersUtils.inputToString("00", 0, 2, new SpannedString("123.1"), 3, 4));
        // EditText had: "123.1", user replaces char at the index 4 with "00".
        Assert.assertEquals("123.00", FiltersUtils.inputToString("00", 0, 2, new SpannedString("123.1"), 4, 5));
    }

    @Test
    public void partialReplacementFromSourceWorks() {
        // EditText had: "123.1", user replaces char at the index 3 with "00" (which is a part of "000").
        Assert.assertEquals("123001", FiltersUtils.inputToString("000", 0, 2, new SpannedString("123.1"), 3, 4));
        // EditText had: "123.1", user replaces char at the index 4 with "0" (which is a part of "000").
        Assert.assertEquals("123.0", FiltersUtils.inputToString("000", 0, 1, new SpannedString("123.1"), 4, 5));
    }
}
