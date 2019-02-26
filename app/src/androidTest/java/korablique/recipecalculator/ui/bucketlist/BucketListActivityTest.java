package korablique.recipecalculator.ui.bucketlist;

import android.content.Context;
import android.content.Intent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.UserNameProvider;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantComputationsThreadsExecutor;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.BundleMatchers.hasValue;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtras;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BucketListActivityTest {
    private Context context = InstrumentationRegistry.getTargetContext();
    private DatabaseHolder databaseHolder;
    private MainThreadExecutor mainThreadExecutor;
    private DatabaseThreadExecutor databaseThreadExecutor;
    private DatabaseWorker databaseWorker;
    private HistoryWorker historyWorker;
    private UserParametersWorker userParametersWorker;
    private FoodstuffsList foodstuffsList;
    private UserNameProvider userNameProvider;

    @Rule
    public ActivityTestRule<BucketListActivity> activityRule =
            InjectableActivityTestRule.forActivity(BucketListActivity.class)
                    .withManualStart()
                    .withSingletones(() -> {
                        context = InstrumentationRegistry.getTargetContext();
                        mainThreadExecutor = new SyncMainThreadExecutor();
                        databaseThreadExecutor = new InstantDatabaseThreadExecutor();
                        databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);
                        databaseWorker = new DatabaseWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        historyWorker = new HistoryWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        userParametersWorker = new UserParametersWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        foodstuffsList = new FoodstuffsList(
                                databaseWorker, mainThreadExecutor, new InstantComputationsThreadsExecutor());
                        userNameProvider = new UserNameProvider(context);
                        return Arrays.asList(mainThreadExecutor, databaseThreadExecutor, databaseWorker,
                                historyWorker, userParametersWorker, foodstuffsList, userNameProvider);
                    })
                    .withActivityScoped((target) -> {
                        BaseActivity activity = (BaseActivity) target;
                        return Collections.singletonList(
                                new RxActivitySubscriptions(activity.getActivityCallbacks()));
                    })
                    .build();

    @Before
    public void setUp() {
        databaseHolder.getDatabase().clearAllTables();
    }

    @Test
    public void containsGivenFoodstuffs() {
        ArrayList<WeightedFoodstuff> foodstuffs = new ArrayList<>();
        foodstuffs.add(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4).withWeight(123));
        foodstuffs.add(Foodstuff.withName("water").withNutrition(1, 2, 3, 4).withWeight(123));
        foodstuffs.add(Foodstuff.withName("beer").withNutrition(1, 2, 3, 4).withWeight(123));

        Intent startIntent =
                BucketListActivity.createStartIntentFor(foodstuffs, InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withText("apple")).check(matches(isDisplayed()));
        onView(withText("water")).check(matches(isDisplayed()));
        onView(withText("beer")).check(matches(isDisplayed()));
    }

    @Test
    public void addsFoodstuffsToHistory() {
        ArrayList<WeightedFoodstuff> foodstuffs = new ArrayList<>();
        foodstuffs.add(Foodstuff.withName("apple").withNutrition(1, 2, 3, 4).withWeight(123));
        foodstuffs.add(Foodstuff.withName("water").withNutrition(1, 2, 3, 4).withWeight(123));
        foodstuffs.add(Foodstuff.withName("beer").withNutrition(1, 2, 3, 4).withWeight(123));

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
        ArrayList<WeightedFoodstuff> ingredients = new ArrayList<>();
        ingredients.add(Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32).withWeight(310));
        ingredients.add(Foodstuff.withName("oil").withNutrition(0, 99.9, 0, 899).withWeight(13));

        Intent startIntent =
                BucketListActivity.createStartIntentFor(ingredients, InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.save_as_single_foodstuff_button)).perform(click());
        String dishName = "carrot with oil";
        onView(withId(R.id.dish_name_edit_text)).perform(typeText(dishName), closeSoftKeyboard());
        onView(withId(R.id.save_button)).perform(click());

        Single<List<Foodstuff>> foodstuffs = foodstuffsList.requestFoodstuffsLike(
                dishName,
                DatabaseWorker.NO_LIMIT);

        Assert.assertEquals(1, foodstuffs.blockingGet().size());
    }
}
