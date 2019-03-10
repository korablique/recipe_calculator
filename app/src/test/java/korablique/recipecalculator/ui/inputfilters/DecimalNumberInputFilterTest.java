package korablique.recipecalculator.ui.inputfilters;

import android.text.SpannedString;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.annotation.Nullable;
import korablique.recipecalculator.BuildConfig;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class DecimalNumberInputFilterTest {
    @Test
    public void numbersWithoutFractionAllowed() {
        DecimalNumberInputFilter filter = DecimalNumberInputFilter.of1DigitAfterPoint();
        Assert.assertNull(filterPastedText("123", filter));

        filter = DecimalNumberInputFilter.ofNDigitsAfterPoint(1);
        Assert.assertNull(filterPastedText("123", filter));

        filter = DecimalNumberInputFilter.ofNDigitsAfterPoint(0);
        Assert.assertNull(filterPastedText("123", filter));
    }

    @Test
    public void lonelyPointAllowed_whenDigitsAfterPointAllowed() {
        DecimalNumberInputFilter filter = DecimalNumberInputFilter.of1DigitAfterPoint();
        Assert.assertNull(filterPastedText("123.", filter));
    }

    @Test
    public void lonelyPointNotAllowed_whenDigitsAfterPointNotAllowed() {
        DecimalNumberInputFilter filter = DecimalNumberInputFilter.ofNDigitsAfterPoint(0);
        Assert.assertEquals("", filterPastedText("123.", filter));
    }

    @Test
    public void allowedNumberOfDigitrsAfterPoint_isExact() {
        DecimalNumberInputFilter filter = DecimalNumberInputFilter.ofNDigitsAfterPoint(2);
        Assert.assertNull(filterPastedText("123.1", filter));
        Assert.assertNull(filterPastedText("123.12", filter));
        Assert.assertEquals("", filterPastedText("123.123", filter));
    }

    @Nullable
    private String filterPastedText(String text, DecimalNumberInputFilter filter) {
        CharSequence result = filter.filter(text, 0, text.length(), new SpannedString(""), 0, 0);
        if (result != null) {
            return result.toString();
        } else {
            return null;
        }
    }
}
