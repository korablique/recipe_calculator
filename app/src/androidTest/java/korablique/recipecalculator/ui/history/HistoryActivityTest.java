package korablique.recipecalculator.ui.history;

import android.app.Instrumentation;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.Date;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.ui.Card;
import korablique.recipecalculator.util.DbUtil;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;
import static korablique.recipecalculator.ui.history.HistoryActivity.BATCH_SIZE;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HistoryActivityTest {
    private DatabaseWorker databaseWorker =
            new DatabaseWorker(new SyncMainThreadExecutor(), new InstantDatabaseThreadExecutor());

    @Rule
    public ActivityTestRule<HistoryActivity> mActivityRule =
            InjectableActivityTestRule.forActivity(HistoryActivity.class)
                .withInjector((HistoryActivity activity) -> {
                    activity.databaseWorker = databaseWorker;
                })
                .withManualStart() // Нужно предотвратить старт UserGoalActivity.
                .build();

    @Before
    public void setUp() throws InterruptedException {
        Card.setAnimationDuration(0);

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Resources resources = context.getResources();

        DbUtil.clearTable(context, HISTORY_TABLE_NAME);

        String goal = resources.getStringArray(R.array.goals_array)[0];
        String gender = resources.getStringArray(R.array.gender_array)[0];
        int age = 24, height = 165, weight = 63;
        float coefficient = 1.2f;
        String defaultFormula = resources.getStringArray(R.array.formula_array)[0];
        UserParameters userParameters = new UserParameters(
                goal, gender, age, height, weight, coefficient, defaultFormula);
        databaseWorker.saveUserParameters(context, userParameters, null);

        mActivityRule.launchActivity(null);
    }

    @After
    public void tearDown() throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            mActivityRule
                    .getActivity()
                    .getCard()
                    .hide();
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
    public void nutritionAndNameModificationWorksAfterActivityRestart() {
        addItem();
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, click()));

        onView(withId(R.id.name_edit_text)).perform(replaceText("new name"));
        onView(withId(R.id.protein_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.fats_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.carbs_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.calories_edit_text)).perform(replaceText("100"));
        onView(withId(R.id.button_ok)).perform(click());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().recreate();
            }
        });

        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, click()));
        onView(withId(R.id.name_edit_text)).check(matches(withText("new name")));
        onView(withId(R.id.protein_edit_text)).check(matches(withText("10.0")));
        onView(withId(R.id.fats_edit_text)).check(matches(withText("10.0")));
        onView(withId(R.id.carbs_edit_text)).check(matches(withText("10.0")));
        onView(withId(R.id.calories_edit_text)).check(matches(withText("100.0")));
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

    @Test
    public void batchesAddedToBottom() {
        Foodstuff[] foodstuffs = new Foodstuff[BATCH_SIZE + 1];
        for (int index = 0; index < foodstuffs.length; index++) {
            foodstuffs[index] = new Foodstuff("foodstuff" + index, -1, 5, 5, 5, 5);
        }
        ArrayList<Long> foodstuffIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                mActivityRule.getActivity(),
                foodstuffs,
                (ids) -> {
                    foodstuffIds.addAll(ids);
                });

        NewHistoryEntry[] entries = new NewHistoryEntry[BATCH_SIZE + 1];
        for (int index = 0; index < entries.length; index++) {
            entries[index] = new NewHistoryEntry(foodstuffIds.get(index), 100, new Date(index, 0, 1));
        }
        databaseWorker.saveGroupOfFoodstuffsToHistory(
                mActivityRule.getActivity(),
                entries,
                null);

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());

        assertNotContains("foodstuff0");
        assertContains("foodstuff" + BATCH_SIZE);
    }

    @Test
    public void historyFoodstuffsOrderIsDescending() {
        // добавить батч продуктов с интервалом в минуту
        Foodstuff[] foodstuffs = new Foodstuff[BATCH_SIZE + 1];
        for (int index = 0; index < foodstuffs.length; index++) {
            foodstuffs[index] = new Foodstuff("foodstuff" + index, -1, 5, 5, 5, 5);
        }
        ArrayList<Long> foodstuffIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                mActivityRule.getActivity(),
                foodstuffs,
                (ids) -> {
                    foodstuffIds.addAll(ids);
                });

        NewHistoryEntry[] entries = new NewHistoryEntry[BATCH_SIZE + 1];
        for (int index = 0; index < entries.length; index++) {
            entries[index] = new NewHistoryEntry(foodstuffIds.get(index), 100, new Date(index));
        }
        databaseWorker.saveGroupOfFoodstuffsToHistory(
                mActivityRule.getActivity(),
                entries,
                null);

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());

        assertContains("foodstuff" + BATCH_SIZE);
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
