package korablique.recipecalculator.ui.calculator;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.ui.Card;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CalculatorActivityTest {
    private DatabaseWorker databaseWorker = new DatabaseWorker(
            new SyncMainThreadExecutor(), new InstantDatabaseThreadExecutor());

    @Rule
    public ActivityTestRule<CalculatorActivity> mActivityRule =
            InjectableActivityTestRule.forActivity(CalculatorActivity.class)
                    .withSingletones(() -> {
                        return Collections.singletonList(databaseWorker);
                    })
                    .build();

    @Before
    public void setUp() {
        Card.setAnimationDuration(0);
    }

    @After
    public void tearDown() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            CalculatorActivity activity = mActivityRule.getActivity();
            if (activity != null) {
                activity.getCard().hide();
            }
        });
    }

    @Test
    public void testCardAppearance() {
        onView(ViewMatchers.withId(R.id.card)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fab_add_foodstuff)).perform(click());
        onView(withId(R.id.card)).check(matches(isDisplayed()));
    }

    @Test
    public void testCanAddProductToTable() {
        onView(withId(R.id.ingredients)).check(matches(hasChildren(is(0))));
        addItem();
        onView(withId(R.id.ingredients)).check(matches(hasChildren(is(1))));
    }

    @Test
    public void testEmptyCardHasNoDeleteButton() {
        onView(withId(R.id.fab_add_foodstuff)).perform(click());
        onView(withId(R.id.button_delete)).check(matches(not(isDisplayed())));
    }
    @Test
    public void testStartTextViewIsShownWhenNoItems() {
        onView(withId(R.id.start_text_view)).check(matches(isDisplayed()));
    }

    @Test
    public void testStartTextViewIsNotShownWhenItemsExist() {
        addItem();
        onView(withId(R.id.start_text_view)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testResultViewsAreNotShownWhenNoItems() {
        onView(withId(R.id.result_weight_text_view)).check(matches(not(isDisplayed())));
        onView(withId(R.id.result_weight_edit_text)).check(matches(not(isDisplayed())));
        onView(withId(R.id.calculate_button)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testResultViewsAreShownWhenItemsExist() {
        addItem();
        onView(withId(R.id.result_weight_text_view)).check(matches(isDisplayed()));
        onView(withId(R.id.result_weight_edit_text)).check(matches(isDisplayed()));
        onView(withId(R.id.calculate_button)).check(matches(isDisplayed()));
    }

    @Test
    public void testResultViewsAreNotShownWhenItemsDeleted() {
        addItem();
        deleteItem();
        onView(withId(R.id.result_weight_text_view)).check(matches(not(isDisplayed())));
        onView(withId(R.id.result_weight_edit_text)).check(matches(not(isDisplayed())));
        onView(withId(R.id.calculate_button)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testStartTextViewIsShownWhenItemsDeleted() {
        addItem();
        deleteItem();
        onView(withId(R.id.start_text_view)).check(matches(isDisplayed()));
    }

    @Test
    public void testOnBackPressedCardHide() throws Throwable {
        onView(withId(R.id.fab_add_foodstuff)).perform(click());
        Espresso.pressBack();
        onView(withId(R.id.activity_calculator_frame_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.card)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSavingProductDoesNotCrashApp() {
        onView(withId(R.id.fab_add_foodstuff)).perform(click());
        onView(withId(R.id.name_edit_text)).perform(typeText("tomato"));
        onView(withId(R.id.weight_edit_text)).perform(typeText("13"));
        onView(withId(R.id.protein_edit_text)).perform(typeText("13"));
        onView(withId(R.id.fats_edit_text)).perform(typeText("12"));
        onView(withId(R.id.carbs_edit_text)).perform(typeText("12"));
        onView(withId(R.id.calories_edit_text)).perform(typeText("13"));
        onView(withId(R.id.button_save)).perform(click());
    }

    private void addItem() {
        onView(withId(R.id.fab_add_foodstuff)).perform(click());
        onView(withId(R.id.name_edit_text)).perform(typeText("tomato"));
        onView(withId(R.id.weight_edit_text)).perform(typeText("12"));
        onView(withId(R.id.protein_edit_text)).perform(typeText("12"));
        onView(withId(R.id.fats_edit_text)).perform(typeText("12"));
        onView(withId(R.id.carbs_edit_text)).perform(typeText("12"));
        onView(withId(R.id.calories_edit_text)).perform(typeText("12"));
        onView(withId(R.id.button_ok)).perform(click());
    }

    private void deleteItem() {
        onView(withParent(withId(R.id.ingredients))).perform(click());
        onView(withId(R.id.button_delete)).perform(click());
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
