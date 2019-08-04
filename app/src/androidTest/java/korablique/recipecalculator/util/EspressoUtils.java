package korablique.recipecalculator.util;

import android.os.Bundle;
import android.view.View;

import junit.framework.AssertionFailedError;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import androidx.test.espresso.ViewAssertion;

import java.util.Objects;

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

    /**
     * Recursively checks whether a Bundle has a given value somewhere inside of it, possibly deep
     * into subbundles (Bundle can contain another Bundle as an extra, creating a tree of Bundles).
     */
    public static <T> Matcher<Bundle> hasValueRecursive(T targetValue) {
        return new BaseMatcher<Bundle>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Matcher hasValueRecursive for value: " + targetValue.toString());
            }

            @Override
            public boolean matches(Object item) {
                Bundle bundle = (Bundle) item;
                if (areEqual(bundle, targetValue)) {
                    return true;
                }
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    if (areEqual(value, targetValue)) {
                        return true;
                    } else if (value instanceof Bundle && matches(value)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Compares given bundles field by field with each other
     * (the Bundle class doesn't override the equals() method).
     */
    private static boolean areBundlesEqual(Bundle lhs, Bundle rhs) {
        if (!lhs.keySet().equals(rhs.keySet())) {
            // Keys differ - bundles are not equal.
            return false;
        }
        for (String key : lhs.keySet()) {
            if (!areEqual(lhs.get(key), rhs.get(key))) {
                return false;
            }
        }
        return true;
    }

    private static boolean areEqual(Object lhs, Object rhs) {
        if (lhs instanceof Bundle && rhs instanceof Bundle) {
            return areBundlesEqual((Bundle)lhs, (Bundle)rhs);
        } else {
            return lhs.equals(rhs);
        }
    }
}
