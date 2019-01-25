package korablique.recipecalculator.ui.calculator;

import android.app.Instrumentation;
import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
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

import java.util.Arrays;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.DatabaseHolder;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.ui.Card;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CalculatorActivityTest {
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private DatabaseHolder databaseHolder;
    private DatabaseWorker databaseWorker;
    private FoodstuffsList foodstuffsList;

    @Rule
    public ActivityTestRule<CalculatorActivity> mActivityRule =
            InjectableActivityTestRule.forActivity(CalculatorActivity.class)
                    .withSingletones(() -> {
                        DatabaseThreadExecutor databaseThreadExecutor = new InstantDatabaseThreadExecutor();
                        databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);
                        databaseWorker = new DatabaseWorker(
                                databaseHolder, new SyncMainThreadExecutor(), databaseThreadExecutor);
                        foodstuffsList = new FoodstuffsList(context, databaseWorker);
                        return Arrays.asList(databaseWorker, foodstuffsList);
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
