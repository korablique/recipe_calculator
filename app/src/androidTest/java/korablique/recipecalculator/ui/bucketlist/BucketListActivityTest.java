package korablique.recipecalculator.ui.bucketlist;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.FoodstuffsDbHelper;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.util.InjectableActivityTestRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.BundleMatchers.hasValue;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtras;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BucketListActivityTest {
    @Rule
    public ActivityTestRule<BucketListActivity> activityRule =
            InjectableActivityTestRule.forActivity(BucketListActivity.class)
                    .withInjector((BucketListActivity activity) -> {})
                    .withManualStart()
                    .build();
    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        FoodstuffsDbHelper.deinitializeDatabase(context);
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
        dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
    }

    @Test
    public void containsGivenFoodstuffs() {
        ArrayList<Foodstuff> foodstuffs = new ArrayList<>();
        foodstuffs.add(new Foodstuff("apple", 123, 1, 2, 3, 4));
        foodstuffs.add(new Foodstuff("water", 123, 1, 2, 3, 4));
        foodstuffs.add(new Foodstuff("beer", 123, 1, 2, 3, 4));

        Intent startIntent =
                BucketListActivity.createStartIntentFor(foodstuffs, InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withText("apple")).check(matches(isDisplayed()));
        onView(withText("water")).check(matches(isDisplayed()));
        onView(withText("beer")).check(matches(isDisplayed()));
    }

    @Test
    public void addsFoodstuffsToHistory() {
        ArrayList<Foodstuff> foodstuffs = new ArrayList<>();
        foodstuffs.add(new Foodstuff("apple", 123, 1, 2, 3, 4));
        foodstuffs.add(new Foodstuff("water", 123, 1, 2, 3, 4));
        foodstuffs.add(new Foodstuff("beer", 123, 1, 2, 3, 4));

        Intent startIntent =
                BucketListActivity.createStartIntentFor(foodstuffs, InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.save_to_history_button)).perform(click());

        Intent expectedIntent =
                HistoryActivity.createStartAndAddIntent(foodstuffs, activityRule.getActivity());
        intended(allOf(
                hasAction(expectedIntent.getAction()),
                hasComponent(expectedIntent.getComponent()),
                hasExtras(hasValue(foodstuffs))));
    }
}
