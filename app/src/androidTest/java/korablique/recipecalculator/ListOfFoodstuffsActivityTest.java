package korablique.recipecalculator;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
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
}
