package korablique.recipecalculator.ui;

import android.text.SpannedString;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import korablique.recipecalculator.BuildConfig;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class DecimalDigitsInputFilterTest {
    @Test
    public void filterOneDigitAfterDecimalPoint() {
        DecimalDigitsInputFilter filter = new DecimalDigitsInputFilter();
        SpannedString dest = new SpannedString("");

        // пишем 4
        String source = "4";
        CharSequence result = filter.filter(source, 0, 1, dest, 0, 0);
        Assert.assertNull(result);

        // пишем 5 (45)
        source = "5";
        dest = new SpannedString("4");
        result = filter.filter(source, 0, 1, dest, 1, 1);
        Assert.assertNull(result);

        // пишем . (45.)
        source = ".";
        dest = new SpannedString("45");
        result = filter.filter(source, 0, 1, dest, 2, 2);
        Assert.assertNull(result);

        // пишем 9 (45.9)
        source = "9";
        dest = new SpannedString("45.");
        result = filter.filter(source, 0, 1, dest, 3, 3);
        Assert.assertNull(result);

        // стираем 9 (45.) стирание с конца
        source = "";
        dest = new SpannedString("45.9");
        result = filter.filter(source, 0, 0, dest, 3, 4);
        Assert.assertNull(result);

        // заменяем 5. на 0 (40) замена с конца
        source = "0";
        dest = new SpannedString("45.");
        result = filter.filter(source, 0, 1, dest, 1, 3);
        Assert.assertNull(result);

        // пишем 0 (400) запись в конец
        source = "0";
        dest = new SpannedString("40");
        result = filter.filter(source, 0, 1, dest, 2, 2);
        Assert.assertNull(result);

        // заменяем средний ноль на 6 (460) замена в середине
        source = "6";
        dest = new SpannedString("400");
        result = filter.filter(source, 0, 1, dest, 1, 2);
        Assert.assertNull(result);

        // стираем 4 (60) стирание в начале
        source = "";
        dest = new SpannedString("460");
        result = filter.filter(source, 0, 0, dest, 0, 1);
        Assert.assertNull(result);

        // пишем 1 (610) запись в середину
        source = "1";
        dest = new SpannedString("60");
        result = filter.filter(source, 0, 1, dest, 1, 1);
        Assert.assertNull(result);

        // стираем 1 (60) стирание в середине
        source = "";
        dest = new SpannedString("610");
        result = filter.filter(source, 0, 0, dest, 1, 2);
        Assert.assertNull(result);

        // вставляем .9 (60.9) вставка в конец
        source = ".9";
        dest = new SpannedString("60");
        result = filter.filter(source, 0, 2, dest, 2, 2);
        Assert.assertNull(result);

        // заменяем 60 на 37 (37.9) замена целой части
        source = "37";
        dest = new SpannedString("60.9");
        result = filter.filter(source, 0, 2, dest, 0, 2);
        Assert.assertNull(result);

        // заменяем 9 на 8 (37.8) замена дробной
        source = "8";
        dest = new SpannedString("37.9");
        result = filter.filter(source, 0, 0, dest, 3, 4);
        Assert.assertNull(result);

        // пытаемся ввести ещё цифру
        source = "9";
        dest = new SpannedString("37.8");
        result = filter.filter(source, 0, 1, dest, 4, 4);
        Assert.assertEquals("", result);

        // пытаемся ввести точку вначале
        source = ".";
        dest = new SpannedString("");
        result = filter.filter(source, 0, 1, dest, 0, 0);
        Assert.assertEquals("", result);
    }
}
