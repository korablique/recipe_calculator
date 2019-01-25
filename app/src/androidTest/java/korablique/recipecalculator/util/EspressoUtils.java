package korablique.recipecalculator.util;

import androidx.test.espresso.ViewAssertion;
import android.view.View;

import junit.framework.AssertionFailedError;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class EspressoUtils {
    private EspressoUtils() {
    }

    public static Matcher<View> matches(ViewAssertion assertion) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                try {
                    assertion.check(item, null);
                } catch (AssertionFailedError e) {
                    return false;
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(assertion.getClass().getCanonicalName());
            }
        };
    }
}
