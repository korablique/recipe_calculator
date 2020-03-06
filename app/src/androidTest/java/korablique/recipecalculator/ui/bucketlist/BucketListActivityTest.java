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
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseBottomDialog;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.CurrentActivityProvider;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.base.prefs.PrefsCleaningHelper;
import korablique.recipecalculator.base.prefs.SharedPrefsManager;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.UserNameProvider;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController;
import korablique.recipecalculator.ui.mainactivity.history.HistoryController;
import korablique.recipecalculator.ui.mainactivity.history.HistoryFragment;
import korablique.recipecalculator.ui.mainactivity.MainActivity;
import korablique.recipecalculator.ui.mainactivity.MainActivityController;
import korablique.recipecalculator.ui.mainactivity.MainActivityFragmentsController;
import korablique.recipecalculator.ui.mainactivity.MainActivitySelectedDateStorage;
import korablique.recipecalculator.util.FloatUtils;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.util.InstantComputationsThreadsExecutor;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;
import korablique.recipecalculator.util.TestingTimeProvider;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
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
import static org.mockito.Mockito.mock;

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
    private HistoryController historyController;
    private TimeProvider timeProvider;
    private CurrentActivityProvider currentActivityProvider;
    private BucketList bucketList;
    private SharedPrefsManager prefsManager;

    @Rule
    public ActivityTestRule<BucketListActivity> activityRule =
            InjectableActivityTestRule.forActivity(BucketListActivity.class)
                    .withManualStart()
                    .withSingletones(() -> {
                        context = InstrumentationRegistry.getTargetContext();
                        PrefsCleaningHelper.INSTANCE.cleanAllPrefs(context);
                        prefsManager = new SharedPrefsManager(context);
                        mainThreadExecutor = new SyncMainThreadExecutor();
                        databaseThreadExecutor = new InstantDatabaseThreadExecutor();
                        databaseHolder = new DatabaseHolder(context, databaseThreadExecutor);
                        databaseWorker = new DatabaseWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        timeProvider = new TestingTimeProvider();
                        historyWorker = new HistoryWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor,
                                timeProvider);
                        userParametersWorker = new UserParametersWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        foodstuffsList = new FoodstuffsList(databaseWorker, mainThreadExecutor,
                                new InstantComputationsThreadsExecutor());
                        userNameProvider = new UserNameProvider(context);
                        currentActivityProvider = new CurrentActivityProvider();
                        bucketList = new BucketList(prefsManager, foodstuffsList);

                        databaseHolder.getDatabase().clearAllTables();

                        return Arrays.asList(mainThreadExecutor, databaseThreadExecutor, databaseWorker,
                                historyWorker, userParametersWorker, foodstuffsList, userNameProvider,
                                timeProvider, currentActivityProvider, bucketList,
                                new CalcKeyboardController());
                    })
                    .withActivityScoped((target) -> {
                        if (target instanceof BucketListActivity) {
                            return Collections.emptyList();
                        }
                        MainActivity activity = (MainActivity) target;
                        ActivityCallbacks activityCallbacks = new ActivityCallbacks();
                        MainActivityController controller = new MainActivityController(activity,
                                activityCallbacks,
                                mock(MainActivityFragmentsController.class));
                        return Arrays.asList(new RxActivitySubscriptions(activity.getActivityCallbacks()),
                                controller);
                    })
                    .withFragmentScoped(target -> {
                        if (target instanceof BaseBottomDialog) {
                            return Collections.emptyList();
                        }
                        BaseFragment fragment = (BaseFragment) target;
                        FragmentCallbacks fragmentCallbacks = new FragmentCallbacks();
                        RxFragmentSubscriptions subscriptions = new RxFragmentSubscriptions(fragmentCallbacks);
                        if (fragment instanceof HistoryFragment) {
                            historyController = new HistoryController((BaseActivity) fragment.getActivity(),
                                    fragment, fragmentCallbacks, historyWorker, timeProvider,
                                    mock(MainActivityFragmentsController.class),
                                    mock(MainActivitySelectedDateStorage.class));
                            return Arrays.asList(subscriptions, historyController);
                        }
                        return Collections.emptyList();
                    })
                    .build();

    @Test
    public void containsFoodstuffsFromBucketList() {
        ArrayList<WeightedFoodstuff> foodstuffs = new ArrayList<>();
        foodstuffs.add(Foodstuff.withId(1).withName("apple").withNutrition(1, 2, 3, 4).withWeight(123));
        foodstuffs.add(Foodstuff.withId(2).withName("water").withNutrition(1, 2, 3, 4).withWeight(123));
        foodstuffs.add(Foodstuff.withId(3).withName("beer").withNutrition(1, 2, 3, 4).withWeight(123));
        mainThreadExecutor.execute(() -> {
            bucketList.add(foodstuffs);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withText("apple")).check(matches(isDisplayed()));
        onView(withText("water")).check(matches(isDisplayed()));
        onView(withText("beer")).check(matches(isDisplayed()));
    }

    @Test
    public void savesDishToFoodstuffsList() {
        ArrayList<WeightedFoodstuff> ingredients = new ArrayList<>();
        ingredients.add(Foodstuff.withId(1).withName("carrot").withNutrition(1.3, 0.1, 6.9, 32).withWeight(310));
        ingredients.add(Foodstuff.withId(2).withName("oil").withNutrition(0, 99.9, 0, 899).withWeight(13));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.save_as_single_foodstuff_button)).perform(click());
        String dishName = "carrot with oil";
        onView(withId(R.id.dish_name_edit_text)).perform(typeText(dishName), closeSoftKeyboard());
        onView(withId(R.id.save_button)).perform(click());

        List<Foodstuff> foodstuffs = foodstuffsList
                .getAllFoodstuffs()
                .filter(foodstuff -> foodstuff.getName().equals(dishName))
                .toList()
                .blockingGet();

        Assert.assertEquals(1, foodstuffs.size());
    }

    @Test
    public void setsActivityResultWhenSavesDish() {
        ArrayList<WeightedFoodstuff> ingredients = new ArrayList<>();
        ingredients.add(Foodstuff.withId(1).withName("carrot").withNutrition(1, 2, 3, 4).withWeight(310));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withId(R.id.save_as_single_foodstuff_button)).perform(click());
        onView(withId(R.id.dish_name_edit_text)).perform(typeText("new super carrot"), closeSoftKeyboard());
        onView(withId(R.id.save_button)).perform(click());

        Intent resultIntent = activityRule.getActivityResult().getResultData();
        Foodstuff resultDish = resultIntent.getParcelableExtra(BucketListActivity.EXTRA_CREATED_FOODSTUFF);
        Assert.assertEquals("new super carrot", resultDish.getName());
    }

    @Test
    public void changingFoodstuffWeightChangesBucketList() {
        ArrayList<WeightedFoodstuff> ingredients = new ArrayList<>();
        ingredients.add(Foodstuff.withId(1).withName("carrot").withNutrition(1, 2, 3, 4).withWeight(310));
        mainThreadExecutor.execute(() -> {
            bucketList.add(ingredients);
        });

        Intent startIntent =
                BucketListActivity.createIntent(
                        InstrumentationRegistry.getTargetContext());
        activityRule.launchActivity(startIntent);

        onView(withText("carrot")).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("10"));
        onView(withId(R.id.button1)).perform(click());

        mainThreadExecutor.execute(() -> {
            Assert.assertEquals(1, bucketList.getList().size());
            Assert.assertTrue(FloatUtils.areFloatsEquals(10, bucketList.getList().get(0).getWeight()));
        });
    }
}
