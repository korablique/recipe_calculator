package korablique.recipecalculator.ui.bucketlist;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.MainThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsDbHelper;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
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
    private MainThreadExecutor mainThreadExecutor = new SyncMainThreadExecutor();
    private DatabaseWorker databaseWorker =
            new DatabaseWorker(mainThreadExecutor, new InstantDatabaseThreadExecutor());

    @Rule
    public ActivityTestRule<BucketListActivity> activityRule =
            InjectableActivityTestRule.forActivity(BucketListActivity.class)
                    .withInjector((BucketListActivity activity) -> {
                        activity.databaseWorker = databaseWorker;
                    })
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

    @Test
    public void savesDishToFoodstuffsList() {
        ArrayList<Foodstuff> ingredients = new ArrayList<>();
        ingredients.add(new Foodstuff("carrot", 310, 1.3, 0.1, 6.9, 32));
        ingredients.add(new Foodstuff("oil", 13, 0, 99.9, 0, 899));

        Intent startIntent =
                BucketListActivity.createStartIntentFor(ingredients, InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.save_as_single_foodstuff_button)).perform(click());
        String dishName = "carrot with oil";
        onView(withId(R.id.dish_name_edit_text)).perform(typeText(dishName), closeSoftKeyboard());
        onView(withId(R.id.save_button)).perform(click());

        final boolean[] saved = new boolean[1];
        databaseWorker.requestFoodstuffsLike(
                activityRule.getActivity(),
                dishName,
                DatabaseWorker.NO_LIMIT,
                new DatabaseWorker.FoodstuffsRequestCallback() {
            @Override
            public void onResult(List<Foodstuff> foodstuffs) {
                saved[0] = foodstuffs.size() == 1;
            }
        });
        Assert.assertTrue(saved[0]);
    }
}
