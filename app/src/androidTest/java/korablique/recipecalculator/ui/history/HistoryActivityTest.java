package korablique.recipecalculator.ui.history;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
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
import static korablique.recipecalculator.database.HistoryWorker.BATCH_SIZE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HistoryActivityTest {
    private MainThreadExecutor mainThreadExecutor = new SyncMainThreadExecutor();
    private DatabaseWorker databaseWorker =
            new DatabaseWorker(mainThreadExecutor, new InstantDatabaseThreadExecutor());
    private HistoryWorker historyWorker;
    private UserParametersWorker userParametersWorker;
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private FoodstuffsList foodstuffsList = new FoodstuffsList(context, databaseWorker);

    @Rule
    public ActivityTestRule<HistoryActivity> mActivityRule =
            InjectableActivityTestRule.forActivity(HistoryActivity.class)
            .withManualStart()
            .withSingletones(() -> {
                Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
                historyWorker = new HistoryWorker(
                        context, new SyncMainThreadExecutor(), new InstantDatabaseThreadExecutor());
                userParametersWorker = new UserParametersWorker(
                        context, new SyncMainThreadExecutor(), new InstantDatabaseThreadExecutor());

                return Arrays.asList(mainThreadExecutor, databaseWorker,
                        historyWorker, userParametersWorker, foodstuffsList);
            })
            .withActivityScoped(target -> {
                BaseActivity activity = (BaseActivity) target;
                RxActivitySubscriptions subscriptions =
                        new RxActivitySubscriptions(activity.getActivityCallbacks());
                return Collections.singletonList(subscriptions);
            })
            .build();

    @Before
    public void setUp() {
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
        userParametersWorker.saveUserParameters(userParameters);
    }

    @After
    public void tearDown() {
        if (mActivityRule.getActivity() == null) {
            return;
        }
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            mActivityRule
                    .getActivity()
                    .getCard()
                    .hide();
        });
        historyWorker = null;
        userParametersWorker = null;
    }

    @Test
    public void canUpdateFoodstuffWeight() {
        mActivityRule.launchActivity(null);

        addItem();
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, click()));
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
        mActivityRule.launchActivity(null);

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
        mActivityRule.launchActivity(null);

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
        mActivityRule.launchActivity(null);

        addItem();
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, click()));
        onView(withId(R.id.button_save)).check(matches(not(isDisplayed())));
    }

    @Test
    public void batchesAddedToBottom() {
        mActivityRule.launchActivity(null);

        Foodstuff[] foodstuffs = new Foodstuff[BATCH_SIZE + 1];
        for (int index = 0; index < foodstuffs.length; index++) {
            foodstuffs[index] = Foodstuff.withName("foodstuff" + index).withNutrition(5, 5, 5, 5);
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
        historyWorker.saveGroupOfFoodstuffsToHistory(entries);

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());

        assertNotContains("foodstuff0");
        assertContains("foodstuff" + BATCH_SIZE);
    }

    @Test
    public void historyFoodstuffsOrderIsDescending() {
        mActivityRule.launchActivity(null);

        // добавить батч продуктов с интервалом в минуту
        Foodstuff[] foodstuffs = new Foodstuff[BATCH_SIZE + 1];
        for (int index = 0; index < foodstuffs.length; index++) {
            foodstuffs[index] = Foodstuff.withName("foodstuff" + index).withNutrition(5, 5, 5, 5);
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
        historyWorker.saveGroupOfFoodstuffsToHistory(entries);

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());

        assertContains("foodstuff" + BATCH_SIZE);
    }

    @Test
    public void containsGivenFoodstuffs() {
        // Проверяем, что если запустить HistoryActivity с интентом на добавление продуктов,
        // HistoryActivity после открытия будет содержать в себе эти добавленные продукты.
        ArrayList<WeightedFoodstuff> foodstuffs = new ArrayList<>();
        foodstuffs.add(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4).withWeight(123));
        foodstuffs.add(Foodstuff.withName("water").withNutrition(1, 2, 3, 4).withWeight(123));
        foodstuffs.add(Foodstuff.withName("beer").withNutrition(1, 2, 3, 4).withWeight(123));

        Intent startIntent =
                HistoryActivity.createStartAndAddIntent(foodstuffs, InstrumentationRegistry.getTargetContext());
        mActivityRule.launchActivity(startIntent);

        onView(withText(containsString("apple"))).check(matches(isDisplayed()));
        onView(withText(containsString("water"))).check(matches(isDisplayed()));
        onView(withText(containsString("beer"))).check(matches(isDisplayed()));
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
