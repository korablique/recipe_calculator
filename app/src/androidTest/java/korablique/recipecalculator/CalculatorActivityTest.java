package korablique.recipecalculator;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CalculatorActivityTest {
    @Rule
    public ActivityTestRule<CalculatorActivity> mActivityRule =
            new ActivityTestRule<>(CalculatorActivity.class);

    @Test
    public void testCardAppearence() {
        onView(withId(R.id.card)).check(matches(not(isDisplayed())));
        onView(withId(R.id.button_add)).perform(click());
        onView(withId(R.id.card)).check(matches(isDisplayed()));
    }

    @Test
    public void testCanAddProductToTable() {
        onView(withId(R.id.ingredients_layout)).check(matches(hasChildren(is(0))));

        onView(withId(R.id.button_add)).perform(click());

        onView(withId(R.id.name_edit_text)).perform(typeText("tomato"));
        onView(withId(R.id.weight_edit_text)).perform(typeText("123"));
        onView(withId(R.id.protein_edit_text)).perform(typeText("123"));
        onView(withId(R.id.fats_edit_text)).perform(typeText("123"));
        onView(withId(R.id.carbs_edit_text)).perform(typeText("123"));
        onView(withId(R.id.calories_edit_text)).perform(typeText("123"));

        onView(withId(R.id.button_ok)).perform(click());
        onView(withId(R.id.ingredients_layout)).check(matches(hasChildren(is(1))));
    }

    @Test
    public void testEmptyCardHasNotDeleteButton() {
        onView(withId(R.id.button_add)).perform(click());
        onView(withId(R.id.button_delete)).check(matches(not(isDisplayed())));
    }

    public static Matcher<View> hasChildren(final Matcher<Integer> numChildrenMatcher) {
        return new TypeSafeMatcher<View>() {

            /**
             * matching with viewgroup.getChildCount()
             */
            @Override
            public boolean matchesSafely(View view) {
                return view instanceof ViewGroup && numChildrenMatcher.matches(((ViewGroup)view).getChildCount());
            }

            /**
             * gets the description
             */
            @Override
            public void describeTo(Description description) {
                description.appendText(" a view with # children is ");
                numChildrenMatcher.describeTo(description);
            }
        };
    }
}
