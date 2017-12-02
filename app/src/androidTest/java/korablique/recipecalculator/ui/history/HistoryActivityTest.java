package korablique.recipecalculator.ui.history;

import android.app.Instrumentation;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.Card;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HistoryActivityTest {
    @Rule
    public ActivityTestRule<HistoryActivity> mActivityRule =
            new ActivityTestRule<>(HistoryActivity.class);

    @Before
    public void setUp() throws InterruptedException {
        Card.setAnimationDuration(0);
        Resources resources = mActivityRule.getActivity().getResources();
        String goal = resources.getStringArray(R.array.goals_array)[0];
        String gender = resources.getStringArray(R.array.gender_array)[0];
        int age = 24, height = 165, weight = 63;
        float coefficient = 1.2f;
        String defaultFormula = resources.getStringArray(R.array.formula_array)[0];
        UserParameters userParameters = new UserParameters(
                goal, gender, age, height, weight, coefficient, defaultFormula);
        final CountDownLatch mutex = new CountDownLatch(1);
        DatabaseWorker.getInstance().saveUserParameters(
                mActivityRule.getActivity(), userParameters, new Runnable() {
                    @Override
                    public void run() {
                        mutex.countDown();
                    }
                });
        mutex.await();
    }

    @After
    public void tearDown() {
        mActivityRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityRule
                        .getActivity()
                        .getCard().hide();
            }
        });
    }

    @Test
    public void canUpdateFoodstuffWeight() {
        addItem();
        onView(ViewMatchers.withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, click()));
        onView(withId(R.id.weight_edit_text)).perform(replaceText("100"));
        onView(withId(R.id.button_ok)).perform(click());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().recreate();
            }
        });
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, click()));
        onView(withId(R.id.weight_edit_text)).check(matches(withText("100.0")));
    }

    @Test
    public void canModifyNutritionAndName() {
        addItem();
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, click()));

        onView(withId(R.id.name_edit_text)).perform(replaceText("new name"));
        onView(withId(R.id.protein_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.fats_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.carbs_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.calories_edit_text)).perform(replaceText("100"));
        onView(withId(R.id.button_ok)).perform(click());

        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, click()));
        onView(withId(R.id.name_edit_text)).check(matches(withText("new name")));
        onView(withId(R.id.protein_edit_text)).check(matches(withText("10.0")));
        onView(withId(R.id.fats_edit_text)).check(matches(withText("10.0")));
        onView(withId(R.id.carbs_edit_text)).check(matches(withText("10.0")));
        onView(withId(R.id.calories_edit_text)).check(matches(withText("100.0")));
    }

    @Test
    public void saveButtonInvisibleOnEditingFoodstuff() {
        addItem();
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, click()));
        onView(withId(R.id.button_save)).check(matches(not(isDisplayed())));
    }

    private void addItem() {
        onView(withId(R.id.history_fab)).perform(click());
        onView(withId(R.id.name_edit_text)).perform(typeText("tomato"));
        onView(withId(R.id.weight_edit_text)).perform(typeText("12"));
        onView(withId(R.id.protein_edit_text)).perform(typeText("12"));
        onView(withId(R.id.fats_edit_text)).perform(typeText("12"));
        onView(withId(R.id.carbs_edit_text)).perform(typeText("12"));
        onView(withId(R.id.calories_edit_text)).perform(typeText("12"));
        onView(withId(R.id.button_ok)).perform(click());
    }
}
