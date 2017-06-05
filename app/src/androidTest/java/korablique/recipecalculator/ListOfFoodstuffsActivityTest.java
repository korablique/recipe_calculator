package korablique.recipecalculator;

import android.support.annotation.NonNull;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.core.deps.guava.base.Preconditions.checkNotNull;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ListOfFoodstuffsActivityTest {
    @Rule
    public ActivityTestRule<ListOfFoodstuffsActivity> mActivityRule =
            new ActivityTestRule<>(ListOfFoodstuffsActivity.class);

    @Before
    public void setUp() {
        Card.setAnimationDuration(0);
    }

    @Test
    public void canSaveProductWithoutEditing() {
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.card)).check(matches(isDisplayed()));
        onView(withId(R.id.button_save)).perform(click());
        onView(withId(R.id.card)).check(matches(not(isDisplayed())));
    }

    @Test
    public void weightIsHidden() {
        onView(anyWithId(R.id.column_name_weight)).check(matches(not(isDisplayed())));
    }

    private Matcher<View> anyWithId(final int id) {
        return new TypeSafeMatcher<View>() {
            private boolean found;
            @Override
            protected boolean matchesSafely(View item) {
                if (found) {
                    return false;
                }
                if (item.getId() == id) {
                    found = true;
                    return true;
                }
                return false;
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("Couldn't find view with id: " + id);
            }
        };
    }


}
